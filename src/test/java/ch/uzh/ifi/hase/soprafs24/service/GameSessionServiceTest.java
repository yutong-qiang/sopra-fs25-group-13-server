package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerAction;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerActionResult;

public class GameSessionServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private WordService wordService;

    private GameSessionService gameSessionService;

    private User testUser;
    private GameSession testGameSession;
    private Player testPlayer;
    private PlayerAction testPlayerAction;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        gameSessionService = new GameSessionService(gameSessionRepository, playerRepository, wordService);
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
        assertNotNull(testGameSession.getCurrentPlayerTurn());
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
        // assert that there is exactly one chameleon set among the players
        List<Boolean> chameleonFlags = dummyPlayers.stream()
                .map(Player::getIsChameleon)
                .collect(Collectors.toList());
        assertEquals(chameleonFlags.stream()
                .filter(Boolean::booleanValue)
                .count(), 1);

    }

    @Test
    public void startVoting_success() throws Exception {
        // given
        testPlayerAction.setActionType("START_VOTING");
        testGameSession.setCurrentState(GameState.READY_FOR_VOTING);
        // when
        PlayerActionResult result = gameSessionService.startVoting(testPlayerAction, testGameSession);
        // then
        Mockito.verify(gameSessionRepository, Mockito.times(1)).save(testGameSession);
        assertEquals(testGameSession.getCurrentState(), GameState.VOTING);
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
    }

    @Test
    public void startVoting_fail() throws Exception {
        // given
        testPlayerAction.setActionType("START_VOTING");
        testGameSession.setCurrentState(GameState.STARTED);
        // assert that startVoting throws an exception
        Exception exception = assertThrows(Exception.class, () -> {
            // when
            gameSessionService.startVoting(testPlayerAction, testGameSession);
        });
        assertEquals("Game session is not in a valid state to start voting", exception.getMessage());
    }

    @Test
    public void doVote_wrong_state_error() throws Exception {
        // given
        testPlayerAction.setActionType("VOTE");
        testPlayerAction.setActionContent("user1");
        testGameSession.setCurrentState(GameState.STARTED);
        // then
        Exception exception = assertThrows(Exception.class, () -> {
            // when
            gameSessionService.doVote(testPlayer, testPlayerAction, testGameSession);
        });
        assertEquals("Game session is not in a valid state to vote", exception.getMessage());
    }

    /**
     * Test that player cannot vote themselves.
     */
    @Test
    public void doVote_self_voting_error() throws Exception {
        // given
        Player player1 = new Player();
        player1.setUser(new User());
        player1.getUser().setUsername("user1");

        List<Player> dummyPlayers = List.of(player1);
        // we simulate a vote from player1 to player1
        testPlayerAction.setActionType("VOTE");
        testPlayerAction.setActionContent("user1");
        testGameSession.setCurrentState(GameState.VOTING);
        Mockito.when(playerRepository.findByGameSession(testGameSession)).thenReturn(dummyPlayers);
        // then
        Exception exception = assertThrows(Exception.class, () -> {
            // when
            gameSessionService.doVote(player1, testPlayerAction, testGameSession);
        });
        assertEquals("Player cannot vote for themselves", exception.getMessage());
    }

    /**
     * Test vote from player. Not all players have voted, so an empty action
     * result is returned.
     */
    @Test
    public void doVote_success() throws Exception {
        // given
        Player player1 = new Player();
        player1.setUser(new User());
        player1.getUser().setUsername("user1");

        Player player2 = new Player();
        player2.setUser(new User());
        player2.getUser().setUsername("user2");

        Player player3 = new Player();
        player3.setUser(new User());
        player3.getUser().setUsername("user3");

        player1.setCurrentAccusedPlayer(player2);
        // player2.setCurrentAccusedPlayer(player1);

        List<Player> dummyPlayers = List.of(
                player1,
                player2,
                player3
        );

        // we simulate a vote from player3 to player1
        testPlayerAction.setActionType("VOTE");
        testPlayerAction.setActionContent("user1");
        testGameSession.setCurrentState(GameState.VOTING);

        Mockito.when(playerRepository.findByGameSession(testGameSession)).thenReturn(dummyPlayers);

        // then
        PlayerActionResult result = gameSessionService.doVote(player3, testPlayerAction, testGameSession);

        Mockito.verify(playerRepository, Mockito.times(1)).save(player3);
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
        assertTrue(result.getActionResult() == null);
    }

    /**
     * Test vote from player when all players have voted and the most voted
     * player is not the chameleon.
     */
    @Test
    public void doVote_chameleon_not_found() throws Exception {
        // given
        Player player1 = new Player();
        player1.setUser(new User());
        player1.getUser().setUsername("user1");
        player1.setIsChameleon(false);

        Player player2 = new Player();
        player2.setUser(new User());
        player2.getUser().setUsername("user2");
        player2.setIsChameleon(true);

        Player player3 = new Player();
        player3.setUser(new User());
        player3.getUser().setUsername("user3");
        player3.setIsChameleon(false);

        player1.setCurrentAccusedPlayer(player2);
        player2.setCurrentAccusedPlayer(player1);

        List<Player> dummyPlayers = List.of(
                player1,
                player2,
                player3
        );

        // we simulate a vote from player3 to player1
        testPlayerAction.setActionType("VOTE");
        testPlayerAction.setActionContent("user1");
        testGameSession.setCurrentState(GameState.VOTING);

        Mockito.when(playerRepository.findByGameSession(testGameSession)).thenReturn(dummyPlayers);

        // then
        PlayerActionResult result = gameSessionService.doVote(player3, testPlayerAction, testGameSession);

        Mockito.verify(playerRepository, Mockito.times(1)).save(player3);
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
        assertEquals(result.getActionResult(), "CHAMELEON_WON");
    }

    /**
     * Test vote from player when all players have voted and the most voted
     * player is the chameleon.
     */
    @Test
    public void doVote_chameleon_found() throws Exception {
        // given
        Player player1 = new Player();
        player1.setUser(new User());
        player1.getUser().setUsername("user1");
        player1.setIsChameleon(true);

        Player player2 = new Player();
        player2.setUser(new User());
        player2.getUser().setUsername("user2");
        player2.setIsChameleon(false);

        Player player3 = new Player();
        player3.setUser(new User());
        player3.getUser().setUsername("user3");
        player3.setIsChameleon(false);

        player1.setCurrentAccusedPlayer(player2);
        player2.setCurrentAccusedPlayer(player1);

        List<Player> dummyPlayers = List.of(
                player1,
                player2,
                player3
        );

        // we simulate a vote from player3 to player1
        testPlayerAction.setActionType("VOTE");
        testPlayerAction.setActionContent("user1");
        testGameSession.setCurrentState(GameState.VOTING);

        Mockito.when(playerRepository.findByGameSession(testGameSession)).thenReturn(dummyPlayers);

        // then
        PlayerActionResult result = gameSessionService.doVote(player3, testPlayerAction, testGameSession);

        Mockito.verify(playerRepository, Mockito.times(1)).save(player3);
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
        assertEquals(result.getActionResult(), "CHAMELEON_FOUND");
    }

    public void giveHint_success() throws Exception {
        // given
        testPlayerAction.setActionType("GIVE_HINT");
        testGameSession.setCurrentState(GameState.STARTED);
        testGameSession.setCurrentPlayerTurn(testPlayer);

        // when
        PlayerActionResult result = gameSessionService.giveHint(testPlayer, testPlayerAction, testGameSession);
        // then
        Mockito.verify(gameSessionRepository, Mockito.times(1)).save(testGameSession);
        Mockito.verify(playerRepository, Mockito.times(1)).save(testPlayer);
        // test player has no nextPlayer set, so it will be considered as the last player
        // therefore the game will transition to the state READY_FOR_VOTING
        assertEquals(testGameSession.getCurrentState(), GameState.READY_FOR_VOTING);
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
    }

    @Test
    public void handleChameleonGuess_success_wins() throws Exception {
        // given
        testPlayerAction.setActionType("CHAMELEON_GUESS");
        testPlayerAction.setActionContent("secret_word");
        testGameSession.setCurrentState(GameState.CHAMELEON_TURN);
        testGameSession.setSecretWord("secret_word");

        // when
        PlayerActionResult result = gameSessionService.handleChameleonGuess(testPlayer, testPlayerAction, testGameSession);
        // then
        Mockito.verify(gameSessionRepository, Mockito.times(1)).save(testGameSession);
        assertEquals(testGameSession.getCurrentState(), GameState.CHAMELEON_WIN);
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
        assertEquals(result.getActionContent(), testPlayerAction.getActionContent());
        assertEquals(result.getActionResult(), "CHAMELEON_WIN");
    }

    @Test
    public void handleChameleonGuess_success_looses() throws Exception {
        // given
        testPlayerAction.setActionType("CHAMELEON_GUESS");
        testPlayerAction.setActionContent("wrong_word");
        testGameSession.setCurrentState(GameState.CHAMELEON_TURN);
        testGameSession.setSecretWord("secret_word");

        // when
        PlayerActionResult result = gameSessionService.handleChameleonGuess(testPlayer, testPlayerAction, testGameSession);
        // then
        Mockito.verify(gameSessionRepository, Mockito.times(1)).save(testGameSession);
        assertEquals(testGameSession.getCurrentState(), GameState.PLAYERS_WIN);
        assertEquals(result.getActionType(), testPlayerAction.getActionType());
        assertEquals(result.getActionContent(), testPlayerAction.getActionContent());
        assertEquals(result.getActionResult(), "PLAYERS_WIN");
    }

    @Test
    public void handleChameleonGuess_fail() throws Exception {
        // given
        testPlayerAction.setActionType("CHAMELEON_GUESS");
        testPlayerAction.setActionContent("secret_word");
        testGameSession.setCurrentState(GameState.VOTING);
        testGameSession.setSecretWord("secret_word");

        // when
        Exception exception = assertThrows(Exception.class, () -> {
            gameSessionService.handleChameleonGuess(testPlayer, testPlayerAction, testGameSession);
        });
        // then
        assertEquals("Game session is not in a valid state for a chameleon guess", exception.getMessage());
    }
}
