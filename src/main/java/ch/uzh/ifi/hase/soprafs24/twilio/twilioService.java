package ch.uzh.ifi.hase.soprafs24.twilio;

import com.twilio.Twilio;
import com.twilio.rest.video.v1.Room;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// if we want to implement multiple rooms --> add a map here to match lobby name and unique id or sth....

@Service
public class twilioService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    private String activeLobbyId = null; // Stores the single active lobby ID
    private String activeRoomSid = null; // Stores the single active Twilio Room SID

    public String createLobby(String adminId) {
        if (activeLobbyId != null) {
            return "A lobby is already active. Only one group can play at a time.";
        }
        
        // Initialize Twilio with credentials
        Twilio.init(accountSid, authToken);
        
        // Create a Twilio Video Room for the lobby
        String lobbyId = "Chameleon_Lobby";
        Room room = Room.creator()
                .setUniqueName(lobbyId)
                .setType(Room.RoomType.GO) // "go" means the room starts immediately
                .create();
        
        activeLobbyId = lobbyId;
        activeRoomSid = room.getSid();
        
        System.out.println("Lobby Created by Admin: " + adminId + " | Lobby ID: " + lobbyId);
        return lobbyId;
    }

    public String joinLobby() {
        if (activeLobbyId != null) {
            return activeRoomSid; // Return existing room SID
        }
        return "No active lobby available.";
    }

    public void closeLobby() {
        activeLobbyId = null;
        activeRoomSid = null;
        System.out.println("Lobby Closed");
    }
}


////////////////////////////////////////////////////////
// still need to add them to application.properties
// Auth Token
// Account SID

// add api for twilio in gameService
// @/game/join
// @/game
// (another one for closing the video)

// but waiting for game service & controller to be finished first
////////////////////////////////////////////////////////