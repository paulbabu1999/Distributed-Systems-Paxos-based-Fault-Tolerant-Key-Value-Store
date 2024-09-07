package client;

import publicInterface.KeyValueStore;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * RMIClient is a client application that interacts with a remote KeyValueStore using RMI.
 * It sends commands to the server and logs the interactions.
 */

public class RMIClient {
    protected static ClientLogger logger = new ClientLogger("clientLog.txt");


    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.err.println("Usage: java RMIClient <server_address> <server_port>");
                System.exit(1);
            }
            String serverAddress = args[0];
            int port = Integer.parseInt(args[1]);


            InetAddress localhost = InetAddress.getLocalHost();
            String hostName = localhost.getHostName();
            String ipAddress = localhost.getHostAddress();
            String clientIdentifier = hostName + "-" + ipAddress;


            Registry registry = LocateRegistry.getRegistry(serverAddress, port);
            KeyValueStore keyValueStore = (KeyValueStore) Naming.lookup("rmi://" + serverAddress + ":" + port + "/KeyValueStore");


            String[] userInput = {
                    "put player Kohli",
                    "put position batting",
                    "put strength placement",
                    "put weakness leg spin",
                    "put favorite aggression"
            };
            for (String input : userInput) {
                String response = keyValueStore.executeCommand(clientIdentifier, input);
                logger.logActivity("Pre-populated byClient " + clientIdentifier + " received response: " + response);


            }
            // Register shutdown hook to gracefully stop the client
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Client is shutting down gracefully...");
                    logger.logActivity("Client " + clientIdentifier + " is shutting down.");
                    // Additional cleanup can be added here if needed
                } catch (Exception e) {
                    logger.logError("Error during client shutdown: " + e.getMessage());
                    e.printStackTrace();
                }
            }));

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter command (type 'exit' to quit):");
                String command = scanner.nextLine().trim(); // Read user input
                if (command.equalsIgnoreCase("exit")) {
                    System.out.println("exiting client");
                    logger.logActivity("Client Closed");
                    break; // Exit the loop and shutdown the client
                }

                // Validate and execute the command
                if (command.isEmpty()) {
                    System.out.println("Please enter a command.");
                    continue;
                }

                // Log command sent by the client
                logger.logActivity("Client " + clientIdentifier + " sent command: " + command);

                // Execute command on the server
                String response = keyValueStore.executeCommand(clientIdentifier, command);

                // Log response received from the server
                logger.logActivity("Client " + clientIdentifier + " received response: " + response);

                // Print response to the console
                System.out.println("Response: " + response);
            }

        } catch (Exception e) {
            logger.logError("Exception in RMIClient: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
