package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.Collections;
import java.util.List;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
                .andExpect(jsonPath("$.username", is(user.getUsername())));
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

        given(appService.getUserById(1L)).willReturn(user);

        MockHttpServletRequestBuilder getRequest = get("/users/1")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())));
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
  /// successfully ends a game session
  /// 204 No Content
  @Test
    public void endGameSession_withTwilio_success() throws Exception {
        // given
        String gameToken = "testGameToken";
        String authToken = "testAuthToken";

        User creator = new User();
        creator.setId(1L);
        creator.setUsername("testUser");
        creator.setToken(authToken);

        given(appService.isUserTokenValid(authToken)).willReturn(true);
        given(appService.getUserByToken(authToken)).willReturn(creator);

        // when/then
        MockHttpServletRequestBuilder deleteRequest = delete("/game/end/" + gameToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authToken);

        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());

        verify(appService).endGameSession(gameToken, creator);
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
                .header("Authorization", invalidToken);

        mockMvc.perform(deleteRequest)
                .andExpect(status().isUnauthorized());
    }

    /// DELETE /game/{gameToken}
  /// not the creator of the game session
  /// 403 Forbidden
  @Test
    public void endGameSession_notCreator_forbidden() throws Exception {
        String gameToken = "testGameToken";
        String authToken = "validToken";

        User notCreator = new User();
        notCreator.setId(2L);
        notCreator.setUsername("notCreator");

        given(appService.isUserTokenValid(authToken)).willReturn(true);
        given(appService.getUserByToken(authToken)).willReturn(notCreator);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to end game"))
                .when(appService).endGameSession(gameToken, notCreator);

        MockHttpServletRequestBuilder deleteRequest = delete("/game/end/" + gameToken)
                .header("Authorization", authToken);

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

//     /// GET /game/word/{gameToken}
//   /// successful retrieves the game word
//   /// 200 OK
//   @Test
//     public void getGameWord_success() throws Exception {
//         // given
//         User user = new User();
//         user.setId(1L);
//         GameSession gameSession = new GameSession();
//         gameSession.setGameToken("testToken");
//         gameSession.setCurrentState(GameState.STARTED);
//         gameSession.setSecretWord("apple");
//         Player player = new Player();
//         player.setIsChameleon(false);
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
//         given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
//         given(appService.getGameSessionByGameToken("testToken")).willReturn(gameSession);
//         given(appService.isUserInGameSession(user, gameSession)).willReturn(true);
//         // when/then
//         MockHttpServletRequestBuilder getRequest = get("/game/word/testToken")
//                 .header("Authorization", "validToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$", is("apple")));
//     }
//     /// GET /game/word/{gameToken}
//   /// fail to retrieve the game word due to unauthorized token
//   /// 401 Unauthorized
//   @Test
//     public void getGameWord_unauthorized() throws Exception {
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(false);
//         MockHttpServletRequestBuilder getRequest = get("/game/word/testToken")
//                 .header("Authorization", "invalidToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isUnauthorized());
//     }
//     /// GET /game/word/{gameToken}
//   /// User not a game session player
//   /// 401 Unauthorized
//   @Test
//     public void getGameWord_userNotInGameSession_unauthorized() throws Exception {
//         // given
//         User user = new User();
//         user.setId(1L);
//         user.setUsername("testUser");
//         user.setToken("validToken");
//         GameSession gameSession = new GameSession();
//         gameSession.setGameToken("testToken");
//         gameSession.setCurrentState(GameState.STARTED);
//         gameSession.setSecretWord("apple");
//         // Map<Long, String> roles = new HashMap<>();
//         // roles.put(2L, "PLAYER"); // User ID 1 is not in the game
//         // gameSession.setRoles(roles);
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
//         given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
//         given(appService.getGameSessionByGameToken(Mockito.anyString())).willReturn(gameSession);
//         given(appService.isUserInGameSession(user, gameSession)).willReturn(false);
//         // when/then
//         MockHttpServletRequestBuilder getRequest = get("/game/word/testToken")
//                 .header("Authorization", "validToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isUnauthorized());
//     }
//     /// GET /game/word/{gameToken}
//   /// fail to retrieve the game word due to user being the chameleon
//   /// 401 Unauthorized
//   @Test
//     public void getGameWord_userIsChameleon_unauthorized() throws Exception {
//         // given
//         User user = new User();
//         user.setId(1L);
//         user.setUsername("testUser");
//         user.setToken("validToken");
//         GameSession gameSession = new GameSession();
//         gameSession.setGameToken("testToken");
//         gameSession.setCurrentState(GameState.STARTED);
//         gameSession.setSecretWord("apple");
//         // Map<Long, String> roles = new HashMap<>();
//         // roles.put(1L, "CHAMELEON");
//         // gameSession.setRoles(roles);
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
//         given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
//         given(appService.getGameSessionByGameToken(Mockito.anyString())).willReturn(gameSession);
//         given(appService.isUserInGameSession(user, gameSession)).willReturn(true);
//         // when/then
//         MockHttpServletRequestBuilder getRequest = get("/game/word/testToken")
//                 .header("Authorization", "validToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isUnauthorized());
//     }
//     /// GET /game/word/{gameToken}
//   /// game session not found
//   /// 404 Not Found
//   @Test
//     public void getGameWord_gameSessionNotFound() throws Exception {
//         User user = new User();
//         user.setId(1L);
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
//         given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
//         given(appService.getGameSessionByGameToken("invalidToken"))
//                 .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));
//         MockHttpServletRequestBuilder getRequest = get("/game/word/invalidToken")
//                 .header("Authorization", "validToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isNotFound());
//     }
//     /// GET /game/word/{gameToken}
//   /// game not started yet
//   /// 400 Bad Request
//   @Test
//     public void getGameWord_gameNotStarted() throws Exception {
//         // given
//         User user = new User();
//         GameSession gameSession = new GameSession();
//         gameSession.setCurrentState(GameState.WAITING_FOR_PLAYERS);
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
//         given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
//         given(appService.getGameSessionByGameToken(Mockito.anyString())).willReturn(gameSession);
//         MockHttpServletRequestBuilder getRequest = get("/game/word/testToken")
//                 .header("Authorization", "validToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isBadRequest());
//     }
//     /// GET /game/role/{gameToken}
//   /// successful retrieves the game role
//   /// 200 OK
//   @Test
//     public void getGameRole_success() throws Exception {
//         // given
//         User user = new User();
//         user.setId(1L);
//         GameSession gameSession = new GameSession();
//         gameSession.setCurrentState(GameState.STARTED);
//         // Map<Long, String> roles = new HashMap<>();
//         // roles.put(1L, "PLAYER");
//         // gameSession.setRoles(roles);
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
//         given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
//         given(appService.getGameSessionByGameToken(Mockito.anyString())).willReturn(gameSession);
//         // when/then
//         MockHttpServletRequestBuilder getRequest = get("/game/role/testToken")
//                 .header("Authorization", "validToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$", is("PLAYER")));
//     }
//     /// GET /game/role/{gameToken}
//   /// invalid or missing token
//   /// 401 Unauthorized
//   @Test
//     public void getGameRole_unauthorized() throws Exception {
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(false);
//         MockHttpServletRequestBuilder getRequest = get("/game/role/testToken")
//                 .header("Authorization", "invalidToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isUnauthorized());
//     }
//     /// GET /game/role/{gameToken}
//   /// game not started yet
//   /// 400 Bad Request
//   @Test
//     public void getGameRole_gameNotStarted() throws Exception {
//         // given
//         User user = new User();
//         GameSession gameSession = new GameSession();
//         gameSession.setCurrentState(GameState.WAITING_FOR_PLAYERS);
//         given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
//         given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
//         given(appService.getGameSessionByGameToken(Mockito.anyString())).willReturn(gameSession);
//         MockHttpServletRequestBuilder getRequest = get("/game/role/testToken")
//                 .header("Authorization", "validToken");
//         mockMvc.perform(getRequest)
//                 .andExpect(status().isBadRequest());
//     }
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
}
