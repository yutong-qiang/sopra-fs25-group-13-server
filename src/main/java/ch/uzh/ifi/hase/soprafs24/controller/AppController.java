package ch.uzh.ifi.hase.soprafs24.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameSessionGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardEntryDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.UserDTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AppService;
import ch.uzh.ifi.hase.soprafs24.service.TwilioService;

/**
 * App Controller This class is responsible for handling all REST request for
 * the app. The controller will receive the request and delegate the execution
 * to the AppService and finally return the result.
 */
//testing commits for M2
@RestController
public class AppController {

    private final AppService appService;
    private final TwilioService twilioService;

    public AppController(AppService appService, TwilioService twilioService) {
        this.appService = appService;
        this.twilioService = twilioService;
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

    @GetMapping("users/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<UserGetDTO> getUserById(@PathVariable Long id) {
        User user = appService.getUserById(id);
        return ResponseEntity.ok(UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
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
    public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
        //// only get username and password ////////
    User loggedInUser = appService.loginUser(userPostDTO.getUsername(), userPostDTO.getPassword());

        // check if a user alr has a valid token, if not generate a new one
        String token = loggedInUser.getToken();
        if (token == null || token.isEmpty()) {
            token = UUID.randomUUID().toString();
            loggedInUser.setToken(token);
        }

        return UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(loggedInUser);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody

    public void logoutUser(@RequestHeader("Authorization") String token) {

        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }

        appService.logoutUser(token);

    }
    // }

    /////////////// create game session ////////////////////////
  @PostMapping("/game")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameSessionGetDTO createGameSession(@RequestHeader("Authorization") String authToken) {
        // verify authToken
        if (!appService.isUserTokenValid(authToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }
        // retrieve user from authToken
        User user = appService.getUserByToken(authToken);
        // create game session
        GameSession gameSession = appService.createGameSession(user);
        // add the user to the game session
        Player player = appService.addToGameSession(user, gameSession);
        // return the game session
        GameSessionGetDTO gameSessionGetDTO = GameDTOMapper.INSTANCE.convertEntityToGameSessionGetDTO(gameSession, player, user);
        return gameSessionGetDTO;
    }

    ////////////////////// end game session ////////////////////////
  @DeleteMapping("/game/end/{gameToken}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void endGameSession(
            @PathVariable String gameToken,
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
        if (gameSession.getCurrentState() != GameState.WAITING_FOR_PLAYERS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game session is not accepting players");
        }
        // add the user to the game session
        Player player = appService.addToGameSession(user, gameSession);
        // return the game session
        GameSessionGetDTO gameSessionGetDTO = GameDTOMapper.INSTANCE.convertEntityToGameSessionGetDTO(gameSession, player, user);
        return gameSessionGetDTO;
    }

    //   //////////////////// get game word (only authorized / non-chameleon users can get it) ////////////////////////
    // @GetMapping("/game/word/{gameToken}")
    //   @ResponseStatus(HttpStatus.OK)
    //   @ResponseBody
    //   public String getGameWord(@PathVariable String gameToken,
    //           @RequestHeader("Authorization") String authToken) {
    //       // Verify auth token
    //       if (!appService.isUserTokenValid(authToken)) {
    //           throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
    //       }
    //       // Get game session
    //       GameSession gameSession = appService.getGameSessionByGameToken(gameToken);
    //       if (gameSession == null) {
    //           throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found");
    //       }
    //       // Get user
    //       User user = appService.getUserByToken(authToken);
    //       // Check if game has started
    //       if (gameSession.getCurrentState() != GameState.STARTED) {
    //           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game has not started yet");
    //       }
    //       // Check if user is in game
    //       if (!appService.isUserInGameSession(user, gameSession)) {
    //           throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a game session player");
    //       }
    //       // Check if user is chameleon
    //       Player player = appService.getPlayerByUserAndGameSession(user, gameSession);
    //       if (player.getIsChameleon()) {
    //           throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chameleon cannot see the word");
    //       }
    //       return gameSession.getSecretWord();
    //   }
    //   //////////////// get game role /////////////////////////
    // @GetMapping("/game/role/{gameToken}")
    //   @ResponseStatus(HttpStatus.OK)
    //   @ResponseBody
    //   public String getGameRole(@PathVariable String gameToken,
    //           @RequestHeader("Authorization") String authToken) {
    //       // Verify auth token
    //       if (!appService.isUserTokenValid(authToken)) {
    //           throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
    //       }
    //       // Get game session
    //       GameSession gameSession = appService.getGameSessionByGameToken(gameToken);
    //       if (gameSession == null) {
    //           throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found");
    //       }
    //       // Get user
    //       User user = appService.getUserByToken(authToken);
    //       // Check if user is in the game session
    //       if (!appService.isUserInGameSession(user, gameSession)) {
    //           throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a game session player");
    //       }
    //       // Check if game has started
    //       if (gameSession.getCurrentState() != GameState.STARTED) {
    //           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game has not started yet");
    //       }
    //       // Get player and check if they are the chameleon
    //       Player player = appService.getPlayerByUserAndGameSession(user, gameSession);
    //       return player.getIsChameleon() ? "CHAMELEON" : "PLAYER";
    //   }
    //////////////////// Retrieve participants /////////////////////////
  @GetMapping("/game/players/{gameToken}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getGamePlayers(@PathVariable String gameToken,
            @RequestHeader("Authorization") String authToken) {
        // Verify auth token
        if (!appService.isUserTokenValid(authToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authToken");
        }

        // Get game session
        GameSession gameSession = appService.getGameSessionByGameToken(gameToken);
        if (gameSession == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found");
        }

        // Get players and convert to DTOs
        List<User> players = appService.getGameSessionPlayers(gameSession);
        List<UserGetDTO> playerDTOs = new ArrayList<>();
        for (User player : players) {
            playerDTOs.add(UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(player));
        }

        return playerDTOs;
    }

    //////////////////// get game info (includes get role and word)/////////////////////////
    @GetMapping("/game/info/{gameToken}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameSessionGetDTO getGameInfo(@PathVariable String gameToken,
            @RequestHeader("Authorization") String authToken) {
        // Verify auth token
        if (!appService.isUserTokenValid(authToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }

        // Get game session
        GameSession gameSession = appService.getGameSessionByGameToken(gameToken);
        if (gameSession == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found");
        }

        // Get user
        User user = appService.getUserByToken(authToken);

        // Check if user is in game
        if (!appService.isUserInGameSession(user, gameSession)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a game session player");
        }

        // Get player info
        Player player = appService.getPlayerByUserAndGameSession(user, gameSession);

        // Create DTO
        GameSessionGetDTO gameSessionGetDTO = GameDTOMapper.INSTANCE.convertEntityToGameSessionGetDTO(gameSession, player, user);

        // Set role
        gameSessionGetDTO.setRole(player.getIsChameleon() ? "CHAMELEON" : "PLAYER");

        // Set secret word (empty for chameleon)
        if (gameSession.getCurrentState() == GameState.STARTED && !player.getIsChameleon()) {
            gameSessionGetDTO.setSecretWord(gameSession.getSecretWord());
        } else {
            gameSessionGetDTO.setSecretWord("");
        }

        // Set current turn
        if (gameSession.getCurrentPlayerTurn() != null) {
            gameSessionGetDTO.setCurrentTurn(gameSession.getCurrentPlayerTurn().getUser().getUsername());
        }

        return gameSessionGetDTO;
    }

    @PostMapping("user/avatar")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void uploadAvatar(
            @RequestHeader("Authorization") String authToken,
            @RequestParam("file") MultipartFile file) {
        // verify authToken
        if (!appService.isUserTokenValid(authToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }
        User user = appService.getUserByToken(authToken);

        String ct = file.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(ct)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG allowed"
            );
        }

        try {
            appService.storeAvatar(user, file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store avatar",
                    e
            );
        }
    }

    @GetMapping("/leaderboard")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<LeaderboardEntryDTO> getLeaderboard() {
        List<User> users = appService.getUsers();
        List<LeaderboardEntryDTO> leaderboard = new ArrayList<>();

        for (User user : users) {
            LeaderboardEntryDTO entry = new LeaderboardEntryDTO();
            entry.setId(user.getId());
            entry.setUsername(user.getUsername());
            entry.setWins(user.getWins());
            entry.setRoundsPlayed(user.getRoundsPlayed());
            entry.setAvatar(user.getAvatar());

            double winRate = user.getRoundsPlayed() > 0
                    ? (double) user.getWins() / user.getRoundsPlayed()
                    : 0.0;
            entry.setWinRate(winRate);

            leaderboard.add(entry);
        }

        // Sort by win rate (descending)
        leaderboard.sort((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()));

        return leaderboard;
    }
}
