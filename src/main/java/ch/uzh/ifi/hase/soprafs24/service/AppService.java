package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.UUID;

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
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.constant.GameState;


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

  private final UserRepository userRepository;
  private final GameSessionRepository gameSessionRepository;
  private final PlayerRepository playerRepository;
  private final TwilioService twilioService;


  @Autowired
  public AppService(UserRepository userRepository,
                    GameSessionRepository gameSessionRepository,
                    PlayerRepository playerRepository,
                    TwilioService twilioService) {
    this.userRepository = userRepository;
    this.gameSessionRepository = gameSessionRepository;
    this.playerRepository = playerRepository;
    this.twilioService = twilioService;
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

  public void logoutUser(String token){
    if (token == null){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
    }
    User user = userRepository.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    
    user.setToken(null);
    userRepository.save(user);
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
    // User userByName = userRepository.findByName(userToBeCreated.getName());

    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format(baseErrorMessage, "username and the name", "are"));
    }
    // else if (userByName != null) {
    //   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
    // }
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
    gameSession.setCurrentState(GameState.WAITING_FOR_PLAYERS);

    gameSession = gameSessionRepository.save(gameSession);
    TwilioService.TwilioRoomInfo twilioInfo = twilioService.createVideoRoom(gameSession.getGameToken());
    
    gameSession.setTwilioRoomSid(twilioInfo.roomSid());
    gameSession.setTwilioVideoChatToken(twilioInfo.token());

    gameSession = gameSessionRepository.save(gameSession);
    gameSessionRepository.flush();

    return gameSession;

    // TODO: implement twilio video chat
    // String roomSid = twilioService.createVideoRoom(gameSession.getGameToken());
    // gameSession.setTwilioRoomSid(roomSid);
    // // save the new game session
    // gameSession = gameSessionRepository.save(gameSession);
    // // flush the changes to the database
    // gameSessionRepository.flush();
    // // return the new game session
    // return gameSession;
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
  public void removeFromGameSession(User user, GameSession gameSession) {
    Player player = playerRepository.findByUserAndGameSession(user, gameSession)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found in game session"));
    
    if (user.equals(gameSession.getCreator())) {
        // Close video room first as admin is leaving
        twilioService.closeVideoRoom(gameSession.getTwilioRoomSid());
        // Delete all players
        List<Player> allPlayers = playerRepository.findByGameSession(gameSession);
        playerRepository.deleteAll(allPlayers);
        // Delete game session
        gameSessionRepository.delete(gameSession);
        gameSessionRepository.flush();
        return;
    }
    playerRepository.delete(player);
    playerRepository.flush();
  }


  //   // Check if this was the last player or if the admin left
  //   List<Player> remainingPlayers = playerRepository.findByGameSession(gameSession);
  //   if (remainingPlayers.isEmpty() || user.equals(gameSession.getCreator())) {
  //       // Close video room
  //       twilioService.closeVideoRoom(gameSession.getTwilioRoomSid());
  //       // Delete game session
  //       gameSessionRepository.delete(gameSession);
  //       gameSessionRepository.flush();
  //   }
  // }

  public void endGameSession(String gameToken, User admin) {
    GameSession gameSession = getGameSessionByGameToken(gameToken);
    if (!gameSession.getCreator().equals(admin)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the game creator can end the game");
    }
    
    // Close video room
    twilioService.closeVideoRoom(gameSession.getTwilioRoomSid());

    List<Player> players = playerRepository.findByGameSession(gameSession);
    playerRepository.deleteAll(players);
    
    // Delete game session
    gameSessionRepository.delete(gameSession);
    gameSessionRepository.flush();
  }

  public boolean isUserInGameSession(User user, GameSession gameSession) {
    return playerRepository.findByUserAndGameSession(user, gameSession).isPresent();
  }

  public List<User> getGameSessionPlayers(GameSession gameSession) {
    List<Player> players = playerRepository.findByGameSession(gameSession);
    return players.stream()
                 .map(Player::getUser)
                 .toList();
  }

  public Player getPlayerByUserAndGameSession(User user, GameSession gameSession) {
    return playerRepository.findByUserAndGameSession(user, gameSession)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
  }

}
