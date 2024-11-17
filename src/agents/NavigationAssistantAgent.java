package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.json.JSONObject;

public class NavigationAssistantAgent extends Agent {
    @Override
    protected void setup() {
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

                    // Calculate directions
                    JSONObject directions = calculateDirections(currentLocation, roomNumber);

                    // Send the reply back with directions
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(directions.toString());
                    send(reply);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block(); // Wait for the next message
            }
        }

        // Method to calculate directions
        private JSONObject calculateDirections(String from, String to) {
            JSONObject directions = new JSONObject();
            directions.put("building", "Building A");
            directions.put("floor", 1);
            directions.put("directions", "Take elevator to floor 1, turn right, room is on the left");
            directions.put("estimatedTime", "2 minutes");
            return directions;
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
}
