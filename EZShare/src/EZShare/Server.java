package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.net.ServerSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {

	private ServerArgs serverArgs;
	private ConcurrentHashMap<Resource, String> resources = new ConcurrentHashMap<>();
	public static final int TIME_OUT_LIMIT = 5000;
	private static Logger logger;
	private Response successResponse;

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
	}

	/**
	 * Examines the command line arguments
	 * 
	 * @return Command object encapsulating the arguments provided
	 */
	public Command parseCommand() {
		if (serverArgs.cmd.hasOption(Constants.advertisedHostNameOption)) {
			logger.debug("Advertised hostname command found");
		}
		if (serverArgs.cmd.hasOption(Constants.portOption)) {
			logger.debug("Port command found");
		}
		if (serverArgs.cmd.hasOption(Constants.secretOption)) {
			logger.debug("Secret command found");
		}

		logger.info("Using secret " + this.serverArgs.getSafeSecret());
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

			if (!parseCommandForErrors(command, output)) {

				switch (command.command) {
				case Constants.queryCommand:
					processQueryCommand(command, output);
					break;
				case Constants.fetchCommand:
					processFetchCommand(command, output);
					break;
				case Constants.exchangeCommand:
					processExchangeCommand(command, output);
					break;
				case Constants.publishCommand:
					processPublishCommand(command, output);
					break;
				case Constants.shareCommand:
					processShareCommand(command, output);
					break;
				case Constants.removeCommand:
					processRemoveCommand(command, output);
					break;
				case Constants.invalidCommand:
					processInvalidCommand(command, output);
					break;
				default:
					processMissingOrInvalidCommand(command, output);
					break;
				}
			}

			clientSocket.close();

		} catch (SocketTimeoutException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}

	private boolean parseCommandForErrors(Command command, DataOutputStream output) {
		// "String values must not contain the "\0" character, nor start or end
		// with whitespace."
		// "The field must not be the single character "*"."
		// TODO Check if every possible case is covered
		boolean errorFound = false;

		ArrayList<String> stringValues = new ArrayList<>();
		stringValues.add(command.secret);
		if (command.resource != null) {
			String[] strings = { command.resource.name, command.resource.description, command.resource.uri,
					command.resource.channel, command.resource.owner, command.resource.ezserver };
			stringValues.addAll(Arrays.asList(strings));
			if (command.resource.tags != null) {
				stringValues.addAll(command.resource.tags);
			}
		}
		if (command.resourceTemplate != null) {
			String[] strings = { command.resourceTemplate.name, command.resourceTemplate.description,
					command.resourceTemplate.uri, command.resourceTemplate.channel, command.resourceTemplate.owner,
					command.resourceTemplate.ezserver };
			stringValues.addAll(Arrays.asList(strings));
			if (command.resourceTemplate.tags != null) {
				stringValues.addAll(command.resourceTemplate.tags);
			}
		}

		for (String value : stringValues) {
			if (value == null) {
				// Do nothing
			} else if (value.equals("*")) {
				sendResponse(buildErrorResponse("String values cannot be *"), output);
				errorFound = true;
				break;
			} else if (value != value.trim()) {
				sendResponse(buildErrorResponse("String values cannot start or end with whitespace(s)"), output);
				errorFound = true;
				break;
			} else if (value.contains("\0")) {
				sendResponse(buildErrorResponse("String values cannot contain \0"), output);
				errorFound = true;
				break;
			}
		}

		return errorFound;
	}

	private void processInvalidCommand(Command command, DataOutputStream output) {
		sendResponse(buildErrorResponse("invalid command"), output);
	}

	private void processMissingOrInvalidCommand(Command command, DataOutputStream output) {
		sendResponse(buildErrorResponse("missing or incorrect type for command"), output);
	}

	private void processPublishCommand(Command command, DataOutputStream output) {
		logger.debug("Processing PUBLISH command");

		Response response = buildErrorResponse("cannot publish resource");

		// Check for invalid resource fields
		if (command.resource == null) {
			response = buildErrorResponse("missing resource");
		} else if (command.resource.uri == null || command.resource.uri.length() == 0
				|| command.resource.uri.equals("")) {
			// "The URI must be present, ..."
			response = buildErrorResponse("invalid resource - missing uri");
		} else {
			try {
				URI uri = new URI(command.resource.uri);

				if (!uri.isAbsolute()) {
					// "... must be absolute ..."
					response = buildErrorResponse("invalid resource - uri must be absolute");
				} else if (uri.getScheme().equals("file")) {
					// "... and cannot be a file scheme."
					response = buildErrorResponse("invalid resource - uri cannot be a file scheme");
				} else if (this.resources.containsKey(command.resource)
						&& !this.resources.get(command.resource).equals(command.resource.owner)) {
					// "Publishing a resource with the same channel and URI but
					// different owner is not allowed."
					response = buildErrorResponse("cannot publish resource - uri already exists in channel");
				} else {
					// SUCCESS
					this.resources.put(command.resource, command.resource.owner);
					response = buildSuccessResponse();
				}
			} catch (URISyntaxException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
				sendResponse(buildErrorResponse("invalid resource - invalid uri"), output);
			}
		}

		sendResponse(response, output);
	}

	private void processQueryCommand(Command command, DataOutputStream output) {
		logger.debug("Processing QUERY command");

		// sendResponse(buildSuccessResponse(), output);

		// TODO Find resources fitting to command

		// TODO Send resources back to client
		// TODO Send { "resultSize" : 2 }
	}

	private void processExchangeCommand(Command command, DataOutputStream output) {
		logger.debug("Processing EXCHANGE command");
		// TODO Auto-generated method stub
	}

	private void processFetchCommand(Command command, DataOutputStream output) {
		logger.debug("Processing FETCH command");
		// TODO Auto-generated method stub
	}

	private void processShareCommand(Command command, DataOutputStream output) {
		logger.debug("Processing SHARE command");

		Response response = buildErrorResponse("cannot share resource");

		// Check for invalid resource fields
		if (command.secret == null || command.secret.equals("") || command.secret.length() == 0) {
			// "The server secret must be present ..."
			response = buildErrorResponse("missing resource and/or secret");
		} else if (!command.secret.equals(this.serverArgs.getSafeSecret())) {
			// "... and must equal the value known to the server."
			response = buildErrorResponse("incorrect secret");
		} else if (command.resource == null) {
			response = buildErrorResponse("missing resource and/or secret");
		} else if (command.resource.uri == null || command.resource.uri.length() == 0
				|| command.resource.uri.equals("")) {
			// "The URI must be present, ..."
			response = buildErrorResponse("invalid resource - missing uri");
		} else {
			try {
				URI uri = new URI(command.resource.uri);

				if (!uri.isAbsolute()) {
					// "..., must be absolute ..."
					response = buildErrorResponse("invalid resource - uri must be absolute");
				} else if (uri.getAuthority() != null) {
					// "..., non-authoritative ...."
					response = buildErrorResponse("invalid resource - uri must be non-authoritative");
				} else if (!uri.getScheme().equals("file")) {
					// "... and must be a file scheme."
					response = buildErrorResponse("invalid resource - uri must be a file scheme");
				} else if (this.resources.containsKey(command.resource)
						&& !this.resources.get(command.resource).equals(command.resource.owner)) {
					// "Sharing a resource with the same channel and URI but
					// different owner is not allowed."
					response = buildErrorResponse("cannot share resource - uri already exists in channel");
				} else {
					File f = new File(uri);
					if (!f.exists()) {
						// "[The URI] must point to a file on the local file
						// system that the server can read as a file."
						response = buildErrorResponse(
								"invalid resource - uri does not point to a file on the local file system");
					} else {
						// SUCCESS
						this.resources.put(command.resource, command.resource.owner);
						response = buildSuccessResponse();
					}
				}
			} catch (URISyntaxException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
				sendResponse(buildErrorResponse("invalid resource - invalid uri"), output);
			}
		}

		sendResponse(response, output);
	}

	private void processRemoveCommand(Command command, DataOutputStream output) {
		logger.debug("Processing REMOVE command");

		Response response = buildErrorResponse("cannot remove resource");

		// Check for invalid resource fields
		if (command.resource == null) {
			response = buildErrorResponse("missing resource");
		} else if (command.resource.uri == null || command.resource.uri.equals("")) {
			// URI must be present
			response = buildErrorResponse("invalid resource - missing uri");
		} else if (!this.resources.containsKey(command.resource)) {
			// Resource must exist
			response = buildErrorResponse("cannot remove resource - resource does not exist");
		} else {
			// SUCCESS
			this.resources.remove(command.resource);
			response = buildSuccessResponse();
		}

		sendResponse(response, output);
	}

	private Response buildSuccessResponse() {
		if (this.successResponse == null) {
			this.successResponse = new Response();
			this.successResponse = this.successResponse.success();
		}
		return this.successResponse;
	}

	private Response buildErrorResponse(String message) {
		Response response = new Response();
		response = response.error(message);
		return response;
	}

	private void sendResponse(Response response, DataOutputStream output) {
		try {
			output.writeUTF(response.toJson());
			output.flush();
			logger.debug("SENT: " + response.toJson());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}
}
