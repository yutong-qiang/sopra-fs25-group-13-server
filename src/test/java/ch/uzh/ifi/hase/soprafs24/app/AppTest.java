package ch.uzh.ifi.hase.soprafs24.app;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameSessionGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.AppService;
import ch.uzh.ifi.hase.soprafs24.service.TwilioService;
import ch.uzh.ifi.hase.soprafs24.service.TwilioService.TwilioRoomInfo;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerAction;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AppTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AppService appService;

    @MockBean
    private TwilioService twilioService;

    private WebSocketStompClient stompClient;

    @BeforeEach
    public void setup() {
        // STOMP-over-SockJS client setup remains unchanged
        SockJsClient sockJsClient = new SockJsClient(
                Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()))
        );
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    public void register_create_start_game_session_success() throws Exception {
        // 1) Register four users
        for (int i = 1; i <= 4; i++) {
            UserPostDTO dto = new UserPostDTO();
            dto.setUsername("testUser" + i);
            dto.setPassword("testPassword" + i);

            ResponseEntity<Void> registerResp = restTemplate.exchange(
                    url("/register"),
                    HttpMethod.POST,
                    new HttpEntity<>(dto, jsonHeaders()),
                    Void.class
            );
            assertEquals(HttpStatus.CREATED, registerResp.getStatusCode(),
                    "User " + i + " registration should return 201");
        }

        // 2) Fetch user1’s token and create a game
        TwilioService.TwilioRoomInfo twilioRoomInfo = new TwilioRoomInfo("testRoom", "testRoomSid");
        Mockito.when(twilioService.createVideoRoom(Mockito.any()))
                .thenReturn(twilioRoomInfo);
        Mockito.when(twilioService.generateToken(Mockito.any(), Mockito.any()))
                .thenReturn("creatorToken");
        String authToken1 = appService.getUserByUsername("testUser1").getToken();
        HttpHeaders authHeaders1 = bearerHeaders(authToken1);

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
        String gameToken = created.getGameToken();
        // 3) Have users 2–4 join
        for (int i = 2; i <= 4; i++) {
            String token = appService.getUserByUsername("testUser" + i).getToken();
            HttpHeaders headers = bearerHeaders(token);
            ResponseEntity<GameSessionGetDTO> joinResp = restTemplate.exchange(
                    url("/game/join/{gameToken}"),
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    GameSessionGetDTO.class,
                    gameToken
            );
            assertEquals(HttpStatus.OK, joinResp.getStatusCode());
            assertNotNull(joinResp.getBody());
            assertEquals(gameToken, joinResp.getBody().getGameToken());
        }
        // 4) STOMP START_GAME frame
        StompSession session = stompClient
                .connect("ws://localhost:" + port + "/game-ws", new StompSessionHandlerAdapter() {
                })
                .get(3, TimeUnit.SECONDS);
        PlayerAction action = new PlayerAction();
        action.setGameSessionToken(gameToken);
        action.setActionType("START_GAME");
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.setDestination("/game/player-action");
        stompHeaders.add("auth-token", authToken1);
        session.send(stompHeaders, action);
        // 5) Verify state change
        // (allow a brief moment if your handler is async)
        Thread.sleep(200);
        assertEquals(GameState.STARTED,
                appService.getGameSessionByGameToken(gameToken).getCurrentState());
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
