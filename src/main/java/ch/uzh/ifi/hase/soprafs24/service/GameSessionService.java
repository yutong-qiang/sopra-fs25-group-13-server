package ch.uzh.ifi.hase.soprafs24.service;

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

        if (playerRepository.findByGameSession(gameSession).size() < 4) {
            throw new IllegalStateException("Not enough players to start the game");
        }

        gameSession.setCurrentState(GameState.STARTED);
        gameSessionRepository.save(gameSession);
        PlayerActionResult result = new PlayerActionResult();
        result.setActionType(action.getActionType());
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
            default -> {
                throw new IllegalArgumentException("Invalid action type: " + action.getActionType());
            }
        }
    }
}
