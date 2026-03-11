package paxos.messages;

import java.io.Serializable;

/**
 * Base class for all Paxos protocol messages
 */
public class PaxosMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        PREPARE, PROMISE, ACCEPT_REQUEST, ACCEPTED, NACK
    }

    private MessageType type;
    private String senderId;
    private ProposalNumber proposalNumber;
    private String proposalValue;
    private ProposalNumber acceptedProposalNumber;
    private String acceptedValue;

    /**
     * Constructor for PaxosMessage
     * @param type The type of message
     * @param senderId The ID of the sender
     * @param proposalNumber The proposal number
     */
    public PaxosMessage(MessageType type, String senderId, ProposalNumber proposalNumber) {
        this.type = type;
        this.senderId = senderId;
        this.proposalNumber = proposalNumber;
    }

    /**
     * Converts message to string format for network transmission
     * @return String representation of the message
     */
    public String toNetworkString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name()).append(":");
        sb.append(senderId).append(":");
        sb.append(proposalNumber.toString()).append(":");
        sb.append(proposalValue != null ? proposalValue : "null").append(":");
        sb.append(acceptedProposalNumber != null ? acceptedProposalNumber.toString() : "null").append(":");
        sb.append(acceptedValue != null ? acceptedValue : "null");
        return sb.toString();
    }

    /**
     * Parses a network string to create a PaxosMessage
     * @param networkString The string received from network
     * @return PaxosMessage object
     */
    public static PaxosMessage fromNetworkString(String networkString) {
        String[] parts = networkString.split(":");
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid message format");
        }

        MessageType type = MessageType.valueOf(parts[0]);
        String senderId = parts[1];
        ProposalNumber proposalNumber = ProposalNumber.fromString(parts[2]);

        PaxosMessage msg = new PaxosMessage(type, senderId, proposalNumber);
        msg.setProposalValue(parts[3].equals("null") ? null : parts[3]);
        msg.setAcceptedProposalNumber(parts[4].equals("null") ? null : ProposalNumber.fromString(parts[4]));
        msg.setAcceptedValue(parts[5].equals("null") ? null : parts[5]);

        return msg;
    }

    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public ProposalNumber getProposalNumber() { return proposalNumber; }
    public void setProposalNumber(ProposalNumber proposalNumber) { this.proposalNumber = proposalNumber; }

    public String getProposalValue() { return proposalValue; }
    public void setProposalValue(String proposalValue) { this.proposalValue = proposalValue; }

    public ProposalNumber getAcceptedProposalNumber() { return acceptedProposalNumber; }
    public void setAcceptedProposalNumber(ProposalNumber acceptedProposalNumber) {
        this.acceptedProposalNumber = acceptedProposalNumber;
    }

    public String getAcceptedValue() { return acceptedValue; }
    public void setAcceptedValue(String acceptedValue) { this.acceptedValue = acceptedValue; }
}