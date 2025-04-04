package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GameSessionGetDTO {
    private Long gameSessionId;
    private String gameToken;
    private String twilioRoomSid;

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

    public String getTwilioRoomSid() {
        return twilioRoomSid;
    }

    public void setTwilioRoomSid(String twilioRoomSid) {
        this.twilioRoomSid = twilioRoomSid;
    } 
}