package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The LearnerInterface defines the remote methods that the Learner class must implement.
 * It extends the Remote interface to allow remote method calls via RMI.
 */
public interface LearnerInterface extends Remote {

    /**
     * This method is used by acceptors to notify learners of a new value that should be learned.
     *
     * @param value The value to be learned, typically a command in the form of a string.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    void learn(String value) throws RemoteException;
}
