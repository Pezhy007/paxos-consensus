package paxos.roles;

import paxos.messages.PaxosMessage;
import paxos.messages.ProposalNumber;
import paxos.network.NetworkHandler;

/**
 * Implements the Acceptor role in Paxos
 */
public class Acceptor {
    private String memberId;
    private NetworkHandler networkHandler;
    private ProposalNumber promisedProposal;
    private ProposalNumber acceptedProposal;
    private String acceptedValue;

    /**
     * Constructor for Acceptor
     * @param memberId The member ID
     * @param networkHandler Network handler for communication
     */
    public Acceptor(String memberId, NetworkHandler networkHandler) {
        this.memberId = memberId;
        this.networkHandler = networkHandler;
    }

    /**
     * Handles a PREPARE message (Phase 1b)
     * @param message The PREPARE message
     */
    public synchronized void handlePrepare(PaxosMessage message) {
        ProposalNumber proposalNumber = message.getProposalNumber();

        System.out.println("[" + memberId + "] Received PREPARE " + proposalNumber +
                " from " + message.getSenderId());

        if (promisedProposal == null || proposalNumber.compareTo(promisedProposal) > 0) {
            // Promise not to accept proposals numbered less than n
            promisedProposal = proposalNumber;

            // Send PROMISE
            PaxosMessage promise = new PaxosMessage(
                    PaxosMessage.MessageType.PROMISE, memberId, proposalNumber);

            // Include any previously accepted proposal
            if (acceptedProposal != null) {
                promise.setAcceptedProposalNumber(acceptedProposal);
                promise.setAcceptedValue(acceptedValue);
            }

            networkHandler.sendMessage(message.getSenderId(), promise);
            System.out.println("[" + memberId + "] Sent PROMISE to " + message.getSenderId());
        } else {
            // Send NACK
            PaxosMessage nack = new PaxosMessage(
                    PaxosMessage.MessageType.NACK, memberId, proposalNumber);
            networkHandler.sendMessage(message.getSenderId(), nack);
            System.out.println("[" + memberId + "] Sent NACK to " + message.getSenderId() +
                    " (already promised " + promisedProposal + ")");
        }
    }

    /**
     * Handles an ACCEPT_REQUEST message (Phase 2b)
     * @param message The ACCEPT_REQUEST message
     */
    public synchronized void handleAcceptRequest(PaxosMessage message) {
        ProposalNumber proposalNumber = message.getProposalNumber();
        String value = message.getProposalValue();

        System.out.println("[" + memberId + "] Received ACCEPT_REQUEST " + proposalNumber +
                " with value: " + value + " from " + message.getSenderId());

        if (promisedProposal == null || proposalNumber.compareTo(promisedProposal) >= 0) {
            // Accept the proposal
            promisedProposal = proposalNumber;
            acceptedProposal = proposalNumber;
            acceptedValue = value;

            // Send ACCEPTED
            PaxosMessage accepted = new PaxosMessage(
                    PaxosMessage.MessageType.ACCEPTED, memberId, proposalNumber);
            accepted.setProposalValue(value);

            // Broadcast to all (including learners)
            networkHandler.broadcast(accepted);
            System.out.println("[" + memberId + "] Broadcasted ACCEPTED for value: " + value);
        } else {
            // Send NACK
            PaxosMessage nack = new PaxosMessage(
                    PaxosMessage.MessageType.NACK, memberId, proposalNumber);
            networkHandler.sendMessage(message.getSenderId(), nack);
            System.out.println("[" + memberId + "] Sent NACK to " + message.getSenderId() +
                    " (promised higher: " + promisedProposal + ")");
        }
    }

    /**
     * Gets the currently accepted value
     * @return The accepted value or null
     */
    public String getAcceptedValue() {
        return acceptedValue;
    }

    /**
     * Gets the currently accepted proposal number
     * @return The accepted proposal number or null
     */
    public ProposalNumber getAcceptedProposal() {
        return acceptedProposal;
    }
}