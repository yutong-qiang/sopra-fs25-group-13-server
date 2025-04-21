package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameSessionGetDTO;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface GameDTOMapper {

  GameDTOMapper INSTANCE = Mappers.getMapper(GameDTOMapper.class);

  @Mapping(target = "gameSessionId", source = "gameSession.id")
  @Mapping(target = "gameToken", source = "gameSession.gameToken")
  @Mapping(target = "twilioVideoChatToken", source = "player.twilioToken")
  @Mapping(target = "gameState", source = "gameSession.currentState")
  @Mapping(target = "currentTurn", source = "gameSession.currentPlayerTurn.user.username")
  @Mapping(target = "role", expression = "java(player.getIsChameleon() ? \"CHAMELEON\" : \"NORMAL\")")
  GameSessionGetDTO convertEntityToGameSessionGetDTO(GameSession gameSession, Player player);
}