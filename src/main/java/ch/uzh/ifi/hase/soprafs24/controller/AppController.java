package ch.uzh.ifi.hase.soprafs24.controller;

// import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameSessionGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
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
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // create user
    User createdUser = appService.createUser(userInput);
    // convert internal representation of user back to API
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
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


  // @PostMapping("/logout")
  // @ResponseStatus(HttpStatus.OK)
  // @ResponseBody

  // /////////////previous code for logout///////////////////////
  // public ResponseEntity<Map<String, String>> logoutUser(HttpServletRequest request){
  //   String token = request.getHeader("Authorization");

  //   if (token == null) {
  //     throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
  //   }

  //   userService.logoutUser(request);

  //   Map<String, String> response = new HashMap<>();
  //   response.put("message", "User logged out successfully");
  //   return ResponseEntity.ok(response);

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
  
}
