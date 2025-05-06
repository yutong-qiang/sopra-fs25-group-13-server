package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.AppService;
import ch.uzh.ifi.hase.soprafs24.service.GameSessionService;
import ch.uzh.ifi.hase.soprafs24.websocket.GameSessionErrorMessage;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerAction;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerActionResult;

/**
 * Game Session Controller This class is responsible for handling websocket
 * messaging for game sessions. The controller will receive the messages and
 * delegate the execution to the appropriate service methods.
 */
@Controller
public class GameSessionController {

    // private final Logger log = LoggerFactory.getLogger(GameSessionController.class);
    private final AppService appService;
    private final GameSessionService gameSessionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public GameSessionController(AppService appService, GameSessionService gameSessionService) {
        this.appService = appService;
        this.gameSessionService = gameSessionService;
    }

    @MessageMapping("/game/player-action")
    public void handlePlayerAction(SimpMessageHeaderAccessor headerAccessor, PlayerAction playerAction) throws Exception {
        // log.info("Received WebSocket message - Action Type: {}", playerAction.getActionType());
        // log.info("Game Token: {}", playerAction.getGameSessionToken());

        // Check if the user is authenticated
        String authToken = headerAccessor.getFirstNativeHeader("auth-token");
        // log.info("Auth Token: {}", authToken);

        if (!appService.isUserTokenValid(authToken)) {
            // log.error("Invalid auth token");
            GameSessionErrorMessage errorMessage = new GameSessionErrorMessage();
            errorMessage.setErrorMessage("Invalid auth token");
            messagingTemplate.convertAndSend("/game/topic/user/" + authToken, errorMessage);
            return;
        }
        // Get the user from the token
        User user = appService.getUserByToken(authToken);
        // log.info("User found: {}", user.getUsername());

        // Check if the game session token is valid
        String gsToken = playerAction.getGameSessionToken();
        if (!appService.isGameTokenValid(gsToken)) {
            GameSessionErrorMessage errorMessage = new GameSessionErrorMessage();
            errorMessage.setErrorMessage("Invalid game session token");
            messagingTemplate.convertAndSend("/game/topic/user/" + authToken, errorMessage);
            return;
        }
        // Get game session from the token
        GameSession gameSession = appService.getGameSessionByGameToken(gsToken);

        // Handle the player action
        try {
            PlayerActionResult result = gameSessionService.handlePlayerAction(user, playerAction, gameSession);
            // log.info("Action processed successfully");
            if (result != null) {
                messagingTemplate.convertAndSend("/game/topic/" + gsToken, result);
            }
        } catch (Exception e) {
            // log.error("Error processing action: {}", e.getMessage());
            GameSessionErrorMessage errorMessage = new GameSessionErrorMessage();
            errorMessage.setErrorMessage("An error occurred while processing the action: " + e.getMessage());
            messagingTemplate.convertAndSend("/game/topic/user/" + authToken, errorMessage);
        }

    }

}
