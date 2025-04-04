package ch.uzh.ifi.hase.soprafs24.controller;

// import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
// import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.UserDTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameSessionGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.UserDTOMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AppService;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */

//testing commits for M2
@RestController
public class AppController {

  private final AppService appService;

  public AppController(AppService appService) {
    this.appService = appService;
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers() {
    // fetch all users in the internal representation
    List<User> users = appService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User userInput = UserDTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // create user
    User createdUser = appService.createUser(userInput);
    // convert internal representation of user back to API
    return UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Map<String, Object> loginUser(@RequestBody UserPostDTO userPostDTO) {
    //// only get username and password ////////
    User loggedInUser = appService.loginUser(userPostDTO.getUsername(), userPostDTO.getPassword());

    // check if a user alr has a valid token, if not generate a new one
    String token = loggedInUser.getToken();
    if (token == null || token.isEmpty()){
      token = UUID.randomUUID().toString();
      loggedInUser.setToken(token);
    }

    // Create response object with token
    Map<String, Object> response = new HashMap<>();
    response.put("token", token);
    response.put("id", loggedInUser.getId());
    response.put("username", loggedInUser.getUsername());
    response.put("name", loggedInUser.getName());

    return response;
  }


  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody

  public ResponseEntity<Map<String, String>> logoutUser(HttpServletRequest request){
    String token = request.getHeader("Authorization");

    if (token == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
    }

    appService.logoutUser(request);

    Map<String, String> response = new HashMap<>();
    response.put("message", "User logged out successfully");
    return ResponseEntity.ok(response);
  }
  // }


  /////////////// create game session ////////////////////////
  @PostMapping("/game")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public GameSessionGetDTO createGameSession(@RequestHeader("Authorization") String authToken) {
    // retrieve user from authToken
    User user = appService.getUserByToken(authToken);
    // create game session
    GameSession gameSession = appService.createGameSession(user);
    // add the user to the game session
    appService.addToGameSession(user, gameSession);
    // return the game session
    GameSessionGetDTO gameSessionGetDTO = GameDTOMapper.INSTANCE.convertEntityToGameSessionGetDTO(gameSession);
    return gameSessionGetDTO;
  }

  @DeleteMapping("/game/{gameToken}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public void endGameSession(@PathVariable String gameToken,
                             @RequestHeader("Authorization") String authToken) {
        // verify authToken
    if (!appService.isUserTokenValid(authToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
    }
        // get user      
    User user = appService.getUserByToken(authToken);
        // end game session
    appService.endGameSession(gameToken, user);
    }

  /////////////// join game session ////////////////////////
  @PostMapping("/game/join/{gameToken}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public GameSessionGetDTO joinGameSession(@RequestHeader("Authorization") String authToken, 
                              @PathVariable("gameToken") String gameToken) {
    // verify authToken
    if (!appService.isUserTokenValid(authToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
    }
    // retrieve user from authToken
    User user = appService.getUserByToken(authToken);
    // check if game token is valid
    if (!appService.isGameTokenValid(gameToken)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
    }
    GameSession gameSession = appService.getGameSessionByGameToken(gameToken);
    // check game session state is waiting for players
    if (gameSession.getCurrentState() != GameSession.GameState.WAITING_FOR_PLAYERS) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game session is not accepting players");
    }
    // add the user to the game session
    appService.addToGameSession(user, gameSession);
    // return the game session
    GameSessionGetDTO gameSessionGetDTO = GameDTOMapper.INSTANCE.convertEntityToGameSessionGetDTO(gameSession);
    return gameSessionGetDTO;
  }
  
}
