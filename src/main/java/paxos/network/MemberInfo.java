package paxos.network;

/**
 * Represents network information for a council member
 */
public class MemberInfo {
    private final String memberId;
    private final String hostname;
    private final int port;

    /**
     * Constructor for MemberInfo
     * @param memberId The member ID
     * @param hostname The hostname
     * @param port The port number
     */
    public MemberInfo(String memberId, String hostname, int port) {
        this.memberId = memberId;
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Gets the member ID
     * @return The member ID
     */
    public String getMemberId() {
        return memberId;
    }

    /**
     * Gets the hostname
     * @return The hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Gets the port number
     * @return The port number
     */
    public int getPort() {
        return port;
    }
}