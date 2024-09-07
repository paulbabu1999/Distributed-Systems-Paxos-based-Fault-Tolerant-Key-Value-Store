package server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Implementation of the AcceptorInterface using RMI for communication.
 * This class handles proposal preparations, acceptances, and maintains
 * leader heartbeat monitoring for Paxos consensus algorithm.
 */
public class Acceptor extends UnicastRemoteObject implements AcceptorInterface {
    private final List<String> acceptors; // List of acceptor URLs for communication
    private final List<String> learners; // List of learner URLs for communication
    private final ServerLogger logger; // ServerLogger instance for logging
    private final Random random; // Random instance for generating random values
    private int highestProposal = -1; // Highest proposal number received
    private String acceptedValue = null; // Value accepted for the highest proposal
    private boolean isLeader; // Flag to indicate if this instance is the leader
    private boolean leaderAlive = true; // Flag to check if the leader is alive
    private Timer heartbeatTimer; // Timer for sending and monitoring heartbeats

    /**
     * Constructs an Acceptor instance.
     *
     * @param logger the ServerLogger instance used for logging activities and errors.
     * @param acceptors the list of acceptor URLs.
     * @param learners the list of learner URLs.
     * @param isLeader flag indicating if this acceptor is the leader.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    public Acceptor(ServerLogger logger, List<String> acceptors, List<String> learners, boolean isLeader) throws RemoteException {
        super();
        this.acceptors = acceptors;
        this.isLeader = isLeader;
        this.learners = learners;
        this.logger = logger;
        this.random = new Random();
    }

    /**
     * Prepares to handle a proposal with a given number.
     *
     * @param proposalNumber the proposal number being prepared.
     * @return "PROMISE" if the proposal number is higher than the current highest proposal; "REJECT" otherwise.
     */
    @Override
    public synchronized String prepare(int proposalNumber) {
        if (proposalNumber > highestProposal) {
            highestProposal = proposalNumber;
            return "PROMISE";
        }
        return "REJECT";
    }

    /**
     * Accepts a proposal with a given number and value.
     *
     * @param proposalNumber the proposal number being accepted.
     * @param value the value associated with the proposal.
     * @return "ACCEPT" if the proposal number is equal to or higher than the highest proposal; "REJECT" otherwise.
     */
    @Override
    public synchronized String accept(int proposalNumber, String value) {
        if (proposalNumber >= highestProposal) {
            highestProposal = proposalNumber;
            acceptedValue = value;
            return "ACCEPT";
        }
        return "REJECT";
    }

    /**
     * Informs all learners about the accepted value.
     *
     * @param value the value to be learned by learners.
     * @return a confirmation message indicating that the value was learned.
     * @throws MalformedURLException if the learner URL is malformed.
     * @throws NotBoundException if the learner URL is not bound.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    @Override
    public String learn(String value) throws MalformedURLException, NotBoundException, RemoteException {
        logger.logActivity("Acceptor Lead: Asking learners to learn the command :" + value);
        for (String learner : learners) {
            logger.logActivity("Acceptor Lead: Learner " + learner + " Learning value");
            LearnerInterface learnerI = (LearnerInterface) Naming.lookup(learner);
            learnerI.learn(value);
        }
        logger.logActivity("Acceptor lead: Learners learned their lesson");
        return "Learned: " + value;
    }

    /**
     * Checks if this acceptor is the leader.
     *
     * @return true if this acceptor is the leader; false otherwise.
     */
    @Override
    public boolean isLeader() {
        return isLeader;
    }

    /**
     * Sets the leader status of this acceptor and starts or stops the heartbeat monitoring accordingly.
     *
     * @param isLeader true if this acceptor should be set as the leader; false otherwise.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    public void setLeader(boolean isLeader) throws RemoteException {
        this.isLeader = isLeader;

        if (isLeader) {
            startHeartbeat(); // Start sending heartbeats if this is the leader
        } else {
            startHeartbeatMonitor(); // Start monitoring heartbeats if this is not the leader
        }
    }

    /**
     * Starts sending heartbeat messages to other acceptors to indicate that the leader is alive.
     */
    private void startHeartbeat() {
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    for (String acceptorUrl : acceptors) {
                        AcceptorInterface acceptor = (AcceptorInterface) Naming.lookup(acceptorUrl);
                        try {
                            acceptor.receiveHeartbeat(); // Send heartbeat to each acceptor
                        } catch (Exception e) {
                            // Handle exception if acceptor is not reachable
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 5000); // Send heartbeat every 5 seconds
    }

    /**
     * Starts monitoring heartbeats from the leader. If no heartbeat is received within the timeout period,
     * it triggers a leader election.
     */
    private void startHeartbeatMonitor() {
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!leaderAlive) {
                    LeaderElection.assignLeadershipAcceptor(); // Trigger leader election
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
     * Handles a prepare request by contacting other acceptors to gather promises.
     *
     * @param proposalNumber the proposal number for which promises are gathered.
     * @return "PROMISE" if a majority of acceptors promise; "REJECT" otherwise.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    @Override
    public String handlePrepareRequest(int proposalNumber) throws RemoteException {
        logger.logActivity("At acceptor - Proposal received - " + proposalNumber);
        int promises = 0;
        List<AcceptorInterface> acceptorList = new ArrayList<>();
        try {
            for (String acceptorUrl : acceptors) {
                AcceptorInterface acceptor = (AcceptorInterface) Naming.lookup(acceptorUrl);
                String response;
                try {
                    response = acceptor.prepare(proposalNumber);
                }
                catch (Exception e){
                    response="REJECT";
                }
                if ("PROMISE".equals(response)) {
                    promises++;
                    acceptorList.add(acceptor);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = promises > acceptors.size() / 2 ? "PROMISE" : "REJECT";
        logger.logActivity("response at acceptor:" + response);
        return response;
    }

    /**
     * Handles an accept request by contacting other acceptors to gather accepts.
     *
     * @param proposalNumber the proposal number being accepted.
     * @param value the value associated with the proposal.
     * @return "ACCEPT" if a majority of acceptors accept; "REJECT" otherwise.
     * @throws RemoteException if there is an issue with RMI communication.
     */
    @Override
    public String handleAcceptRequest(int proposalNumber, String value) throws RemoteException {
        logger.logActivity("Acceptor Lead: Accept request received for proposal " + proposalNumber + " with Command " + value);
        int accepts = 0;
        try {
            for (String acceptorUrl : acceptors) {
                AcceptorInterface acceptor = (AcceptorInterface) Naming.lookup(acceptorUrl);
                String response;
                try {
                    response = acceptor.accept(proposalNumber, value);
                }
                catch (Exception e){
                    response="REJECT";

                }
                logger.logActivity("Acceptor " + acceptorUrl + " gave response " + response);
                if ("ACCEPT".equals(response)) {
                    accepts++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = accepts > acceptors.size() / 2 ? "ACCEPT" : "REJECT";
        logger.logActivity("Acceptor Lead: Response given to proposal " + proposalNumber + " : " + response);
        return response;
    }
}
