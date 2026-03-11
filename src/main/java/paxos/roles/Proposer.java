package paxos.roles;

import paxos.messages.PaxosMessage;
import paxos.messages.ProposalNumber;
import paxos.network.NetworkHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Implements the Proposer role in Paxos
 */
public class Proposer {
    private String memberId;
    private NetworkHandler networkHandler;
    private int proposalRound = 0;
    private ProposalNumber currentProposal;
    private String proposedValue;
    private Map<String, PaxosMessage> promises;
    private Map<String, PaxosMessage> accepted;
    private int majoritySize;
    private CountDownLatch promiseLatch;
    private CountDownLatch acceptedLatch;

    /**
     * Constructor for Proposer
     * @param memberId The member ID
     * @param networkHandler Network handler for communication
     * @param totalMembers Total number of members
     */
    public Proposer(String memberId, NetworkHandler networkHandler, int totalMembers) {
        this.memberId = memberId;
        this.networkHandler = networkHandler;
        this.majoritySize = (totalMembers / 2) + 1;
        this.promises = new ConcurrentHashMap<>();
        this.accepted = new ConcurrentHashMap<>();
    }

    /**
     * Proposes a value for consensus
     * @param value The value to propose
     * @return true if consensus reached, false otherwise
     */
    public boolean propose(String value) {
        proposedValue = value;
        proposalRound++;
        currentProposal = new ProposalNumber(proposalRound, memberId);
        promises.clear();
        accepted.clear();

        System.out.println("[" + memberId + "] Starting proposal " + currentProposal +
                " with value: " + value);

        // Phase 1a: Send PREPARE
        sendPrepare();

        // Wait for promises (Phase 1b)
        if (!waitForPromises()) {
            System.out.println("[" + memberId + "] Failed to get majority promises");
            return false;
        }

        // Phase 2a: Send ACCEPT_REQUEST
        sendAcceptRequest();

        // Wait for accepted (Phase 2b)
        if (!waitForAccepted()) {
            System.out.println("[" + memberId + "] Failed to get majority acceptances");
            return false;
        }

        System.out.println("[" + memberId + "] Consensus reached for: " + proposedValue);
        return true;
    }

    /**
     * Sends PREPARE messages (Phase 1a)
     */
    private void sendPrepare() {
        PaxosMessage prepareMsg = new PaxosMessage(
                PaxosMessage.MessageType.PREPARE, memberId, currentProposal);
        networkHandler.broadcast(prepareMsg);
        promiseLatch = new CountDownLatch(1);
    }

    /**
     * Sends ACCEPT_REQUEST messages (Phase 2a)
     */
    private void sendAcceptRequest() {
        // Check if any promise had a previously accepted value
        String valueToPropose = proposedValue;
        ProposalNumber highestAccepted = null;

        for (PaxosMessage promise : promises.values()) {
            if (promise.getAcceptedProposalNumber() != null) {
                if (highestAccepted == null ||
                        promise.getAcceptedProposalNumber().compareTo(highestAccepted) > 0) {
                    highestAccepted = promise.getAcceptedProposalNumber();
                    valueToPropose = promise.getAcceptedValue();
                }
            }
        }

        proposedValue = valueToPropose;
        System.out.println("[" + memberId + "] Proposing value in Phase 2: " + proposedValue);

        PaxosMessage acceptRequest = new PaxosMessage(
                PaxosMessage.MessageType.ACCEPT_REQUEST, memberId, currentProposal);
        acceptRequest.setProposalValue(proposedValue);
        networkHandler.broadcast(acceptRequest);
        acceptedLatch = new CountDownLatch(1);
    }

    /**
     * Handles a PROMISE message
     * @param message The PROMISE message
     */
    public synchronized void handlePromise(PaxosMessage message) {
        if (message.getProposalNumber().equals(currentProposal)) {
            promises.put(message.getSenderId(), message);
            System.out.println("[" + memberId + "] Received PROMISE from " +
                    message.getSenderId() + " (" + promises.size() + "/" + majoritySize + ")");

            if (promises.size() >= majoritySize && promiseLatch != null) {
                promiseLatch.countDown();
            }
        }
    }

    /**
     * Handles an ACCEPTED message
     * @param message The ACCEPTED message
     */
    public synchronized void handleAccepted(PaxosMessage message) {
        if (message.getProposalNumber().equals(currentProposal)) {
            accepted.put(message.getSenderId(), message);
            System.out.println("[" + memberId + "] Received ACCEPTED from " +
                    message.getSenderId() + " (" + accepted.size() + "/" + majoritySize + ")");

            if (accepted.size() >= majoritySize && acceptedLatch != null) {
                acceptedLatch.countDown();
            }
        }
    }

    /**
     * Waits for majority promises
     * @return true if majority received, false if timeout
     */
    private boolean waitForPromises() {
        try {
            return promiseLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Waits for majority acceptances
     * @return true if majority received, false if timeout
     */
    private boolean waitForAccepted() {
        try {
            return acceptedLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Gets the current proposed value
     * @return The proposed value
     */
    public String getProposedValue() {
        return proposedValue;
    }
}