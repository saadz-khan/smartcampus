package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

public class GUIAgent extends Agent {
    private JFrame frame;
    private Map<String, AID> agentAIDs = new HashMap<>();
    private JTextArea notificationsArea;

    @Override
    protected void setup() {
        SwingUtilities.invokeLater(this::createAndShowGUI);

        // Retrieve AIDs of other agents
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                registerAgent("UserAgent");
                registerAgent("BookingAgent");
                registerAgent("NotificationAgent");
                registerAgent("NavigationAgent");
            }
        });

        // Add behaviour to listen for notifications
        addBehaviour(new NotificationListenerBehaviour());
    }

    private void registerAgent(String agentName) {
        AID targetAgent = new AID(agentName, AID.ISLOCALNAME);
        agentAIDs.put(agentName, targetAgent);
        System.out.println("Registered AID for " + agentName + ": " + targetAgent);
    }

    private void createAndShowGUI() {
        frame = new JFrame("Smart Campus GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Panels
        JPanel userPanel = createUserManagementPanel();
        tabbedPane.addTab("User Management", userPanel);

        JPanel bookingPanel = createFacilityBookingPanel();
        tabbedPane.addTab("Facility Booking", bookingPanel);

        JPanel navigationPanel = createNavigationPanel();
        tabbedPane.addTab("Navigation", navigationPanel);

        JPanel notificationPanel = createNotificationPanel();
        tabbedPane.addTab("Notifications", notificationPanel);

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 2));
        JTextField studentIdField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JButton registerButton = new JButton("Register");

        panel.add(new JLabel("Student ID:"));
        panel.add(studentIdField);
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel(""));
        panel.add(registerButton);

        registerButton.addActionListener(e -> {
            String studentId = studentIdField.getText();
            String name = nameField.getText();
            String email = emailField.getText();

            JSONObject request = new JSONObject();
            request.put("studentId", studentId);
            request.put("name", name);
            request.put("email", email);

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(agentAIDs.get("UserAgent")); // Use AID for UserAgent
            msg.setContent(request.toString());
            send(msg);

            // Add behaviour to listen for response
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    ACLMessage reply = blockingReceive();
                    if (reply != null) {
                        SwingUtilities.invokeLater(() -> {
                            notificationsArea.append("User Registration: " + reply.getContent() + "\n");
                        });
                    }
                }
            });
        });

        return panel;
    }

    private JPanel createFacilityBookingPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 2));
        JTextField studentIdField = new JTextField();
        JTextField dateField = new JTextField();
        JTextField timeSlotField = new JTextField();
        JTextField capacityField = new JTextField();
        JTextArea availableRoomsArea = new JTextArea(5, 20);

        JButton checkAvailabilityButton = new JButton("Check Availability");
        JButton bookRoomButton = new JButton("Book Room");

        panel.add(new JLabel("Student ID:"));
        panel.add(studentIdField);
        panel.add(new JLabel("Date (YYYY-MM-DD):"));
        panel.add(dateField);
        panel.add(new JLabel("Time Slot:"));
        panel.add(timeSlotField);
        panel.add(new JLabel("Capacity:"));
        panel.add(capacityField);
        panel.add(checkAvailabilityButton);
        panel.add(bookRoomButton);
        panel.add(new JLabel("Available Rooms:"));
        panel.add(new JScrollPane(availableRoomsArea));

        checkAvailabilityButton.addActionListener(e -> {
            String date = dateField.getText();
            String timeSlot = timeSlotField.getText();
            int capacity = Integer.parseInt(capacityField.getText());

            JSONObject request = new JSONObject();
            request.put("date", date);
            request.put("timeSlot", timeSlot);
            request.put("capacity", capacity);

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(agentAIDs.get("BookingAgent")); // Use AID for BookingAgent
            msg.setContent(request.toString());
            send(msg);

            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    ACLMessage reply = blockingReceive();
                    if (reply != null) {
                        SwingUtilities.invokeLater(() -> {
                            notificationsArea.append("Facility Booking: " + reply.getContent() + "\n");
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                availableRoomsArea.setText(reply.getContent());
                            }
                        });
                    }
                }
            });
        });

        return panel;
    }

    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2));
        JTextField currentLocationField = new JTextField();
        JTextField roomNumberField = new JTextField();
        JTextArea directionsArea = new JTextArea(5, 20);
        JButton getDirectionsButton = new JButton("Get Directions");

        panel.add(new JLabel("Current Location:"));
        panel.add(currentLocationField);
        panel.add(new JLabel("Room Number:"));
        panel.add(roomNumberField);
        panel.add(getDirectionsButton);
        panel.add(new JLabel());
        panel.add(new JLabel("Directions:"));
        panel.add(new JScrollPane(directionsArea));

        getDirectionsButton.addActionListener(e -> {
            String currentLocation = currentLocationField.getText();
            String roomNumber = roomNumberField.getText();

            JSONObject request = new JSONObject();
            request.put("currentLocation", currentLocation);
            request.put("roomNumber", roomNumber);

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(agentAIDs.get("NavigationAgent")); // Use AID for NavigationAgent
            msg.setContent(request.toString());
            send(msg);

            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    ACLMessage reply = blockingReceive();
                    if (reply != null) {
                        SwingUtilities.invokeLater(() -> {
                            notificationsArea.append("Navigation Directions: " + reply.getContent() + "\n");
                            directionsArea.setText(reply.getContent());
                        });
                    }
                }
            });
        });

        return panel;
    }

    private JPanel createNotificationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        notificationsArea = new JTextArea();
        notificationsArea.setEditable(false);
        panel.add(new JLabel("Notifications:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(notificationsArea), BorderLayout.CENTER);
        return panel;
    }

    private class NotificationListenerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                SwingUtilities.invokeLater(() -> {
                    notificationsArea.append("Notification: " + msg.getContent() + "\n");
                });
            } else {
                block();
            }
        }
    }
}
