package server;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The AcceptorInterface defines the remote methods that the Acceptor class must implement.
 * It extends the Remote interface to allow remote method calls via RMI.
 */
public interface AcceptorInterface extends Remote {

    /**
     * Handles the prepare phase of the Paxos algorithm.
     *
     * @param proposalNumber The proposal number being prepared.
     * @return A response indicating whether the proposal was accepted or rejected.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    String prepare(int proposalNumber) throws RemoteException;

    /**
     * Handles the accept phase of the Paxos algorithm.
     *
     * @param proposalNumber The proposal number being accepted.
     * @param value The value associated with the proposal.
     * @return A response indicating whether the proposal was accepted or rejected.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    String accept(int proposalNumber, String value) throws RemoteException;

    /**
     * Instructs the acceptor to notify learners about the accepted value.
     *
     * @param value The value to be learned by learners.
     * @return A response indicating that the value has been learned.
     * @throws RemoteException If there is a communication issue with the remote method call.
     * @throws MalformedURLException If there is an issue with the URL formatting.
     * @throws NotBoundException If the URL is not bound in the RMI registry.
     */
    String learn(String value) throws RemoteException, MalformedURLException, NotBoundException;

    /**
     * Checks if this acceptor is currently the leader.
     *
     * @return True if the acceptor is the leader, otherwise false.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    boolean isLeader() throws RemoteException;

    /**
     * Sets the leadership status of the acceptor.
     *
     * @param isLeader A boolean indicating whether this acceptor should be the leader.
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
     * Handles a prepare request from a proposer.
     *
     * @param proposalNumber The proposal number being prepared.
     * @return A response indicating whether the proposal was promised or rejected.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    String handlePrepareRequest(int proposalNumber) throws RemoteException;

    /**
     * Handles an accept request from a proposer.
     *
     * @param proposalNumber The proposal number being accepted.
     * @param value The value associated with the proposal.
     * @return A response indicating whether the proposal was accepted or rejected.
     * @throws RemoteException If there is a communication issue with the remote method call.
     */
    String handleAcceptRequest(int proposalNumber, String value) throws RemoteException;
}
