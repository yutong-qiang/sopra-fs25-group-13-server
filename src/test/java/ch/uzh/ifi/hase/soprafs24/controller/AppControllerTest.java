package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.AppService;
import ch.uzh.ifi.hase.soprafs24.service.TwilioService;

/**
 * UserControllerTest This is a WebMvcTest which allows to test the
 * UserController i.e. GET/POST request without actually sending them over the
 * network. This tests if the UserController works.
 */
@WebMvcTest(AppController.class)
public class AppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppService appService;

    @MockBean
    private TwilioService twilioService;

    /// GET /users
    /// successfully gets list of all users
    /// 200 OK
    @Test
    public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
        // given
        User user = new User();
        // user.setName("Firstname Lastname");
        user.setUsername("firstname@lastname");

        List<User> allUsers = Collections.singletonList(user);

        // this mocks the AppService -> we define above what the AppService should
        // return when getUsers() is called
        given(appService.getUsers()).willReturn(allUsers);

        // when
        MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                // .andExpect(jsonPath("$[0].name", is(user.getName())))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())));
    }

    /// POST /register
    /// Registration successful
    /// 201 Created
    @Test
    public void createUser_validInput_userCreated() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");
        user.setToken("1");
        user.setWins(0);
        user.setRoundsPlayed(0);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");

        given(appService.createUser(Mockito.any())).willReturn(user);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.wins", is(user.getWins())))
                .andExpect(jsonPath("$.roundsPlayed", is(user.getRoundsPlayed())));
    }

    /// POST /register
    /// valication failed (e.g. username taken)
    /// 400 Bad Request
    @Test
    public void createUser_duplicateUsername_throwsException() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("firstname@lastname");
        userPostDTO.setPassword("password");

        given(appService.createUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken!"));

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
        System.out.println("Test completed: POST /users with duplicate username returned conflict");
    }

    /// POST /login
    /// Login successful
    /// 200 OK
    @Test
    public void loginUser_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        // user.setName("Test User");
        user.setUsername("testUsername");
        user.setToken("1");

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("testPassword");

        given(appService.loginUser(Mockito.anyString(), Mockito.anyString())).willReturn(user);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())));
    }

    /// POST /login
    /// invalid credentials (wrong username or password)
    /// 401 Unauthorized
    @Test
    public void loginUser_invalidCredentials_throwsException() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("wrongUsername");
        userPostDTO.setPassword("wrongPassword");

        given(appService.loginUser(Mockito.anyString(), Mockito.anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    /// POST /logout
    /// Logout successful
    /// 200 OK
    @Test
    public void logoutUser_success() throws Exception {
        // given
        String token = "test-token";

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    /// POST /logout
    /// invalid session (token is invalid)
    /// 401 Unauthorized
    @Test
    public void logoutUser_invalidSession() throws Exception {
        String token = "invalid-token";

        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session"))
                .when(appService).logoutUser(token);

        MockHttpServletRequestBuilder postRequest = post("/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token);

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    /// GET /users/{id}
    /// successful retrieves a user by id
    /// 200 OK
    @Test
    public void getUserById_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setWins(5);
        user.setRoundsPlayed(10);

        given(appService.getUserById(1L)).willReturn(user);

        MockHttpServletRequestBuilder getRequest = get("/users/1")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.wins", is(user.getWins())))
                .andExpect(jsonPath("$.roundsPlayed", is(user.getRoundsPlayed())));
    }

    /// GET /users/{id}
    /// fail to retrieve a user by id due to user not found
    /// 404 Not Found
    @Test
    public void getUserById_notFound() throws Exception {
        given(appService.getUserById(1L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        MockHttpServletRequestBuilder getRequest = get("/users/1")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    /// GET /game/players/{gameToken}
    /// successful retrieves list of players in a game session
    /// 200 OK
    @Test
    public void getGamePlayers_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        List<User> players = Collections.singletonList(user);
        GameSession gameSession = new GameSession();
        gameSession.setGameToken("testToken");

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getGameSessionByGameToken("testToken")).willReturn(gameSession);
        given(appService.getGameSessionPlayers(gameSession)).willReturn(players);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/game/players/testToken")
                .header("Authorization", "validToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(user.getId().intValue())))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())));
    }

    /// GET /game/players/{gameToken}
    /// fail to retrieve list of players due to unauthorized token
    /// 401 Unauthorized
    @Test
    public void getGamePlayers_unauthorized() throws Exception {
        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(false);

        MockHttpServletRequestBuilder getRequest = get("/game/players/testToken")
                .header("Authorization", "invalidToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    /// GET /game/players/{gameToken}
    /// unable to join the game because game is not found
    /// 404 Not Found
    @Test
    public void getGamePlayers_gameNotFound() throws Exception {
        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getGameSessionByGameToken(Mockito.anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        MockHttpServletRequestBuilder getRequest = get("/game/players/invalidToken")
                .header("Authorization", "validToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    /// POST /game
    /// Game session created
    /// 201 Created
    @Test
    public void createGameSession_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        // user.setName("Test User");
        user.setUsername("testUsername");
        user.setToken("1");

        // given a GameSession object
        GameSession gameSession = new GameSession();
        gameSession.setId(1L);
        gameSession.setCreator(user);
        gameSession.setGameToken("abc123");
        gameSession.setCurrentState(GameState.WAITING_FOR_PLAYERS);

        // Create a player object that will be returned by addToGameSession
        Player player = new Player();
        player.setUser(user);
        player.setGameSession(gameSession);

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
        given(appService.createGameSession(Mockito.any())).willReturn(gameSession);
        given(appService.addToGameSession(Mockito.any(), Mockito.any())).willReturn(player);

        MockHttpServletRequestBuilder postRequest = post("/game")
                .header("Authorization", "*");

        mockMvc.perform(postRequest).andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameSessionId", is(gameSession.getId().intValue())))
                .andExpect(jsonPath("$.gameToken", is(gameSession.getGameToken())));
    }

    /// POST /game with twilio
    /// successful creates a new game session with Twilio video integration
    /// 201 Created
    @Test
    public void createGameSession_withTwilio_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setToken("testToken");

        GameSession gameSession = new GameSession();
        gameSession.setId(1L);
        gameSession.setCreator(user);
        gameSession.setGameToken("testGameToken");
        gameSession.setTwilioRoomSid("RM123456789");
        gameSession.setCurrentState(GameState.WAITING_FOR_PLAYERS);

        // Create a player object that will be returned by addToGameSession
        Player player = new Player();
        player.setUser(user);
        player.setGameSession(gameSession);
        player.setTwilioToken("mock_twilio_token");

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
        given(appService.createGameSession(Mockito.any())).willReturn(gameSession);
        given(appService.addToGameSession(Mockito.any(), Mockito.any())).willReturn(player);

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/game")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "testToken");

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameSessionId", is(gameSession.getId().intValue())))
                .andExpect(jsonPath("$.gameToken", is(gameSession.getGameToken())))
                .andExpect(jsonPath("$.twilioRoomSid", is(gameSession.getTwilioRoomSid())));
    }

    /// POST /game
    /// Invalid game creation request
    /// 401 Unauthorized
    @Test
    public void createGameSession_invalidAuthToken_unauthorized() throws Exception {
        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(false);

        MockHttpServletRequestBuilder postRequest = post("/game")
                .header("Authorization", "invalidToken");

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    /// POST /game
    /// Missing or Invalid authToken
    /// 401 Unauthorized
    @Test
    public void createGameSession_invalidRequest_badRequest() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setToken("validToken");

        given(appService.isUserTokenValid("validToken")).willReturn(true);
        given(appService.getUserByToken("validToken")).willReturn(user);
        given(appService.createGameSession(user))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid game creation request"));

        MockHttpServletRequestBuilder postRequest = post("/game")
                .header("Authorization", "validToken");

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    /// POST /game/join/{gameToken}
    /// successful joins a game session
    /// 200 OK
    @Test
    public void joinGameSession_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        // user.setName("Test User");
        user.setUsername("testUsername");
        user.setToken("1");

        // given a GameSession object
        GameSession gameSession = new GameSession();
        gameSession.setId(1L);
        gameSession.setCreator(user);
        gameSession.setGameToken("abc123");
        gameSession.setCurrentState(GameState.WAITING_FOR_PLAYERS);

        // Create a player object that will be returned by addToGameSession
        Player player = new Player();
        player.setUser(user);
        player.setGameSession(gameSession);
        player.setTwilioToken("mock_twilio_token");

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.isGameTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
        given(appService.getGameSessionByGameToken(Mockito.any())).willReturn(gameSession);
        given(appService.addToGameSession(Mockito.any(), Mockito.any())).willReturn(player);

        MockHttpServletRequestBuilder postRequest = post("/game/join/abc123")
                .header("Authorization", "*");

        mockMvc.perform(postRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameSessionId", is(gameSession.getId().intValue())))
                .andExpect(jsonPath("$.gameToken", is(gameSession.getGameToken())));
    }

    /// POST /game/join/{gameToken}
    /// unauthorized user (token is invalid)
    /// 401 Unauthorized
    @Test
    public void joinGameSession_unauthorizedUser() throws Exception {
        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(false);
        MockHttpServletRequestBuilder postRequest = post("/game/join/abc123")
                .header("Authorization", "*");
        mockMvc.perform(postRequest).andExpect(status().isUnauthorized());
    }

    /// POST /game/join/{gameToken}
    /// game session is full
    /// 403 Forbidden
    @Test
    public void joinGameSession_gameNotJoinable() throws Exception {
        GameSession gameSession = new GameSession();
        User user = new User();

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.isGameTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getGameSessionByGameToken(Mockito.any())).willReturn(gameSession);
        given(appService.getUserByToken(Mockito.any())).willReturn(user);
        given(appService.addToGameSession(Mockito.any(), Mockito.any())).willThrow(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Game session is full")
        );

        MockHttpServletRequestBuilder postRequest = post("/game/join/abc123")
                .header("Authorization", "*");

        mockMvc.perform(postRequest).andExpect(status().isForbidden());
    }

    /// POST /game/join/{gameToken}
    /// game session not found (game token is invalid)
    /// 404 Not Found
    @Test
    public void joinGameSession_gameSessionNotFound() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setToken("*");

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
        given(appService.isGameTokenValid(Mockito.anyString())).willReturn(false);
        given(appService.getGameSessionByGameToken(Mockito.anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        MockHttpServletRequestBuilder postRequest = post("/game/join/abc123")
                .header("Authorization", "*");
        mockMvc.perform(postRequest).andExpect(status().isNotFound());
    }

    /// DELETE /game/{gameToken}
    /// successfully ends a game session and updates stats
    /// 204 No Content
    @Test
    public void endGameSession_withTwilio_success() throws Exception {
        // given
        String gameToken = "testGameToken";
        String authToken = "testAuthToken";
        Long winnerId = 1L;

        User creator = new User();
        creator.setId(1L);
        creator.setUsername("testUser");
        creator.setToken(authToken);
        creator.setWins(0);
        creator.setRoundsPlayed(0);

        User winner = new User();
        winner.setId(winnerId);
        winner.setUsername("winner");
        winner.setWins(0);
        winner.setRoundsPlayed(0);

        given(appService.isUserTokenValid(authToken)).willReturn(true);
        given(appService.getUserByToken(authToken)).willReturn(creator);
        given(appService.getUserById(winnerId)).willReturn(winner);

        // when/then
        MockHttpServletRequestBuilder deleteRequest = delete("/game/end/" + gameToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authToken)
                .param("winnerId", winnerId.toString());

        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());

        verify(appService).endGameSession(gameToken, creator, winner);
    }

    /// DELETE /game/{gameToken}
    /// invalid token
    /// 401 Unauthorized
    @Test
    public void endGameSession_invalidToken_unauthorized() throws Exception {
        String gameToken = "testGameToken";
        String invalidToken = "invalidToken";

        given(appService.isUserTokenValid(invalidToken)).willReturn(false);

        MockHttpServletRequestBuilder deleteRequest = delete("/game/end/" + gameToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", invalidToken)
                .param("winnerId", "1");

        mockMvc.perform(deleteRequest)
                .andExpect(status().isUnauthorized());

        verify(appService, Mockito.never()).endGameSession(Mockito.any(), Mockito.any(), Mockito.any());
    }

    /// DELETE /game/{gameToken}
    /// not the creator of the game session
    /// 403 Forbidden
    @Test
    public void endGameSession_notCreator_forbidden() throws Exception {
        String gameToken = "testGameToken";
        String authToken = "validToken";
        Long winnerId = 1L;

        User notCreator = new User();
        notCreator.setId(2L);
        notCreator.setUsername("notCreator");

        User winner = new User();
        winner.setId(winnerId);
        winner.setUsername("winner");

        given(appService.isUserTokenValid(authToken)).willReturn(true);
        given(appService.getUserByToken(authToken)).willReturn(notCreator);
        given(appService.getUserById(winnerId)).willReturn(winner);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to end game"))
                .when(appService).endGameSession(gameToken, notCreator, winner);

        MockHttpServletRequestBuilder deleteRequest = delete("/game/end/" + gameToken)
                .header("Authorization", authToken)
                .param("winnerId", winnerId.toString());

        mockMvc.perform(deleteRequest)
                .andExpect(status().isForbidden());
    }

    /// GET /game/info/{gameToken}
    /// successful retrieves game information for a regular player
    /// 200 OK
    @Test
    public void getGameInfo_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        GameSession gameSession = new GameSession();
        gameSession.setId(1L);
        gameSession.setGameToken("testToken");
        gameSession.setCurrentState(GameState.STARTED);
        gameSession.setSecretWord("apple");

        Player player = new Player();
        player.setIsChameleon(false);
        player.setUser(user);

        Player currentTurnPlayer = new Player();
        currentTurnPlayer.setUser(user);
        gameSession.setCurrentPlayerTurn(currentTurnPlayer);

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
        given(appService.getGameSessionByGameToken("testToken")).willReturn(gameSession);
        given(appService.isUserInGameSession(user, gameSession)).willReturn(true);
        given(appService.getPlayerByUserAndGameSession(user, gameSession)).willReturn(player);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/game/info/testToken")
                .header("Authorization", "validToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameSessionId", is(gameSession.getId().intValue())))
                .andExpect(jsonPath("$.gameToken", is(gameSession.getGameToken())))
                .andExpect(jsonPath("$.role", is("PLAYER")))
                .andExpect(jsonPath("$.secretWord", is("apple")))
                .andExpect(jsonPath("$.gameState", is("STARTED")))
                .andExpect(jsonPath("$.currentTurn", is(user.getUsername())));
    }

    /// GET /game/info/{gameToken}
    /// successful retrieves game information for a chameleon (empty secret word)
    /// 200 OK
    @Test
    public void getGameInfo_chameleon_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        GameSession gameSession = new GameSession();
        gameSession.setId(1L);
        gameSession.setGameToken("testToken");
        gameSession.setCurrentState(GameState.STARTED);
        gameSession.setSecretWord("apple");

        Player player = new Player();
        player.setIsChameleon(true);
        player.setUser(user);

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
        given(appService.getGameSessionByGameToken("testToken")).willReturn(gameSession);
        given(appService.isUserInGameSession(user, gameSession)).willReturn(true);
        given(appService.getPlayerByUserAndGameSession(user, gameSession)).willReturn(player);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/game/info/testToken")
                .header("Authorization", "validToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("CHAMELEON")))
                .andExpect(jsonPath("$.secretWord", is("")));
    }

    /// GET /game/info/{gameToken}
    /// fail to retrieve due to unauthorized token
    /// 401 Unauthorized
    @Test
    public void getGameInfo_unauthorized() throws Exception {
        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(false);

        MockHttpServletRequestBuilder getRequest = get("/game/info/testToken")
                .header("Authorization", "invalidToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    /// GET /game/info/{gameToken}
    /// fail to retrieve due to game session not found
    /// 404 Not Found
    @Test
    public void getGameInfo_gameNotFound() throws Exception {
        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getGameSessionByGameToken(Mockito.anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));

        MockHttpServletRequestBuilder getRequest = get("/game/info/invalidToken")
                .header("Authorization", "validToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    /// GET /game/info/{gameToken}
    /// fail to retrieve due to user not in game session
    /// 401 Unauthorized
    @Test
    public void getGameInfo_userNotInGame() throws Exception {
        // given
        User user = new User();
        GameSession gameSession = new GameSession();
        gameSession.setGameToken("testToken");

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
        given(appService.getGameSessionByGameToken(Mockito.anyString())).willReturn(gameSession);
        given(appService.isUserInGameSession(user, gameSession)).willReturn(false);

        MockHttpServletRequestBuilder getRequest = get("/game/info/testToken")
                .header("Authorization", "validToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    /// GET /users
    /// successfully gets list of all users with their stats
    /// 200 OK
    @Test
    public void getAllUsers_withStats_success() throws Exception {
        // given
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setWins(5);
        user1.setRoundsPlayed(10);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setWins(3);
        user2.setRoundsPlayed(8);

        List<User> allUsers = List.of(user1, user2);

        given(appService.getUsers()).willReturn(allUsers);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(user1.getId().intValue())))
                .andExpect(jsonPath("$[0].username", is(user1.getUsername())))
                .andExpect(jsonPath("$[0].wins", is(user1.getWins())))
                .andExpect(jsonPath("$[0].roundsPlayed", is(user1.getRoundsPlayed())))
                .andExpect(jsonPath("$[1].id", is(user2.getId().intValue())))
                .andExpect(jsonPath("$[1].username", is(user2.getUsername())))
                .andExpect(jsonPath("$[1].wins", is(user2.getWins())))
                .andExpect(jsonPath("$[1].roundsPlayed", is(user2.getRoundsPlayed())));
    }


    @Test
    public void uploadAvatar_success() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setToken("validToken");

        byte[] avatar = "testAvatar".getBytes();
        MockMultipartFile avatarFile = new MockMultipartFile("file", "avatar.jpeg", "image/jpeg", avatar);

        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);

        // when/then
        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart("/user/avatar")
                .file(avatarFile)
                .header("Authorization", "validToken");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    /// POST /user/avatar
    /// fail to upload avatar due to wrong file type (png)
    /// 415 Unsupported Media Type
    @Test
    public void uploadAvatar_invalidFileType() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setToken("validToken");
        byte[] avatar = "testAvatar".getBytes();
        MockMultipartFile avatarFile = new MockMultipartFile("file", "avatar.png", "image/png", avatar);
        given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
        given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
        // when/then
        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart("/user/avatar")
                .file(avatarFile)
                .header("Authorization", "validToken");
        mockMvc.perform(postRequest)
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    public void getLeaderboard_success() throws Exception {
        // given
        List<User> users = new ArrayList<>();
        
        // Create test users with different win rates
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("player1");
        user1.setWins(10);
        user1.setRoundsPlayed(20);
        users.add(user1);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("player2");
        user2.setWins(5);
        user2.setRoundsPlayed(10);
        users.add(user2);

        User user3 = new User();
        user3.setId(3L);
        user3.setUsername("player3");
        user3.setWins(0);
        user3.setRoundsPlayed(5);
        users.add(user3);

        // when
        given(appService.getUsers()).willReturn(users);

        // then
        mockMvc.perform(get("/leaderboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Check first place (player1)
                .andExpect(jsonPath("$[0].id", is(user1.getId().intValue())))
                .andExpect(jsonPath("$[0].username", is(user1.getUsername())))
                .andExpect(jsonPath("$[0].wins", is(user1.getWins())))
                .andExpect(jsonPath("$[0].roundsPlayed", is(user1.getRoundsPlayed())))
                .andExpect(jsonPath("$[0].winRate", is(0.5)))
                // Check second place (player2)
                .andExpect(jsonPath("$[1].id", is(user2.getId().intValue())))
                .andExpect(jsonPath("$[1].username", is(user2.getUsername())))
                .andExpect(jsonPath("$[1].wins", is(user2.getWins())))
                .andExpect(jsonPath("$[1].roundsPlayed", is(user2.getRoundsPlayed())))
                .andExpect(jsonPath("$[1].winRate", is(0.5)))
                // Check third place (player3)
                .andExpect(jsonPath("$[2].id", is(user3.getId().intValue())))
                .andExpect(jsonPath("$[2].username", is(user3.getUsername())))
                .andExpect(jsonPath("$[2].wins", is(user3.getWins())))
                .andExpect(jsonPath("$[2].roundsPlayed", is(user3.getRoundsPlayed())))
                .andExpect(jsonPath("$[2].winRate", is(0.0)));
    }

    @Test
    public void getLeaderboard_emptyList() throws Exception {
        // given
        List<User> emptyList = new ArrayList<>();
        given(appService.getUsers()).willReturn(emptyList);

        // when/then
        mockMvc.perform(get("/leaderboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getLeaderboard_zeroRoundsPlayed() throws Exception {
        // given
        List<User> users = new ArrayList<>();
        
        User user = new User();
        user.setId(1L);
        user.setUsername("player1");
        user.setWins(0);
        user.setRoundsPlayed(0);
        users.add(user);

        given(appService.getUsers()).willReturn(users);

        // when/then
        mockMvc.perform(get("/leaderboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(user.getId().intValue())))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())))
                .andExpect(jsonPath("$[0].wins", is(user.getWins())))
                .andExpect(jsonPath("$[0].roundsPlayed", is(user.getRoundsPlayed())))
                .andExpect(jsonPath("$[0].winRate", is(0.0)));
    }

    /**
     * Helper Method to convert userPostDTO into a JSON string such that the
     * input can be processed Input will look like this: {"name": "Test User",
     * "username": "testUsername"}
     *
     * @param object
     * @return string
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }

    private Object multipart(String useravatar) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
