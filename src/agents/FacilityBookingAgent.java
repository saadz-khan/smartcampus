package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.sql.*;

public class FacilityBookingAgent extends Agent {
    private Connection connection;
    private AID userManagementAgent;
    private AID notificationAgent;

    @Override
    protected void setup() {
        try {
            // Connect to SQLite database
            connection = DriverManager.getConnection("jdbc:sqlite:smartcampus.db");
            System.out.println("Connected to SQLite database.");

            // Initialize database schema and populate initial data
            initializeDatabase();

            // Register the agent's service with the Directory Facilitator (DF)
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("facility-booking");
            sd.setName("smart-campus-room-booking");
            dfd.addServices(sd);

            DFService.register(this, dfd);

            // Find the UserManagementAgent
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription userSD = new ServiceDescription();
                userSD.setType("user-management");
                template.addServices(userSD);

                DFAgentDescription[] result = DFService.search(this, template);
                if (result.length > 0) {
                    userManagementAgent = result[0].getName();
                    System.out.println("Found UserManagementAgent: " + userManagementAgent.getName());
                } else {
                    System.out.println("UserManagementAgent not found");
                }
            } catch (FIPAException e) {
                e.printStackTrace();
            }

            // Find the NotificationAgent
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription notifSD = new ServiceDescription();
                notifSD.setType("notification");
                template.addServices(notifSD);

                DFAgentDescription[] result = DFService.search(this, template);
                if (result.length > 0) {
                    notificationAgent = result[0].getName();
                    System.out.println("Found NotificationAgent: " + notificationAgent.getName());
                } else {
                    System.out.println("NotificationAgent not found");
                }
            } catch (FIPAException e) {
                e.printStackTrace();
            }

            // Add behaviors for room availability, booking requests, and cancellations
            addBehaviour(new RoomAvailabilityBehavior());
            addBehaviour(new BookingRequestBehavior());
            addBehaviour(new CancelBookingBehavior());

        } catch (SQLException | FIPAException e) {
            e.printStackTrace();
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create rooms table
            String createRoomsTable = "CREATE TABLE IF NOT EXISTS rooms (" +
                    "room_number TEXT PRIMARY KEY, " +
                    "capacity INTEGER, " +
                    "location TEXT, " +
                    "floor INTEGER)";
            stmt.executeUpdate(createRoomsTable);

            // Create bookings table
            String createBookingsTable = "CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "room_number TEXT, " +
                    "date TEXT, " +
                    "time_slot TEXT, " +
                    "student_id TEXT, " +
                    "FOREIGN KEY(room_number) REFERENCES rooms(room_number))";
            stmt.executeUpdate(createBookingsTable);

            // Prepopulate rooms if not already populated
            String checkRooms = "SELECT COUNT(*) FROM rooms";
            ResultSet rs = stmt.executeQuery(checkRooms);
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Populating rooms with predefined data...");
                populateRooms();
            }
        }
    }

    private void populateRooms() throws SQLException {
        String insertRoomSQL = "INSERT INTO rooms (room_number, capacity, location, floor) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertRoomSQL)) {
            pstmt.setString(1, "101");
            pstmt.setInt(2, 4);
            pstmt.setString(3, "Building A");
            pstmt.setInt(4, 1);
            pstmt.executeUpdate();

            pstmt.setString(1, "102");
            pstmt.setInt(2, 10);
            pstmt.setString(3, "Building B");
            pstmt.setInt(4, 2);
            pstmt.executeUpdate();
        }
    }

    private class RoomAvailabilityBehavior extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                try {
                    JSONObject request = new JSONObject(msg.getContent());
                    String date = request.getString("date");
                    String timeSlot = request.getString("timeSlot");
                    int capacity = request.optInt("capacity", 0);

                    JSONArray availableRooms = new JSONArray();

                    String query = "SELECT * FROM rooms r WHERE r.capacity >= ? " +
                            "AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.room_number = r.room_number " +
                            "AND b.date = ? AND b.time_slot = ?)";
                    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                        pstmt.setInt(1, capacity);
                        pstmt.setString(2, date);
                        pstmt.setString(3, timeSlot);
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            JSONObject room = new JSONObject();
                            room.put("roomNumber", rs.getString("room_number"));
                            room.put("capacity", rs.getInt("capacity"));
                            room.put("location", rs.getString("location"));
                            room.put("floor", rs.getInt("floor"));
                            availableRooms.put(room);
                        }
                    }

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(availableRooms.toString());
                    send(reply);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

    private class BookingRequestBehavior extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                try {
                    JSONObject request = new JSONObject(msg.getContent());
                    String roomNumber = request.getString("roomNumber");
                    String date = request.getString("date");
                    String timeSlot = request.getString("timeSlot");
                    String studentId = request.getString("studentId");
                    String name = request.optString("name");   // Optional
                    String email = request.optString("email"); // Optional

                    // Check if the student is registered
                    ACLMessage query = new ACLMessage(ACLMessage.QUERY_REF);
                    query.addReceiver(userManagementAgent);
                    JSONObject queryContent = new JSONObject();
                    queryContent.put("studentId", studentId);
                    query.setContent(queryContent.toString());
                    send(query);

                    // Wait for the reply
                    MessageTemplate replyMt = MessageTemplate.MatchSender(userManagementAgent);
                    ACLMessage reply = blockingReceive(replyMt);

                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.FAILURE) {
                            // Student is not registered
                            // Attempt to register the student
                            ACLMessage registrationRequest = new ACLMessage(ACLMessage.REQUEST);
                            registrationRequest.addReceiver(userManagementAgent);
                            JSONObject registrationData = new JSONObject();
                            registrationData.put("studentId", studentId);
                            registrationData.put("name", name);
                            registrationData.put("email", email);
                            registrationRequest.setContent(registrationData.toString());
                            send(registrationRequest);

                            // Wait for the registration reply
                            ACLMessage regReply = blockingReceive(replyMt);

                            if (regReply != null && regReply.getPerformative() == ACLMessage.INFORM) {
                                // Registration successful, send notification
                                sendNotification(studentId, "Registration", "Registration successful");
                                System.out.println("Student registered successfully.");
                            } else {
                                // Registration failed
                                ACLMessage replyMsg = msg.createReply();
                                replyMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                replyMsg.setContent("Registration failed: " + regReply.getContent());
                                send(replyMsg);
                                return;
                            }
                        }

                        // Proceed with booking as the student is registered
                        ACLMessage bookingReply = msg.createReply();

                        String checkQuery = "SELECT COUNT(*) FROM bookings WHERE room_number = ? AND date = ? AND time_slot = ?";
                        try (PreparedStatement pstmt = connection.prepareStatement(checkQuery)) {
                            pstmt.setString(1, roomNumber);
                            pstmt.setString(2, date);
                            pstmt.setString(3, timeSlot);
                            ResultSet rs = pstmt.executeQuery();
                            rs.next();
                            if (rs.getInt(1) > 0) {
                                bookingReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                bookingReply.setContent("Room is already booked");
                            } else {
                                String insertBooking = "INSERT INTO bookings (room_number, date, time_slot, student_id) VALUES (?, ?, ?, ?)";
                                try (PreparedStatement insertStmt = connection.prepareStatement(insertBooking)) {
                                    insertStmt.setString(1, roomNumber);
                                    insertStmt.setString(2, date);
                                    insertStmt.setString(3, timeSlot);
                                    insertStmt.setString(4, studentId);
                                    insertStmt.executeUpdate();
                                    bookingReply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    bookingReply.setContent("Room booked successfully");

                                    // Send notification about booking
                                    sendNotification(studentId, "Booking", "Room booked successfully");
                                    System.out.println("Room booked successfully.");
                                }
                            }
                        }

                        send(bookingReply);

                    } else {
                        // No reply from UserManagementAgent
                        ACLMessage replyMsg = msg.createReply();
                        replyMsg.setPerformative(ACLMessage.FAILURE);
                        replyMsg.setContent("No response from UserManagementAgent");
                        send(replyMsg);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

    private class CancelBookingBehavior extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                try {
                    JSONObject request = new JSONObject(msg.getContent());
                    String roomNumber = request.getString("roomNumber");
                    String date = request.getString("date");
                    String timeSlot = request.getString("timeSlot");

                    ACLMessage reply = msg.createReply();

                    String deleteQuery = "DELETE FROM bookings WHERE room_number = ? AND date = ? AND time_slot = ?";
                    try (PreparedStatement pstmt = connection.prepareStatement(deleteQuery)) {
                        pstmt.setString(1, roomNumber);
                        pstmt.setString(2, date);
                        pstmt.setString(3, timeSlot);
                        int rowsAffected = pstmt.executeUpdate();
                        if (rowsAffected > 0) {
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent("Booking cancelled successfully");
                        } else {
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("Booking not found");
                        }
                    }

                    send(reply);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

    // Method to send notification
    private void sendNotification(String userId, String type, String message) {
        if (notificationAgent != null) {
            ACLMessage notifMsg = new ACLMessage(ACLMessage.REQUEST);
            notifMsg.addReceiver(notificationAgent);
            JSONObject notification = new JSONObject();
            notification.put("userId", userId);
            notification.put("type", type);
            notification.put("message", message);
            notifMsg.setContent(notification.toString());
            send(notifMsg);
        } else {
            System.out.println("NotificationAgent not available");
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            if (connection != null) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException | FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("FacilityBookingAgent " + getAID().getName() + " terminating.");
    }
}
