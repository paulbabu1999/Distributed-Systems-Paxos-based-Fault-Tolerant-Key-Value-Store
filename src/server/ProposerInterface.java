package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The ProposerInterface defines the remote methods that the Proposer class must implement.
 * It extends the Remote interface to allow remote method calls via RMI.
 */
public interface ProposerInterface extends Remote {

    /**
     * Sets the value for the proposal.
     *
     * @param value The value to be set for the proposal.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    void setValue(String value) throws RemoteException;

    /**
     * Sets the leadership status of the proposer.
     *
     * @param isLeader A boolean indicating whether this proposer is the leader or not.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    void setLeader(boolean isLeader) throws RemoteException;

    /**
     * Receives a heartbeat signal to indicate that the leader is still alive.
     *
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    void receiveHeartbeat() throws RemoteException;

    /**
     * Initiates a proposal for the given client.
     *
     * @param client The identifier of the client making the proposal.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    void propose(String client) throws RemoteException;
}
