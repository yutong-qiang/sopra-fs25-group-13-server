package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.AppRepository;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
public class AppServiceIntegrationTest {

  @Qualifier("appRepository")
  @Autowired
  private AppRepository appRepository;

  @Autowired
  private AppService appService;

  @BeforeEach
  public void setup() {
    appRepository.deleteAll();
  }

  @Test
  public void createUser_validInputs_success() {
    // given
    assertNull(appRepository.findByUsername("testUsername"));

    User testUser = new User();
    testUser.setName("testName");
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");

    // when
    User createdUser = appService.createUser(testUser);

    // then
    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getName(), createdUser.getName());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
  }

  @Test
  public void createUser_duplicateUsername_throwsException() {
    assertNull(appRepository.findByUsername("testUsername"));

    User testUser = new User();
    testUser.setName("testName");
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
    User createdUser = appService.createUser(testUser);

    // attempt to create second user with same username
    User testUser2 = new User();

    // change the name but forget about the username
    testUser2.setName("testName2");
    testUser2.setUsername("testUsername");
    testUser2.setPassword("testPassword2");

    // check that an error is thrown
    assertThrows(ResponseStatusException.class, () -> appService.createUser(testUser2));
  }
}
