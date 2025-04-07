import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * ParticipantInterface defines the operations that a participant must implement
 * to support the two-phase commit protocol.
 */
public interface ParticipantInterface extends Remote {

  /**
   * Prepares the operation and returns a boolean indicating readiness.
   *
   * @param operation The operation to execute.
   * @param key The key involved in the operation.
   * @param value The value (if applicable).
   * @param clientName The client performing the operation.
   * @param participantName The server replica performing the operation.
   * @return True if ready to commit; False if not ready.
   * @throws RemoteException If a remote communication error occurs.
   */
  boolean prepare(String operation, String key, String value, String clientName, String participantName) throws RemoteException;

  /**
   * Commits the prepared operation and calls the actual string operations from the KeyValueStoreOperations
   * class.
   *
   * @param operation The operation to execute.
   * @param key The key involved in the operation.
   * @param value The value (if applicable).
   * @throws RemoteException If a remote communication error occurs.
   */
  boolean commit(String operation, String key, String value) throws RemoteException;

  /**
   * Rolls back the operation.
   *
   * @throws RemoteException If a remote communication error occurs.
   */
  void rollback() throws RemoteException;

  /**
   * Calls the initiatePrepare function of the coordinator for the PUT operation, which passes control
   * and performs 2PC
   *
   * @param key The key to store.
   * @param value The value associated with the key.
   * @param clientName The client performing the operation.
   * @param participantName The server replica performing the operation.
   * @return A success message or error message.
   * @throws RemoteException If an RMI error occurs.
   */
  String put(String key, String value, String clientName, String participantName) throws RemoteException;

  /**
   * Calls the initiatePrepare function of the coordinator for the GET operation, which passes control.
   * and performs 2PC.
   *
   * @param key The key whose value is being retrieved.
   * @param clientName The client performing the operation.
   * @param participantName The server replica performing the operation.
   * @return The value associated with the key, or an error message if the key is not found.
   * @throws RemoteException If an RMI error occurs.
   */
  String get(String key, String clientName, String participantName) throws RemoteException;

  /**
   * Calls the initiatePrepare function of the coordinator for the DELETE operation, which passes control
   * and performs 2PC.
   *
   * @param key The key to delete.
   * @param clientName The client performing the operation.
   * @param participantName The server replica performing the operation.
   * @return A success or failure message.
   * @throws RemoteException If an RMI error occurs.
   */
  String delete(String key, String clientName, String participantName) throws RemoteException;
}
