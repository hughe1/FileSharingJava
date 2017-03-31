package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

public class Server {

	private ServerArgs serverArgs;

	public static void main(String[] args) {
		Server server = new Server(args);
		server.parseCommand();
		server.listen();
	}

	/**
	 * Constructor for Server
	 * 
	 * @param args
	 *            String[] command line arguments
	 */
	public Server(String[] args) {
		serverArgs = new ServerArgs(args);
	}

	/**
	 * Examines the command line arguments
	 * 
	 * @return Command object encapsulating the arguments provided
	 */
	public Command parseCommand() {
		if (serverArgs.cmd.hasOption("advertisedhostname")) {
			System.out.println("advertisedhostname command found");
		} else if (serverArgs.cmd.hasOption("port")) {
			System.out.println("port command found");
		}
		return null;
	}

	/**
	 * 
	 */
	public void listen() {
		// TODO: Implement blocking until client request

		ServerSocketFactory factory = ServerSocketFactory.getDefault();

		try (ServerSocket server = factory.createServerSocket(this.serverArgs.getSafePort())) {
			System.out.println("Listening for request...");

			// Wait for connection
			while (true) {
				Socket client = server.accept();
				System.out.println("Received request");

				Thread t = new Thread(() -> serveClient(client));
				t.start();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void serveClient(Socket client) {
		try (Socket clientSocket = client) {
			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

			String request = input.readUTF();
			Command command = (new Command()).fromJson(request);
			System.out.println("Request: " + command.toJsonPretty());
			// TODO: Handle command
			// TODO: Send result to client via writeUTF
			output.writeUTF("Something");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
