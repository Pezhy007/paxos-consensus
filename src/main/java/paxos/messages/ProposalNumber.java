package paxos.messages;

import java.io.Serializable;

/**
 * Represents a unique proposal number in Paxos
 * Format: round.memberId (e.g., 3.M1)
 */
public class ProposalNumber implements Serializable, Comparable<ProposalNumber> {
    private static final long serialVersionUID = 1L;

    private int round;
    private String memberId;

    /**
     * Constructor for ProposalNumber
     * @param round The round number
     * @param memberId The member ID
     */
    public ProposalNumber(int round, String memberId) {
        this.round = round;
        this.memberId = memberId;
    }

    /**
     * Compares two proposal numbers for ordering
     * @param other The other ProposalNumber to compare
     * @return negative if this < other, positive if this > other, 0 if equal
     */
    @Override
    public int compareTo(ProposalNumber other) {
        if (this.round != other.round) {
            return Integer.compare(this.round, other.round);
        }
        return this.memberId.compareTo(other.memberId);
    }

    /**
     * Checks equality of two proposal numbers
     * @param obj The object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProposalNumber other = (ProposalNumber) obj;
        return round == other.round && memberId.equals(other.memberId);
    }

    /**
     * Generates hash code for the proposal number
     * @return hash code
     */
    @Override
    public int hashCode() {
        return round * 31 + memberId.hashCode();
    }

    /**
     * Converts proposal number to string format
     * @return String representation (e.g., "3.M1")
     */
    @Override
    public String toString() {
        return round + "." + memberId;
    }

    /**
     * Parses string to create ProposalNumber
     * @param str String in format "round.memberId"
     * @return ProposalNumber object
     */
    public static ProposalNumber fromString(String str) {
        if (str == null || str.equals("null")) {
            return null;
        }
        String[] parts = str.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid ProposalNumber format: " + str);
        }
        return new ProposalNumber(Integer.parseInt(parts[0]), parts[1]);
    }

    // Getters
    public int getRound() { return round; }
    public String getMemberId() { return memberId; }
}