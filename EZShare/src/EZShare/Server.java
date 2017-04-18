package EZShare;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import java.net.Socket;
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
	
	private ServerArgs serverArgs;
	private ConcurrentHashMap<Resource, String> resources = new ConcurrentHashMap<>();
	private ConcurrentHashMap<ServerInfo, Boolean> servers = new ConcurrentHashMap<>();
		
	private static Logger logger;
	private HashMap<InetAddress, Long> clientAccesses = new HashMap<>();

	public static void main(String[] args) {
		Server server = new Server(args);

		if (server.serverArgs.hasOption(ServerArgs.DEBUG_OPTION)) {
			System.setProperty("log4j.configurationFile", "../logging-config-debug.xml");
		} else {
			System.setProperty("log4j.configurationFile", "../logging-config-default.xml");
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
		if (serverArgs.cmd.hasOption(ServerArgs.ADVERTISED_HOST_NAME_OPTION)) {
			logger.debug("Advertised hostname command found");
		}
		if (serverArgs.cmd.hasOption(ServerArgs.PORT_OPTION)) {
			logger.debug("Port command found");
		}
		if (serverArgs.cmd.hasOption(ServerArgs.SECRET_OPTION)) {
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
		ServerSocketFactory factory = ServerSocketFactory.getDefault();

		try (ServerSocket server = factory.createServerSocket(this.serverArgs.getSafePort())) {
			logger.info("Listening for request...");

			// Schedule EXCHANGE with server from servers list every X seconds
			// (standard: 600 seconds = 10 minutes)
			logger.info("Setting up server exchange every " + this.serverArgs.getSafeExchangeInterval() + " seconds");
			Timer timer = new Timer();
			timer.schedule(new ExchangeJob(), 0, this.serverArgs.getSafeExchangeInterval() * 1000);

			int limit = this.serverArgs.getSafeConnectionInterval();

			// Wait for connection
			while (true) {
				Socket client = server.accept();
				logger.info("Received request");

				// "The server will ensure that the time between successive
				// connections from any IP address will be no less than a limit
				// (1 second by default but configurable on the command line)."
				Long timestamp = this.clientAccesses.get(client.getInetAddress());
				Long currentTime = System.currentTimeMillis();
				if (timestamp != null && ((limit * 1000) + timestamp >= currentTime)) {
					// "An incomming [sic] request that violates this rule will
					// be closed immediately with no response."
					logger.info("Same client sent request less than " + limit + " second(s) ago. Closed client.");
					client.close();
				} else {
					// TODO: replace this with a call to a class that will serve
					// client
					// i.e., implement a runnable class
					// lambda expression
					Thread t = new Thread(() -> this.serveClient(client));
					t.start();
				}
				// Record client access
				this.clientAccesses.put(client.getInetAddress(), currentTime);
			}

		} catch (IOException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
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

				switch (command.getCommand()) {
				case Command.QUERY_COMMAND:
					processQueryCommand(command, output);
					break;
				case Command.FETCH_COMMAND:
					processFetchCommand(command, output);
					break;
				case Command.EXCHANGE_COMMAND:
					processExchangeCommand(command, output,
							new ServerInfo(client.getInetAddress().getHostName(), client.getPort()));
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
				case Command.INVALID_COMMAND:
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
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}

	private boolean parseCommandForErrors(Command command, DataOutputStream output) {
		// "String values must not contain the "\0" character, nor start or end
		// with whitespace."
		// "The field must not be the single character "*"."
		// TODO AF Can someone re-check if every possible case is covered
		
		boolean errorFound = false;

		ArrayList<String> stringValues = new ArrayList<>();
		
		stringValues.add(command.getSecret());
		if (command.getResource() != null) {
			String[] strings = { command.getResource().getName(), command.getResource().getDescription(), command.getResource().getURI(),
					command.getResource().getChannel(), command.getResource().getOwner(), command.getResource().getEzserver() };
			stringValues.addAll(Arrays.asList(strings));
			if (command.getResource().getTags() != null) {
				stringValues.addAll(command.getResource().getTags());
			}
		}
		if (command.getResourceTemplate() != null) {
			String[] strings = { command.getResourceTemplate().getName(), command.getResourceTemplate().getDescription(),
					command.getResourceTemplate().getURI(), command.getResourceTemplate().getChannel(), command.getResourceTemplate().getOwner(),
					command.getResourceTemplate().getEzserver() };
			stringValues.addAll(Arrays.asList(strings));
			if (command.getResourceTemplate().getTags() != null) {
				stringValues.addAll(command.getResourceTemplate().getTags());
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
		if (command.getResource() == null) {
			response = buildErrorResponse("missing resource");
		} else if (!command.getResource().hasURI()) {
			// "The URI must be present, ..."
			response = buildErrorResponse("invalid resource - missing uri");
		} else {
			try {
				URI uri = new URI(command.getResource().getURI());

				if (!uri.isAbsolute()) {
					// "... must be absolute ..."
					response = buildErrorResponse("invalid resource - uri must be absolute");
				} else if (uri.getScheme().equals("file")) {
					// "... and cannot be a file scheme."
					response = buildErrorResponse("invalid resource - uri cannot be a file scheme");
				} else if (this.resources.containsKey(command.getResource())
						&& !this.resources.get(command.getResource()).equals(command.getResource().getOwner())) {
					// "Publishing a resource with the same channel and URI but
					// different owner is not allowed."
					response = buildErrorResponse("cannot publish resource - uri already exists in channel");
				} else {
					// SUCCESS
					command.getResource().setEzserver(this.serverArgs.getSafeHost() + ":" + this.serverArgs.getSafePort());
					this.resources.put(command.getResource(), command.getResource().getOwner());
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

		if (command.getResourceTemplate() == null) {
			sendResponse(buildErrorResponse("missing resourceTemplate"), output);
		} else {

			sendResponse(buildSuccessResponse(), output);

			int count = 0;
			// TODO AF Is there a better way than iterating over the whole map?
			for (ConcurrentHashMap.Entry<Resource, String> entry : this.resources.entrySet()) {
				Resource resource = entry.getKey();
				String owner = entry.getValue();

				// Query rules:
				// "(The template channel equals (case sensitive) the resource
				// channel AND
				boolean equalChannel = resource.getChannel().equals(command.getResourceTemplate().getChannel());

				// If the template contains an owner that is not "", then the
				// candidate owner must equal it (case sensitive) AND
				boolean equalOrNoOwner = command.getResourceTemplate().getOwner().isEmpty() ? true
						: resource.getOwner().equals(command.getResourceTemplate().getOwner());

				// Any tags present in the template also are present in the
				// candidate (case insensitive) AND
				boolean equalTags = command.getResourceTemplate().getTags().size() == 0 ? true
						: resource.getTags().containsAll(command.getResourceTemplate().getTags());

				// If the template contains a URI then the candidate URI matches
				// (case sensitive) AND
				boolean equalOrNoUri = command.getResourceTemplate().getURI().isEmpty() ? true
						: resource.getURI().equals(command.getResourceTemplate().getURI());

				// (The candidate name contains the template name as a substring
				// (for non "" template name) OR
				boolean nameIsSubstring = command.getResourceTemplate().getName().isEmpty() ? true
						: resource.getName().equals(command.getResourceTemplate().getName());

				// The candidate description contains the template description
				// as a substring (for non "" template descriptions) OR
				boolean descriptionIsSubstring = command.getResourceTemplate().getDescription().isEmpty()
						? true : resource.getDescription().equals(command.getResourceTemplate().getDescription());

				// The template description and name are both ""))"
				boolean noDescriptionAndName = command.getResourceTemplate().getName().isEmpty()
						&& command.getResourceTemplate().getDescription().isEmpty();

				if (equalChannel && equalOrNoOwner && equalTags && equalOrNoUri
						&& (nameIsSubstring || descriptionIsSubstring || noDescriptionAndName)) {
					count++;

					// "The server will never reveal the owner of a resource in
					// a response. If a resource has an owner then it will be
					// replaced with the "*" character."
					resource.setOwner("*");

					sendString(resource.toJson(), output);

					// Reset owner
					resource.setOwner(owner);
				}
			}

			// Relay
			if (command.getRelay()) {
				// "The owner and channel information in the original query are
				// both set to "" in the forwarded query"
				command.getResourceTemplate().setOwner(Resource.DEFAULT_OWNER);
				command.getResourceTemplate().setChannel(Resource.DEFAULT_CHANNEL);

				// "Relay field is set to false"
				command.setRelay(false);

				// Forward query to all servers in servers list
				final CountDownLatch latch = new CountDownLatch(this.servers.size());
				final ArrayList<Integer> countArray = new ArrayList<>();
				for (ConcurrentHashMap.Entry<ServerInfo, Boolean> entry : this.servers.entrySet()) {
					ServerInfo serverInfo = entry.getKey();

					// Create a new thread for each ServerInfo object
					Thread relayThread = new Thread("RelayHandler") {
						@Override
						public void run() {
							Socket socket;
							try {
								socket = new Socket(serverInfo.getHostname(), serverInfo.getPort());
								socket.setSoTimeout(TIME_OUT_LIMIT); // wait for
								// 5
								// seconds
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
									if (fromServer.contains("\"ezserver\":\"" + serverInfo.getHostname() + ":"
											+ serverInfo.getPort() + "\"")) {
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
							} catch (IOException e) {
								logger.error(e.getClass().getName() + " " + e.getMessage());
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
			}

			sendResponse(buildResultSizeResponse(count), output);
		}
	}

	private void processExchangeCommand(Command command, DataOutputStream output, ServerInfo source) {
		logger.debug("Processing EXCHANGE command");

		if (command.getServerList() == null || command.getServerList().size() == 0) {
			sendResponse(buildErrorResponse("missing or invalid server list"), output);
		} else {
			for (ServerInfo serverInfo : command.getServerList()) {
				// Check if that is our current server
				if (serverInfo.getHostname() != serverArgs.getSafeHost()
						|| serverInfo.getPort() != serverArgs.getSafePort()) {
					// Add server to server list
					servers.put(serverInfo, true);
				}
			}

			sendResponse(buildSuccessResponse(), output);
		}
	}

	private void processFetchCommand(Command command, DataOutputStream output) {
		logger.debug("Processing FETCH command");
		
		// Check for invalid resourceTemplate fields
		if (command.getResourceTemplate() == null) {
			sendResponse(buildErrorResponse("missing resourceTemplate"), output);
		} else if (command.getResourceTemplate().getURI() == null || command.getResourceTemplate().getURI().length() == 0
				|| command.getResourceTemplate().getURI().isEmpty()) {
			sendResponse(buildErrorResponse("invalid resourceTemplate - missing uri"), output);
		} else if (!this.resources.containsKey(command.getResourceTemplate())) {
			sendResponse(buildErrorResponse("resource doesn't exist"), output);
		} else {
			int foundResources = 0;
			
			// TODO AF Is there a better way than iterating over the whole map?
			for (ConcurrentHashMap.Entry<Resource, String> entry : this.resources.entrySet()) {
				Resource resource = entry.getKey();
				String owner = entry.getValue();
				if (resource.getChannel().equals(command.getResourceTemplate().getChannel())
						&& resource.getURI().equals(command.getResourceTemplate().getURI())) {
					sendResponse(buildSuccessResponse(), output);

					try {
						URI uri = new URI(resource.getURI());
						File file = new File(uri);
						int length = (int) file.length();

						resource.setResourceSize(length);

						// "The server will never reveal the owner of a resource
						// in a response. If a resource has an owner then it
						// will be replaced with the "*" character."
						resource.setOwner("*");

						sendString(resource.toJson(), output);

						// Reset owner
						resource.setOwner(owner);

						sendFile(file, output);

						foundResources++;
						break;
					} catch (URISyntaxException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						sendResponse(buildErrorResponse("invalid resource - invalid uri"), output);
					} catch (IOException e) {
						logger.error(e.getClass().getName() + " " + e.getMessage());
						sendResponse(buildErrorResponse("invalid resource - unable to send file"), output);
					}
				}
			}
			
			sendResponse(buildResultSizeResponse(foundResources), output);
		}
	}

	private void processShareCommand(Command command, DataOutputStream output) {
		logger.debug("Processing SHARE command");

		Response response = buildErrorResponse("cannot share resource");

		// Check for invalid resource fields
		if (command.getSecret() == null || command.getSecret().isEmpty() || command.getSecret().length() == 0) {
			// "The server secret must be present ..."
			response = buildErrorResponse("missing resource and/or secret");
		} else if (!command.getSecret().equals(this.serverArgs.getSafeSecret())) {
			// "... and must equal the value known to the server."
			response = buildErrorResponse("incorrect secret");
		} else if (command.getResource() == null) {
			response = buildErrorResponse("missing resource and/or secret");
		} else if (command.getResource().getURI() == null || command.getResource().getURI().length() == 0
				|| command.getResource().getURI().isEmpty()) {
			// "The URI must be present, ..."
			response = buildErrorResponse("invalid resource - missing uri");
		} else {
			try {
				URI uri = new URI(command.getResource().getURI());

				if (!uri.isAbsolute()) {
					// "..., must be absolute ..."
					response = buildErrorResponse("invalid resource - uri must be absolute");
				} else if (uri.getAuthority() != null) {
					// "..., non-authoritative ...."
					response = buildErrorResponse("invalid resource - uri must be non-authoritative");
				} else if (!uri.getScheme().equals("file")) {
					// "... and must be a file scheme."
					response = buildErrorResponse("invalid resource - uri must be a file scheme");
				} else if (this.resources.containsKey(command.getResource())
						&& !this.resources.get(command.getResource()).equals(command.getResource().getOwner())) {
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
						command.getResource().setEzserver(this.serverArgs.getSafeHost() + ":" + this.serverArgs.getSafePort());
						this.resources.put(command.getResource(), command.getResource().getOwner());
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
		if (command.getResource() == null) {
			response = buildErrorResponse("missing resource");
		} else if (command.getResource().getURI() == null || command.getResource().getURI().isEmpty()) {
			// URI must be present
			response = buildErrorResponse("invalid resource - missing uri");
		} else if (!this.resources.containsKey(command.getResource())) {
			// Resource must exist
			response = buildErrorResponse("cannot remove resource - resource does not exist");
		} else {
			// SUCCESS
			this.resources.remove(command.getResource());
			response = buildSuccessResponse();
		}

		sendResponse(response, output);
	}

	/*
	 * Methods for building a new success, error or result-size message
	 */
	private Response buildSuccessResponse() {
		Response response = new Response();
		response.setToSuccess();
		return response;
	}

	private Response buildErrorResponse(String message) {
		Response response = new Response();
		response.setToError(message);
		return response;
	}
	
	private Response buildResultSizeResponse(int size) {
		Response response = new Response();
		response.setToResultSize(size);
		return response;
	}

	private void sendResponse(Response response, DataOutputStream output) {
		sendString(response.toJson(), output);
	}

	private void sendString(String string, DataOutputStream output) {
		try {
			output.writeUTF(string);
			output.flush();
			logger.debug("SENT: " + string);
		} catch (IOException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}

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
					String[] args = { "-" + ClientArgs.EXCHANGE_OPTION, "-" + ClientArgs.SERVERS_OPTION, serversAsString };
					ClientArgs exchangeArgs = new ClientArgs(args);
					Command command = new Command().buildExchange(exchangeArgs);

					Socket socket;
					try {
						socket = new Socket(randomServer.getHostname(), randomServer.getPort());
						socket.setSoTimeout(TIME_OUT_LIMIT); // wait for seconds
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

	private void removeServer(ServerInfo serverInfo) {
		logger.debug("Removing server due to not being reachable or a communication error having occurred");
		servers.remove(serverInfo);
	}

	/**
	 * 
	 * @param file
	 * @param socket
	 * @throws IOException
	 */
	public void sendFile(File file, DataOutputStream os) throws IOException {
		// define an array as the length of the file
		byte[] bytes = new byte[(int) file.length()];
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);
		// read the file into the bytes array
		bis.read(bytes);
		// write the bytes array onto the stream
		os.write(bytes);
		os.flush();
		// close bis - no longer needed
		bis.close();
	}
}
