package ch.uzh.ifi.hase.soprafs24.websocket;

public class PlayerAction {

    private String gameSessionToken;
    private String actionType;
    private String actionContent;

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
