package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.twilio.Twilio;
import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VideoGrant;
import com.twilio.rest.video.v1.Room;

// if we want to implement multiple rooms --> add a map here to match lobby name and unique id or sth....
@Service
public class TwilioService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.api.key}")
    private String apiKey;

    @Value("${twilio.api.secret}")
    private String apiSecret;

    public record TwilioRoomInfo(String roomSid, String token) {

    }

    public TwilioRoomInfo createVideoRoom(String gameToken) {
        try {
            Twilio.init(accountSid, authToken);

            Room room = Room.creator()
                    .setUniqueName(gameToken) // Use gameToken as room name
                    .setType(Room.RoomType.GROUP)
                    .setMaxParticipants(8)
                    .setRecordParticipantsOnConnect(false)
                    .create();

            String token = generateToken(gameToken, room.getSid());
            System.out.println("Created Twilio room: " + room.getSid() + " with token: " + token);

            return new TwilioRoomInfo(room.getSid(), token);

        } catch (Exception e) {
            System.out.println("Error creating Twilio room: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create video room: " + e.getMessage());
        }
        // String mockRoomSid = "RM" + UUID.randomUUID().toString().substring(0, 10);
        // System.out.println("Created mock video room: " + mockRoomSid);
        // return mockRoomSid;
    }

    private String generateToken(String identity, String roomId) {
        try {
            VideoGrant grant = new VideoGrant();
            grant.setRoom(roomId);

            AccessToken token = new AccessToken.Builder(accountSid, apiKey, apiSecret)
                    .identity(identity)
                    .grant(grant)
                    .build();

            return token.toJwt();
        } catch (Exception e) {
            System.out.println("Error generating Twilio token: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate video token: " + e.getMessage());
        }
    }

    public void closeVideoRoom(String roomSid) {
        if (roomSid != null) {
            try {
                Twilio.init(accountSid, authToken);
                Room.updater(roomSid, Room.RoomStatus.COMPLETED).update();
            } catch (Exception e) {
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
