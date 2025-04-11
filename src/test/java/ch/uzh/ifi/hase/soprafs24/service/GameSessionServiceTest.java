package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameSessionRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerAction;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerActionResult;

public class GameSessionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private PlayerRepository playerRepository;

    private GameSessionService gameSessionService;

    private User testUser;
    private GameSession testGameSession;
    private Player testPlayer;
    private PlayerAction testPlayerAction;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        gameSessionService = new GameSessionService(userRepository, gameSessionRepository, playerRepository);
        // given
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUsername");

        testGameSession = new GameSession();
        testGameSession.setCreator(testUser);
        testGameSession.setGameToken("testToken");
        testGameSession.setTwilioRoomSid("RM123456789");
        Mockito.when(gameSessionRepository.save(Mockito.any())).thenReturn(testGameSession);

        testPlayer = new Player();
        testPlayer.setGameSession(testGameSession);
        testPlayer.setUser(testUser);
        Mockito.when(playerRepository.save(Mockito.any())).thenReturn(testPlayer);

        testPlayerAction = new PlayerAction();
    }

    @Test
    public void handlePlayerAction_success() throws Exception {
        // given
        testPlayerAction.setActionType("TEST_ACTION");
        PlayerActionResult playerActionResult = new PlayerActionResult();
        playerActionResult.setActionType("TEST_ACTION");
        Mockito.when(playerRepository.findByUserAndGameSession(testUser, testGameSession)).thenReturn(Optional.of(testPlayer));
        // when
        gameSessionService.handlePlayerAction(testUser, testPlayerAction, testGameSession);
        // then
        assertEquals(testPlayerAction.getActionType(), playerActionResult.getActionType());
    }

    @Test
    public void handlePlayerAction_fail() throws Exception {
        // given
        testPlayerAction.setActionType("TEST_ACTION");
        Mockito.when(playerRepository.findByUserAndGameSession(testUser, testGameSession)).thenReturn(Optional.empty());
        Exception exception = assertThrows(Exception.class, () -> {
            gameSessionService.handlePlayerAction(testUser, testPlayerAction, testGameSession);
        });
        assertEquals("User not part of the game session", exception.getMessage());
    }

    @Test
    public void handlePlayerAction_admin_success() throws Exception {
        // given
        testPlayerAction.setActionType("TEST_ADMIN_ACTION");
        PlayerActionResult playerActionResult = new PlayerActionResult();
        playerActionResult.setActionType("TEST_ADMIN_ACTION");
        Mockito.when(playerRepository.findByUserAndGameSession(testUser, testGameSession)).thenReturn(Optional.of(testPlayer));
        // when
        gameSessionService.handlePlayerAction(testUser, testPlayerAction, testGameSession);
        // then
        assertEquals(testPlayerAction.getActionType(), playerActionResult.getActionType());
    }

    @Test
    public void handlePlayerAction_admin_fail() throws Exception {
        // given
        testPlayerAction.setActionType("TEST_ADMIN_ACTION");
        User otherUser = new User();
        testGameSession.setCreator(otherUser);
        Mockito.when(playerRepository.findByUserAndGameSession(testUser, testGameSession)).thenReturn(Optional.of(testPlayer));

        Exception exception = assertThrows(Exception.class, () -> {
            // when
            gameSessionService.handlePlayerAction(testUser, testPlayerAction, testGameSession);
        });
        assertEquals("Only the game session creator can perform this action", exception.getMessage());
    }

    @Test
    public void startGame_success() throws Exception {
        // given
        testPlayerAction.setActionType("START_GAME");
        testGameSession.setCurrentState(GameState.WAITING_FOR_PLAYERS);
        List<Player> dummyPlayers = List.of(
                new Player(),
                new Player(),
                new Player(),
                new Player()
        );
        Mockito.when(playerRepository.findByGameSession(testGameSession)).thenReturn(dummyPlayers);
        // when
        PlayerActionResult result = gameSessionService.startGame(testPlayerAction, testGameSession);
        // then
        Mockito.verify(gameSessionRepository, Mockito.times(1)).save(testGameSession);
        assertEquals(testGameSession.getCurrentState(), GameState.STARTED);
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
    }
}
