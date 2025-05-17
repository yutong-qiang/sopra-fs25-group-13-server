package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameSessionRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

public class AppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TwilioService twilioService;

    private AppService appService;
    private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        appService = new AppService(userRepository, gameSessionRepository, playerRepository, twilioService);

        // given
        testUser = new User();
        testUser.setId(1L);
        // testUser.setName("testName");
        testUser.setUsername("testUsername");
        testUser.setRoundsPlayed(5);
        testUser.setWins(2);

        // when -> any object is being save in the userRepository -> return the dummy
        // testUser
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
    }

    @Test
    public void createUser_validInputs_success() {
        // when -> any object is being save in the userRepository -> return the dummy
        // testUser
        User createdUser = appService.createUser(testUser);

        // then
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

        assertEquals(testUser.getId(), createdUser.getId());
        // assertEquals(testUser.getName(), createdUser.getName());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertNotNull(createdUser.getToken());
    }

    @Test
    public void createUser_duplicateUsername_throwsException() {
        // given -> a first user has already been created
        appService.createUser(testUser);

        // when -> setup additional mocks for UserRepository
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

        // then -> attempt to create second user with same username -> check that an error is thrown
        assertThrows(ResponseStatusException.class, () -> appService.createUser(testUser));
    }

    @Test
    public void createGameSession_withTwilio_success() {
        // given
        User creator = new User();
        creator.setId(1L);
        creator.setUsername("testUser");

        String mockTwilioToken = "mock_twilio_token_123";

        // Mock TwilioService
        when(twilioService.createVideoRoom(anyString())).thenReturn(new TwilioService.TwilioRoomInfo("RM123", mockTwilioToken));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(twilioService.generateToken(anyString(), anyString())).thenReturn(mockTwilioToken);

        // when
        GameSession createdSession = appService.createGameSession(creator);

        // then
        assertNotNull(createdSession);
        assertEquals(creator, createdSession.getCreator());
        assertEquals("RM123", createdSession.getTwilioRoomSid());
        assertEquals(GameState.WAITING_FOR_PLAYERS, createdSession.getCurrentState());
        verify(twilioService).createVideoRoom(createdSession.getGameToken());
        verify(gameSessionRepository).save(any(GameSession.class));
        verify(gameSessionRepository).flush();
        verify(twilioService).generateToken(creator.getUsername(), "RM123");
        verify(playerRepository).save(any(Player.class));
        verify(playerRepository).flush();
    }

    @Test
    public void createGameSession_withTwilio_fails() {
        // given
        User creator = new User();
        creator.setId(1L);
        creator.setUsername("testUser");
        // Mock TwilioService
        when(twilioService.createVideoRoom(anyString())).thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to create video room: " + "Twilio error"));

        // when
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            appService.createGameSession(creator);
        });
        // then
        assertEquals("Failed to create video room: Twilio error", exception.getReason());
    }

    @Test
    public void endGameSession_withTwilio_success() {
        // given
        User creator = new User();
        creator.setId(1L);
        creator.setUsername("testUser");
        creator.setRoundsPlayed(2);
        creator.setWins(1);


        GameSession gameSession = new GameSession();
        gameSession.setCreator(creator);
        gameSession.setGameToken("testToken");
        gameSession.setTwilioRoomSid("RM123456789");

        Player player = new Player();
        player.setUser(creator);


        when(gameSessionRepository.findByGameToken("testToken"))
            .thenReturn(java.util.Optional.of(gameSession));
        when(playerRepository.findByGameSession(gameSession))
            .thenReturn(List.of(player));
        when(userRepository.save(creator)).thenReturn(creator);


        // when
        appService.endGameSession("testToken", creator);

        // then
        verify(twilioService).closeVideoRoom("RM123456789");
        verify(gameSessionRepository).delete(gameSession);
        verify(gameSessionRepository).flush();
    }

    @Test
    public void storeAvatar_validInputs_success() {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        byte[] avatar = new byte[]{1, 2, 3};

        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        appService.storeAvatar(user, avatar);

        // then
        verify(userRepository).save(user);
        assertEquals(avatar, user.getAvatar());
    }

    @Test
    public void incrementRoundsPlayed_incrementsSuccessfully() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        appService.incrementRoundsPlayed(testUser);

        assertEquals(6, testUser.getRoundsPlayed());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    public void incrementWins_incrementsSuccessfully() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        appService.incrementWins(testUser);

        assertEquals(3, testUser.getWins());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    public void isUserTokenValid_validToken_returnsTrue() {
        when(userRepository.existsByToken("validToken")).thenReturn(true);

        boolean result = appService.isUserTokenValid("validToken");

        assertTrue(result);
    }

    @Test
    public void isUserTokenValid_invalidToken_returnsFalse() {
        when(userRepository.existsByToken("invalidToken")).thenReturn(false);

        boolean result = appService.isUserTokenValid("invalidToken");

        assertFalse(result);
    }

    @Test
    public void isGameTokenValid_validGameToken_returnsTrue() {
        when(gameSessionRepository.existsByGameToken("game123")).thenReturn(true);

        boolean result = appService.isGameTokenValid("game123");

        assertTrue(result);
    }

    @Test
    public void isGameTokenValid_invalidGameToken_returnsFalse() {
        when(gameSessionRepository.existsByGameToken("invalidToken")).thenReturn(false);

        boolean result = appService.isGameTokenValid("invalidToken");

        assertFalse(result);
    }

    @Test
    public void getUserByToken_successful() {
        when(userRepository.findByToken("abc")).thenReturn(Optional.of(testUser));

        User result = appService.getUserByToken("abc");

        assertEquals(testUser, result);
    }

    @Test
    public void getUserByUsername_successful() {
        when(userRepository.findByUsername("testUser")).thenReturn(testUser);

        User result = appService.getUserByUsername("testUser");

        assertEquals(testUser, result);
    }


}
