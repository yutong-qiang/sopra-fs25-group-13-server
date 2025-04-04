package ch.uzh.ifi.hase.soprafs24.websocket;

public class GameSessionErrorMessage {

    private String errorMessage;

    // public GameSessionErrorMessages(String errorMessage) {
    //     this.errorMessage = errorMessage;
    // }
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
