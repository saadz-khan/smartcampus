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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

    private boolean isFutureDate(String date) {
        try {
            LocalDate bookingDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            return bookingDate.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            System.err.println("Invalid date format: " + date);
            return false;
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
            try (ResultSet rs = stmt.executeQuery(checkRooms)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("Populating rooms with predefined data...");
                    populateRooms();
                }
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
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                JSONObject room = new JSONObject();
                                room.put("roomNumber", rs.getString("room_number"));
                                room.put("capacity", rs.getInt("capacity"));
                                room.put("location", rs.getString("location"));
                                room.put("floor", rs.getInt("floor"));
                                availableRooms.put(room);
                            }
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
                    String timeSlot = request.getString("timeSlot"); // Format: "HH:MM-HH:MM"
                    String studentId = request.getString("studentId");

                    ACLMessage reply = msg.createReply();

                    // Validate booking date
                    if (!isFutureDate(date)) {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        reply.setContent("Invalid date. Booking date must be in the future.");
                        send(reply);
                        return;
                    }

                    // Validate booking duration
                    if (!isValidTimeSlot(timeSlot)) {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        reply.setContent("Invalid time slot. Maximum booking duration is 2 hours.");
                        send(reply);
                        return;
                    }

                    // Check if the student already has an active booking
                    if (hasActiveBooking(studentId, date)) {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        reply.setContent("You already have an active booking. Cancel it before making a new one.");
                        send(reply);
                        return;
                    }

                    // Check room availability
                    String checkQuery = "SELECT COUNT(*) FROM bookings WHERE room_number = ? AND date = ? AND time_slot = ?";
                    try (PreparedStatement pstmt = connection.prepareStatement(checkQuery)) {
                        pstmt.setString(1, roomNumber);
                        pstmt.setString(2, date);
                        pstmt.setString(3, timeSlot);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            rs.next();
                            if (rs.getInt(1) > 0) {
                                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                reply.setContent("Room is already booked for the given date and time slot.");
                                send(reply);
                                return;
                            }
                        }
                    }

                    // Proceed with booking
                    String insertBooking = "INSERT INTO bookings (room_number, date, time_slot, student_id) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertBooking)) {
                        insertStmt.setString(1, roomNumber);
                        insertStmt.setString(2, date);
                        insertStmt.setString(3, timeSlot);
                        insertStmt.setString(4, studentId);
                        insertStmt.executeUpdate();

                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContent("Room booked successfully.");
                        send(reply);

                        // Send notification
                        sendNotification(studentId, "Booking", "Room booked successfully for " + date + " at " + timeSlot);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }

        private boolean isValidTimeSlot(String timeSlot) {
            try {
                String[] parts = timeSlot.split("-");
                String[] startParts = parts[0].split(":");
                String[] endParts = parts[1].split(":");

                int startHour = Integer.parseInt(startParts[0]);
                int startMinute = Integer.parseInt(startParts[1]);
                int endHour = Integer.parseInt(endParts[0]);
                int endMinute = Integer.parseInt(endParts[1]);

                // Calculate duration in minutes
                int duration = (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
                return duration > 0 && duration <= 120; // Maximum 2 hours
            } catch (Exception e) {
                System.err.println("Invalid time slot format: " + timeSlot);
                return false;
            }
        }

        private boolean hasActiveBooking(String studentId, String date) {
            String query = "SELECT COUNT(*) FROM bookings WHERE student_id = ? AND date = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, studentId);
                pstmt.setString(2, date);
                try (ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
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
