package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * The class contains functionality to parse command-line arguments according to
 * the assignment specifications. Additionally, it can also communicate with an
 * EZShare.Server that is listening for EZShare.Client, according to the protocol
 * defined in the assignment specifications.
 * 
 * Aaron's server: sunrise.cis.unimelb.edu.au:3780
 * 
 * @author Koteski, B
 *
 */
public class Client {
	
	private ClientArgs clientArgs;
	
	public static void main(String[] args) {
		Client client = new Client(args);
		
		Command command = client.parseCommand();
		ServerInfo serverInfo = client.parseServerInfo();
		
		System.out.println(command.toJsonPretty());
		System.out.println(serverInfo);
		
		try {
			Socket socket = new Socket(serverInfo.getHostname(),serverInfo.getPort());
			socket.setSoTimeout(5000);
			DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
			
			outToServer.writeUTF(command.toJson());
			
			boolean run = false;
			do {
				String fromServer = inFromServer.readUTF();
//				if(fromServer.contains("success")) run = true;
//				if(fromServer.contains("resultSize")) run = false;
				System.out.println(fromServer);
			} while (run);
			
			
			socket.close();
		} catch(SocketTimeoutException e) {
			System.out.println("socket timed out...");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println("IOException");
		}
	}
	

	/**
	 * Constructor for Client
	 * 
	 * @param args String[] command line arguments
	 */
	public Client(String[] args) {
		clientArgs = new ClientArgs(args);		
	}
	
	/**
	 * 
	 * @return
	 */
	public Command parseCommand() {
		return new Command(clientArgs);
	}
	
	/**
	 * 
	 * @return
	 */
	public ServerInfo parseServerInfo() {
		return new ServerInfo(clientArgs.getSafeHost(), clientArgs.getSafePort());
	}
	
}
