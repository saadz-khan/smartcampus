package agents;

import java.sql.*;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.JSONObject;

public class UserManagementAgent extends Agent {
    private Connection connection;

    protected void setup() {
        try {
            // Connect to SQLite database
            connection = DriverManager.getConnection("jdbc:sqlite:smartcampus.db");
            System.out.println("Connected to SQLite database.");

            // Initialize database schema and populate sample data
            initializeDatabase();

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
        } catch (SQLException e) {
            System.err.println("Database connection error:");
            e.printStackTrace();
        }
    }

    protected void takeDown() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            // Create the students table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS students (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id TEXT UNIQUE NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT NOT NULL)";
            stmt.executeUpdate(createTableSQL);

            System.out.println("Students table created or already exists.");

            // Insert sample data if the table is empty
            String checkTableSQL = "SELECT COUNT(*) FROM students";
            ResultSet rs = stmt.executeQuery(checkTableSQL);
            rs.next();
            if (rs.getInt(1) == 0) {
                System.out.println("Populating students table with sample data...");
                populateSampleData();
            } else {
                System.out.println("Students table already populated.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateSampleData() {
        String insertSQL = "INSERT INTO students (student_id, name, email) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            // Add sample student records
            pstmt.setString(1, "12345678");
            pstmt.setString(2, "John Doe");
            pstmt.setString(3, "john.doe@example.com");
            pstmt.executeUpdate();

            pstmt.setString(1, "87654321");
            pstmt.setString(2, "Jane Smith");
            pstmt.setString(3, "jane.smith@example.com");
            pstmt.executeUpdate();

            pstmt.setString(1, "11223344");
            pstmt.setString(2, "Alice Johnson");
            pstmt.setString(3, "alice.johnson@example.com");
            pstmt.executeUpdate();

            System.out.println("Sample data inserted into students table.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class StudentRegistrationBehavior extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                try {
                    JSONObject request = new JSONObject(msg.getContent());
                    String studentId = request.getString("studentId");
                    String name = request.getString("name");
                    String email = request.getString("email");

                    ACLMessage reply = msg.createReply();

                    if (!isValidStudentId(studentId)) {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Invalid student ID format");
                    } else if (isStudentIdExists(studentId)) {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Student ID already exists");
                    } else {
                        registerStudent(studentId, name, email);
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
            return studentId.matches("^\\d{8}$"); // Example: 8-digit ID
        }

        private boolean isStudentIdExists(String studentId) {
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM students WHERE student_id = ?");
                stmt.setString(1, studentId);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                return rs.getInt(1) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        private void registerStudent(String studentId, String name, String email) {
            try {
                PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO students (student_id, name, email) VALUES (?, ?, ?)"
                );
                stmt.setString(1, studentId);
                stmt.setString(2, name);
                stmt.setString(3, email);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
