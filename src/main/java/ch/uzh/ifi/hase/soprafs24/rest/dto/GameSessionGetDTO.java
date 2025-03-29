package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GameSessionGetDTO {
    private Long gameSessionId;
    private String gameToken;

    public Long getGameSessionId() {
        return gameSessionId;
    }

    public void setGameSessionId(Long gameSessionId) {
        this.gameSessionId = gameSessionId;
    }

    public String getGameToken() {
        return gameToken;
    }

    public void setGameToken(String gameToken) {
        this.gameToken = gameToken;
    }
}