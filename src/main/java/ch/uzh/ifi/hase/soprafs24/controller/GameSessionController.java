package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs24.service.AppService;
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

    private final AppService appService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public GameSessionController(AppService appService) {
        this.appService = appService;
    }

    @MessageMapping("/game/player-action")
    public void handlePlayerAction(SimpMessageHeaderAccessor headerAccessor, PlayerAction playerAction) throws Exception {
        System.out.println("\n\n\n\n\n\n\n\nHEREEE");

        // Check if the user is authenticated
        String authToken = headerAccessor.getFirstNativeHeader("auth-token");
        if (!appService.isUserTokenValid(authToken)) {
            GameSessionErrorMessage errorMessage = new GameSessionErrorMessage();
            errorMessage.setErrorMessage("Invalid auth token");
            messagingTemplate.convertAndSend("/game/topic/user/" + authToken, errorMessage);
            return;
        }

        // Check if the game session token is valid
        String gsToken = playerAction.getGameSessionToken();
        if (!appService.isGameTokenValid(gsToken)) {
            GameSessionErrorMessage errorMessage = new GameSessionErrorMessage();
            errorMessage.setErrorMessage("Invalid game session token");
            messagingTemplate.convertAndSend("/game/topic/user/" + authToken, errorMessage);
            return;
        }

        PlayerActionResult result = new PlayerActionResult();
        System.out.println("HEREEE sending result");
        messagingTemplate.convertAndSend("/game/topic/" + gsToken, result);
        // Handle the player action
        // PlayerActionResult result = gameService.handlePlayerAction(playerAction);
        // if (result.isSuccess()) {
        //     // Send the result to the user
        //     messagingTemplate.convertAndSend("/game/topic/user/" + authToken, result);
        // } else {
        //     // Send an error message to the user
        //     GameSessionErrorMessage errorMessage = new GameSessionErrorMessage();
        //     errorMessage.setErrorMessage(result.getErrorMessage());
        //     messagingTemplate.convertAndSend("/game/topic/user/" + authToken, errorMessage);
        // }
    }

}
