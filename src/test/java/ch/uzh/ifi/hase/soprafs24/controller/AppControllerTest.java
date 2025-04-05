package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.verify;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.AppService;
import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.service.TwilioService;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(AppController.class)
public class AppControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AppService appService;

  @MockBean
  private TwilioService twilioService;

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

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    // user.setName("Test User");
    user.setUsername("testUsername");
    user.setToken("1");

    UserPostDTO userPostDTO = new UserPostDTO();
    // userPostDTO.setName("Test User");
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
        // .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())));
  }

  @Test
  public void createUser_duplicateUsername_throwsException() throws Exception {
    // given
    UserPostDTO userPostDTO = new UserPostDTO();
    // userPostDTO.setName("Firstname Lastname");
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

    given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
    given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
    given(appService.createGameSession(Mockito.any())).willReturn(gameSession);

    MockHttpServletRequestBuilder postRequest = post("/game")
        .header("Authorization", "*");

    mockMvc.perform(postRequest).andExpect(status().isCreated())
      .andExpect(jsonPath("$.gameSessionId", is(gameSession.getId().intValue())))
      .andExpect(jsonPath("$.gameToken", is(gameSession.getGameToken())));
  }


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

    given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
    given(appService.isGameTokenValid(Mockito.anyString())).willReturn(true);
    given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
    given(appService.getGameSessionByGameToken(Mockito.any())).willReturn(gameSession);

    MockHttpServletRequestBuilder postRequest = post("/game/join/abc123")
        .header("Authorization", "*");

    mockMvc.perform(postRequest).andExpect(status().isOk())
      .andExpect(jsonPath("$.gameSessionId", is(gameSession.getId().intValue())))
      .andExpect(jsonPath("$.gameToken", is(gameSession.getGameToken())));
  }


  @Test
  public void joinGameSession_unauthorizedUser() throws Exception {
    given(appService.isUserTokenValid(Mockito.anyString())).willReturn(false);
    MockHttpServletRequestBuilder postRequest = post("/game/join/abc123")
        .header("Authorization", "*");
    mockMvc.perform(postRequest).andExpect(status().isUnauthorized());
  }

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

  @Test
  public void joinGameSession_gameNotJoinable() throws Exception {
      // given a GameSession object not in waiting state
    GameSession gameSession = new GameSession();
    gameSession.setId(1L);
    gameSession.setCurrentState(GameState.VOTING);

    given(appService.isUserTokenValid(Mockito.anyString())).willReturn(true);
    given(appService.isGameTokenValid(Mockito.anyString())).willReturn(true);
    given(appService.getGameSessionByGameToken(Mockito.any())).willReturn(gameSession);
    MockHttpServletRequestBuilder postRequest = post("/game/join/abc123")
        .header("Authorization", "*");
    mockMvc.perform(postRequest).andExpect(status().isForbidden());
  }

  @Test
  public void joinGameSession_gameFull() throws Exception {
    // given a GameSession object
    GameSession gameSession = new GameSession();
    // given a user
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

    given(appService.getUserByToken(Mockito.anyString())).willReturn(user);
    given(appService.createGameSession(Mockito.any())).willReturn(gameSession);

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
    MockHttpServletRequestBuilder deleteRequest = delete("/game/" + gameToken)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", authToken);

    mockMvc.perform(deleteRequest)
        .andExpect(status().isNoContent());

    verify(appService).endGameSession(gameToken, creator);
  }

  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
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