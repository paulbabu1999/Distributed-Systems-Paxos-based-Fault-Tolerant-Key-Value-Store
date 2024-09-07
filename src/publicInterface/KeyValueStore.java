package publicInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The KeyValueStore interface defines remote methods for managing key-value pairs.
 * It extends the Remote interface to indicate that it supports remote method invocation (RMI).
 */
public interface KeyValueStore extends Remote {

    /**
     * Executes a command on the key-value store based on the provided client identifier and command.
     *
     * @param clientIdentifier the identifier of the client making the request
     * @param command          the command to be executed (e.g., PUT key value, GET key, DELETE key)
     * @return a response string based on the command execution
     * @throws RemoteException if there is an issue with the remote method invocation
     */
    String executeCommand(String clientIdentifier, String command) throws RemoteException;
}
