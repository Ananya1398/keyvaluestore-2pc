import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * CoordinatorInterface defines the two-phase commit process.
 */
public interface CoordinatorInterface extends Remote {

  /**
   * Registers a participant with the coordinator.
   *
   * @param participantId The ID of the participant.
   * @param host The hostname of the participant.
   * @param port The port of the participant.
   * @throws RemoteException If a remote communication error occurs.
   */
  void registerParticipant(String participantId, String host, int port) throws RemoteException;

  /**
   * Initiates the prepare phase of the two-phase commit.
   *
   * @param operation The operation (PUT, GET, DELETE).
   * @param key The key involved in the operation.
   * @param value The value (if applicable).
   * @param clientName The client initiating the request.
   * @return True if all participants are ready to commit; False if any participant rejects the request.
   * @throws RemoteException If a remote communication error occurs.
   */
  boolean initiatePreparePhase(String operation, String key, String value, String clientName, String participantName) throws RemoteException;

  /**
   * Sends a commit message to all participants.
   *
   * @param operation The operation (PUT, GET, DELETE).
   * @param key The key involved in the operation.
   * @param value The value (if applicable).
   * @throws RemoteException If a remote communication error occurs.
   */
  boolean sendCommitToAll(String operation, String key, String value, String clientName, String participantName) throws RemoteException;

  /**
   * Sends a rollback message to all participants.
   *
   * @throws RemoteException If a remote communication error occurs.
   */
  void sendRollbackToAll(String clientName, String participantName) throws RemoteException;
}
