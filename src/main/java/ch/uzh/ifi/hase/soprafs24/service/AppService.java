package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameSessionRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.AppRepository;

/**
 * App Service
 * This class is the "worker" and responsible for all functionality related to
 * the app
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class AppService {

  private final Logger log = LoggerFactory.getLogger(AppService.class);

  private final AppRepository userRepository;
  private final GameSessionRepository gameSessionRepository;
  private final PlayerRepository playerRepository;

  @Autowired
  public AppService(AppRepository userRepository,
                    GameSessionRepository gameSessionRepository,
                    PlayerRepository playerRepository) {
    this.userRepository = userRepository;
    this.gameSessionRepository = gameSessionRepository;
    this.playerRepository = playerRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User loginUser(String username, String password){
    User user = userRepository.findByUsername(username);
    if (user == null || !(user.getPassword().equals(password))){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    if (user.getToken() == null || user.getToken().isEmpty()){
      String token = UUID.randomUUID().toString();
      user.setToken(token);
      userRepository.save(user);
    }
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

  // get User by Username
  public User getUserByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  // check if user token (authToken) is valid
  public boolean isUserTokenValid(String token){
    return userRepository.existsByToken(token);
  }

  // check if game token is valid
  public boolean isGameTokenValid(String gameToken){
    return gameSessionRepository.existsByGameToken(gameToken);
  }

  // get game session by game token
  public GameSession getGameSessionByGameToken(String gameToken){
    return gameSessionRepository.findByGameToken(gameToken).orElseThrow(
      () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found")
    );
  }

  //// createGameSession ////
  public GameSession createGameSession(User creator) {
    // create GameSession entity
    GameSession gameSession = new GameSession();
    gameSession.setCreator(creator);
    gameSession.setGameToken(UUID.randomUUID().toString());
    gameSession.setCurrentState(GameSession.GameState.WAITING_FOR_PLAYERS);
    // save the new game session
    gameSession = gameSessionRepository.save(gameSession);
    // flush the changes to the database
    gameSessionRepository.flush();
    // return the new game session
    return gameSession;
  }

  // add user to game session, making them a player
  public Player addToGameSession(User participant, GameSession gameSession) {
    // if user is already a player in the game session, return it
    Player player = playerRepository.findByUserAndGameSession(participant, gameSession)
                                    .orElse(null);
    if (player != null) {
      return player;
    }
    // get list of players in game session
    List<Player> players = playerRepository.findByGameSession(gameSession);
    // check that the game is not full
    if (players.size() >= 8) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game session is full");
    }
    // create player entity
    player = new Player();
    player.setUser(participant);
    player.setGameSession(gameSession);
    // save the changes to the game session
    playerRepository.save(player);
    // flush the changes to the database
    playerRepository.flush();
    // return the player object
    return player;
  }

}
