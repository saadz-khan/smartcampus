package agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.JSONObject;

public class UserManagementAgent extends Agent {
    private JSONObject userDatabase;
    
    protected void setup() {
        // Initialize user database
        userDatabase = new JSONObject();
        
        // Register with Directory Facilitator
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("user-management");
        sd.setName("smart-campus-user-management");
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        // Add registration behavior
        addBehaviour(new StudentRegistrationBehavior());
    }
    
    private class StudentRegistrationBehavior extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                try {
                    JSONObject request = new JSONObject(msg.getContent());
                    String studentId = request.getString("studentId");
                    
                    ACLMessage reply = msg.createReply();
                    
                    // Validate student ID format
                    if (!isValidStudentId(studentId)) {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Invalid student ID format");
                    }
                    // Check if student ID already exists
                    else if (userDatabase.has(studentId)) {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Student ID already exists");
                    }
                    // Create new user profile
                    else {
                        userDatabase.put(studentId, request);
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("Registration successful");
                    }
                    
                    send(reply);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
        
        private boolean isValidStudentId(String studentId) {
            // Implement student ID validation logic
            return studentId.matches("^\\d{8}$"); // Example: 8-digit ID
        }
    }
}
