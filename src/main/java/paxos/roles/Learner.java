package paxos.roles;

import paxos.messages.PaxosMessage;
import paxos.messages.ProposalNumber;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the Learner role in Paxos
 */
public class Learner {
    private String memberId;
    private Map<ProposalNumber, Map<String, String>> acceptances;
    private String learnedValue;
    private int majoritySize;

    /**
     * Constructor for Learner
     * @param memberId The member ID
     * @param totalMembers Total number of members
     */
    public Learner(String memberId, int totalMembers) {
        this.memberId = memberId;
        this.acceptances = new ConcurrentHashMap<>();
        this.majoritySize = (totalMembers / 2) + 1;
    }

    /**
     * Handles an ACCEPTED message
     * @param message The ACCEPTED message
     */
    public synchronized void handleAccepted(PaxosMessage message) {
        ProposalNumber proposalNumber = message.getProposalNumber();
        String value = message.getProposalValue();
        String senderId = message.getSenderId();

        // Track acceptances for this proposal
        acceptances.computeIfAbsent(proposalNumber, k -> new ConcurrentHashMap<>())
                .put(senderId, value);

        Map<String, String> proposalAcceptances = acceptances.get(proposalNumber);

        System.out.println("[" + memberId + "] Learner received ACCEPTED from " + senderId +
                " for proposal " + proposalNumber + " with value: " + value +
                " (" + proposalAcceptances.size() + "/" + majoritySize + ")");

        // Check if we have a majority for this proposal
        if (proposalAcceptances.size() >= majoritySize && learnedValue == null) {
            // Verify all acceptances are for the same value
            Set<String> values = new HashSet<>(proposalAcceptances.values());
            if (values.size() == 1) {
                learnedValue = value;
                System.out.println("\n========================================");
                System.out.println("CONSENSUS: " + value + " has been elected Council President!");
                System.out.println("========================================\n");
            }
        }
    }

    /**
     * Gets the learned value
     * @return The learned value or null if not yet learned
     */
    public String getLearnedValue() {
        return learnedValue;
    }

    /**
     * Checks if a value has been learned
     * @return true if value has been learned
     */
    public boolean hasLearned() {
        return learnedValue != null;
    }
}