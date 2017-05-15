package EZShare;

/**
 * The ServerInfo class represents server information. The class primarily
 * provides a container for host name and port. Usage is intended in the
 * EXCHANGE command builder to form the serverList.
 * 
 * The class implements hashCode and equals so that objects can be easily hashed
 * and compared against other ServerInfo objects.
 * 
 */
public class ServerInfo {
	private String hostname;
	private Integer port;

	/**
	 * ServerInfo constructor. Enforces encapsulation.
	 * 
	 * @param hostname
	 *            String, e.g.: "115.146.85.165"
	 * @param port
	 *            int, e.g.: 3780
	 */
	public ServerInfo(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((hostname.equals("")) ? 0 : hostname.hashCode());
		result = PRIME * result + ((port == 0) ? 0 : port.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		// Safe to cast as ServerInfo object now
		ServerInfo other = (ServerInfo) obj;
		if (hostname == null) {
			if (other.hostname != null)
				return false;
		} else if (!hostname.equals(other.hostname))
			return false;

		if (port == null) {
			if (other.port != null)
				return false;
		} else if (!port.equals(other.port))
			return false;

		// Matching hostname and port
		return true;
	}

	/**
	 * @return the host name
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	
	@Override
	public String toString() {
		return "{\"" + this.hostname + "\":" + this.port + "}";
	}

}
