package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {
	public static final int TIME_OUT_LIMIT = 5000;
	public static final int BUF_SIZE = 1024 * 4; // when sending a file

	private static final String ERROR_CANNOT_PUBLISH_RESOURCE = "cannot publish resource";
	private static final String ERROR_CANNOT_REMOVE_RESOURCE = "cannot remove resource";
	private static final String ERROR_CANNOT_SHARE_RESOURCE = "cannot share resource";
	private static final String ERROR_INCORRECT_SECRET = "incorrect secret";
	private static final String ERROR_INVALID_COMMAND = "invalid command";
	private static final String ERROR_INVALID_RESOURCE = "invalid resource";
	private static final String ERROR_INVALID_RESOURCE_TEMPLATE = "invalid resourceTemplate";
	private static final String ERROR_MISSING_OR_INCORRECT_TYPE_FOR_COMMAND = "missing or incorrect type for command";
	private static final String ERROR_MISSING_OR_INVALID_SERVER_LIST = "missing or invalid server list";
	private static final String ERROR_MISSING_RESOURCE = "missing resource";
	private static final String ERROR_MISSING_RESOURCE_ANDOR_SECRET = "missing resource and/or secret";
	private static final String ERROR_MISSING_RESOURCE_TEMPLATE = "missing resource template";

	private ConcurrentHashMap<Resource, String> resources = new ConcurrentHashMap<>();
	private ConcurrentHashMap<ServerInfo, Boolean> servers = new ConcurrentHashMap<>();
	private HashMap<InetAddress, Long> clientAccesses = new HashMap<>();

	private ServerArgs serverArgs;

	private static Logger logger;

	public static void main(String[] args) {
		// instantiate a server and have it listen on a port
		// specified in command line arguments
		new Server(args).listen();
	}

	/**
	 * Constructor for Server
	 * 
	 * @param args
	 *            String[] command line arguments
	 */
	public Server(String[] args) {
		serverArgs = new ServerArgs(args);
		this.setupLogger();
		this.printServerInfo();
		// TODO: populate resources from file/Database
	}

	/**
	 * Sets up the logger
	 */
	private void setupLogger() {
		if (serverArgs.hasOption(ServerArgs.DEBUG_OPTION)) {
			System.setProperty("log4j.configurationFile", "logging-config-debug.xml");
		} else {
			System.setProperty("log4j.configurationFile", "logging-config-default.xml");
		}
		logger = LogManager.getRootLogger();
		logger.debug("Debugger enabled");
	}

	/**
	 * Prints the server's basic info
	 */
	private void printServerInfo() {
		if (serverArgs.hasOption(ServerArgs.ADVERTISED_HOST_NAME_OPTION)) {
			logger.debug("Advertised hostname command found");
		}
		if (serverArgs.hasOption(ServerArgs.PORT_OPTION)) {
			logger.debug("Port command found");
		}
		if (serverArgs.hasOption(ServerArgs.SECRET_OPTION)) {
			logger.debug("Secret command found");
		}
		logger.info("Using secret " + this.serverArgs.getSafeSecret());
		logger.info("Using advertised hostname " + this.serverArgs.getSafeHost());
		logger.info("Bound to port " + this.serverArgs.getSafePort());
	}

	/**
	 * It initiates the server. The only method that one should call after
	 * instantiating a Server object.
	 */
	public void listen() {
		ServerSocketFactory factory = ServerSocketFactory.getDefault();

		try {
			InetAddress inetAddress = InetAddress.getByName(this.serverArgs.getSafeHost());
			ServerSocket server = factory.createServerSocket(this.serverArgs.getSafePort(), 0, inetAddress);

			logger.info("Listening for request...");
			this.setExchangeTimer();

			// Wait for connection
			while (true) {
				Socket client = server.accept();
				logger.info("Received request");

				if (isFrequentClient(client)) {
					// "An incoming request that violates the request interval
					// limit will
					// be closed immediately with no response."
					client.close();
				} else {
					// otherwise server the client
					Thread t = new Thread(() -> this.serveClient(client));
					t.start();
				}
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Checks if a client's IP address has been logged and if so, it ensures
	 * that the client isn't making requests that violate the request interval
	 * limit. The method also keeps track of client requests.
	 * 
	 * @param client
	 *            Socket connection.
	 * @return true if the client has violated the time interval limit,
	 *         otherwise return false.
	 */
	private boolean isFrequentClient(Socket client) {
		int limit = this.serverArgs.getSafeConnectionInterval();
		// "The server will ensure that the time between successive
		// connections from any IP address will be no less than a limit
		// (1 second by default but configurable on the command line)."
		Long timestamp = this.clientAccesses.get(client.getInetAddress());
		Long currentTime = System.currentTimeMillis();
		// Record client access
		this.clientAccesses.put(client.getInetAddress(), currentTime);
		if (timestamp != null && ((limit * 1000) + timestamp >= currentTime)) {
			// "An incomming [sic] request that violates this rule will
			// be closed immediately with no response."
			logger.info("Same client sent request less than " + limit + " second(s) ago. Closed client.");
			return true;
		}
		return false;
	}

	/**
	 * Creates a process which will periodically talk to other servers to
	 * Exchange details of other servers.
	 */
	private void setExchangeTimer() {
		// Schedule EXCHANGE with server from servers list every X seconds
		// (standard: 600 seconds = 10 minutes)
		logger.info("Setting up server exchange every " + this.serverArgs.getSafeExchangeInterval() + " seconds");
		Timer timer = new Timer();
		timer.schedule(new ExchangeJob(), 0, this.serverArgs.getSafeExchangeInterval() * 1000);
	}

	/**
	 * The method is called into it's own thread. It is responsible for serving
	 * the client connection based on what command the server will receive.
	 * 
	 * @param client
	 *            Socket connection from a client.
	 */
	private void serveClient(Socket client) {
		try (Socket clientSocket = client) {
			clientSocket.setSoTimeout(TIME_OUT_LIMIT);
			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

			String request = input.readUTF();
			logger.debug("RECEIVED: " + request);

			// Test if request is JSON valid
			Command command = getSafeCommand(request);

			if (command == null) {
				processMissingOrIncorrectTypeForCommand(output);
			} else {
				if (!parseCommandForErrors(command, output)) {
					substituteNullFields(command);
					
					switch (command.getCommand()) {
					case Command.QUERY_COMMAND:
						processQueryCommand(command, output);
						break;
					case Command.FETCH_COMMAND:
						processFetchCommand(command, output, clientSocket.getOutputStream());
						break;
					case Command.EXCHANGE_COMMAND:
						processExchangeCommand(command, output);
						break;
					case Command.PUBLISH_COMMAND:
						processPublishCommand(command, output);
						break;
					case Command.SHARE_COMMAND:
						processShareCommand(command, output);
						break;
					case Command.REMOVE_COMMAND:
						processRemoveCommand(command, output);
						break;
					default:
						processInvalidCommand(output);
						break;
					}
				}

				clientSocket.close();
			}

		} catch (SocketTimeoutException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		} catch (IOException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}

	/**
	 * Safely reads the JSON string into a Command object
	 * 
	 * @param request
	 *            The JSON string to be transformed into a Command object
	 * @return Null if the JSON is not readable or the Command object
	 */
	private Command getSafeCommand(String request) {
		try {
			Command command = (new Command()).fromJson(request);
			return command;
		} catch (com.google.gson.JsonSyntaxException ex) {
			return null;
		}
	}

	/**
	 * Checks if the command is erroneous
	 * 
	 * @param command
	 *            The command to be checked for errors
	 * @param output
	 *            The DataOutputStream of the socket to send errors to
	 * @return A boolean value of whether or not the command was erroneous
	 */
	private boolean parseCommandForErrors(Command command, DataOutputStream output) {
		// "String values must not contain the "\0" character, nor start or end
		// with whitespace."
		// "The owner field must not be the single character "*"."

		boolean errorFound = false;

		if (command.getCommand() == null) {
			processMissingOrIncorrectTypeForCommand(output);
			errorFound = true;
		} else {
			ArrayList<String> stringValues = new ArrayList<>();

			stringValues.add(command.getSecret());
			if (command.getResource() != null) {
				String[] strings = { command.getResource().getName(), command.getResource().getDescription(),
						command.getResource().getURI(), command.getResource().getChannel(),
						command.getResource().getOwner(), command.getResource().getEzserver() };
				stringValues.addAll(Arrays.asList(strings));
				if (command.getResource().getTags() != null) {
					stringValues.addAll(command.getResource().getTags());
				}
			}
			if (command.getResourceTemplate() != null) {
				String[] strings = { command.getResourceTemplate().getName(),
						command.getResourceTemplate().getDescription(), command.getResourceTemplate().getURI(),
						command.getResourceTemplate().getChannel(), command.getResourceTemplate().getOwner(),
						command.getResourceTemplate().getEzserver() };
				stringValues.addAll(Arrays.asList(strings));
				if (command.getResourceTemplate().getTags() != null) {
					stringValues.addAll(command.getResourceTemplate().getTags());
				}
			}

			for (String value : stringValues) {
				if (value == null) {
					// Do nothing
				} else if (value != value.trim()) {
					sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
					errorFound = true;
					break;
				} else if (value.contains("\0")) {
					sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
					errorFound = true;
					break;
				}
			}

			if (command.getResourceTemplate() != null && command.getResourceTemplate().getOwner() != null
					&& command.getResourceTemplate().getOwner().equals(Resource.HIDDEN_OWNER)) {
				sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
				errorFound = true;
			} else if (command.getResource() != null && command.getResource().getOwner() != null
					&& command.getResource().getOwner().equals(Resource.HIDDEN_OWNER)) {
				sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE), output);
				errorFound = true;
			}
		}

		return errorFound;
	}

	/**
	 * Substitutes any null fields in the command with the default value
	 * @param command The command to be checked
	 */
	private void substituteNullFields(Command command) {
		if (command.getResource() != null) {
			command.getResource().setNullResourceFieldsToDefault();
		} else if (command.getResourceTemplate() != null) {
			command.getResourceTemplate().setNullResourceFieldsToDefault();			
		}
	}
	
	/**
	 * Processes an invalid command
	 * 
	 * @param output
	 *            The DataOutputStream of the socket to send errors to
	 */
	private void processInvalidCommand(DataOutputStream output) {
		sendResponse(buildErrorResponse(ERROR_INVALID_COMMAND), output);
	}

	/**
	 * Processes a missing or incorrect type for command
	 * 
	 * @param output
	 *            The DataOutputStream of the socket to send errors to
	 */
	private void processMissingOrIncorrectTypeForCommand(DataOutputStream output) {
		sendResponse(buildErrorResponse(ERROR_MISSING_OR_INCORRECT_TYPE_FOR_COMMAND), output);
	}

	/**
	 * Processes a PUBLISH command
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 */
	private void processPublishCommand(Command command, DataOutputStream output) {
		logger.debug("Processing PUBLISH command");

		Response response = buildErrorResponse(ERROR_CANNOT_PUBLISH_RESOURCE);

		// Check for invalid resource fields
		if (command.getResource() == null) {
			response = buildErrorResponse(ERROR_MISSING_RESOURCE);
		} else if (!command.getResource().hasURI()) {
			// "The URI must be present, ..."
			response = buildErrorResponse(ERROR_INVALID_RESOURCE);
		} else {
			try {
				URI uri = new URI(command.getResource().getURI());

				if (!uri.isAbsolute()) {
					// "... must be absolute ..."
					response = buildErrorResponse(ERROR_INVALID_RESOURCE);
				} else if (uri.getScheme().equals("file")) {
					// "... and cannot be a file scheme."
					response = buildErrorResponse(ERROR_INVALID_RESOURCE);
				} else if (this.resources.containsKey(command.getResource())
						&& !this.resources.get(command.getResource()).equals(command.getResource().getOwner())) {
					// "Publishing a resource with the same channel and URI but
					// different owner is not allowed."
					response = buildErrorResponse(ERROR_CANNOT_PUBLISH_RESOURCE);
				} else {
					if (this.resources.containsKey(command.getResource())) {
						this.resources.remove(command.getResource());
					}

					// SUCCESS
					command.getResource()
					.setEzserver(this.serverArgs.getSafeHost() + ":" + this.serverArgs.getSafePort());
					this.resources.put(command.getResource(), command.getResource().getOwner());
					response = buildSuccessResponse();
				}
			} catch (URISyntaxException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
				response = buildErrorResponse(ERROR_INVALID_RESOURCE);
			}
		}

		sendResponse(response, output);
	}

	/**
	 * Processes a QUERY command
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 */
	private void processQueryCommand(Command command, DataOutputStream output) {
		logger.debug("Processing QUERY command");

		if (command.getResourceTemplate() == null) {
			sendResponse(buildErrorResponse(ERROR_MISSING_RESOURCE_TEMPLATE), output);
		} else if (command.getRelay() == null) {
			sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
		} else {
			sendResponse(buildSuccessResponse(), output);

			// Count how many matching resources there are
			int count = 0;

			// TODO AF Is there a better way than iterating over the whole map?
			for (ConcurrentHashMap.Entry<Resource, String> entry : this.resources.entrySet()) {
				Resource resource = entry.getKey();
				String owner = entry.getValue();

				if (isMatchingResource(command.getResourceTemplate(), resource)) {
					count++;

					// "The server will never reveal the owner of a resource in
					// a response. If a resource has an owner then it will be
					// replaced with the "*" character."
					if (!resource.getSafeOwner().equals(Resource.DEFAULT_OWNER)) {
						resource.setOwner(Resource.HIDDEN_OWNER);
					}

					sendString(resource.toJson(), output);

					// Reset owner
					resource.setOwner(owner);
				}
			}

			// Relay query to all servers in server list
			if (command.getRelay()) {
				count += processQueryRelay(command, output);
			}

			sendResponse(buildResultSizeResponse(count), output);
		}
	}

	/**
	 * Processes a QUERY relay
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 * @return The number of resources found
	 */
	private int processQueryRelay(Command command, DataOutputStream output) {
		logger.debug("Relaying query...");
		int count = 0;

		// "The owner and channel information in the original query are
		// both set to "" in the forwarded query"
		command.getResourceTemplate().setOwner(Resource.DEFAULT_OWNER);
		command.getResourceTemplate().setChannel(Resource.DEFAULT_CHANNEL);

		// "Relay field is set to false"
		command.setRelay(false);

		final CountDownLatch latch = new CountDownLatch(this.servers.size());
		final ArrayList<Integer> countArray = new ArrayList<>();

		// Forward query to all servers in servers list
		for (ConcurrentHashMap.Entry<ServerInfo, Boolean> entry : this.servers.entrySet()) {
			ServerInfo serverInfo = entry.getKey();

			// Create a new thread for each ServerInfo object
			Thread relayThread = new Thread("RelayHandler") {
				@Override
				public void run() {
					try {
						Socket socket = new Socket();
						SocketAddress socketAddress = new InetSocketAddress(serverInfo.getHostname(),
								serverInfo.getPort());
						socket.connect(socketAddress, TIME_OUT_LIMIT);

						DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
						DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

						sendString(command.toJson(), outToServer);

						int resourceCount = 0;
						boolean run = false;
						do {
							String fromServer = inFromServer.readUTF();
							logger.debug("RECEIVED: " + fromServer);
							if (fromServer.contains("success")) {
								run = true;
							}
							if (fromServer.contains("resultSize")) {
								run = false;
							}
							if (fromServer.contains(
									"\"ezserver\":\"" + serverInfo.getHostname() + ":" + serverInfo.getPort() + "\"")) {
								resourceCount++;
								sendString(fromServer, output);
							}
						} while (run);

						synchronized (countArray) {
							countArray.add(resourceCount);
						}
						socket.close();
					} catch (UnknownHostException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						removeServer(serverInfo);
					} catch (IOException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						removeServer(serverInfo);
					} finally {
						latch.countDown();
					}
				}
			};
			relayThread.start();
		}

		try {
			// Wait for all threads to be finished
			latch.await();
		} catch (InterruptedException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}

		for (int i : countArray) {
			count += i;
		}

		return count;
	}

	/**
	 * Processes an EXCHANGE command
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 */
	private void processExchangeCommand(Command command, DataOutputStream output) {
		logger.debug("Processing EXCHANGE command");

		if (command.getServerList() == null || command.getServerList().size() == 0) {
			sendResponse(buildErrorResponse(ERROR_MISSING_OR_INVALID_SERVER_LIST), output);
		} else {
			for (ServerInfo serverInfo : command.getServerList()) {
				// Check if that is our current server
				if (!serverInfo.getHostname().equals(serverArgs.getSafeHost())
						|| serverInfo.getPort() != serverArgs.getSafePort()) {
					// Add server to server list
					servers.put(serverInfo, true);
				}
			}

			sendResponse(buildSuccessResponse(), output);
		}
	}

	/**
	 * Processes a FETCH command
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 * @param os
	 *            The OutputStream of the socket
	 */
	private void processFetchCommand(Command command, DataOutputStream output, OutputStream os) {
		logger.debug("Processing FETCH command");

		// Check for invalid resourceTemplate fields
		if (command.getResourceTemplate() == null) {
			sendResponse(buildErrorResponse(ERROR_MISSING_RESOURCE_TEMPLATE), output);
		} else if (command.getResourceTemplate().getURI() == null
				|| command.getResourceTemplate().getURI().length() == 0
				|| command.getResourceTemplate().getURI().isEmpty()) {
			sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
		} else if (!this.resources.containsKey(command.getResourceTemplate())) {
			sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
		} else if (command.getResourceTemplate().getURI() == null
				|| command.getResourceTemplate().getURI().length() == 0
				|| command.getResourceTemplate().getURI().isEmpty()) {
			// "The URI must be present, ..."
			sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
		} else {
			try {
				URI uri = new URI(command.getResourceTemplate().getURI());
				if (!uri.getScheme().equals("file")) {
					sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
				} else {
					int foundResources = 0;

					// TODO AF Is there a better way than iterating over the
					// whole map?
					for (ConcurrentHashMap.Entry<Resource, String> entry : this.resources.entrySet()) {
						Resource resource = entry.getKey();
						String owner = entry.getValue();
						if (resource.getChannel().equals(command.getResourceTemplate().getChannel())
								&& resource.getURI().equals(command.getResourceTemplate().getURI())) {
							sendResponse(buildSuccessResponse(), output);

							File file = new File(uri);
							int length = (int) file.length();

							resource.setResourceSize(length);

							// "The server will never reveal the owner of a
							// resource
							// in a response. If a resource has an owner then it
							// will be replaced with the "*" character."
							if (!resource.getSafeOwner().equals(Resource.DEFAULT_OWNER)) {
								resource.setOwner(Resource.HIDDEN_OWNER);
							}

							sendString(resource.toJson(), output);

							// Reset owner
							resource.setOwner(owner);

							this.sendFile(file, os);

							foundResources++;
							break;
						}
					}
					sendResponse(buildResultSizeResponse(foundResources), output);
				}
			} catch (URISyntaxException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
				sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
			} catch (IOException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
				sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
			}
		}
	}

	/**
	 * Processes a SHARE command
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 */
	private void processShareCommand(Command command, DataOutputStream output) {
		logger.debug("Processing SHARE command");

		Response response = buildErrorResponse(ERROR_CANNOT_SHARE_RESOURCE);

		// Check for invalid resource fields
		if (command.getSecret() == null || command.getSecret().isEmpty() || command.getSecret().length() == 0) {
			// "The server secret must be present ..."
			response = buildErrorResponse(ERROR_MISSING_RESOURCE_ANDOR_SECRET);
		} else if (!command.getSecret().equals(this.serverArgs.getSafeSecret())) {
			// "... and must equal the value known to the server."
			response = buildErrorResponse(ERROR_INCORRECT_SECRET);
		} else if (command.getResource() == null) {
			response = buildErrorResponse(ERROR_MISSING_RESOURCE_ANDOR_SECRET);
		} else if (command.getResource().getURI() == null || command.getResource().getURI().length() == 0
				|| command.getResource().getURI().isEmpty()) {
			// "The URI must be present, ..."
			response = buildErrorResponse(ERROR_INVALID_RESOURCE);
		} else {
			try {
				URI uri = new URI(command.getResource().getURI());

				if (!uri.isAbsolute()) {
					// "..., must be absolute ..."
					response = buildErrorResponse(ERROR_INVALID_RESOURCE);
				} else if (uri.getAuthority() != null) {
					// "..., non-authoritative ...."
					response = buildErrorResponse(ERROR_INVALID_RESOURCE);
				} else if (!uri.getScheme().equals("file")) {
					// "... and must be a file scheme."
					response = buildErrorResponse(ERROR_INVALID_RESOURCE);
				} else if (this.resources.containsKey(command.getResource())
						&& !this.resources.get(command.getResource()).equals(command.getResource().getOwner())) {
					// "Sharing a resource with the same channel and URI but
					// different owner is not allowed."
					response = buildErrorResponse(ERROR_CANNOT_SHARE_RESOURCE);
				} else {
					File f = new File(uri);
					if (!f.exists()) {
						// "[The URI] must point to a file on the local file
						// system that the server can read as a file."
						response = buildErrorResponse(ERROR_INVALID_RESOURCE);
					} else {
						// SUCCESS
						if (this.resources.containsKey(command.getResource())) {
							this.resources.remove(command.getResource());
						}

						command.getResource()
						.setEzserver(this.serverArgs.getSafeHost() + ":" + this.serverArgs.getSafePort());
						command.getResource().setResourceSize(f.length());
						this.resources.put(command.getResource(), command.getResource().getOwner());
						response = buildSuccessResponse();
						logger.debug("successfully stored a resource: " + command.getResource());
					}
				}
			} catch (URISyntaxException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
				response = buildErrorResponse(ERROR_INVALID_RESOURCE);
			}
		}

		sendResponse(response, output);
	}

	/**
	 * Processes a REMOVE command
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 */
	private void processRemoveCommand(Command command, DataOutputStream output) {
		logger.debug("Processing REMOVE command");

		Response response = buildErrorResponse(ERROR_CANNOT_REMOVE_RESOURCE);

		// Check for invalid resource fields
		if (command.getResource() == null) {
			response = buildErrorResponse(ERROR_MISSING_RESOURCE);
		} else if (command.getResource().getURI() == null || command.getResource().getURI().isEmpty()) {
			// URI must be present
			response = buildErrorResponse(ERROR_INVALID_RESOURCE);
		} else if (!this.resources.containsKey(command.getResource())) {
			// Resource must exist
			response = buildErrorResponse(ERROR_CANNOT_REMOVE_RESOURCE);
		} else {
			String owner = this.resources.get(command.getResource());
			if (!owner.equals(command.getResource().getOwner())) {
				// Resource must match primary key
				response = buildErrorResponse(ERROR_CANNOT_REMOVE_RESOURCE);
			} else {
				// SUCCESS
				this.resources.remove(command.getResource());
				response = buildSuccessResponse();
			}
		}

		sendResponse(response, output);
	}

	/**
	 * 
	 * @param template
	 *            The template that states what values a resource should have
	 * @param resource
	 *            The resource that has to match the template
	 * @return Whether or not the resource matches the template
	 */
	private boolean isMatchingResource(Resource template, Resource resource) {
		// Query matching resource rules:
		// "(The template channel equals (case sensitive) the resource
		// channel AND
		boolean equalChannel = resource.getChannel().equals(template.getChannel());

		// If the template contains an owner that is not "", then the
		// candidate owner must equal it (case sensitive) AND
		boolean equalOrNoOwner = template.getOwner().isEmpty() ? true : resource.getOwner().equals(template.getOwner());

		// Any tags present in the template also are present in the
		// candidate (case insensitive) AND
		boolean equalTags = template.getTags().size() == 0 ? true : resource.getTags().containsAll(template.getTags());

		// If the template contains a URI then the candidate URI matches
		// (case sensitive) AND
		boolean equalOrNoUri = template.getURI().isEmpty() ? true : resource.getURI().equals(template.getURI());

		// (The candidate name contains the template name as a substring
		// (for non "" template name) OR
		boolean nameIsSubstring = template.getName().isEmpty() ? false
				: resource.getName().contains(template.getName());

		// The candidate description contains the template description
		// as a substring (for non "" template descriptions) OR
		boolean descriptionIsSubstring = template.getDescription().isEmpty() ? false
				: resource.getDescription().contains(template.getDescription());

		// The template description and name are both ""))"
		boolean noDescriptionAndName = template.getName().isEmpty() && template.getDescription().isEmpty();

		return equalChannel && equalOrNoOwner && equalTags && equalOrNoUri
				&& (nameIsSubstring || descriptionIsSubstring || noDescriptionAndName);
	}

	/**
	 * Randomly selects a server from the server list and forwards the server
	 * list to it
	 * 
	 * @author alexandrafritzen
	 *
	 */
	class ExchangeJob extends TimerTask {
		private ServerInfo source;

		public ExchangeJob() {
			super();
		}

		public ExchangeJob(ServerInfo source) {
			super();
			this.source = source;
		}

		public void run() {
			logger.info("Exchanging server list...");
			if (servers.size() > 0) {
				// "The server contacts a randomly selected server from the
				// Server Records ..."
				int randomServerLocation = ThreadLocalRandom.current().nextInt(0, servers.size());
				List<ServerInfo> keysAsArray = new ArrayList<ServerInfo>(servers.keySet());
				ServerInfo randomServer = keysAsArray.get(randomServerLocation);

				logger.debug("Randomly selected server " + randomServer.getHostname() + ":" + randomServer.getPort());
				if (this.source == null || !this.source.equals(randomServer)) {
					// Make servers JSON appropriate
					String serversAsString = serverArgs.getSafeHost() + ":" + serverArgs.getSafePort() + ",";
					for (ConcurrentHashMap.Entry<ServerInfo, Boolean> entry : servers.entrySet()) {
						ServerInfo serverInfo = entry.getKey();
						// TODO AF This check needed? Project says: "It provides
						// the
						// selected server with a copy of its entire Server
						// Records
						// list."
						if (!randomServer.equals(serverInfo)) {
							serversAsString += serverInfo.getHostname() + ":" + serverInfo.getPort() + ",";
						}
					}

					// Remove last comma of string
					serversAsString = serversAsString.substring(0, serversAsString.length() - 1);

					// "... and initiates an EXCHANGE command with it."
					String[] args = { "-" + ClientArgs.EXCHANGE_OPTION, "-" + ClientArgs.SERVERS_OPTION,
							serversAsString };
					ClientArgs exchangeArgs = new ClientArgs(args);
					Command command = new Command().buildExchange(exchangeArgs);

					try {
						Socket socket = new Socket();
						SocketAddress socketAddress = new InetSocketAddress(randomServer.getHostname(),
								randomServer.getPort());
						socket.connect(socketAddress, TIME_OUT_LIMIT);

						DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
						DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

						sendString(command.toJson(), outToServer);
						String fromServer = inFromServer.readUTF();
						logger.debug("RECEIVED: " + fromServer);
						if (!fromServer.contains("success")) {
							removeServer(randomServer);
						}

						socket.close();
					} catch (UnknownHostException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						removeServer(randomServer);
					} catch (IOException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						removeServer(randomServer);
					}
				} else {
					logger.info("Randomly selected server was exchange command source -- no action taken");
				}
			} else {
				logger.info("No server in server list");
			}
		}
	}

	/**
	 * Removes a given server from the server list
	 * 
	 * @param serverInfo
	 *            The ServerInfo object to be removed
	 */
	private void removeServer(ServerInfo serverInfo) {
		logger.debug("Removing server " + serverInfo.toString() + " from server list");
		servers.remove(serverInfo);
	}

	/*
	 * Methods for building a new success, error or result-size message
	 */
	/**
	 * Builds a success response
	 * 
	 * @return A Response object with response field set to "success"
	 */
	private Response buildSuccessResponse() {
		Response response = new Response();
		response.setToSuccess();
		return response;
	}

	/**
	 * Builds an error response
	 * 
	 * @param message
	 *            The value of the errorMessage to be set
	 * @return A Response object with response field set to "error" and
	 *         errorMessage field to the given message
	 */
	private Response buildErrorResponse(String message) {
		Response response = new Response();
		response.setToError(message);
		return response;
	}

	/**
	 * Builds a result size response
	 * 
	 * @param size
	 *            The value of the resultSize to be set
	 * @return A Response object with resultSize field set to the given
	 *         resultSize
	 */
	private Response buildResultSizeResponse(int size) {
		Response response = new Response();
		response.setToResultSize(size);
		return response;
	}

	/*
	 * Methods for sending a response, a string, and a file
	 */
	/**
	 * Sends a Response object to the output
	 * 
	 * @param response
	 *            The Response object to be sent
	 * @param output
	 *            The DataOutputStream object the object shall be sent to
	 */
	private void sendResponse(Response response, DataOutputStream output) {
		sendString(response.toJson(), output);
	}

	/**
	 * Sends a string to the output
	 * 
	 * @param string
	 *            The string to be sent
	 * @param output
	 *            The DataOutputStream object the string shall be sent to
	 */
	private void sendString(String string, DataOutputStream output) {
		try {
			output.writeUTF(string);
			output.flush();
			logger.debug("SENT: " + string);
		} catch (IOException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}

	/**
	 * Sends a file in bytes to the OutputStream
	 * 
	 * @param file
	 *            The file to be sent
	 * @param out
	 *            The OutoutStream to send the file to
	 * @throws IOException
	 */
	private void sendFile(File file, OutputStream out) throws IOException {
		FileInputStream in = new FileInputStream(file);
		byte[] bytes = new byte[BUF_SIZE];
		int count;
		// this will read up to bytes.length bytes from the file
		while ((count = in.read(bytes)) > 0) {
			// send exactly count bytes to the stream, i.e., to the client
			out.write(bytes, 0, count);
		}
		in.close();
	}
}
