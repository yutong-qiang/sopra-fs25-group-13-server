package ch.uzh.ifi.hase.soprafs24.rest.dto;

// import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
public class PlayerGetDTO {

    private String username;
    private String givenHint;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGivenHint() {
        return givenHint;
    }

    public void setGivenHint(String givenHint) {
        this.givenHint = givenHint;
    }

}
