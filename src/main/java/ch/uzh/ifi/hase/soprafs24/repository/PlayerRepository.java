package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;

@Repository("playerRepository")
public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByUser(User user);
    List<Player> findByGameSession(GameSession gameSession);
    Optional<Player> findByUserAndGameSession(User user, GameSession gameSession);
    boolean existsByUserAndGameSession(User user, GameSession gameSession);
}
