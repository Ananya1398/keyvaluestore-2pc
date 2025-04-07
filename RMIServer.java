import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * The RMIServer class registers the Participants and Coordinator remote object to the RMI registry,
 * allowing clients to call its methods remotely.
 */
public class RMIServer {

  private static final String SERVER_LIST_FILE = "servers.txt";

  /**
   * Logs a message with a timestamp and thread name
   * @param message The message to log.
   */
  private static void log(String message) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    String timestamp = sdf.format(new Date());
    String fullThreadName = Thread.currentThread().getName();
    System.out.println("[" + timestamp + "] [" + fullThreadName + "] " + message);
  }

  /**
   * Main method to start the RMI server(s) with a user-defined port.
   * The coordinator is started on one port before this.
   * The server replicas are started on 5 consecutive ports including user-defined port.
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      log("Missing arguments. Please use this format: java KeyValueStoreRMIServer <port>");
      return;
    }

    int port;
    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      log("Invalid port number. Please enter a valid integer for the port.");
      return;
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(SERVER_LIST_FILE, false))) {
      writer.write("");
    } catch (IOException e) {
      log("Failed to clear server list file: " + e.getMessage());
      return;
    }

    CoordinatorInterface coordinator = null;
    int coordinatorPort = port -1;

    try {
      log("Starting Coordinator on port " + coordinatorPort);

      try {
        Registry registry = LocateRegistry.createRegistry(coordinatorPort);
        coordinator = new Coordinator();

        registry.rebind("Coordinator", coordinator);
        log("Coordinator is running on port " + coordinatorPort);
      } catch (RemoteException e) {
        log("Coordinator port already in use: " + coordinatorPort);
        return;
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(SERVER_LIST_FILE, true))) {
        writer.write("Coordinator running on port: " + coordinatorPort);
        writer.newLine();
        log("Coordinator added to server list on port: " + coordinatorPort);
      }catch (IOException e) {
        log("Failed to write coordinator info to file: " + e.getMessage());
      }

      for (int i = 0; i < 5; i++) {
        int nextPort = port + (i);
        String participantId = "ServerReplica-" + (i + 1);

        log("Starting " + participantId + " on port " + nextPort);

        Registry participantRegistry;
        try {
          participantRegistry = LocateRegistry.createRegistry(nextPort);
        } catch (RemoteException e) {
          log("Port " + nextPort + " already in use. Trying to bind to existing registry.");
          participantRegistry = LocateRegistry.getRegistry(nextPort);
        }
        ParticipantInterface participant = new Participant(participantId, coordinator);

        participantRegistry.rebind(participantId, participant);
        log(participantId + " is running on port " + nextPort + "... Waiting for client connections...");
        writeServerInfo(nextPort, participantId);

        try {
          coordinator.registerParticipant(participantId, "localhost", nextPort);
          log(participantId + " registered with coordinator.");
        } catch (Exception e) {
          log("Failed to register " + participantId + " with coordinator: " + e.getMessage());
        }
      }

      log("All 5 servers started successfully!\n");
      displayAvailableServers();

    } catch (RemoteException e) {
      log("RMI Server exception: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Writes the server ID and port information to the servers.txt file.
   *
   * @param port The server port number.
   * @param serverId The server identifier.
   */
  private static void writeServerInfo(int port, String serverId) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(SERVER_LIST_FILE, true))) {
      writer.write(serverId + " running on port: " + port);
      writer.newLine();
      log(serverId + " added to server list on port: " + port);
    } catch (IOException e) {
      log("Failed to write server info to file: " + e.getMessage());
    }
  }

  /**
   * Displays the list of available servers from the file.
   * This will help clients know which ports to connect to.
   */
  private static void displayAvailableServers() {
    log("Available coordinator and participants:");
    try (BufferedReader reader = new BufferedReader(new FileReader(SERVER_LIST_FILE))) {
      String line;
      while ((line = reader.readLine()) != null) {
        log(line);
      }
    } catch (IOException e) {
      log("Failed to read server list: " + e.getMessage());
    }
  }
}
