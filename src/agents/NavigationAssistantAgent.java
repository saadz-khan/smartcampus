package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NavigationAssistantAgent extends Agent {
    // Simulated room database
    private static final Map<String, RoomInfo> roomDatabase = new HashMap<>();

    @Override
    protected void setup() {
        // Initialize room database
        roomDatabase.put("101", new RoomInfo("Building A", 1, "Take elevator to floor 1, turn right, room is on the left", "2 minutes"));
        roomDatabase.put("102", new RoomInfo("Building B", 2, "Walk to Building B, take stairs to floor 2, room is straight ahead", "5 minutes"));

        // Register this agent with the Directory Facilitator (DF)
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("navigation");
            sd.setName("smart-campus-navigation");
            dfd.addServices(sd);

            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Add behavior to handle navigation requests
        addBehaviour(new NavigationBehavior());
    }

    private class NavigationBehavior extends CyclicBehaviour {
        @Override
        public void action() {
            // Template to filter REQUEST messages
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                try {
                    // Parse the incoming message as a JSON object
                    JSONObject request = new JSONObject(msg.getContent());
                    String roomNumber = request.getString("roomNumber");
                    String currentLocation = request.getString("currentLocation");
                    String userId = request.optString("userId", "Unknown");

                    // Generate response based on room availability
                    JSONObject response = generateDirections(currentLocation, roomNumber);

                    // Send the reply back
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(response.toString());
                    send(reply);

                    // Send notification
                    sendNotification(userId, response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block(); // Wait for the next message
            }
        }

        // Method to generate directions based on room number
        private JSONObject generateDirections(String from, String roomNumber) {
            JSONObject response = new JSONObject();

            if (roomDatabase.containsKey(roomNumber)) {
                RoomInfo roomInfo = roomDatabase.get(roomNumber);
                response.put("building", roomInfo.building);
                response.put("floor", roomInfo.floor);
                response.put("directions", roomInfo.directions);
                response.put("estimatedTime", roomInfo.estimatedTime);
                response.put("from", from);
                response.put("to", roomNumber);
            } else {
                response.put("error", "Room " + roomNumber + " not found in the database.");
            }

            return response;
        }

        // Method to send a notification to the NotificationAgent
        private void sendNotification(String userId, JSONObject response) {
            ACLMessage notification = new ACLMessage(ACLMessage.REQUEST);
            AID notificationAgent = new AID("NotificationAgent", AID.ISLOCALNAME); // Assumes NotificationAgent's local name
            notification.addReceiver(notificationAgent);

            JSONObject notificationContent = new JSONObject();
            notificationContent.put("type", "navigation");
            notificationContent.put("userId", userId);

            if (response.has("error")) {
                notificationContent.put("message", "Navigation failed: " + response.getString("error"));
            } else {
                notificationContent.put("message", "Directions to " + response.getString("to") + " provided successfully.");
            }

            notification.setContent(notificationContent.toString());
            send(notification);
        }
    }

    @Override
    protected void takeDown() {
        // Deregister from the DF when the agent is taken down
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("NavigationAssistantAgent " + getAID().getName() + " terminating.");
    }

    // Inner class to represent room information
    private static class RoomInfo {
        String building;
        int floor;
        String directions;
        String estimatedTime;

        RoomInfo(String building, int floor, String directions, String estimatedTime) {
            this.building = building;
            this.floor = floor;
            this.directions = directions;
            this.estimatedTime = estimatedTime;
        }
    }
}
