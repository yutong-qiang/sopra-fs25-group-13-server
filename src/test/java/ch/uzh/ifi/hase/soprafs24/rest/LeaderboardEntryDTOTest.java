package ch.uzh.ifi.hase.soprafs24.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardEntryDTO;
public class LeaderboardEntryDTOTest {

    @Test
    public void testLeaderboardEntryDTO() {
        // given
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        
        // when
        dto.setId(1L);
        dto.setUsername("testUser");
        dto.setWins(10);
        dto.setRoundsPlayed(20);
        dto.setWinRate(0.5);
        byte[] avatar = new byte[]{1, 2, 3};
        dto.setAvatar(avatar);

        // then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("testUser", dto.getUsername());
        assertEquals(10, dto.getWins());
        assertEquals(20, dto.getRoundsPlayed());
        assertEquals(0.5, dto.getWinRate());
        assertEquals(avatar, dto.getAvatar());
    }
}