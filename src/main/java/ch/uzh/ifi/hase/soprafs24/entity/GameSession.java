package ch.uzh.ifi.hase.soprafs24.entity;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;

@Entity
@Table(name = "GameSession")
public class GameSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String gameToken;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne
    @JoinColumn(name = "current_player_id", nullable = false)
    private Player currentPlayerTurn;

    @Column(nullable = true)
    private String twilioRoomSid;

    @Column(nullable = true, length = 1000)
    private String twilioVideoChatToken;

    @Column(nullable = true)
    private GameState currentState;

    @Column(nullable = true)
    private String secretWord; //secret word to be given to non-chameleon players

    @ElementCollection
    private Map<Long, Long> votes; //Map<UserId, UserId>

    // Getters and Setters
    public Long getId() {
        return id;
    }

    // Getters and Setters
    public void setId(Long id) {
        this.id = id;
    }

    public String getGameToken() {
        return gameToken;
    }

    public void setGameToken(String gameToken) {
        this.gameToken = gameToken;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getTwilioVideoChatToken() {
        return twilioVideoChatToken;
    }

    public void setTwilioVideoChatToken(String twilioVideoChatToken) {
        this.twilioVideoChatToken = twilioVideoChatToken;
    }

    public String getTwilioRoomSid() {
        return twilioRoomSid;
    }

    public void setTwilioRoomSid(String twilioRoomSid) {
        this.twilioRoomSid = twilioRoomSid;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameState currentState) {
        this.currentState = currentState;
    }

    public String getSecretWord() {
        return secretWord;
    }

    public void setSecretWord(String secretWord) {
        this.secretWord = secretWord;
    }

    public Map<Long, Long> getVotes() {
        return votes;
    }

    public void setVotes(Map<Long, Long> votes) {
        this.votes = votes;
    }

    public Player getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }

    public void setCurrentPlayerTurn(Player currentPlayerTurn) {
        this.currentPlayerTurn = currentPlayerTurn;
    }
}
