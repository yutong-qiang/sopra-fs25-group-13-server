package ch.uzh.ifi.hase.soprafs24.controller;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import ch.uzh.ifi.hase.soprafs24.service.AppService;
import ch.uzh.ifi.hase.soprafs24.service.GameSessionService;
import ch.uzh.ifi.hase.soprafs24.service.TwilioService;
import ch.uzh.ifi.hase.soprafs24.websocket.GameSessionErrorMessage;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerAction;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerActionResult;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GameSessionControllerTest {

    @LocalServerPort
    private int port;

    @MockBean
    private AppService appService;

    @MockBean
    private GameSessionService gameSessionService;

    @MockBean
    private TwilioService twilioService;

    private WebSocketStompClient stompClient;

    @BeforeEach
    public void setup() {
        SockJsClient sockJsClient = new SockJsClient(
                Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()))
        );
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    // @Test
    // public void testPlayerAction_invalidToken_receivesError() throws Exception {
    //     // Mock user token
    //     String token = "aaa";

    //     // Configure the mock service to return false for token validation.
    //     given(appService.isUserTokenValid(Mockito.any())).willReturn(false);

    //     // Connect to the WebSocket server.
    //     String url = "ws://localhost:" + port + "/game-ws";
    //     StompSession session = stompClient.connect(url, new StompSessionHandlerAdapter() {
    //     }).get(1, TimeUnit.SECONDS);

    //     // The stomp client will trigger the latch when the message is received.
    //     final CountDownLatch errorReceived = new CountDownLatch(1);
    //     // AtomicReference to store the error message.
    //     final AtomicReference<String> errorMessageRef = new AtomicReference<>();

    //     // Subscribe to the user topic.
    //     String userEndpoint = "/game/topic/user/" + token;
    //     session.subscribe(userEndpoint, new StompFrameHandler() {
    //         @Override
    //         public Type getPayloadType(StompHeaders headers) {
    //             return GameSessionErrorMessage.class;
    //         }

    //         @Override
    //         public void handleFrame(StompHeaders headers, Object payload) {
    //             GameSessionErrorMessage errorMessage = (GameSessionErrorMessage) payload;
    //             errorMessageRef.set(errorMessage.getErrorMessage());
    //             errorReceived.countDown();
    //         }
    //     });

    //     // Send a message with an invalid token in the headers.
    //     StompHeaders headers = new StompHeaders();
    //     headers.setDestination("/game/player-action");
    //     headers.add("auth-token", token);
    //     session.send(headers, new PlayerAction());

    //     if (!errorReceived.await(3, TimeUnit.SECONDS)) {
    //         fail("Expected error message was not received.");
    //     }
    //     String errorMsg = errorMessageRef.get();
    //     assertNotNull("Error message should not be null", errorMsg);
    //     assertTrue(errorMsg.contains("Invalid auth token"), "Unexpected error message");
    // }

    // @Test
    // public void testPlayerAction_succesful() throws Exception {
    //     // given
    //     String userToken = "abc";
    //     String gsToken = "def";
    //     PlayerAction playerAction = new PlayerAction();
    //     playerAction.setGameSessionToken(gsToken);

    //     given(appService.isUserTokenValid(userToken)).willReturn(true);
    //     given(appService.isGameTokenValid(gsToken)).willReturn(true);
    //     given(gameSessionService.handlePlayerAction(Mockito.any(), Mockito.any(), Mockito.any()))
    //             .willReturn(new PlayerActionResult());

    //     // Connect to the WebSocket server.
    //     String url = "ws://localhost:" + port + "/game-ws";
    //     StompSession session = stompClient.connect(url, new StompSessionHandlerAdapter() {
    //     }).get(1, TimeUnit.SECONDS);

    //     // The stomp client will trigger the latch when the message is received.
    //     final CountDownLatch messageReceived = new CountDownLatch(1);
    //     // AtomicReference to store the PlayerActionResult.
    //     final AtomicReference<PlayerActionResult> playerActionResult = new AtomicReference<>();

    //     // Subscribe to the game session topic.
    //     String userEndpoint = "/game/topic/" + gsToken;
    //     session.subscribe(userEndpoint, new StompFrameHandler() {
    //         @Override
    //         public Type getPayloadType(StompHeaders headers) {
    //             return PlayerActionResult.class;
    //         }

    //         @Override
    //         public void handleFrame(StompHeaders headers, Object payload) {
    //             playerActionResult.set((PlayerActionResult) payload);
    //             messageReceived.countDown();
    //         }
    //     });

    //     // Send a message with the given game session token.
    //     StompHeaders headers = new StompHeaders();
    //     headers.setDestination("/game/player-action");
    //     headers.add("auth-token", userToken);
    //     session.send(headers, playerAction);

    //     if (!messageReceived.await(3, TimeUnit.SECONDS)) {
    //         fail("Expected message was not received.");
    //     }
    //     // Check if the PlayerActionResult is not null.
    //     assertNotNull(playerActionResult.get(), "Message payload should not be null");
    // }

    // @Test
    // public void testPlayerAction_service_error() throws Exception {
    //     // Mock user token
    //     String token = "aaa";

    //     given(appService.isUserTokenValid(token)).willReturn(true);
    //     given(appService.isGameTokenValid(Mockito.any())).willReturn(true);
    //     given(gameSessionService.handlePlayerAction(Mockito.any(), Mockito.any(), Mockito.any()))
    //             .willThrow(new Exception("Service error"));

    //     // Connect to the WebSocket server.
    //     String url = "ws://localhost:" + port + "/game-ws";
    //     StompSession session = stompClient.connect(url, new StompSessionHandlerAdapter() {
    //     }).get(1, TimeUnit.SECONDS);

    //     // The stomp client will trigger the latch when the message is received.
    //     final CountDownLatch errorReceived = new CountDownLatch(1);
    //     // AtomicReference to store the error message.
    //     final AtomicReference<String> errorMessageRef = new AtomicReference<>();

    //     // Subscribe to the user topic.
    //     String userEndpoint = "/game/topic/user/" + token;
    //     session.subscribe(userEndpoint, new StompFrameHandler() {
    //         @Override
    //         public Type getPayloadType(StompHeaders headers) {
    //             return GameSessionErrorMessage.class;
    //         }

    //         @Override
    //         public void handleFrame(StompHeaders headers, Object payload) {
    //             GameSessionErrorMessage errorMessage = (GameSessionErrorMessage) payload;
    //             errorMessageRef.set(errorMessage.getErrorMessage());
    //             errorReceived.countDown();
    //         }
    //     });

    //     // Send a message with an invalid token in the headers.
    //     StompHeaders headers = new StompHeaders();
    //     headers.setDestination("/game/player-action");
    //     headers.add("auth-token", token);
    //     session.send(headers, new PlayerAction());

    //     if (!errorReceived.await(3, TimeUnit.SECONDS)) {
    //         fail("Expected error message was not received.");
    //     }
    //     String errorMsg = errorMessageRef.get();
    //     assertNotNull("Error message should not be null", errorMsg);
    //     assertTrue(errorMsg.contains("Service error"), "Unexpected error message");
    // }
}
