package ch.uzh.ifi.hase.soprafs24.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "Player")
public class Player implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @Column(nullable = false)
    private boolean isChameleon;

    @ManyToOne
    @JoinColumn(name = "next_player_id")
    private Player nextPlayer;

    @ManyToOne
    @JoinColumn(name = "accused_player_id")
    private Player currentAccusedPlayer;

    private String givenHint;

    // Getters and setters for the fields
    public Long geId() {
        return id;
    }

    public void seId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    public Player getCurrentAccusedPlayer() {
        return currentAccusedPlayer;
    }

    public void setCurrentAccusedPlayer(Player currentAccusedPlayer) {
        this.currentAccusedPlayer = currentAccusedPlayer;
    }

    public boolean getIsChameleon() {
        return isChameleon;
    }

    public void setIsChameleon(boolean isChameleon) {
        this.isChameleon = isChameleon;
    }

    public String getGivenHint() {
        return givenHint;
    }

    public void setGivenHint(String givenHint) {
        this.givenHint = givenHint;
    }

    public Player getNextPlayer() {
        return nextPlayer;
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
    }
}
