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

public class NotificationAgent extends Agent {
    @Override
    protected void setup() {
        // Register this agent with the Directory Facilitator (DF)
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("notification");
            sd.setName("smart-campus-notifications");
            dfd.addServices(sd);

            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Add the behavior to handle notifications
        addBehaviour(new NotificationBehavior());
    }

    // Inner class to handle incoming requests
    private class NotificationBehavior extends CyclicBehaviour {
        @Override
        public void action() {
            // Template to filter REQUEST messages
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                try {
                    // Parse the incoming message as a JSON object
                    JSONObject notification = new JSONObject(msg.getContent());
                    String type = notification.getString("type");
                    String userId = notification.getString("userId");
                    String message = notification.getString("message");

                    // Handle the notification
                    sendNotification(userId, type, message);

                    // Send a reply back to the sender
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Notification sent successfully");
                    send(reply);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block(); // Wait for the next message
            }
        }

        // Method to send a notification
        private void sendNotification(String userId, String type, String message) {
            // Print the notification to the console
            //System.out.println("Notification for user " + userId + ": " + message);

            // Forward the notification to the GUIAgent
            ACLMessage guiMessage = new ACLMessage(ACLMessage.INFORM);
            AID guiAgent = new AID("GUIAgent", AID.ISLOCALNAME); // Assumes GUIAgent's local name
            guiMessage.addReceiver(guiAgent);

            JSONObject guiNotification = new JSONObject();
            guiNotification.put("userId", userId);
            guiNotification.put("type", type);
            guiNotification.put("message", message);

            guiMessage.setContent(guiNotification.toString());
            send(guiMessage);
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
        System.out.println("NotificationAgent " + getAID().getName() + " terminating.");
    }
}
