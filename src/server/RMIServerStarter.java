package server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * The RMIServerStarter class starts multiple RMI servers that host a KeyValueStore implementation.
 * It binds the KeyValueStoreImpl to the RMI registry on multiple ports, logs server activities and errors,
 * and gracefully handles shutdown by unbinding and cleaning up resources.
 */
public class RMIServerStarter {
    private static final int NUM_SERVERS = 5; // Number of servers to start
    protected static ServerLogger logger = new ServerLogger("serverLog.txt"); // Logger instance for logging server activities

    /**
     * Main method to start the RMI servers.
     *
     * @param args command-line arguments (expects hostname and 5 ports)
     */
    public static void main(String[] args) {
        // Check if the correct number of arguments are provided
        if (args.length != 6) {
            System.out.println("Usage: java RMIServerStarter <hostname> <port1> <port2> <port3> <port4> <port5>");
            logger.logError("Initialization Error: Incorrect number of arguments");
            return;
        }

        // Extract hostname and ports from command-line arguments
        String host = args[0];
        int[] ports = new int[NUM_SERVERS];

        try {
            // Parse the ports from command-line arguments
            for (int i = 0; i < NUM_SERVERS; i++) {
                ports[i] = Integer.parseInt(args[i + 1]);
            }
        } catch (NumberFormatException e) {
            logger.logError("Initialization Error: Ports must be integers");
            System.out.println("Ports must be integers.");
            return;
        }

        try {
            // Create a list of replica URLs for the key-value store
            List<String> replicas = new ArrayList<>();
            for (int i = 0; i < NUM_SERVERS; i++) {
                replicas.add("rmi://" + host + ":" + ports[i] + "/KeyValueStore");
            }

            // Start RMI servers on the specified ports
            for (int i = 0; i < NUM_SERVERS; i++) {
                int port = ports[i];
                LocateRegistry.createRegistry(port); // Create an RMI registry on the specified port
                KeyValueStoreImpl keyValueStore = new KeyValueStoreImpl(logger, replicas.toArray(new String[0]), replicas.get(i));
                Naming.rebind(replicas.get(i), keyValueStore); // Bind the key-value store implementation to the RMI registry
                System.out.println("Server is running at " + replicas.get(i));
                logger.logActivity("Server is running at " + replicas.get(i));
            }

            // Elect leaders and schedule acceptor failure simulation
            KeyValueStoreImpl.electLeaders();
            KeyValueStoreImpl.scheduleAcceptorFailure();
            logger.logActivity("Leaders elected");

            // Wait indefinitely to keep the servers running
            synchronized (RMIServerStarter.class) {
                RMIServerStarter.class.wait();
            }

        } catch (Exception e) {
            logger.logError("Exception in RMIServerStarter: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
