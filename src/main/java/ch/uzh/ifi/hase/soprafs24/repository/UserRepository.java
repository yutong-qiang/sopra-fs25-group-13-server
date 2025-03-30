package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, Long> {
  User findByName(String name);

  User findByUsername(String username);

  boolean existsByUsername(String username); 
  
  boolean existsByToken(String token);

  Optional<User> findByToken(String token);
}
