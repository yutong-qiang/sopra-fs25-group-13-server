package ch.uzh.ifi.hase.soprafs24.websocket;

public class PlayerActionResult {

    private String actionType;
    private String actionContent;
    private String actionResult;
    private String username;
    private String chameleonUsername;

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

    public String getActionResult() {
        return actionResult;
    }

    public void setActionResult(String actionResult) {
        this.actionResult = actionResult;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getChameleonUsername() {
        return chameleonUsername;
    }

    public void setChameleonUsername(String chameleonUsername) {
        this.chameleonUsername = chameleonUsername;
    }
}
