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
import org.json.JSONArray;

public class FacilityBookingAgent extends Agent {
    private JSONObject roomDatabase;
    private JSONObject bookingDatabase;

    @Override
    protected void setup() {
        // Initialize room and booking databases
        roomDatabase = initializeRooms();
        bookingDatabase = new JSONObject();

        // Register the agent's service with the Directory Facilitator (DF)
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("facility-booking");
            sd.setName("smart-campus-room-booking");
            dfd.addServices(sd);

            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Add behaviors for room availability, booking requests, and cancellations
        addBehaviour(new RoomAvailabilityBehavior());
        addBehaviour(new BookingRequestBehavior());
        addBehaviour(new CancelBookingBehavior());
    }

    // Initialize room data
    private JSONObject initializeRooms() {
        JSONObject rooms = new JSONObject();

        // Predefine some rooms
        rooms.put("101", new JSONObject()
                .put("roomNumber", "101")
                .put("capacity", 4)
                .put("location", "Building A")
                .put("floor", 1));
        rooms.put("102", new JSONObject()
                .put("roomNumber", "102")
                .put("capacity", 10)
                .put("location", "Building B")
                .put("floor", 2));

        return rooms;
    }

    private synchronized boolean isRoomBooked(String roomKey, String date, String timeSlot) {
        if (bookingDatabase.has(date)) {
            JSONObject dailyBookings = bookingDatabase.getJSONObject(date);
            if (dailyBookings.has(timeSlot)) {
                JSONArray bookedRooms = dailyBookings.getJSONArray(timeSlot);
                return bookedRooms.toList().contains(roomKey);
            }
        }
        return false;
    }

    private synchronized void bookRoom(String roomNumber, String date, String timeSlot) {
        // Ensure the date key exists
        if (!bookingDatabase.has(date)) {
            bookingDatabase.put(date, new JSONObject());
        }
        JSONObject dailyBookings = bookingDatabase.getJSONObject(date);

        // Ensure the timeSlot key exists
        if (!dailyBookings.has(timeSlot)) {
            dailyBookings.put(timeSlot, new JSONArray());
        }

        // Add the room to the booking list
        dailyBookings.getJSONArray(timeSlot).put(roomNumber);
    }

    private synchronized boolean cancelBooking(String roomNumber, String date, String timeSlot) {
        if (bookingDatabase.has(date)) {
            JSONObject dailyBookings = bookingDatabase.getJSONObject(date);
            if (dailyBookings.has(timeSlot)) {
                JSONArray bookedRooms = dailyBookings.getJSONArray(timeSlot);

                // Find and remove the room
                for (int i = 0; i < bookedRooms.length(); i++) {
                    if (bookedRooms.getString(i).equals(roomNumber)) {
                        bookedRooms.remove(i);
                        return true;
                    }
                }
            }
        }
        return false;
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

                    synchronized (roomDatabase) {
                        for (String roomKey : roomDatabase.keySet()) {
                            JSONObject room = roomDatabase.getJSONObject(roomKey);
                            if (room.getInt("capacity") >= capacity && !isRoomBooked(roomKey, date, timeSlot)) {
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
                    String timeSlot = request.getString("timeSlot");

                    ACLMessage reply = msg.createReply();

                    if (isRoomBooked(roomNumber, date, timeSlot)) {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        reply.setContent("Room is already booked");
                    } else {
                        bookRoom(roomNumber, date, timeSlot);
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContent("Room booked successfully");
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

                    if (cancelBooking(roomNumber, date, timeSlot)) {
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("Booking cancelled successfully");
                    } else {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Booking not found");
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

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("FacilityBookingAgent " + getAID().getName() + " terminating.");
    }
}
