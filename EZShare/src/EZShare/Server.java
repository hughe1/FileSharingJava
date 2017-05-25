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
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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
	private ConcurrentHashMap<ServerInfo, Boolean> secureServers = new ConcurrentHashMap<>();

	private HashMap<InetAddress, Long> clientAccesses = new HashMap<>();

	private ConcurrentHashMap<Socket, String> subscriptions = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, Resource> subscriptionTemplates = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, Integer> subscriptionResultCount = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, ArrayList<SubscriptionRelayThread>> subscriptionRelays = new ConcurrentHashMap<>();

	private ServerArgs serverArgs;

	private static Logger logger;

	public static void main(String[] args) {
		// instantiate a server and have it listen on a port
		// specified in command line arguments
		new Server(args).listen();
	}

	/**
	 * Server listens for incoming connections Secure connections on one thread,
	 * insecure on the other
	 */
	private void listen() {
		Thread secureThread = new Thread(() -> secureListen());
		secureThread.start();
		Thread insecureThread = new Thread(() -> insecureListen());
		insecureThread.start();
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
		logger.info("Bound to insecure port " + this.serverArgs.getSafePort());
		logger.info("Bound to secure port " + this.serverArgs.getSafeSport());
	}

	/**
	 * Listens for insecure connections
	 */
	public void insecureListen() {
		ServerSocketFactory factory = ServerSocketFactory.getDefault();

		try {

			InetAddress inetAddress = InetAddress.getByName(this.serverArgs.getSafeHost());
			ServerSocket server = factory.createServerSocket(this.serverArgs.getSafePort(), 0, inetAddress);

			logger.info("Listening for insecure request...");
			this.setExchangeTimer();

			// Wait for connection
			while (true) {
				Socket client = server.accept();

				logger.info("Received insecure request");

				if (isFrequentClient(client)) {
					// "An incoming request that violates the request interval
					// limit will
					// be closed immediately with no response."
					client.close();
				} else {
					// otherwise server the client
					Thread t = new Thread(() -> this.serveClient(client, false));
					t.start();
				}
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Listens for secure connections
	 */
	public void secureListen() {

		// Specify the keystore details (this can be specified as VM arguments
		// as well)
		// the keystore file contains an application's own certificate and
		// private key
		System.setProperty("javax.net.ssl.keyStore", "serverKeystore/keystore.jks");
		// Password to access the private key from the keystore file
		System.setProperty("javax.net.ssl.keyStorePassword", "somePassword");
		// Enable debugging to view the handshake and communication which
		// happens between the SSLClient and the SSLServer
		// System.setProperty("javax.net.debug","all");

		try {

			InetAddress inetAddress = InetAddress.getByName(this.serverArgs.getSafeHost());

			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory
					.createServerSocket(this.serverArgs.getSafeSport(), 0, inetAddress);

			logger.info("Listening for secure request...");

			// Wait for connection
			while (true) {
				SSLSocket client = (SSLSocket) sslserversocket.accept();

				logger.info("Received secure request");

				if (isFrequentClient(client)) {
					// "An incoming request that violates the request interval
					// limit will
					// be closed immediately with no response."
					client.close();
				} else {
					// otherwise server the client
					Thread t = new Thread(() -> this.serveClient(client, true));
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
	private void serveClient(Socket client, boolean secure) {
		try (Socket clientSocket = client) {
			clientSocket.setSoTimeout(TIME_OUT_LIMIT);
			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

			String request = receiveUTF(input);

			// Test if request is JSON valid
			Command command = getSafeCommand(request);

			if (command == null) {
				processMissingOrIncorrectTypeForCommand(output);
			} else {
				if (!parseCommandForErrors(command, output)) {
					substituteNullFields(command);

					switch (command.getCommand()) {
					case Command.QUERY_COMMAND:
						processQueryCommand(command, output, secure);
						break;
					case Command.FETCH_COMMAND:
						processFetchCommand(command, output, clientSocket.getOutputStream());
						break;
					case Command.EXCHANGE_COMMAND:
						processExchangeCommand(command, output, secure);
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
					case Command.SUBSCRIBE_COMMAND:
						processSubscribeCommand(command, output, clientSocket, secure);
						// SUBSCRIBE request --> wait for UNSUBSCRIBE
						boolean run = true;

						// Make sure socket doesn't time out
						clientSocket.setSoTimeout(0);
						while (run) {
							request = receiveUTF(input);

							// Test if request is JSON valid
							command = getSafeCommand(request);

							if (command == null) {
								// Ignore
								// processMissingOrIncorrectTypeForCommand(output);
							} else {
								if (!parseCommandForErrors(command, output)) {
									substituteNullFields(command);

									switch (command.getCommand()) {
									case Command.UNSUBSCRIBE_COMMAND:
										processUnsubscribeCommand(command, output, clientSocket);
										run = false;
										break;
									default:
										// Ignore
										break;
									}
								}
							}
						}
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
	 * 
	 * @param command
	 *            The command to be checked
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

					// Notify subscriptions
					// if this is in a thread it will be asynchronous! i.e., the
					// program goes on.
					new Thread(() -> onAddedResource(command.getResource())).start();
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
	private void processQueryCommand(Command command, DataOutputStream output, boolean secure) {
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
				count += processQueryRelay(command, output, secure);
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
	private int processQueryRelay(Command command, DataOutputStream output, boolean secure) {
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

		ConcurrentHashMap<ServerInfo, Boolean> serverList = null;
		if (secure) {
			serverList = this.secureServers;
		} else {
			serverList = this.servers;
		}

		// Forward query to all servers in servers list
		for (ConcurrentHashMap.Entry<ServerInfo, Boolean> entry : serverList.entrySet()) {
			ServerInfo serverInfo = entry.getKey();

			// Create a new thread for each ServerInfo object
			Thread relayThread = new Thread("QueryRelayHandler") {
				@Override
				public void run() {
					try {
						Socket socket = null;
						if (secure) {
							System.setProperty("javax.net.ssl.trustStore", "clientKeystore/keystore.jks");
							SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
							socket = (SSLSocket) sslsocketfactory.createSocket(serverInfo.getHostname(),
									serverInfo.getPort());
						} else {
							socket = new Socket();
							SocketAddress socketAddress = new InetSocketAddress(serverInfo.getHostname(),
									serverInfo.getPort());
							socket.connect(socketAddress, TIME_OUT_LIMIT);
						}

						DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
						DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

						sendString(command.toJson(), outToServer);

						int resourceCount = 0;
						boolean run = false;
						do {
							String fromServer = receiveUTF(inFromServer);
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
						removeServer(serverInfo, secure);
					} catch (IOException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						removeServer(serverInfo, secure);
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
	private void processExchangeCommand(Command command, DataOutputStream output, boolean secure) {
		logger.debug("Processing EXCHANGE command");

		if (command.getServerList() == null || command.getServerList().size() == 0) {
			sendResponse(buildErrorResponse(ERROR_MISSING_OR_INVALID_SERVER_LIST), output);
		} else {
			for (ServerInfo serverInfo : command.getServerList()) {
				// Check if that is our current server
				if (!serverInfo.getHostname().equals(serverArgs.getSafeHost())
						|| serverInfo.getPort() != serverArgs.getSafePort()) {
					// Add server to server list

					if (secure) {
						secureServers.put(serverInfo, true);
					} else {
						servers.put(serverInfo, true);
					}

					// Extend subscriptions to include these servers
					for (ConcurrentHashMap.Entry<Socket, ArrayList<SubscriptionRelayThread>> entry : this.subscriptionRelays
							.entrySet()) {
						Socket socket = entry.getKey();
						Resource resource = this.subscriptionTemplates.get(socket);
						String id = this.subscriptions.get(socket);

						Command subCommand = new Command();
						subCommand.setCommand(Command.SUBSCRIBE_COMMAND);
						subCommand.setId(id);
						subCommand.setRelay(false);
						subCommand.setResourceTemplate(resource);

						try {
							// Create a new thread for each ServerInfo object
							SubscriptionRelayThread relayThread = new SubscriptionRelayThread(serverInfo, subCommand,
									new DataOutputStream(socket.getOutputStream()), socket, secure);
							relayThread.start();

							ArrayList<SubscriptionRelayThread> list = entry.getValue();
							list.add(relayThread);
							this.subscriptionRelays.put(socket, list);
						} catch (IOException e) {
							logger.error(e.getClass().getName() + " " + e.getMessage());
						}

					}
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

						logger.debug("Successfully stored a resource: " + command.getResource());

						// Notify subscriptions
						new Thread(() -> onAddedResource(command.getResource())).start();
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
	 * Processes a SUBSCRIBE command
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 * @param socket
	 *            The socket of the client
	 */
	private void processSubscribeCommand(Command command, DataOutputStream output, Socket socket, boolean secure) {
		logger.debug("Processing SUBSCRIBE command");

		if (command.getResourceTemplate() == null) {
			sendResponse(buildErrorResponse(ERROR_MISSING_RESOURCE_TEMPLATE), output);
		} else if (command.getRelay() == null) {
			sendResponse(buildErrorResponse(ERROR_INVALID_RESOURCE_TEMPLATE), output);
		} else if (command.getId() == null || command.getId().length() == 0) {
			sendResponse(buildErrorResponse(ERROR_MISSING_RESOURCE_TEMPLATE), output);
		} else {
			String id = command.getId();

			this.subscriptionTemplates.put(socket, command.getResourceTemplate());
			this.subscriptions.put(socket, id);

			sendResponse(buildSuccessResponseWithId(id), output);

			int count = 0;
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

			this.subscriptionResultCount.put(socket, count);

			// Relay subscription to all servers in server list
			if (command.getRelay()) {
				processSubscriptionRelay(command, output, socket, secure);
			}

		}
	}

	/**
	 * Processes a SUBSCRIBE relay
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 * @param socket
	 *            The socket of the client
	 */
	private void processSubscriptionRelay(Command command, DataOutputStream output, Socket socket, boolean secure) {
		logger.debug("Relaying subscription...");

		// "The owner and channel information in the original query are
		// both set to "" in the forwarded query"
		command.getResourceTemplate().setOwner(Resource.DEFAULT_OWNER);
		command.getResourceTemplate().setChannel(Resource.DEFAULT_CHANNEL);

		// "Relay field is set to false"
		command.setRelay(false);

		// TODO @Huge&Annie Forward subscription to only secure or unsecure
		// servers depending on security of socket

		// Forward subscription to all servers in servers list
		ArrayList<SubscriptionRelayThread> threads = new ArrayList<>();

		ConcurrentHashMap<ServerInfo, Boolean> serverList = null;
		if (secure) {
			serverList = this.secureServers;
		} else {
			serverList = this.servers;
		}

		for (ConcurrentHashMap.Entry<ServerInfo, Boolean> entry : serverList.entrySet()) {
			ServerInfo serverInfo = entry.getKey();

			// Create a new thread for each ServerInfo object
			SubscriptionRelayThread relayThread = new SubscriptionRelayThread(serverInfo, command, output, socket,
					secure);
			relayThread.start();

			threads.add(relayThread);
		}

		this.subscriptionRelays.put(socket, threads);
	}

	/**
	 * A thread to handle subscription relays
	 * 
	 * @author alexandrafritzen
	 *
	 */
	class SubscriptionRelayThread extends Thread {
		private Socket socket;
		private Socket clientSocket;
		private ServerInfo serverInfo;
		private Command command;
		private DataOutputStream outputToClient;
		private DataOutputStream outputToServer;
		private DataInputStream inputFomServer;
		private boolean secure;

		/**
		 * Constructor
		 * 
		 * @param serverInfo
		 *            The ServerInfo object of the server to be relayed to
		 * @param command
		 *            The command to send to the server
		 * @param output
		 *            The DataOutputStream to the client
		 * @param clientSocket
		 *            The socket of the client
		 */
		public SubscriptionRelayThread(ServerInfo serverInfo, Command command, DataOutputStream output,
				Socket clientSocket, boolean secure) {
			this.serverInfo = serverInfo;
			this.command = command;
			this.outputToClient = output;
			this.clientSocket = clientSocket;
		}

		/**
		 * Runs the thread
		 */
		public void run() {
			try {
				if (secure) {
					System.setProperty("javax.net.ssl.trustStore","clientKeystore/keystore.jks");

					SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					this.socket = (SSLSocket) sslsocketfactory.createSocket(serverInfo.getHostname(),
							serverInfo.getPort());
				} else {
					this.socket = new Socket(serverInfo.getHostname(), serverInfo.getPort());
				}

				this.socket.setSoTimeout(0);
				this.socket.setKeepAlive(true);

				this.inputFomServer = new DataInputStream(socket.getInputStream());
				this.outputToServer = new DataOutputStream(socket.getOutputStream());

				sendString(command.toJson(), outputToServer);

				String fromServer = receiveUTF(inputFomServer);

				boolean run = false;
				if (fromServer.contains("success")) {
					run = true;
				}

				while (run) {
					fromServer = receiveUTF(inputFomServer);

					if (fromServer.contains("resultSize") || fromServer.contains("error")) {
						run = false;
					} else {
						sendString(fromServer, this.outputToClient);
						addToResultCountOfSocket(clientSocket);
					}
				}
				this.socket.close();
			} catch (UnknownHostException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
				removeServer(serverInfo, secure);
			} catch (IOException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
				removeServer(serverInfo, secure);
			}
		}

		/**
		 * Sends an unsubscribe Command object to the server
		 */
		public void unsubscribe() {
			String[] unsubscribeArgs = { "-unsubscribe" };
			ClientArgs unsubArgs = new ClientArgs(unsubscribeArgs);
			Command unsubscribeCommand = new Command(unsubArgs);

			sendString(unsubscribeCommand.toJson(), this.outputToServer);
		}
	}

	/**
	 * Adds 1 to the result count of the socket subscription
	 * 
	 * @param socket
	 *            The socket that is the key in the subscriptionResultCount
	 *            hashmap
	 */
	private void addToResultCountOfSocket(Socket socket) {
		int count = 0;
		if (this.subscriptionResultCount.containsKey(socket)) {
			count = this.subscriptionResultCount.get(socket);
		}
		count++;
		this.subscriptionResultCount.put(socket, count);
	}

	/**
	 * Is called when a resource has been added/overwritten using a SHARE or
	 * PUBLISH command and sends that resource to all matching subscribers
	 * 
	 * @param resource
	 *            The resource that was added/overwritten
	 */
	private void onAddedResource(Resource resource) {
		logger.debug("Forwarding added resource to subscribers...");
		for (ConcurrentHashMap.Entry<Socket, Resource> entry : this.subscriptionTemplates.entrySet()) {
			Socket socket = entry.getKey();
			Resource template = entry.getValue();

			if (isMatchingResource(template, resource)) {
				addToResultCountOfSocket(socket);

				// "The server will never reveal the owner of a resource in
				// a response. If a resource has an owner then it will be
				// replaced with the "*" character."
				String owner = resource.getOwner();
				if (!resource.getSafeOwner().equals(Resource.DEFAULT_OWNER)) {
					resource.setOwner(Resource.HIDDEN_OWNER);
				}

				// Send matching resource to subscribed client
				try {
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					sendString(resource.toJson(), output);
				} catch (IOException e) {
					logger.error(e.getClass().getName() + " " + e.getMessage());
				}

				// Reset owner
				resource.setOwner(owner);
			}
		}
	}

	/**
	 * Processes an UNSUBSCRIBE command
	 * 
	 * @param command
	 *            The Command object to be used
	 * @param output
	 *            The DataOutputStream of the socket to send messages to
	 * @param socket
	 *            The socket of the client
	 */
	private void processUnsubscribeCommand(Command command, DataOutputStream output, Socket socket) {
		logger.debug("Processing UNSUBSCRIBE command");

		if (command.getId() == null || command.getId().length() == 0) {
			sendResponse(buildErrorResponse(ERROR_MISSING_RESOURCE_TEMPLATE), output);
		} else {
			int size = 0;
			if (this.subscriptionResultCount.containsKey(socket)) {
				size = this.subscriptionResultCount.get(socket);
			}

			subscriptions.remove(socket);
			subscriptionResultCount.remove(socket);
			subscriptionTemplates.remove(socket);

			if (this.subscriptionRelays.containsKey(socket)) {
				for (SubscriptionRelayThread thread : this.subscriptionRelays.get(socket)) {
					thread.unsubscribe();
					try {
						thread.join();
					} catch (InterruptedException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
					}
				}
				subscriptionRelays.remove(socket);
			}
			// "If all subscriptions are stopped and the connection is closed,
			// the server responds with:"
			sendResponse(buildResultSizeResponse(size), output);
		}
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

		private void doExchange(ConcurrentHashMap<ServerInfo, Boolean> serverList, boolean secure) {
			if (serverList.size() > 0) {
				// "The server contacts a randomly selected server from the
				// Server Records ..."
				int randomServerLocation = ThreadLocalRandom.current().nextInt(0, serverList.size());
				List<ServerInfo> keysAsArray = new ArrayList<ServerInfo>(serverList.keySet());
				ServerInfo randomServer = keysAsArray.get(randomServerLocation);

				logger.debug("Randomly selected server " + randomServer.getHostname() + ":" + randomServer.getPort());
				if (this.source == null || !this.source.equals(randomServer)) {
					// Make servers JSON appropriate
					String serversAsString = serverArgs.getSafeHost() + ":" + (secure ? serverArgs.getSafeSport() : serverArgs.getSafePort()) + ",";
					for (ConcurrentHashMap.Entry<ServerInfo, Boolean> entry : serverList.entrySet()) {
						ServerInfo serverInfo = entry.getKey();

						if (!randomServer.equals(serverInfo)) {
							serversAsString += serverInfo.getHostname() + ":" + serverInfo.getPort() + ",";
						}
					}

					// Remove last comma of string
					serversAsString = serversAsString.substring(0, serversAsString.length() - 1);

					String secureString = "";
					if (secure) {
						secureString = "-secure";
					}

					// "... and initiates an EXCHANGE command with it."
					String[] args = { "-" + ClientArgs.EXCHANGE_OPTION, "-" + ClientArgs.SERVERS_OPTION,
							serversAsString, secureString };
					// , secureString, "-port",
					// Integer.toString(randomServer.getPort()), "-host",
					// randomServer.getHostname() };

					ClientArgs exchangeArgs = new ClientArgs(args);
					Command command = new Command().buildExchange(exchangeArgs);

					// Client.main(args);

					try {
						Socket socket = null;
						if (secure) {
							System.setProperty("javax.net.debug","all");

							System.setProperty("javax.net.ssl.trustStore", "serverKeystore/keystore.jks");
							SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
							socket = (SSLSocket) sslsocketfactory.createSocket(randomServer.getHostname(),
									randomServer.getPort());
						} else {
							socket = new Socket();
							SocketAddress socketAddress = new InetSocketAddress(randomServer.getHostname(),
									randomServer.getPort());
							socket.connect(socketAddress, TIME_OUT_LIMIT);
						}

						DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
						DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

						sendString(command.toJson(), outToServer);
						String fromServer = receiveUTF(inFromServer);
						if (!fromServer.contains("success")) {
							removeServer(randomServer, secure);
						}

						socket.close();
					} catch (UnknownHostException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						removeServer(randomServer, secure);
						e.printStackTrace();
					} catch (IOException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						removeServer(randomServer, secure);
						e.printStackTrace();
					}
				} else {
					logger.info("Randomly selected server was exchange command source -- no action taken");
				}
			} else {
				String secureText = "insecure";
				if (secure) {
					secureText = "secure";
				}
				logger.info("No " + secureText + " server in server list");
			}
		}

		public void run() {
			logger.info("Exchanging insecure server list...");
			doExchange(servers, false);
			logger.info("Exchanging secure server list...");
			doExchange(secureServers, true);
		}
	}

	/**
	 * Removes a given server from the server list
	 * 
	 * @param serverInfo
	 *            The ServerInfo object to be removed
	 */
	private void removeServer(ServerInfo serverInfo, boolean secure) {
		if (secure) {
			logger.debug("Removing secure server " + serverInfo.toString() + " from server list");
			secureServers.remove(serverInfo);
		} else {
			logger.debug("Removing insecure server " + serverInfo.toString() + " from server list");
			servers.remove(serverInfo);
		}
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
	 * Builds a success response with a subscription id
	 * 
	 * @return A Response object with response field set to "success" and the id
	 *         field set to the given id
	 */
	private Response buildSuccessResponseWithId(String id) {
		Response response = new Response();
		response.setToId(id);
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

	/**
	 * Receives a UTF string from a given DataInputStream and logs it
	 * 
	 * @param in
	 *            The DataInputStream to readUTF from
	 * @return The string that was read
	 * @throws IOException
	 */
	private String receiveUTF(DataInputStream in) throws IOException {
		String string = in.readUTF();
		logger.debug("RECEIVED: " + string);
		return string;
	}
}
