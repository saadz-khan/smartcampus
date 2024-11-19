# **SmartCampus System Documentation**

### **SENG 696 Assignment 2: SmartCampus Development**  
**Developer:** Saad Zafar Khan  
**UCID:** 30258277  

---

## **Project Overview**
The SmartCampus system facilitates seamless interaction between students and campus facilities. It integrates user management, facility booking, navigation assistance, and notification systems through JADE agents. 

This project implements key user scenarios (UCs), focusing on efficient agent communication, real-time data validation, and an extensible architecture.

---

## **Use Cases**

### **1. User Management Agent**
**UC1: Student Registration**
- **Primary Actor:** Student  
- **Brief:** Allows students to register with valid university credentials.  
- **Preconditions:** Student ID must be valid and unique.  
- **Success Scenario:**  
  1. User provides details:
     - Student ID  
     - Name  
     - Email  
  2. System validates:
     - Student ID format (8 digits).  
     - Email domain (`@ucalgary.ca`).  
  3. System creates the user profile and sends confirmation.  

- **Extensions:**  
  - Invalid Student ID: Displays error and retries registration.  
  - Duplicate ID: Prompts user to log in.  

**Frequency:** Low (one-time registration per student).

---

### **2. Facility Booking Agent**
**UC2: View Available Study Rooms**
- **Primary Actor:** Student  
- **Brief:** Allows students to view available study rooms based on search criteria.  
- **Preconditions:** User is logged in.  
- **Success Scenario:**  
  1. Student specifies:
     - Date (mandatory).  
     - Time slot (mandatory).  
     - Capacity (optional).  
  2. System retrieves and displays available rooms with details:
     - Room number, capacity, location, available time slots.  

**UC3: Book Study Room**
- **Primary Actor:** Student  
- **Brief:** Enables students to book rooms for a maximum of 2 hours.  
- **Preconditions:** User is logged in, and room is available.  
- **Success Scenario:**  
  1. Student selects a room and confirms:
     - Date, start time, and end time.  
  2. System validates:
     - Room availability.  
     - User’s existing bookings.  
  3. System confirms the booking and sends a notification.  

**UC4: Cancel Room Booking**
- **Primary Actor:** Student  
- **Brief:** Allows students to cancel existing bookings.  
- **Preconditions:** User has an active booking.  
- **Success Scenario:**  
  1. User selects a booking to cancel.  
  2. System confirms cancellation and updates availability.  

---

### **3. Navigation Assistant Agent**
**UC5: Get Directions to Room**
- **Primary Actor:** Student  
- **Brief:** Provides navigation assistance for campus buildings.  
- **Preconditions:** User has an active booking.  
- **Success Scenario:**  
  1. User selects a booking.  
  2. System calculates and displays:
     - Building name, floor number, text directions, and estimated walking time.  

---

### **4. Notification Agent**
**UC6: Manage Notifications**
- **Primary Actor:** System  
- **Brief:** Sends automated notifications for booking-related events.  
- **Notifications Sent:**
  - Booking confirmation.  
  - 15-minute reminders.  
  - Cancellation confirmation.  

---

## **Technical Implementation**

### **1. Agent Classes**

#### **User Management Agent**
Handles:
- User registration validation (ID format, email domain, uniqueness).
- Profile management.  

#### **Facility Booking Agent**
Handles:
- Room availability checks.  
- Booking creation and validation (conflicts, duration limits).  
- Booking cancellation.

#### **Navigation Assistant Agent**
Handles:
- Direction requests using an external navigation API (e.g., OpenRouteService or Google Maps).  
- Generates walking routes with step-by-step directions.  

#### **Notification Agent**
Handles:
- Sending real-time notifications for bookings.  

---

### **2. Key Features**

#### **Data Validation**
- **Student Registration:**  
  - Student ID: 8-digit numeric format.  
  - Email: Must be `@ucalgary.ca`.  

- **Booking Validation:**  
  - Booking date must be in the future.  
  - Bookings are allowed up to 15 minutes in advance.  

#### **Real-Time API Integration**
- Navigation API (e.g., OpenRouteService) provides walking directions with distance, estimated time, and step-by-step instructions.  

#### **Business Rules**
- Maximum booking duration: **2 hours**.  
- Only **1 active booking** allowed per student at a time.  

#### **Error Handling**
- Invalid inputs (e.g., incorrect date, unavailable rooms) trigger user-friendly error messages.  
- Automatic retries for failed API calls.

---

### **3. Database Schema**
#### Tables:
- **Students:** Stores user profiles (`student_id`, `name`, `email`).  
- **Rooms:** Predefined set of study rooms (`room_number`, `capacity`, `location`).  
- **Bookings:** Tracks bookings (`room_number`, `date`, `time_slot`, `student_id`).  

---

### **4. Communication Protocols**
- **ACLMessage:**  
  - Format: JSON for structured message exchange.  
  - Example:
    ```json
    {
        "type": "BOOKING_REQUEST",
        "roomId": "102",
        "userId": "12345678",
        "startTime": "2024-11-03T14:00:00",
        "endTime": "2024-11-03T16:00:00"
    }
    ```

- **Navigation API Request Example:**
    ```json
    {
        "coordinates": [
            [-114.0719, 51.0447],  // From
            [-114.0690, 51.0465]   // To
        ]
    }
    ```

---

### **5. Testing Strategy**

#### **Unit Testing**
- Individual agent behaviors (e.g., booking validation, user registration).  

#### **Integration Testing**
- Inter-agent communication (e.g., user ID validation, notification delivery).  
- Navigation API calls.  

#### **System Testing**
- Simulate concurrent room bookings.  
- Performance of navigation requests.  

---

### **How to Run**

1. **Setup JADE Framework**:  
   - Install JADE on your machine.  
   - Set up agents in `src` directory.

2. **Run the Agents**:
   - Start the JADE container.  
   - Deploy the agents (`UserManagementAgent`, `FacilityBookingAgent`, `NavigationAssistantAgent`, `NotificationAgent`).  

3. **Predefined Data**:
   - Rooms and basic attributes are preloaded into the system.

4. **Navigate to System Menu**:
   - Access the command-line interface for booking and navigation tasks.  

---

### **Future Enhancements**

1. **Enhanced Navigation**:
   - Integration of real-time traffic data.
   - Indoor navigation for complex buildings.

2. **User Dashboard**:
   - Add a graphical interface for booking management.

3. **Push Notifications**:
   - Mobile app integration for notifications.  
