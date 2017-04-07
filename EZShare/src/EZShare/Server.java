package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.net.ServerSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {

	private ServerArgs serverArgs;
	private ConcurrentHashMap<Resource, String> resources = new ConcurrentHashMap<>();
	private HashMap<String, Consumer<Command>> processManager = new HashMap<>();
	public static final int TIME_OUT_LIMIT = 5000;
	private static Logger logger;

	public static void main(String[] args) {
		// TODO: Remove if -- solely for testing purposes
		if (args.length == 0) {
			String[] args2 = { "-" + Constants.debugOption };
			args = args2;
		}

		Server server = new Server(args);

		if (server.serverArgs.hasOption(Constants.debugOption)) {
			System.setProperty("log4j.configurationFile", "logging-config-debug.xml");
		} else {
			System.setProperty("log4j.configurationFile", "logging-config-default.xml");
		}
		logger = LogManager.getRootLogger();
		logger.debug("Debugger enabled");

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
		// TODO: populate resources from file/Database
		processManager.put("PUBLISH", this::processPublish);
		processManager.put("QUERY", this::processQuery);
	}

	/**
	 * Examines the command line arguments
	 * 
	 * @return Command object encapsulating the arguments provided
	 */
	public Command parseCommand() {
		if (serverArgs.cmd.hasOption("advertisedhostname")) {
			logger.debug("Advertised hostname command found");
		} else if (serverArgs.cmd.hasOption("port")) {
			logger.debug("Port command found");
		}

		logger.info("Using advertised hostname " + this.serverArgs.getSafeHost());
		logger.info("Bound to port " + this.serverArgs.getSafePort());
		return null;
	}

	/**
	 * 
	 */
	public void listen() {
		// TODO: Implement blocking until client request
		ServerSocketFactory factory = ServerSocketFactory.getDefault();

		try (ServerSocket server = factory.createServerSocket(this.serverArgs.getSafePort())) {
			logger.info("Listening for request...");

			// Wait for connection
			while (true) {
				Socket client = server.accept();
				logger.info("Received request");

				// TODO: replace this with a call to a class that will serve
				// client
				// i.e., implement a runnable class
				// lambda expression
				Thread t = new Thread(() -> serveClient(client));
				t.start();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}

	}

	/**
	 * 
	 * @param client
	 */
	private void serveClient(Socket client) {
		try (Socket clientSocket = client) {
			clientSocket.setSoTimeout(TIME_OUT_LIMIT);
			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

			String request = input.readUTF();
			Command command = (new Command()).fromJson(request);
			logger.debug("RECEIVED: " + command.toJson());

			Response response = new Response();
			// this handles all types of queries
			if (this.processManager.containsKey(command.command)) {	
				this.processManager.get(command.command).accept(command);

				// TODO: Mock-up response, needs actual response from process
				// manager above
				response.response = "success";
			} else if (command.command == "INVALID") {
				response.response = "error";
				response.errorMessage = "invalid command";
			} else {
				response.response = "error";
				response.errorMessage = "missing or incorrect type for command";
			}
			
			output.writeUTF(response.toJson());
			output.flush();
			logger.debug("SENT: " + response.toJson());
			clientSocket.close();

		} catch (SocketTimeoutException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}

	private void processPublish(Command command) {
		// TODO Auto-generated method stub
	}

	private void processQuery(Command command) {
		// TODO Auto-generated method stub
	}
}
