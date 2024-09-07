package server;

import publicInterface.KeyValueStore;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of the KeyValueStore interface using a ConcurrentHashMap
 * to store key-value pairs. This class handles PUT, GET, and DELETE commands
 * from clients and logs activities and errors using a provided ServerLogger.
 */
public class KeyValueStoreImpl extends UnicastRemoteObject implements KeyValueStore {
    private static final Random random = new Random(); // Random instance for generating delays
    static List<String> acceptorUrls = new ArrayList<>(); // List of acceptor URLs
    static List<String> proposerUrls = new ArrayList<>(); // List of proposer URLs
    static List<String> learnerUrls = new ArrayList<>(); // List of learner URLs
    static List<AcceptorInterface> acceptorObjs = new ArrayList<>(); // List of acceptor objects
    private static ServerLogger logger; // ServerLogger instance for logging
    private static ProposerInterface leaderProposer; // The current leader proposer
    private static boolean intialised = false; // Flag indicating initialization status
    private static final ScheduledExecutorService failureScheduler = Executors.newSingleThreadScheduledExecutor(); // Scheduler for handling failures
    private final String[] replicas; // List of replica URLs
    private final String serverAddress; // Address of the server
    public Map<String, String> store = new ConcurrentHashMap<>(); // ConcurrentHashMap for storing key-value pairs

    /**
     * Constructs a KeyValueStoreImpl instance.
     *
     * @param logger the ServerLogger instance used for logging activities and errors.
     * @param replicas the list of replica URLs for replication.
     * @param address the server address for RMI binding.
     * @throws RemoteException if there is an issue with RMI communication.
     * @throws MalformedURLException if the address is malformed.
     */
    protected KeyValueStoreImpl(ServerLogger logger, String[] replicas, String address) throws RemoteException, MalformedURLException {
        super();
        KeyValueStoreImpl.logger = logger; // Initialize ServerLogger
        this.replicas = replicas; // Initialize replicas
        this.serverAddress = address; // Initialize server address

        if (!intialised) {
            // Initialize URLs for acceptors, proposers, and learners
            for (String replica : replicas) {
                acceptorUrls.add(replica + "/acceptor");
                proposerUrls.add(replica + "/proposer");
                learnerUrls.add(replica + "/learner");
            }
            intialised = true;
        }

        // Create and bind the acceptor, proposer, and learner instances
        ProposerInterface proposer = new Proposer(logger, proposerUrls, acceptorUrls, false);
        AcceptorInterface acceptor = new Acceptor(logger, acceptorUrls, learnerUrls, false);
        acceptorObjs.add(acceptor);

        LearnerInterface learner = new Learner(logger, this);
        Naming.rebind(address + "/acceptor", acceptor);
        Naming.rebind(address + "/proposer", proposer);
        Naming.rebind(address + "/learner", learner);
    }

    /**
     * Elects leaders for proposers and acceptors.
     */
    protected static void electLeaders() {
        new LeaderElection(proposerUrls, acceptorUrls);
        leaderProposer = LeaderElection.assignLeadershipProposer(); // Elect leader proposer
        LeaderElection.assignLeadershipAcceptor(); // Elect leader acceptor
    }

    /**
     * Schedules a failure for an acceptor at a random interval.
     *
     * @throws MalformedURLException if the URL is malformed.
     * @throws NotBoundException if the URL is not bound.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    protected static void scheduleAcceptorFailure() throws MalformedURLException, NotBoundException, RemoteException {
        int r = random.nextInt(acceptorObjs.size()); // Randomly select an acceptor
        AcceptorInterface acceptor = acceptorObjs.get(r);
        String url = acceptorUrls.get(r);
        if (acceptor == null) {
            scheduleAcceptorFailure(); // Retry if the acceptor is null
            return;
        }
        int delay = random.nextInt(10) + 10; // Random delay between 10 to 20 seconds
        failureScheduler.schedule(() -> {
            try {
                logger.logError("Acceptor " + url + " is failing...");
                shutdown(acceptor, url); // Shut down the acceptor
                int restartDelay = random.nextInt(10) + 10; // Random restart delay between 10 to 20 seconds
                failureScheduler.schedule(() -> restart(url, r), restartDelay, TimeUnit.SECONDS); // Schedule restart
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Shuts down an acceptor.
     *
     * @param acceptor the acceptor to be shut down.
     * @param url the URL of the acceptor.
     */
    private static synchronized void shutdown(AcceptorInterface acceptor, String url) {
        try {
            if (acceptor == null) {
                logger.logError("Acceptor " + url + " is null and cannot be shut down.");
                return;
            }

            // Unexport the object
            boolean unexported = UnicastRemoteObject.unexportObject(acceptor, true);

            if (unexported) {
                logger.logActivity("Acceptor " + url + " has been shut down.");
            } else {
                logger.logError("Acceptor " + url + " was not successfully unexported.");
            }
        } catch (NoSuchObjectException e) {
            // Handle case where the object was already unexported
            logger.logError("Acceptor " + url + " was already unexported or not exported.");
        } catch (Exception e) {
            logger.logError("Failed to shut down acceptor " + url + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Restarts an acceptor by creating a new instance and rebinding it.
     *
     * @param url the URL of the acceptor to be restarted.
     * @param r the index of the acceptor in the list.
     */
    private static void restart(String url, int r) {
        try {
            AcceptorInterface acceptor = new Acceptor(logger, acceptorUrls, learnerUrls, false);
            acceptorObjs.set(r, acceptor); // Replace the old acceptor with the new one
            Naming.rebind(url, acceptor); // Rebind the acceptor to the URL
            logger.logActivity("Acceptor has restarted.");
            scheduleAcceptorFailure(); // Schedule the next failure after restarting
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a command on the key-value store based on the provided client identifier and command.
     *
     * @param clientIdentifier the identifier of the client making the request.
     * @param command the command to be executed (e.g., PUT key value, GET key, DELETE key).
     * @return a response string based on the command execution.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    @Override
    public String executeCommand(String clientIdentifier, String command) throws RemoteException {
        String[] parts = command.split(" ", 3); // Split the command into parts
        String operation = parts[0].toUpperCase(); // Extract the operation (PUT, GET, DELETE)

        switch (operation) {
            case "PUT":
                if (parts.length == 3) {
                    return paxosCommit(clientIdentifier, "PUT", parts[1], parts[2]); // Commit a PUT operation
                }
                break;
            case "DELETE":
                if (parts.length == 2) {
                    return paxosCommit(clientIdentifier, "DELETE", parts[1], null); // Commit a DELETE operation
                }
                break;
            case "GET":
                if (parts.length == 2) {
                    for (int i = 0; i < 2; i++) {
                        if (parts[i] == null || parts[i].trim().isEmpty()) {
                            System.out.println("Invalid command");

                            logger.logError("Invalid operation format");
                            return "NULL";
                        }
                    }
                    String value = store.get(parts[1]); // Retrieve the value for the key
                    logger.logActivity(clientIdentifier + " GET command: Key '" + parts[1] + "'" + " at server " + serverAddress);
                    return value != null ? value : "NULL"; // Return the value or NULL if not found
                }
                else{
                    System.out.println("Invalid command");
                    return "NULL";
                }

            default:
                logger.logError(clientIdentifier + " Invalid command: " + command + " at server " + serverAddress);
                return "Invalid command"; // Return error for invalid commands
        }

        return "NULL"; // Default return value
    }

    /**
     * Commits a Paxos operation (PUT or DELETE) by proposing the operation to the leader proposer.
     *
     * @param clientIdentifier the identifier of the client making the request.
     * @param operation the operation to be committed (PUT or DELETE).
     * @param key the key for the operation.
     * @param value the value for the operation (null for DELETE).
     * @return the key for the operation or an error message.
     */
    private String paxosCommit(String clientIdentifier, String operation, String key, String value) {
        try {
            if (leaderProposer == null) {
                logger.logError(clientIdentifier + " No leader available for Paxos commit at server " + serverAddress);
                return "ERROR: No leader Here";
            }

            leaderProposer.setValue(operation + " " + key + " " + value); // Set the value to be proposed
            leaderProposer.propose(clientIdentifier); // Propose the operation

        } catch (Exception e) {
            logger.logError(clientIdentifier + " Error during Paxos commit: " + e.getMessage() + " at server " + serverAddress);
            return "ERROR";
        }

        return key; // Return the key for successful operations
    }
}
