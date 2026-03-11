package paxos.network;

import paxos.messages.PaxosMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles network communication for a council member
 */
public class NetworkHandler {
    private String memberId;
    private ServerSocket serverSocket;
    private NetworkConfig networkConfig;
    private ExecutorService executor;
    private MessageHandler messageHandler;
    private String profile;
    private Random random = new Random();
    private volatile boolean running = true;

    // Network profile constants
    private static final int RELIABLE_DELAY = 10; // ms
    private static final int STANDARD_MIN_DELAY = 100;
    private static final int STANDARD_MAX_DELAY = 500;
    private static final int LATENT_MIN_DELAY = 2000;
    private static final int LATENT_MAX_DELAY = 5000;
    private static final double FAILURE_PROBABILITY = 0.3;

    /**
     * Interface for message handling callbacks
     */
    public interface MessageHandler {
        void handleMessage(PaxosMessage message);
    }

    /**
     * Constructor for NetworkHandler
     * @param memberId The member ID
     * @param networkConfig Network configuration
     * @param profile Network profile (reliable/standard/latent/failure)
     * @param messageHandler Callback for handling received messages
     * @throws IOException if server socket cannot be created
     */
    public NetworkHandler(String memberId, NetworkConfig networkConfig,
                          String profile, MessageHandler messageHandler) throws IOException {
        this.memberId = memberId;
        this.networkConfig = networkConfig;
        this.profile = profile;
        this.messageHandler = messageHandler;
        this.executor = Executors.newCachedThreadPool();

        MemberInfo info = networkConfig.getMemberInfo(memberId);
        if (info == null) {
            throw new IllegalArgumentException("Member " + memberId + " not found in config");
        }
        this.serverSocket = new ServerSocket(info.getPort());
    }

    /**
     * Starts listening for incoming connections
     */
    public void startListening() {
        executor.submit(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleIncomingConnection(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Handles an incoming connection
     * @param socket The client socket
     */
    private void handleIncomingConnection(Socket socket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            String messageStr = reader.readLine();
            if (messageStr != null) {
                // Simulate network delays based on profile
                simulateNetworkDelay();

                // Simulate failures
                if (shouldSimulateFailure()) {
                    System.out.println("[" + memberId + "] Simulating message drop");
                    return;
                }

                PaxosMessage message = PaxosMessage.fromNetworkString(messageStr);
                System.out.println("[" + memberId + "] Received: " + message.getType() +
                        " from " + message.getSenderId());
                messageHandler.handleMessage(message);
            }
        } catch (Exception e) {
            System.err.println("Error handling incoming connection: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Sends a message to another member
     * @param targetMemberId Target member ID
     * @param message Message to send
     */
    public void sendMessage(String targetMemberId, PaxosMessage message) {
        executor.submit(() -> {
            try {
                // Simulate network delays before sending
                simulateNetworkDelay();

                // Simulate failures
                if (shouldSimulateFailure()) {
                    System.out.println("[" + memberId + "] Simulating send failure to " + targetMemberId);
                    return;
                }

                MemberInfo targetInfo = networkConfig.getMemberInfo(targetMemberId);
                if (targetInfo == null) {
                    System.err.println("Target member " + targetMemberId + " not found");
                    return;
                }

                try (Socket socket = new Socket(targetInfo.getHostname(), targetInfo.getPort());
                     PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                    writer.println(message.toNetworkString());
                    System.out.println("[" + memberId + "] Sent: " + message.getType() +
                            " to " + targetMemberId);
                }
            } catch (Exception e) {
                System.err.println("Error sending message to " + targetMemberId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Broadcasts a message to all other members
     * @param message Message to broadcast
     */
    public void broadcast(PaxosMessage message) {
        Set<String> otherMembers = networkConfig.getOtherMemberIds(memberId);
        for (String targetId : otherMembers) {
            sendMessage(targetId, message);
        }
    }

    /**
     * Simulates network delay based on profile
     */
    private void simulateNetworkDelay() {
        try {
            switch (profile.toLowerCase()) {
                case "reliable":
                    Thread.sleep(RELIABLE_DELAY);
                    break;
                case "standard":
                    int standardDelay = STANDARD_MIN_DELAY +
                            random.nextInt(STANDARD_MAX_DELAY - STANDARD_MIN_DELAY);
                    Thread.sleep(standardDelay);
                    break;
                case "latent":
                    int latentDelay = LATENT_MIN_DELAY +
                            random.nextInt(LATENT_MAX_DELAY - LATENT_MIN_DELAY);
                    Thread.sleep(latentDelay);
                    break;
                case "failure":
                    // Failure profile has standard delay when not failing
                    Thread.sleep(STANDARD_MIN_DELAY);
                    break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Determines if a failure should be simulated
     * @return true if failure should be simulated
     */
    private boolean shouldSimulateFailure() {
        return profile.equalsIgnoreCase("failure") && random.nextDouble() < FAILURE_PROBABILITY;
    }

    /**
     * Shuts down the network handler
     */
    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            serverSocket.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Simulates a complete failure (crash)
     */
    public void simulateCrash() {
        System.out.println("[" + memberId + "] CRASHING!");
        running = false;
        shutdown();
        System.exit(0);
    }
}