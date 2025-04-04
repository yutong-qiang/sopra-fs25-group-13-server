package ch.uzh.ifi.hase.soprafs24.service;

import com.twilio.Twilio;
import com.twilio.rest.video.v1.Room;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

// if we want to implement multiple rooms --> add a map here to match lobby name and unique id or sth....

@Service
public class TwilioService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;


    public String createVideoRoom(String gameToken) {
        try{
        Twilio.init(accountSid, authToken);
        
        Room room = Room.creator()
                .setUniqueName(gameToken)  // Use gameToken as room name
                .setType(Room.RoomType.GROUP)
                .setMaxParticipants(8)
                .setRecordParticipantsOnConnect(false)
                .create();
        
        System.out.println("Created Twilio room: " + room.getSid());
        return room.getSid();
        }  
        
        catch (Exception e) {
            System.out.println("Error creating Twilio room: " + e.getMessage());
            return "RM" + UUID.randomUUID().toString().substring(0, 10);
        }
        // String mockRoomSid = "RM" + UUID.randomUUID().toString().substring(0, 10);
        // System.out.println("Created mock video room: " + mockRoomSid);
        // return mockRoomSid;
    }

    public void closeVideoRoom(String roomSid) {
        if (roomSid != null) {
            try {
                Twilio.init(accountSid, authToken);
                Room.updater(roomSid, Room.RoomStatus.COMPLETED).update();
            }
            catch (Exception e) {
                System.out.println("Error closing video room: " + e.getMessage());
            }
        }
        System.out.println("Video room closed: " + roomSid);
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