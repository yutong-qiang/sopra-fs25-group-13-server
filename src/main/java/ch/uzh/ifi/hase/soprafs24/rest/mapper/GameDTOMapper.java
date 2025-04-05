package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs24.entity.GameSession;
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


  @Mapping(source = "id", target = "gameSessionId")
  @Mapping(source = "gameToken", target = "gameToken")
  @Mapping(source = "twilioRoomSid", target = "twilioRoomSid")
  GameSessionGetDTO convertEntityToGameSessionGetDTO(GameSession gameSession);
}