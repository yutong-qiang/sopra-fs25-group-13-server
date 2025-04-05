package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.repository.GameSessionRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import java.util.ArrayList;
import ch.uzh.ifi.hase.soprafs24.constant.GameState;


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

    // Mock TwilioService
    when(twilioService.createVideoRoom(anyString())).thenReturn("RM123456789");
    when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    GameSession createdSession = appService.createGameSession(creator);

    // then
    assertNotNull(createdSession);
    assertEquals(creator, createdSession.getCreator());
    assertEquals("RM123456789", createdSession.getTwilioRoomSid());
    assertEquals(GameState.WAITING_FOR_PLAYERS, createdSession.getCurrentState());
    verify(twilioService).createVideoRoom(createdSession.getGameToken());
    verify(gameSessionRepository).save(any(GameSession.class));
    verify(gameSessionRepository).flush();
  }

  @Test
  public void endGameSession_withTwilio_success() {
    // given
    User creator = new User();
    creator.setId(1L);
    creator.setUsername("testUser");

    GameSession gameSession = new GameSession();
    gameSession.setCreator(creator);
    gameSession.setGameToken("testToken");
    gameSession.setTwilioRoomSid("RM123456789");

    when(gameSessionRepository.findByGameToken("testToken"))
        .thenReturn(java.util.Optional.of(gameSession));
    when(playerRepository.findByGameSession(any(GameSession.class)))
        .thenReturn(new ArrayList<>());

    // when
    appService.endGameSession("testToken", creator);

    // then
    verify(twilioService).closeVideoRoom("RM123456789");
    verify(gameSessionRepository).delete(gameSession);
    verify(gameSessionRepository).flush();
  }

}
