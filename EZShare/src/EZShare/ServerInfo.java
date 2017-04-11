package EZShare;

/**
 * This represents server information. Primarily provides a container for
 * hostname and port. Usage is intended in the EXCHANGE command builder to form
 * the serverList. Gson class will easily parse this into JSON and back to an
 * object.
 * 
 * @author Koteski, B
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hostname.equals("")) ? 0 : hostname.hashCode());
		result = prime * result + ((port == 0) ? 0 : port.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
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
		return true;
	}
	
	/**
	 * @return the hostname
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
