### Instructions to Execute the SmartCampus Multi-Agent System

#### **Prerequisites**
1. **Java Development Kit (JDK):**
    - Ensure that JDK 11 or higher is installed.
    - Verify installation by running:
      ```bash
      java -version
      ```
2. **JADE Framework:**
    - Download the JADE library from the [official website](http://jade.tilab.com/).
    - Add the JADE JAR file to your project's classpath.
3. **SQLite:**
    - Install SQLite for database operations.
    - Verify SQLite installation by running:
      ```bash
      sqlite3 --version
      ```

4. **IDE or Command Line:**
    - Use an IDE like IntelliJ IDEA, Eclipse, or a text editor with terminal access.
    - Alternatively, use the terminal/command prompt for compilation and execution.

5. **Database Initialization:**
    - The program will automatically create and initialize the database (`smartcampus.db`) in the working directory. No manual setup is required.

---

#### **Program Structure**
1. **Agents Directory**:
    - Contains all agent classes (`UserManagementAgent`, `FacilityBookingAgent`, `NavigationAssistantAgent`, and `GUIAgent`).
2. **Database**:
    - SQLite database for storing room and user data.
3. **Main Class**:
    - The `GUIAgent` serves as the entry point for running the program and interacting with the MAS.

---

#### **Steps to Execute**
1. **Clone or Download the Project**
    - Ensure all source files are in a single directory.
      ```
      src/
      ├── agents/
      │   ├── FacilityBookingAgent.java
      │   ├── UserManagementAgent.java
      │   ├── NavigationAssistantAgent.java
      │   ├── GUIAgent.java
      └── SmartCampusLauncher.java
      ```

2. **Verify Agent Initialization**
    - The JADE framework will launch with a list of initialized agents:
        - `UserManagementAgent`
        - `FacilityBookingAgent`
        - `NavigationAssistantAgent`
        - `GUIAgent`
    - Confirm that agents are registered successfully with the JADE Directory Facilitator (DF).

3. **Interact with the System**
    - **GUI Tab Descriptions:**
        - **User Management**: Register new users by entering their details.
        - **Facility Booking**: Check room availability, book a room, or cancel a booking.
        - **Navigation**: Request directions to a booked room.
        - **Notifications**: View system notifications (e.g., booking confirmations).

4. **Debugging and Logs**
    - Check the terminal for real-time logs:
        - Successful agent registration.
        - Database initialization and queries.
        - ACL message exchanges between agents.
    - Errors will be logged with detailed stack traces.

---

#### **Troubleshooting**
1. **JADE Library Not Found**:
    - Ensure the `jade.jar` file is in the classpath.
    - Recompile with the correct path:
      ```bash
      javac -cp /path/to/jade.jar src/agents/*.java
      ```

2. **Database Issues**:
    - Delete `smartcampus.db` from the working directory if the schema is corrupted.
    - Restart the program to regenerate the database.

3. **ACL Message Errors**:
    - Ensure all agents are initialized before starting GUI interactions.
    - Restart the program to reset agent registrations.

---
