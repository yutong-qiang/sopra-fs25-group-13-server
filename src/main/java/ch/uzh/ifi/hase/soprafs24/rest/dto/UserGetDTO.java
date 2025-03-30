package ch.uzh.ifi.hase.soprafs24.rest.dto;

// import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

public class UserGetDTO {

  private Long id;
  private String name;
  private String username;
  // private UserStatus status;
  private String token;
  private int wins;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public int getWins() {
    return wins;
  }

  public void setWins(int wins) {
    this.wins = wins;
  }

  // public UserStatus getStatus() {
  //   return status;
  // }

  // public void setStatus(UserStatus status) {
  //   this.status = status;
  // }
}
