package paxos.network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages network configuration for council members
 */
public class NetworkConfig {
    private Map<String, MemberInfo> memberConfigs;

    /**
     * Constructor - loads configuration from file
     * @param configFile Path to configuration file
     * @throws IOException if file cannot be read
     */
    public NetworkConfig(String configFile) throws IOException {
        memberConfigs = new HashMap<>();
        loadConfig(configFile);
    }

    /**
     * Loads configuration from file
     * @param configFile Path to configuration file
     * @throws IOException if file cannot be read
     */
    private void loadConfig(String configFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String memberId = parts[0].trim();
                    String hostname = parts[1].trim();
                    int port = Integer.parseInt(parts[2].trim());
                    memberConfigs.put(memberId, new MemberInfo(memberId, hostname, port));
                }
            }
        }
    }

    /**
     * Gets member information by ID
     * @param memberId The member ID
     * @return MemberInfo object or null if not found
     */
    public MemberInfo getMemberInfo(String memberId) {
        return memberConfigs.get(memberId);
    }

    /**
     * Gets all member IDs
     * @return Set of all member IDs
     */
    public Set<String> getAllMemberIds() {
        return new HashSet<>(memberConfigs.keySet());
    }

    /**
     * Gets all member IDs except the specified one
     * @param excludeMemberId Member ID to exclude
     * @return Set of member IDs
     */
    public Set<String> getOtherMemberIds(String excludeMemberId) {
        Set<String> others = new HashSet<>(memberConfigs.keySet());
        others.remove(excludeMemberId);
        return others;
    }
}