package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameSessionRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerAction;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerActionResult;

/**
 * GameSessionService is responsible for handling players actions and producing
 * the corresponding PlayerActionResult objects inside of a game session.
 */
@Service
@Transactional
public class GameSessionService {

    private final Logger log = LoggerFactory.getLogger(GameSessionService.class);

    private final GameSessionRepository gameSessionRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public GameSessionService(UserRepository userRepository,
            GameSessionRepository gameSessionRepository,
            PlayerRepository playerRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.playerRepository = playerRepository;
    }

    public boolean isAdminAction(PlayerAction action) {
        switch (action.getActionType()) {
            case "TEST_ADMIN_ACTION" -> {
                return true;
            }
            case "START_GAME" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public PlayerActionResult startGame(PlayerAction action, GameSession gameSession) {
        if (gameSession.getCurrentState() != GameState.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Game session is not in a valid state to start");
        }
        List<Player> players = playerRepository.findByGameSession(gameSession);
        if (players.size() < 4) {
            throw new IllegalStateException("Not enough players to start the game");
        }
        gameSession.setCurrentState(GameState.STARTED);
        // generate random player order list
        List<Integer> playerOrder = IntStream.range(0, players.size())
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(playerOrder);
        // generate random player roles
        List<Boolean> playerRoles = new ArrayList<>(Collections.nCopies(players.size(), false));
        playerRoles.set(0, true);
        Collections.shuffle(playerRoles);
        // for each player set the next player and the isChameleon
        for (int i = 0; i < players.size(); i++) {
            Player currentPlayer = players.get(playerOrder.get(i));
            // last player has no next player
            if (i < playerRoles.size() - 1) {
                Player nextPlayer = players.get(playerOrder.get(i + 1));
                currentPlayer.setNextPlayer(nextPlayer);
            }
            currentPlayer.setIsChameleon(playerRoles.get(i));
            playerRepository.save(currentPlayer);
        }
        // set the first player as the current player turn
        gameSession.setCurrentPlayerTurn(players.get(0));
        // save and return
        gameSessionRepository.save(gameSession);
        PlayerActionResult result = new PlayerActionResult();
        result.setActionType(action.getActionType());
        return result;
    }

    public PlayerActionResult giveHint(Player player, PlayerAction action, GameSession gameSession) {
        if (gameSession.getCurrentState() != GameState.STARTED) {
            throw new IllegalStateException("Game session is not in a valid state to give a hint");
        }
        // check if this is the player turn
        if (gameSession.getCurrentPlayerTurn() != player) {
            throw new IllegalStateException("Wrong player turn");
        }
        player.setGivenHint(action.getActionContent());
        playerRepository.save(player);
        // set the next player turn
        Player nextPlayer = player.getNextPlayer();
        gameSession.setCurrentPlayerTurn(nextPlayer);
        // nextPlayer is null -> all players have given their hint, time to vote!
        if (nextPlayer == null) {
            gameSession.setCurrentState(GameState.READY_FOR_VOTING);
        }
        gameSessionRepository.save(gameSession);
        // return the result
        PlayerActionResult result = new PlayerActionResult();
        result.setActionType(action.getActionType());
        result.setActionContent(action.getActionContent());
        return result;
    }

    public PlayerActionResult handlePlayerAction(User user, PlayerAction action, GameSession gameSession) throws Exception {
        // get the player performing the action
        Player player = playerRepository.findByUserAndGameSession(user, gameSession).orElseThrow(
                () -> new Exception("User not part of the game session")
        );

        // in case of an admin action, check if the user is the creator of the game session
        if (isAdminAction(action) && gameSession.getCreator() != user) {
            throw new Exception("Only the game session creator can perform this action");
        }

        switch (action.getActionType()) {
            case "TEST_ACTION" -> {
                PlayerActionResult result = new PlayerActionResult();
                result.setActionType(action.getActionType());
                return result;
            }
            case "TEST_ADMIN_ACTION" -> {
                PlayerActionResult result = new PlayerActionResult();
                result.setActionType(action.getActionType());
                return result;
            }
            case "START_GAME" -> {
                return startGame(action, gameSession);
            }
            case "GIVE_HINT" -> {
                return giveHint(player, action, gameSession);
            }
            default -> {
                throw new IllegalArgumentException("Invalid action type: " + action.getActionType());
            }
        }
    }
}
