package paxos;

import paxos.messages.PaxosMessage;
import paxos.network.NetworkConfig;
import paxos.network.NetworkHandler;
import paxos.roles.Proposer;
import paxos.roles.Acceptor;
import paxos.roles.Learner;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a council member participating in the Paxos consensus algorithm
 */
public class CouncilMember implements NetworkHandler.MessageHandler {
    private static final int TOTAL_MEMBERS = 9;

    private String memberId;
    private String profile;
    private NetworkConfig networkConfig;
    private NetworkHandler networkHandler;
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;
    private ExecutorService executor;

    /**
     * Constructor for CouncilMember
     * @param memberId The member ID (M1-M9)
     * @param profile Network profile (reliable/standard/latent/failure)
     * @param configFile Path to network configuration file
     * @throws IOException if network initialization fails
     */
    public CouncilMember(String memberId, String profile, String configFile) throws IOException {
        this.memberId = memberId;
        this.profile = profile;
        this.networkConfig = new NetworkConfig(configFile);
        this.networkHandler = new NetworkHandler(memberId, networkConfig, profile, this);
        this.proposer = new Proposer(memberId, networkHandler, TOTAL_MEMBERS);
        this.acceptor = new Acceptor(memberId, networkHandler);
        this.learner = new Learner(memberId, TOTAL_MEMBERS);
        this.executor = Executors.newCachedThreadPool();

        System.out.println("Council Member " + memberId + " initialized with profile: " + profile);
    }

    /** Starts the council member */
    public void start() {
        networkHandler.startListening();
        System.out.println("Council Member " + memberId + " is listening for messages...");

        // Start input handler for proposals (interactive only)
        startInputHandler();
    }

    /** Handles incoming messages */
    @Override
    public void handleMessage(PaxosMessage message) {
        executor.submit(() -> {
            switch (message.getType()) {
                case PREPARE:
                    acceptor.handlePrepare(message);
                    break;
                case PROMISE:
                    proposer.handlePromise(message);
                    break;
                case ACCEPT_REQUEST:
                    acceptor.handleAcceptRequest(message);
                    break;
                case ACCEPTED:
                    proposer.handleAccepted(message);
                    learner.handleAccepted(message);
                    if (learner.hasLearned()) {
                        // keep your existing line if you like:
                        System.out.println("Consensus reached: " + learner.getLearnedValue());
                        // add the assignment-friendly line:
                        System.out.println("CONSENSUS: " + learner.getLearnedValue() + " has been elected Council President!");
                    }
                    break;
                case NACK:
                    System.out.println("[" + memberId + "] Received NACK from " + message.getSenderId());
                    break;
            }
        });
    }

    /** Starts the input handler for initiating proposals */
    private void startInputHandler() {
        // CHANGE: Skip stdin thread in non-interactive (tests/CI) environments
        if (System.console() == null) {
            return;
        }

        Thread inputThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("Enter a candidate name to propose (or 'exit' to quit):");
                while (true) {
                    // CHANGE: guard against closed stdin to avoid NoSuchElementException
                    if (!scanner.hasNextLine()) {
                        try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                        continue;
                    }
                    String input = scanner.nextLine().trim();

                    if (input.equalsIgnoreCase("exit")) {
                        shutdown();
                        break;
                    }

                    if (input.equalsIgnoreCase("crash") && profile.equalsIgnoreCase("failure")) {
                        networkHandler.simulateCrash();
                        break;
                    }

                    if (!input.isEmpty() && !learner.hasLearned()) {
                        System.out.println("Proposing " + input + " for Council President");
                        executor.submit(() -> {
                            boolean success = proposer.propose(input);
                            if (!success) {
                                System.out.println("Proposal failed. You can try again.");
                            }
                        });
                    } else if (learner.hasLearned()) {
                        System.out.println("Consensus already reached: " + learner.getLearnedValue() + " is the President!");
                    }
                }
            } catch (Exception ignored) {
                // CHANGE: swallow scanner/STDIN exceptions quietly in interactive thread
            }
        }, "InputHandler");
        inputThread.setDaemon(true);
        inputThread.start();
    }

    /** Initiates a proposal programmatically */
    public void proposeCandidate(String candidate) {
        if (!learner.hasLearned()) {
            executor.submit(() -> {
                // Add a small delay to ensure all members are ready
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("[" + memberId + "] Proposing " + candidate);
                boolean success = proposer.propose(candidate);
                if (!success) {
                    System.out.println("[" + memberId + "] Proposal failed");
                }
            });
        }
    }

    /** Shuts down the council member */
    public void shutdown() {
        System.out.println("Shutting down Council Member " + memberId);
        networkHandler.shutdown();
        executor.shutdown();
    }

    /**
     * Main method to run a council member
     * @param args Command line arguments: memberId [--profile profileType] [--propose candidate]
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java CouncilMember <memberId> [--profile <type>] [--propose <candidate>]");
            System.err.println("Profiles: reliable, standard, latent, failure");
            System.exit(1);
        }

        String memberId = args[0];
        String profile = "standard"; // Default profile
        String proposalCandidate = null;

        // Parse command line arguments
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--profile") && i + 1 < args.length) {
                profile = args[i + 1];
                i++;
            } else if (args[i].equals("--propose") && i + 1 < args.length) {
                proposalCandidate = args[i + 1];
                i++;
            }
        }

        try {
            CouncilMember member = new CouncilMember(memberId, profile, "config/network.config");
            member.start();

            // If a proposal was specified, initiate it (works in headless mode)
            if (proposalCandidate != null) {
                member.proposeCandidate(proposalCandidate);
            }

            // Keep the program running
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Error starting council member: " + e.getMessage());
            e.printStackTrace();
        }
    }
}