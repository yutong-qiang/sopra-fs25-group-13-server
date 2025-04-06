package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GameSessionGetDTO {
    private Long gameSessionId;
    private String gameToken;
    private String twilioVideoChatToken;
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
}