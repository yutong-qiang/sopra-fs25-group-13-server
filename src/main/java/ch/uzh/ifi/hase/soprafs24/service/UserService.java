package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  @Autowired
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User loginUser(String username, String password){
    User user = userRepository.findByUsername(username);
    if (user == null || !(user.getPassword().equals(password))){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

///////if a login user doesnt have a token, generate it for the person
    if (user.getToken() == null || user.getToken().isEmpty()){
      String token = UUID.randomUUID().toString();
      user.setToken(token);
      userRepository.save(user);
    }
/////////////////////////////////////////////////////////////
    userRepository.save(user);
    return user;
  }

  public User logoutUser(HttpServletRequest request){
    String token = request.getHeader("Authorization");
    if (token == null){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
    }
    User user = userRepository.findByToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    
    user.setToken(null);
    userRepository.save(user);
    return user;
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    checkIfUserExists(newUser);
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
    User userByName = userRepository.findByName(userToBeCreated.getName());

    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null && userByName != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format(baseErrorMessage, "username and the name", "are"));
    } else if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
    } else if (userByName != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
    }
  }

  public User getUserById(Long id){
    return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
    "User with ID " + id + " not found"));
  }

  public User getUserByToken(String token) {
    // Assuming you have a token stored in the user entity, find the user by token
    return userRepository.findByToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
}


  public boolean isTokenValid(String token){
    return userRepository.existsByToken(token);
  }
}
