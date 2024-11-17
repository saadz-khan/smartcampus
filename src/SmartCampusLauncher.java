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
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.MAIN_PORT, "1200"); // Change port to an unused one
            profile.setParameter(Profile.GUI, "true");

            AgentContainer mainContainer = rt.createMainContainer(profile);

            // Create instances of all required agents
            AgentController userAgent = mainContainer.createNewAgent(
                    "UserAgent",
                    "agents.UserManagementAgent",
                    null
            );

            AgentController bookingAgent = mainContainer.createNewAgent(
                    "BookingAgent",
                    "agents.FacilityBookingAgent",
                    null
            );

            AgentController notificationAgent = mainContainer.createNewAgent(
                    "NotificationAgent",
                    "agents.NotificationAgent",
                    null
            );

            AgentController navigationAgent = mainContainer.createNewAgent(
                    "NavigationAgent",
                    "agents.NavigationAssistantAgent",
                    null
            );

            // Start all agents
            userAgent.start();
            bookingAgent.start();
            notificationAgent.start();
            navigationAgent.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
