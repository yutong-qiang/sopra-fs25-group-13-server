package ch.uzh.ifi.hase.soprafs24.app;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameSessionGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerAction;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerActionResult;

public class MockClient {

    private final int port;
    private final TestRestTemplate restTemplate;

    private String username;
    private String authToken;

    private GameSessionGetDTO gameSession;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    // getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public GameSessionGetDTO getGameSession() {
        return gameSession;
    }

    public MockClient(int port, TestRestTemplate restTemplate) {
        this.port = port;
        this.restTemplate = restTemplate;
    }

    public void createUser(String username) {
        this.username = username;
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername(username);
        dto.setPassword(username + "Password");

        ResponseEntity<UserGetDTO> registerResp = restTemplate.exchange(
                url("/register"),
                HttpMethod.POST,
                new HttpEntity<>(dto, jsonHeaders()),
                UserGetDTO.class
        );
        assertEquals(HttpStatus.CREATED, registerResp.getStatusCode(),
                "User " + username + " registration should return 201");
        authToken = registerResp.getBody().getToken();
        assertNotNull(authToken, "User " + username + " should have a token");
    }

    public void createGame() {
        HttpHeaders authHeaders1 = bearerHeaders(this.authToken);

        ResponseEntity<GameSessionGetDTO> createResp = restTemplate.exchange(
                url("/game"),
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders1),
                GameSessionGetDTO.class
        );
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        GameSessionGetDTO created = createResp.getBody();
        assertNotNull(created);
        assertEquals(GameState.WAITING_FOR_PLAYERS, created.getGameState());
        assertNotNull(created.getGameToken());
        this.gameSession = created;
    }

    public void joinGame(String gameSessionToken, TestRestTemplate restTemplate) {
        HttpHeaders headers = bearerHeaders(this.authToken);
        ResponseEntity<GameSessionGetDTO> joinResp = restTemplate.exchange(
                url("/game/join/{gameToken}"),
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                GameSessionGetDTO.class,
                gameSessionToken
        );
        assertEquals(HttpStatus.OK, joinResp.getStatusCode());
        assertNotNull(joinResp.getBody());
        gameSession = joinResp.getBody();
        assertEquals(gameSessionToken, gameSession.getGameToken());
    }

    public void connectWebSocket() throws Exception {
        // STOMP-over-SockJS client setup remains unchanged
        SockJsClient sockJsClient = new SockJsClient(
                Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()))
        );
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String ws_endpoint = "ws://localhost:" + port + "/game-ws";
        stompSession = stompClient
                .connect(ws_endpoint, new StompSessionHandlerAdapter() {
                }).get(3, TimeUnit.SECONDS);

        // Subscribe to the game session topic
        stompSession.subscribe("/game/topic/" + gameSession.getGameToken(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PlayerActionResult.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                handleWsMessage((PlayerActionResult) payload);
            }
        });
    }

    public void sendPlayerAction(String actionType, String actionContent) {
        PlayerAction action = new PlayerAction();
        action.setGameSessionToken(gameSession.getGameToken());
        action.setActionType(actionType);
        action.setActionContent(actionContent);

        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.setDestination("/game/player-action");
        stompHeaders.add("auth-token", this.authToken);
        stompSession.send(stompHeaders, action);
        System.out.println(username + ": sent action type: " + actionType);
    }

    private void updateGameState() {
        HttpHeaders headers = bearerHeaders(this.authToken);
        ResponseEntity<GameSessionGetDTO> result = restTemplate.exchange(
                url("/game/info/{gameToken}"),
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                GameSessionGetDTO.class,
                gameSession.getGameToken()
        );
        gameSession = result.getBody();
        assertNotNull(gameSession);
    }

    private void handleWsMessage(PlayerActionResult playActionResult) {
        // Handle the message received from the server
        System.out.println(username + ": received action " + playActionResult.getActionType() + " : " + playActionResult.getActionContent());
        updateGameState();
        switch (playActionResult.getActionType()) {
            case "START_GAME":
            if (!GameState.STARTED.equals(gameSession.getGameState())) {
            System.out.println(username + ": Error: Expected game state STARTED but was " + gameSession.getGameState());
            }
            break;
            case "START_VOTING":
            if (!GameState.READY_FOR_VOTING.equals(gameSession.getGameState())) {
            System.out.println(username + ": Error: Expected game state READY_FOR_VOTING but was " + gameSession.getGameState());
            }
            break;
        }
    }

    // helper to build full URL
    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // JSON content headers
    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // Authorization header helper
    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", token);
        return h;
    }

}
