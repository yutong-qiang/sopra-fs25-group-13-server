package ch.uzh.ifi.hase.soprafs24.controller;

// import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
// import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class AppController {

  private final UserService userService;

  AppController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers() {
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
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
    User createdUser = userService.createUser(userInput);
    // convert internal representation of user back to API
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Map<String, Object> loginUser(@RequestBody UserPostDTO userPostDTO) {
    //// only get username and password ////////
    User loggedInUser = userService.loginUser(userPostDTO.getUsername(), userPostDTO.getPassword());

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
}
