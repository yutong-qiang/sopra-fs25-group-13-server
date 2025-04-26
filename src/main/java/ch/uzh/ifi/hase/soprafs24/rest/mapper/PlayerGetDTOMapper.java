package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerGetDTO;

/**
 * DTOMapper This class is responsible for generating classes that will
 * automatically transform/map the internal representation of an entity (e.g.,
 * the User) to the external/API representation (e.g., UserGetDTO for getting,
 * UserPostDTO for creating) and vice versa. Additional mappers can be defined
 * for new entities. Always created one mapper for getting information (GET) and
 * one mapper for creating information (POST).
 */
@Mapper
public interface PlayerGetDTOMapper {

    PlayerGetDTOMapper INSTANCE = Mappers.getMapper(PlayerGetDTOMapper.class);

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "givenHint", target = "givenHint")
    PlayerGetDTO convertEntityToPlayerGetDTO(Player player);
}
