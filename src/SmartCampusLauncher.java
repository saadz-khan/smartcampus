import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class SmartCampusLauncher {
    public static void main(String[] args) {
        try {
            // Initialize JADE runtime
            Runtime rt = Runtime.instance();

            // Create the main container profile
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "127.0.0.1"); // Use localhost or specific IP
            profile.setParameter(Profile.LOCAL_PORT, "1200");     // Use a non-default port
            profile.setParameter(Profile.GUI, "true");            // Enable GUI

            // Create the main container
            AgentContainer mainContainer = rt.createMainContainer(profile);

            // Log the container creation
            System.out.println("Main container created. Launching agents...");

            // Create and start UserManagementAgent
            AgentController userAgent = mainContainer.createNewAgent(
                    "UserAgent",
                    "agents.UserManagementAgent",
                    null
            );
            userAgent.start();

            // Create and start FacilityBookingAgent
            AgentController bookingAgent = mainContainer.createNewAgent(
                    "BookingAgent",
                    "agents.FacilityBookingAgent",
                    null
            );
            bookingAgent.start();

            // Create and start NotificationAgent
            AgentController notificationAgent = mainContainer.createNewAgent(
                    "NotificationAgent",
                    "agents.NotificationAgent",
                    null
            );
            notificationAgent.start();

            // Create and start NavigationAssistantAgent
            AgentController navigationAgent = mainContainer.createNewAgent(
                    "NavigationAgent",
                    "agents.NavigationAssistantAgent",
                    null
            );
            navigationAgent.start();

            System.out.println("All agents launched successfully!");

        } catch (Exception e) {
            // Log any errors during container or agent creation
            System.err.println("Error while launching agents:");
            e.printStackTrace();
        }
    }
}
