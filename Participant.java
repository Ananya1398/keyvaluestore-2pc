import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The Participant class implements the ParticipantInterface. It represents a server replica in a
 * two-phase commit (2PC) system. Each participant manages its own key-value store and performs
 * operations like PUT, GET, and DELETE based on client requests. The participant communicates with
 * the coordinator to ensure that operations are consistent across all replicas using the 2PC protocol.
 * The coordinator calls the prepare, commit and rollback functions defined here to make all participants
 * consistent.
 */
public class Participant extends UnicastRemoteObject implements ParticipantInterface {

  private final String participantId;
  private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
  private final CoordinatorInterface coordinator;
  private final KeyValueStoreOperations keyValueStore;

  /**
   * Constructor for Participant.
   *
   * @param participantId ID of the participant
   * @param coordinator   Reference to the coordinator
   * @throws RemoteException If an RMI error occurs
   */
  protected Participant(String participantId, CoordinatorInterface coordinator) throws RemoteException {
    super();
    this.participantId = participantId;
    this.coordinator = coordinator;
    this.keyValueStore = new KeyValueStoreOperations(); // Use the shared logic
  }

  /**
   * Logs a message with a timestamp and thread name.
   * @param message The message to log.
   */
  private void log(String message) {
    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    String threadName = Thread.currentThread().getName();
    System.out.println("[" + timestamp + "] [" + threadName + "] " + participantId + ": " + message);
  }

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
  @Override
  public boolean prepare(String operation, String key, String value, String clientName, String participantName) throws RemoteException {
    log(clientName + participantName +"Received PREPARE for operation: " + operation + " on key: " + key + " with value: " + value);
    return true;
  }

  /**
   * Commits the prepared operation and calls the actual string operations from the KeyValueStoreOperations
   * class.
   *
   * @param operation The operation to execute.
   * @param key The key involved in the operation.
   * @param value The value (if applicable).
   * @throws RemoteException If a remote communication error occurs.
   */
  @Override
  public boolean commit(String operation, String key, String value) throws RemoteException {
    log("Received COMMIT for operation: " + operation + " on key: " + key + " with value: " + value);

    switch (operation.toUpperCase()) {
      case "PUT":
        return keyValueStore.put(key, value, participantId, store);


      case "DELETE":
        return keyValueStore.delete(key, participantId, store);


      case "GET":
        return keyValueStore.get(key, participantId, store);


      default:
        log("Invalid operation during COMMIT: " + operation);
        return false;

    }
  }

  /**
   * Rolls back the operation.
   *
   * @throws RemoteException If a remote communication error occurs.
   */
  @Override
  public void rollback() throws RemoteException {
    log("Received ROLLBACK — No state change was committed.");
  }

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
  @Override
  public String put(String key, String value, String clientName, String participantName) throws RemoteException {
    if (coordinator.initiatePreparePhase("PUT", key, value, clientName, participantName)) {
      log(clientName+ " " + participantName + " Prepare successful for PUT operation." + " Key: " + key + " Value: " + value);
      return(clientName + " " + participantName + " Successfully added key: " + key + " with Value: " + value);
    } else {
      log(clientName+ " " + participantName + " Error: PUT operation aborted.");
      return(clientName+ " " + participantName + " Error: PUT operation aborted due to key: ." + key + " already existing or prepare/commit issues");
    }
  }

  /**
   * Retrieves the value associated with a key.
   * Calls the initiatePrepare function of the coordinator for the GET operation, which passes control.
   *
   * @param key The key whose value is being retrieved.
   * @param clientName The client performing the operation.
   * @param participantName The server replica performing the operation.
   * @return The value associated with the key, or an error message if the key is not found.
   * @throws RemoteException If an RMI error occurs.
   */
  @Override
  public String get(String key, String clientName, String participantName) throws RemoteException {
    log(clientName + " " + participantName + " received GET request for key=" + key);

    if (coordinator.initiatePreparePhase("GET", key, null, clientName, participantName)) {
      String value = store.get(key);
      if (value != null) {
        log(participantName + " :GET successful for Key:" + key + ", Value is" + value);
        return participantName + " :Value for key " + key + " is: " + value;
      } else {
        log(participantName + " :GET failed for Key:" + key + " key not found");
        return participantName + " :GET failed for Key:" + key + " Key not found";
      }
    } else {
      log(participantName + " :Error: GET operation aborted.");
      return participantName + " :Error: GET operation aborted.";
    }
  }

  /**
   * Calls the initiatePrepare function of the coordinator for the DELETE operation, which passes control
   * and performs 2PC
   *
   * @param key The key to delete.
   * @param clientName The client performing the operation.
   * @param participantName The server replica performing the operation.
   * @return A success or failure message.
   * @throws RemoteException If an RMI error occurs.
   */
  @Override
  public String delete(String key, String clientName, String participantName) throws RemoteException {
    if (coordinator.initiatePreparePhase("DELETE", key, null, clientName, participantName)) {
      log(clientName+ " " + participantName + " Prepare successful for DELETE operation."  + " Key: " + key);
      return(clientName + " "  + participantName + " Successfully DELETED key: " + key);
    } else {
      log(clientName+ " " + participantName + " Error: DELETE operation aborted."  + " Key: " + key);
      return(clientName+ " " + participantName + " Error: DELETE operation aborted due to key: ." + key + " not existing or prepare/commit issues");
    }
  }
}