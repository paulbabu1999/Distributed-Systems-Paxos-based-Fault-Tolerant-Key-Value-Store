package server;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implementation of the ProposerInterface using RMI for communication.
 * This class handles proposing values in the Paxos consensus algorithm,
 * including managing leader status and sending/monitoring heartbeats.
 */
public class Proposer extends UnicastRemoteObject implements ProposerInterface {
    private final List<String> acceptors; // List of acceptor URLs for communication
    private final List<String> proposers; // List of proposer URLs for communication
    private final ServerLogger logger; // ServerLogger instance for logging
    private int proposalNumber = 0; // Current proposal number
    private String value; // Value to be proposed
    private boolean isLeader; // Flag to indicate if this instance is the leader
    private boolean leaderAlive = true; // Flag to check if the leader is alive
    private Timer heartbeatTimer; // Timer for sending and monitoring heartbeats

    /**
     * Constructs a Proposer instance.
     *
     * @param logger the ServerLogger instance used for logging activities and errors.
     * @param proposers the list of proposer URLs.
     * @param acceptors the list of acceptor URLs.
     * @param isLeader flag indicating if this proposer is the leader.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    public Proposer(ServerLogger logger, List<String> proposers, List<String> acceptors, boolean isLeader) throws RemoteException {
        super();
        this.acceptors = acceptors;
        this.isLeader = isLeader;
        this.proposers = proposers;
        this.logger = logger;
    }

    /**
     * Sets the value to be proposed.
     *
     * @param value the value to be proposed.
     */
    @Override
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Sets the leader status of this proposer.
     *
     * @param isLeader true if this proposer should be set as the leader; false otherwise.
     */
    @Override
    public void setLeader(boolean isLeader) {
        this.isLeader = isLeader;

        // Uncomment these lines if heartbeat functionality is needed for proposers
        // if (isLeader) {
        //     startHeartbeat();
        // } else {
        //     startHeartbeatMonitor();
        // }
    }

    /**
     * Starts sending heartbeat messages to other proposers to indicate that the leader is alive.
     */
    private void startHeartbeat() {
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    for (String proposerUrl : proposers) {
                        ProposerInterface proposer = (ProposerInterface) Naming.lookup(proposerUrl);
                        proposer.receiveHeartbeat(); // Send heartbeat to each proposer
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 5000); // Send heartbeat every 5 seconds
    }

    /**
     * Starts monitoring heartbeats from the leader proposers. If no heartbeat is received within the timeout period,
     * it triggers a leader election.
     */
    private void startHeartbeatMonitor() {
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!leaderAlive) {
                    System.out.println("Leader Proposer Success");
                    LeaderElection.assignLeadershipProposer(); // Trigger leader election
                    heartbeatTimer.cancel(); // Stop monitoring heartbeats
                } else {
                    leaderAlive = false; // Reset for the next cycle
                }
            }
        }, 0, 7000); // Check every 7 seconds (heartbeat timeout)
    }

    /**
     * Receives a heartbeat signal from the leader, indicating that the leader is alive.
     */
    @Override
    public void receiveHeartbeat() {
        leaderAlive = true; // Update leader status
    }

    /**
     * Proposes a value to the acceptors if this proposer is the leader.
     *
     * @param client the client initiating the proposal.
     */
    @Override
    public synchronized void propose(String client) throws RemoteException {
        if (!isLeader) {
            logger.logError("This proposer is not the leader. Cannot propose.");
            return;
        }
        try {
            proposalNumber++; // Increment the proposal number
            logger.logActivity("Proposal received from " + client);

            // Determine the leader among acceptors
            AcceptorInterface leaderAcceptor = null;
            for (String acceptorUrl : acceptors) {

                try {
                    AcceptorInterface acceptor = (AcceptorInterface) Naming.lookup(acceptorUrl);


                    if (acceptor.isLeader()) {
                        leaderAcceptor = acceptor;
                        break;
                    }
                } catch (Exception e) {

                }
            }

            if (leaderAcceptor == null) {
                logger.logError("No leader found among acceptors.");
                return;
            }

            // Phase 1: Prepare
            logger.logActivity("Proposer: Request Prepare for proposal " + proposalNumber);
            String prepareResponse = leaderAcceptor.handlePrepareRequest(proposalNumber);
            if ("PROMISE".equals(prepareResponse)) {
                // Phase 2: Accept
                logger.logActivity("Proposer: PROMISE Response received for proposal " + proposalNumber);

                String acceptResponse = leaderAcceptor.handleAcceptRequest(proposalNumber, value);
                logger.logActivity("Proposer: Acceptor Lead responded " + acceptResponse + " to the command " + value);
                if ("ACCEPT".equals(acceptResponse)) {
                    // Notify learners (all acceptors)
                    leaderAcceptor.learn(value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
