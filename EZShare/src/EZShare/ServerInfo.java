package EZShare;


/**
 * This represents server information. Primarily provides a container
 * for hostname and port. Usage is intended in the EXCHANGE command builder
 * to form the serverList. Gson class will easily parse this into JSON and
 * back to an object.
 * 
 * @author Koteski, B
 *
 */
public class ServerInfo {
	private String hostname;
	private int port;
	
	/**
	 * ServerInfo constructor. Enforces encapsulation.
	 * @param hostname String, e.g.: "115.146.85.165"
	 * @param port int, e.g.: 3780 
	 */
	public ServerInfo (String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
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
