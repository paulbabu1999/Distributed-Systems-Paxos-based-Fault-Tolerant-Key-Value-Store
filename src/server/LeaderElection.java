package server;

import java.rmi.Naming;
import java.util.List;
import java.util.Random;

/**
 * The LeaderElection class is responsible for electing leaders among proposers and acceptors.
 * It ensures that there is a single leader for each type of participant in the system.
 */
public class LeaderElection {
    private static final long MIN_INTERVAL_MS = 1000; // Minimum interval between leadership elections (1 second)
    private static List<String> proposers; // List of proposer URLs
    private static List<String> acceptors; // List of acceptor URLs
    private static final Random random = new Random(); // Random number generator for leader election
    private static volatile long lastCallTimeProposer = 0; // Last time leadership was assigned to a proposer
    private static volatile long lastCallTimeAcceptor = 0; // Last time leadership was assigned to an acceptor

    /**
     * Constructs a LeaderElection instance with the given lists of proposers and acceptors.
     *
     * @param proposers List of proposer URLs.
     * @param acceptors List of acceptor URLs.
     */
    public LeaderElection(List<String> proposers, List<String> acceptors) {
        LeaderElection.proposers = proposers;
        LeaderElection.acceptors = acceptors;
    }

    /**
     * Elects a leader from the given list of participants (proposers or acceptors).
     *
     * @param participants List of participant URLs.
     * @return The URL of the elected leader.
     */
    public static String electLeaderProposer(List<String> participants) {
        return participants.get(random.nextInt(participants.size()));  // Randomly select a proposer
    }

    /**
     * Elects a leader from the given list of participants (proposers or acceptors).
     *
     * @param participants List of participant URLs.
     * @return The URL of the elected leader.
     */
    public static String electLeaderAcceptor(List<String> participants) {
        return participants.get(random.nextInt(participants.size()));  // Randomly select an acceptor
    }

    /**
     * Assigns leadership to a proposer. Only one proposer can be a leader at a time.
     * Ensures that leadership is not assigned more frequently than the specified interval.
     *
     * @return The ProposerInterface instance of the elected leader.
     */
    public synchronized static ProposerInterface assignLeadershipProposer() {
        long currentTime = System.currentTimeMillis();
        // Ensure leadership assignment is not performed too frequently
        if (currentTime - lastCallTimeProposer < MIN_INTERVAL_MS) {
            return null;
        }
        lastCallTimeProposer = currentTime;

        String proposerLeaderUrl = electLeaderProposer(proposers);

        try {
            // Assign leadership to proposers
            for (String proposerUrl : proposers) {
                ProposerInterface proposer = (ProposerInterface) Naming.lookup(proposerUrl);
                proposer.setLeader(proposerUrl.equals(proposerLeaderUrl));
            }
            System.out.println("Proposer Leader elected: " + proposerLeaderUrl);
            return (ProposerInterface) Naming.lookup(proposerLeaderUrl);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Assigns leadership to an acceptor. Only one acceptor can be a leader at a time.
     * Ensures that leadership is not assigned more frequently than the specified interval.
     */
    public synchronized static void assignLeadershipAcceptor() {
        long currentTime = System.currentTimeMillis();
        // Ensure leadership assignment is not performed too frequently
        if (currentTime - lastCallTimeAcceptor < MIN_INTERVAL_MS) {
            return;
        }
        lastCallTimeAcceptor = currentTime;

        String acceptorLeaderUrl = electLeaderAcceptor(acceptors);

        try {
            AcceptorInterface acceptor;
            // Assign leadership to acceptors
            for (String acceptorUrl : acceptors) {
                acceptor = (AcceptorInterface) Naming.lookup(acceptorUrl);
                if (acceptor == null) {
                    continue;
                }
                boolean isLeader = acceptorUrl.equals(acceptorLeaderUrl);
                try {
                    acceptor.setLeader(isLeader);
                } catch (Exception e) {
                }
            }
            System.out.println("Acceptor Leader elected: " + acceptorLeaderUrl);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
