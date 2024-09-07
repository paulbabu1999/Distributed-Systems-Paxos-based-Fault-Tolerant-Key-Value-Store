package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

/**
 * Implementation of the LearnerInterface using RMI for communication.
 * This class is responsible for learning and applying values based on
 * the Paxos consensus algorithm.
 */
public class Learner extends UnicastRemoteObject implements LearnerInterface {
    private final KeyValueStoreImpl server; // Reference to the key-value store implementation
    private final ServerLogger logger; // ServerLogger instance for logging

    /**
     * Constructs a Learner instance.
     *
     * @param logger the ServerLogger instance used for logging activities and errors.
     * @param server the KeyValueStoreImpl instance that this learner will update.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    public Learner(ServerLogger logger, KeyValueStoreImpl server) throws RemoteException {
        super();
        this.logger = logger;
        this.server = server;
    }

    /**
     * Learns and applies the value received from the leader in the Paxos algorithm.
     * This method updates the key-value store based on the provided value.
     *
     * @param value the value containing the operation and associated data (e.g., "PUT key value" or "DELETE key").
     */
    @Override
    public void learn(String value) {
        // Log and print learning operation
        logger.logActivity("Learning value at server: " + server);

        // Split the value into operation and key/value parts
        String[] parts = value.split(" ", 3);

        // Check if the operation is provided and not empty
        if (parts.length < 1 || parts[0].isEmpty()) {
            logger.logError("Operation not provided or empty in value: " + value);
            return;
        }

        String operation = parts[0].toUpperCase();

        // Handle different operations based on the exact case command
        switch (operation) {
            case "PUT":
                if (parts.length == 3) {
                    for (int i = 0; i < 3; i++) {
                        if (parts[i] == null || parts[i].trim().isEmpty()) {
                            logger.logError("Invalid operation format: missing part at index " + i + " in value: " + value);
                            return;
                        }
                    }

                    server.store.put(parts[1], parts[2]);
                    logger.logActivity("PUT operation successful for key: " + parts[1] + " with value: " + parts[2]);
                } else {
                    logger.logError("Invalid PUT operation format: " + value);
                }
                break;

            case "DELETE":
                for (int i = 0; i < 2; i++) {
                    if (parts[i] == null || parts[i].trim().isEmpty()) {
                        logger.logError("Invalid operation format: missing part at index " + i + " in value: " + value);
                        return;
                    }
                }


                    ;
                    // Perform DELETE operation
                    if (server.store.containsKey(parts[1])) {
                        server.store.remove(parts[1]);
                        logger.logActivity("DELETE operation successful for key: " + parts[1]);
                    } else {
                        logger.logError("DELETE operation failed: Key " + parts[1] + " not found");
                    }

                break;

            default:
                logger.logError("Unknown operation " + operation + " in value: " + value);
        }
    }
}

