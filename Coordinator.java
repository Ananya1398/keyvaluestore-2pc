import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * The Coordinator class manages the two-phase commit (2PC) process among multiple participants.
 * It initiates the prepare phase, and handles the commit or rollback phase depending on the outcome
 * of the prepare phase. The class ensures consistency across all participants by coordinating the
 * execution of PUT, GET, and DELETE operations.
 */

public class Coordinator extends UnicastRemoteObject implements CoordinatorInterface {

  private final List<ParticipantInterface> participants = new ArrayList<>();

  /**
   * Constructor for Coordinator.
   * @throws RemoteException If an RMI error occurs.
   */
  protected Coordinator() throws RemoteException {
    super();
  }

  /**
   * Logs a message with a timestamp.
   * @param message The message to log.
   */
  private static void log(String message) {
    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    String logMessage = "[" + timestamp + "] " + message;
    System.out.println(logMessage);
  }

  /**
   * Registers a participant with the coordinator.
   * @param participantId The ID of the participant.
   * @param host The hostname of the participant.
   * @param port The port of the participant.
   */
  @Override
  public void registerParticipant(String participantId, String host, int port) throws RemoteException {
    try {
      Registry registry = LocateRegistry.getRegistry(host, port);
      ParticipantInterface participant = (ParticipantInterface) registry.lookup(participantId);

      participants.add(participant);

      log("Registered participant: " + participantId + " at " + host + ":" + port);
    } catch (Exception e) {
      log("Failed to register participant: " + participantId + " - " + e.getMessage());
      throw new RemoteException("Failed to register participant.");
    }
  }

  /**
   * Initiates the prepare phase of the two-phase commit.
   * If commit fails, rollback happens and it returns false.
   *
   * @param operation The operation to commit (PUT, DELETE).
   * @param key The key involved.
   * @param value The value involved.
   * @param clientName The client initiating the request.
   * @return True if all participants committed successfully; false otherwise.
   */
  @Override
  public boolean initiatePreparePhase(String operation, String key, String value, String clientName, String participantName) throws RemoteException {
    log("Starting PREPARE phase for operation: " + operation + " on key: " + key);

    for (ParticipantInterface participant : participants) {
      boolean isPrepared;
      try {
        isPrepared = participant.prepare(operation, key, value, clientName, participantName);
        if (!isPrepared) {
          log(clientName+ " " + participantName + " PREPARE phase failed at participant. Sending ROLLBACK...");
          sendRollbackToAll(clientName, participantName);
          return false;
        }
      } catch (RemoteException e) {
        log(clientName+ " " + participantName + " Participant unreachable during PREPARE. Rolling back...");
        sendRollbackToAll(clientName, participantName);
        throw new RemoteException("Participant unreachable during PREPARE");
      }
    }

    log("All participants prepared successfully. Proceeding to COMMIT phase...");

    boolean allCommitted = sendCommitToAll(operation, key, value, clientName, participantName);
    if (allCommitted) {
      log("COMMIT phase successful. Operation committed on all participants.");
      return true;
    } else {
      log("COMMIT phase failed. Triggering ROLLBACK...");
      sendRollbackToAll(clientName, participantName);
      return false;
    }
  }

  /**
   * Sends a commit message to all participants.
   * If any participant fails, rollback is triggered.
   *
   * @param operation The operation to commit.
   * @param key The key involved.
   * @param value The value involved.
   */
  @Override
  public boolean sendCommitToAll(String operation, String key, String value, String clientName, String participantName) throws RemoteException {
    log("Sending COMMIT message to all participants...");

    for (ParticipantInterface participant : participants) {
      try {
        participant.commit(operation, key, value);
        log(clientName + " " + participantName + " Committed operation " + operation + " on key: " + key);
      } catch (RemoteException e) {
        log(clientName+ " " + participantName + " Failed to commit to participant: " + e.getMessage());
        sendRollbackToAll(clientName, participantName);
        return false;
      }
    }
    return true;
  }

  /**
   * Sends a rollback message to all participants.
   * Called if any participant fails during commit phase.
   */
  @Override
  public void sendRollbackToAll(String clientName, String participantName) throws RemoteException {
    log("Sending ROLLBACK message to all participants...");

    for (ParticipantInterface participant : participants) {
      try {
        participant.rollback();
        log("Rollback successful.");
      } catch (RemoteException e) {
        log("Failed to rollback on participant: " + e.getMessage());
      }
    }
  }
}
