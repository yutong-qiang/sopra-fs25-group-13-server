package ch.uzh.ifi.hase.soprafs24.websocket;

public class PlayerAction {
    private Long userId;
    private String gameSessionToken;
    private String actionType;
    private String actionContent;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGameSessionToken() {
        return gameSessionToken;
    }

    public void setGameSessionToken(String gameSessionToken) {
        this.gameSessionToken = gameSessionToken;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionContent() {
        return actionContent;
    }

    public void setActionContent(String actionContent) {
        this.actionContent = actionContent;
    }
}