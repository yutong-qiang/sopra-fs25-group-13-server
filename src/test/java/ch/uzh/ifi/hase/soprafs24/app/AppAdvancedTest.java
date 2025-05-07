package ch.uzh.ifi.hase.soprafs24.app;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.service.AppService;
import ch.uzh.ifi.hase.soprafs24.service.TwilioService;
import ch.uzh.ifi.hase.soprafs24.service.TwilioService.TwilioRoomInfo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AppAdvancedTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AppService appService;

    @MockBean
    private TwilioService twilioService;

    @Test
    public void register_create_start_game_session_success() throws Exception {
        // Register four users
        List<MockClient> clients;
        clients = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            MockClient client = new MockClient(port, restTemplate);
            client.createUser("testUser" + (i + 1));
            clients.add(client);
        }

        // mock twilio service
        TwilioService.TwilioRoomInfo twilioRoomInfo = new TwilioRoomInfo("testRoom", "testRoomSid");
        Mockito.when(twilioService.createVideoRoom(Mockito.any()))
                .thenReturn(twilioRoomInfo);
        Mockito.when(twilioService.generateToken(Mockito.any(), Mockito.any()))
                .thenReturn("creatorToken");

        // client 0 creates a game session
        MockClient adminClient = clients.get(0);
        adminClient.createGame();
        String gameToken = adminClient.getGameSession().getGameToken();

        // Have clients 2–4 join
        clients.get(1).joinGame(gameToken, restTemplate);
        clients.get(2).joinGame(gameToken, restTemplate);
        clients.get(3).joinGame(gameToken, restTemplate);

        // clients connect to websocket
        clients.get(0).connectWebSocket();
        clients.get(1).connectWebSocket();
        clients.get(2).connectWebSocket();
        clients.get(3).connectWebSocket();

        // admin starts game
        adminClient.sendPlayerAction("START_GAME", null);
        // Verify state change (allow a brief moment if your handler is async)
        Thread.sleep(2000);
        assertEquals(GameState.STARTED,
                appService.getGameSessionByGameToken(gameToken).getCurrentState());

        // determine the chameleon
        // the chameleon is the client such that its gameSession.getSecretWord() is empty
        MockClient chameleonClient = clients.stream()
                .filter(client -> client.getGameSession().getSecretWord().isEmpty())
                .findFirst()
                .orElse(null);
        assertNotNull(chameleonClient, "Chameleon client should not be null");

        // each client gives a hint
        for (int i = 0; i < clients.size(); i++) {
                String current_turn = clients.get(0).getGameSession().getCurrentTurn();
                System.out.println("Current turn: " + current_turn);
                // find client with username == current_turn
                MockClient currentClient = clients.stream()
                        .filter(client -> client.getUsername().equals(current_turn))
                        .findFirst()
                        .orElse(null);
                assertNotNull(currentClient, "Iteration " + i + ", current client should not be null");
                // give hint
                currentClient.sendPlayerAction("GIVE_HINT", currentClient.getUsername() + "_TestHint");
                Thread.sleep(500);
        }
        // Verify state change
        Thread.sleep(2000);
        assertEquals(GameState.READY_FOR_VOTING,
                appService.getGameSessionByGameToken(gameToken).getCurrentState());
        
        // admin starts voting
        adminClient.sendPlayerAction("START_VOTING", null);
        // Verify state change
        Thread.sleep(2000);
        assertEquals(GameState.VOTING,
                appService.getGameSessionByGameToken(gameToken).getCurrentState());
        // each client votes
        MockClient firstNonChameleonClient = clients.stream()
                .filter(client -> !client.getUsername().equals(chameleonClient.getUsername()))
                .findFirst()
                .orElse(null);
        for (MockClient client : clients) {
            if (client.getUsername().equals(chameleonClient.getUsername())) {
                // chameleon votes for firstNonChameleonClient
                client.sendPlayerAction("VOTE", firstNonChameleonClient.getUsername());
            } else {
                // other clients vote for the chameleon
                client.sendPlayerAction("VOTE", chameleonClient.getUsername());
            }
            Thread.sleep(500);
        }

        // Verify state change
        Thread.sleep(1000);
        assertEquals(GameState.CHAMELEON_TURN,
                appService.getGameSessionByGameToken(gameToken).getCurrentState());

        // chameleon guesses the secret word
        String secretWord = appService.getGameSessionByGameToken(gameToken).getSecretWord();
        chameleonClient.sendPlayerAction("CHAMELEON_GUESS", secretWord);
        // Verify state change
        Thread.sleep(2000);
        assertEquals(GameState.CHAMELEON_WIN,
                appService.getGameSessionByGameToken(gameToken).getCurrentState());
    }
}
