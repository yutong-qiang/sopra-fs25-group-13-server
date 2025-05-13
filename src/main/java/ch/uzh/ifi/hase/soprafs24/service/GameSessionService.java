package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameSessionRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerAction;
import ch.uzh.ifi.hase.soprafs24.websocket.PlayerActionResult;

/**
 * GameSessionService is responsible for handling players actions and producing
 * the corresponding PlayerActionResult objects inside of a game session.
 */
@Service
@Transactional
public class GameSessionService {

    private final Logger log = LoggerFactory.getLogger(GameSessionService.class);

    private final GameSessionRepository gameSessionRepository;
    private final PlayerRepository playerRepository;
    private final WordService wordService;
    private final AppService appService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> votingSessions = ConcurrentHashMap.newKeySet();

    @Autowired

    public GameSessionService(
            GameSessionRepository gameSessionRepository,
            PlayerRepository playerRepository,
            WordService wordService,
            AppService appService,
            SimpMessagingTemplate messagingTemplate) {
        this.gameSessionRepository = gameSessionRepository;
        this.playerRepository = playerRepository;
        this.wordService = wordService;
        this.appService = appService;
        this.messagingTemplate = messagingTemplate;
    }

    private static final Set<String> ADMIN_ACTIONS = Set.of(
            "TEST_ADMIN_ACTION", "START_GAME", "START_VOTING", "END_VOTING"
    );

    public boolean isAdminAction(PlayerAction action) {
        return ADMIN_ACTIONS.contains(action.getActionType());
    }

    public PlayerActionResult startGame(PlayerAction action, GameSession gameSession) {
        log.info("Starting game for session: {}", gameSession.getGameToken());
        if (gameSession.getCurrentState() != GameState.WAITING_FOR_PLAYERS) {
            log.error("Invalid game state: {}", gameSession.getCurrentState());
            throw new IllegalStateException("Game session is not in a valid state to start");
        }
        List<Player> players = playerRepository.findByGameSession(gameSession);

        //////////// remember to change to 4 players ///////////////////
        if (players.size() < 1) {
            ///////////////////////////////////////////////////////////////////
            throw new IllegalStateException("Not enough players to start the game");
        }
        gameSession.setCurrentState(GameState.STARTED);

        // Generate and set the secret word
        String secretWord = wordService.getRandomWord();
        gameSession.setSecretWord(secretWord);

        // generate random player order list
        List<Integer> playerOrder = IntStream.range(0, players.size())
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(playerOrder);
        log.info("Player IDs: {}", players.stream().map(Player::getId).collect(Collectors.toList()));
        log.info("Player order: {}", playerOrder);
        // generate random player roles
        List<Boolean> playerRoles = new ArrayList<>(Collections.nCopies(players.size(), false));
        playerRoles.set(0, true);
        Collections.shuffle(playerRoles);
        // for each player set the next player and the isChameleon
        for (int i = 0; i < players.size(); i++) {
            Player currentPlayer = players.get(playerOrder.get(i));
            // last player has no next player
            if (i < playerRoles.size() - 1) {
                Player nextPlayer = players.get(playerOrder.get(i + 1));
                currentPlayer.setNextPlayer(nextPlayer);
                log.info("Set next player for {}: {}", currentPlayer.getId(), nextPlayer.getId());
            }
            currentPlayer.setIsChameleon(playerRoles.get(i));
            playerRepository.save(currentPlayer);
        }
        // set the first player as the current player turn
        gameSession.setCurrentPlayerTurn(players.get(playerOrder.get(0)));
        // save and return
        gameSessionRepository.save(gameSession);
        PlayerActionResult result = new PlayerActionResult();
        result.setActionType(action.getActionType());
        return result;
    }

    public PlayerActionResult startVoting(PlayerAction action, GameSession gameSession) {
        if (gameSession.getCurrentState() != GameState.READY_FOR_VOTING) {
            throw new IllegalStateException("Game session is not in a valid state to start voting");
        }
        gameSession.setCurrentState(GameState.VOTING);
        gameSessionRepository.save(gameSession);

        int votingDuration = 30;
        long votingStartTime = System.currentTimeMillis();

        PlayerActionResult result = new PlayerActionResult();
        result.setActionType(action.getActionType());
        result.setActionContent("{\"votingStartTime\": " + votingStartTime + ", \"votingDuration\": " + votingDuration + "}");
        votingSessions.add(gameSession.getGameToken());
        scheduler.schedule(() -> endVoting(gameSession.getGameToken()), votingDuration, TimeUnit.SECONDS);
        return result;
    }

    public PlayerActionResult computeVotingResult(GameSession gameSession) {
        if (!votingSessions.remove(gameSession.getGameToken())) {
            log.info("Voting session already ended or not found for game token: {}", gameSession.getGameToken());
            return null;
        }
        List<Player> players = playerRepository.findByGameSession(gameSession);
        Map<Player, Long> voteCount = players.stream()
                .filter(p -> p.getCurrentAccusedPlayer() != null)
                .map(Player::getCurrentAccusedPlayer)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Player mostVotedPlayer = null;

        if (!voteCount.isEmpty()) {
            long maxVotes = voteCount.values().stream().max(Long::compareTo).orElse(0L);
            List<Player> mostVoted = voteCount.entrySet().stream()
                    .filter(e -> e.getValue() == maxVotes)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            mostVotedPlayer = mostVoted.get(ThreadLocalRandom.current().nextInt(mostVoted.size()));
        }

        PlayerActionResult result = new PlayerActionResult();
        result.setActionType("END_VOTING");

        if (mostVotedPlayer != null && mostVotedPlayer.getIsChameleon()) {
            result.setActionResult("CHAMELEON_FOUND");
            gameSession.setCurrentState(GameState.CHAMELEON_TURN);
        } else {
            result.setActionResult("CHAMELEON_WON");
            gameSession.setCurrentState(GameState.CHAMELEON_WIN);
        }
        gameSessionRepository.save(gameSession);
        return result;
    }

    public void endVoting(String gameToken) {
        GameSession gameSession = gameSessionRepository.findByGameToken(gameToken).orElse(null);
        if (gameSession == null || gameSession.getCurrentState() != GameState.VOTING) {
            return;
        }
        PlayerActionResult result = computeVotingResult(gameSession);
        if (result != null) {
            messagingTemplate.convertAndSend("/game/topic/" + gameToken, result);
        }
    }

    public PlayerActionResult doVote(Player player, PlayerAction action, GameSession gameSession) {
        if (gameSession.getCurrentState() != GameState.VOTING) {
            throw new IllegalStateException("Game session is not in a valid state to vote");
        }
        String accused_username = action.getActionContent();
        // find player with matching username
        List<Player> players = playerRepository.findByGameSession(gameSession);
        Player accusedPlayer = players.stream()
                .filter(p -> p.getUser().getUsername().equals(accused_username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Accused player not found"));
        // if (accusedPlayer == player) {
        //     throw new IllegalArgumentException("Player cannot vote for themselves");
        // }
        player.setCurrentAccusedPlayer(accusedPlayer);
        playerRepository.save(player);
        // check if all players have voted
        boolean allPlayersVoted = players.stream()
                .allMatch(p -> p.getCurrentAccusedPlayer() != null);

        if (!allPlayersVoted) {
            PlayerActionResult result = new PlayerActionResult();
            result.setActionType(action.getActionType());
            result.setActionContent(action.getActionContent());
            return result;
        }

        // if voting phase has already ended, this will return null
        return computeVotingResult(gameSession);
    }

    public PlayerActionResult giveHint(Player player, PlayerAction action, GameSession gameSession) {
        if (gameSession.getCurrentState() != GameState.STARTED) {
            throw new IllegalStateException("Game session is not in a valid state to give a hint");
        }
        // check if this is the player turn
        if (!gameSession.getCurrentPlayerTurn().getId().equals(player.getId())) {
            throw new IllegalStateException("Wrong player turn");
        }

        String hint = action.getActionContent();

        // check if the hint is null or empty
        if (hint == null || hint.trim().isEmpty()) {
            throw new IllegalStateException("Hint cannot be empty");
        }

        // check if hint contains only one word
        if (hint.split("\\s+").length > 1) {
            throw new IllegalStateException("Hint must be a single word");
        }

        hint = hint.toLowerCase();
        String secretWord = gameSession.getSecretWord().toLowerCase();

        // check if the hint is a substring of the secret word
        if (hint.contains(secretWord)) {
            throw new IllegalStateException("Hint cannot contain parts of the secret word");
        }

        player.setGivenHint(hint);
        playerRepository.save(player);
        // set the next player turn
        Player nextPlayer = player.getNextPlayer();
        gameSession.setCurrentPlayerTurn(nextPlayer);
        // nextPlayer is null -> all players have given their hint, time to vote!
        if (nextPlayer == null) {
            gameSession.setCurrentState(GameState.READY_FOR_VOTING);
            log.info("All players have given their hints, ready for voting");
        } else {
            log.info("Next player: {}", nextPlayer.getId());
        }
        gameSessionRepository.save(gameSession);
        // return the result
        PlayerActionResult result = new PlayerActionResult();
        result.setActionType(action.getActionType());
        result.setActionContent(action.getActionContent());
        return result;
    }

    public PlayerActionResult handleChameleonGuess(Player player, PlayerAction action, GameSession gameSession) {
        if (gameSession.getCurrentState() != GameState.CHAMELEON_TURN) {
            throw new IllegalStateException("Game session is not in a valid state for a chameleon guess");
        }
        String guess = action.getActionContent();
        // check if the guess is null or empty
        if (guess == null || guess.trim().isEmpty()) {
            throw new IllegalStateException("Hint cannot be empty");
        }
        // check that guess contains only one word
        if (guess.split("\\s+").length > 1) {
            throw new IllegalStateException("Hint must be a single word");
        }

        guess = guess.toLowerCase();
        String secretWord = gameSession.getSecretWord().toLowerCase();

        PlayerActionResult result = new PlayerActionResult();
        result.setActionType(action.getActionType());
        result.setActionContent(action.getActionContent());
        boolean chameleon_win = guess.equals(secretWord);
        result.setActionResult(chameleon_win ? "CHAMELEON_WIN" : "PLAYERS_WIN");

        gameSession.setCurrentState(chameleon_win ? GameState.CHAMELEON_WIN : GameState.PLAYERS_WIN);
        gameSessionRepository.save(gameSession);

        endGame(gameSession);
        return result;
    }

    public PlayerActionResult handlePlayerAction(User user, PlayerAction action, GameSession gameSession) throws Exception {
        log.info("Handling player action: {}", action.getActionType());
        log.info("User attempting action - ID: {}, Username: {}", user.getId(), user.getUsername());
        log.info("Game creator - ID: {}, Username: {}", gameSession.getCreator().getId(), gameSession.getCreator().getUsername());

        // get the player performing the action
        Player player = playerRepository.findByUserAndGameSession(user, gameSession).orElseThrow(
                () -> new Exception("User not part of the game session")
        );
        log.info("Found player in game session, ID: {}, Username: {}", player.getId(), player.getUser().getUsername());

        // in case of an admin action, check if the user is the creator
        if (isAdminAction(action) && !gameSession.getCreator().equals(user)) {
            log.error("User is not game creator");
            throw new Exception("Only the game session creator can perform this action");
        }

        switch (action.getActionType()) {
            case "TEST_ACTION" -> {
                PlayerActionResult result = new PlayerActionResult();
                result.setActionType(action.getActionType());
                return result;
            }
            case "TEST_ADMIN_ACTION" -> {
                PlayerActionResult result = new PlayerActionResult();
                result.setActionType(action.getActionType());
                return result;
            }
            case "START_GAME" -> {
                log.info("Processing START_GAME action");
                return startGame(action, gameSession);
            }
            case "GIVE_HINT" -> {
                return giveHint(player, action, gameSession);
            }
            case "START_VOTING" -> {
                return startVoting(action, gameSession);
            }
            case "VOTE" -> {
                return doVote(player, action, gameSession);
            }
            case "CHAMELEON_GUESS" -> {
                return handleChameleonGuess(player, action, gameSession);
            }
            default -> {
                throw new IllegalArgumentException("Invalid action type: " + action.getActionType());
            }
        }
    }

    public void endGame(GameSession gameSession) {
        // Get all players in the game
        List<Player> players = playerRepository.findByGameSession(gameSession);
        
        // Increment rounds played for each player
        for (Player player : players) {
            User user = player.getUser();
            appService.incrementRoundsPlayed(user);

            if (gameSession.getCurrentState() == GameState.CHAMELEON_WIN && player.getIsChameleon()) {
                appService.incrementWins(user);  // Chameleon wins
            }
            else if ((gameSession.getCurrentState() == GameState.PLAYERS_WIN ||
                      gameSession.getCurrentState() == GameState.CHAMELEON_TURN) && !player.getIsChameleon()) {
                appService.incrementWins(user);  // Players win
            }
    
        }
        
    }
}
