package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The class contains functionality to parse command-line arguments according to
 * the assignment specifications. Additionally, it can also communicate with an
 * EZShare.Server that is listening for EZShare.Client, according to the
 * protocol defined in the assignment specifications.
 * 
 * Aaron's server: sunrise.cis.unimelb.edu.au:3780
 */
public class Client {
	public static final int TIME_OUT_LIMIT = 10000;
	public static final int BUF_SIZE = 1024 * 4;
	private Logger logger;
	private ClientArgs clientArgs;
	private Socket socket;
	// private SSLSocket socket;
	private ServerInfo serverInfo;

	public static void main(String[] args) {
		// create a client and run it
		new Client(args).run();
	}

	/**
	 * Constructor for Client
	 * 
	 * @param args
	 *            String[] command line arguments
	 */
	public Client(String[] args) {
		this.clientArgs = new ClientArgs(args);
		this.configLogger();
		this.serverInfo = this.parseServerInfo();
	}

	/**
	 * The only method that should be called after creating a client object. It
	 * is responsible for doing the client work. Furthermore, it establishes a
	 * connection with the server, sends a command and processes the response.
	 */
	public void run() {
		Command command = this.parseCommand();

		if (command.getCommand() == null) {
			// No command found
			logger.error("No or too many commands found.");
			clientArgs.printArgsHelp("Client");
		} else {
			try {
				logger.info("Connecting to host " + serverInfo.getHostname() + " at port " + serverInfo.getPort());

				if (clientArgs.hasOption(ClientArgs.SECURE_OPTION)) {

					// Create secure socket

					// Location of the Java keystore file containing the
					// collection of
					// certificates trusted by this application (trust store).
					System.setProperty("javax.net.ssl.trustStore", "clientKeyStore/keystore.jks");

					// Debug option
					// System.setProperty("javax.net.debug","all");

					// Create SSL socket and connect it to the remote server
					SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					this.socket = (SSLSocket) sslsocketfactory.createSocket(serverInfo.getHostname(),
							serverInfo.getPort());
				} else {

					// Create insecure socket
					this.socket = new Socket(serverInfo.getHostname(), serverInfo.getPort());
				}

				this.socket.setSoTimeout(TIME_OUT_LIMIT);

				DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
				DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

				this.submitCommand(command, outToServer);

				// block call, wait for client response
				String fromServer = receiveString(inFromServer);
				if (fromServer.contains("error")) {
					// onError
					Response response = new Response().fromJson(fromServer);
					logger.error("Error: " + response.getErrorMessage());
				} else if (fromServer.contains("success")) {
					// onSuccess
					logger.info("Success!");
					this.onSuccess(command, inFromServer, outToServer);
				} else {
					// something went wrong
					logger.error("Something went wrong when receiving reponse from Server");
				}
				socket.close();

			} catch (UnknownHostException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
			} catch (SocketTimeoutException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
			} catch (IOException e) {
				logger.error(e.getClass().getName() + " " + e.getMessage());
			}
		}
	}

	/**
	 * Submits a command to a DataOutPutStream
	 * 
	 * @param command
	 *            the object containing the instructions to the server
	 * @param outToServer
	 *            is the output stream
	 * @throws IOException
	 *             if method cannot write to the output stream
	 */
	private void submitCommand(Command command, DataOutputStream outToServer) throws IOException {
		logger.info("Sending " + command.getCommand() + " command...");
		sendString(command.toJson(), outToServer);
		logger.info(command.getCommand() + " command sent. Waiting for response..");
	}

	/**
	 * Parses the Client's clientArgs into a Command object
	 * 
	 * @return The parsed Command object
	 */
	private Command parseCommand() {
		return new Command(clientArgs);
	}

	/**
	 * Configures the logger based on whether the client wrote "-debug" or not
	 */
	private void configLogger() {
		// Configure logger
		if (this.clientArgs.hasOption(ClientArgs.DEBUG_OPTION)) {
			System.setProperty("log4j.configurationFile", "logging-config-debug.xml");
		} else {
			System.setProperty("log4j.configurationFile", "logging-config-default.xml");
		}
		logger = LogManager.getRootLogger();
		logger.debug("Debugger enabled");
	}

	/**
	 * Parses host and port into a ServerInfo object
	 * 
	 * @return The parsed ServerInfo object
	 */
	private ServerInfo parseServerInfo() {
		return new ServerInfo(clientArgs.getSafeHost(), clientArgs.getSafePort());
	}

	/**
	 * This is called when the client responds and the response contains a
	 * {"response" : "success"} JSON message
	 * 
	 * @param command
	 * @param inFromServer
	 * @throws IOException
	 * @throws SocketTimeoutException
	 */
	private void onSuccess(Command command, DataInputStream inFromServer, DataOutputStream outToServer)
			throws SocketTimeoutException, IOException {
		// publish, remove, share, exchange -> only print the response
		// query, fetch -> have to deal with these dynamically
		switch (command.getCommand()) {
		case Command.QUERY_COMMAND:
			this.processQuery(inFromServer);
			break;
		case Command.FETCH_COMMAND:
			this.processFetch(inFromServer);
			break;
		case Command.SUBSCRIBE_COMMAND:
			this.processSubscribe(inFromServer, outToServer);
			break;
		default:
			// not much to do here
			break;
		}
	}

	/**
	 * Responsible for processing a query command. By the time this method is
	 * called, the client has already called readUTF once. This method should
	 * process anything after the initial JSON object from the server.
	 * 
	 * @param inFromServer
	 * @throws IOException
	 */
	private void processFetch(DataInputStream inFromServer) throws IOException {
		String resourceString = receiveString(inFromServer);
		Resource resource = new Resource().fromJson(resourceString);

		// Get file name
		String[] strings = resource.getURI().split("/");
		String fileName = strings[strings.length - 1];

		this.receiveFile(fileName, resource.getResourceSize());
		receiveString(inFromServer);
	}

	/**
	 * Responsible for processing a query command. By the time this method is
	 * called, the client has already called readUTF once. This method should
	 * process anything after the initial JSON object from the server.
	 * 
	 * @param inFromServer
	 * @throws SocketTimeoutException,
	 *             IOException
	 */
	private void processQuery(DataInputStream inFromServer) throws SocketTimeoutException, IOException {
		boolean run = true;
		while (run) {
			String fromServer = receiveString(inFromServer);
			if (fromServer.contains("resultSize")) {
				Response response = new Response().fromJson(fromServer);
				run = false;
				logger.info("Query finished. Found " + response.getResultSize() + " results.");
			} else {
				logger.info(fromServer);
			}
		}
	}

	/**
	 * Responsible for processing a subscribe command. By the time this method
	 * is called, the client has already called readUTF once. This method should
	 * process anything after the initial JSON object from the server.
	 * 
	 * @param inFromServer
	 * @throws SocketTimeoutException,
	 *             IOException
	 */
	private void processSubscribe(DataInputStream inFromServer, DataOutputStream outToServer) {
		// Make sure socket doesn't timeout or die
		try {
			this.socket.setKeepAlive(true);
			this.socket.setSoTimeout(0);
		} catch (SocketException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}

		Thread subscriptionThread = new Thread("SubscriptionHandler") {
			@Override
			public void run() {
				boolean run = true;
				try {
					while (run) {
						String fromServer = receiveString(inFromServer);

						if (fromServer.contains("resultSize")) {
							Response response = new Response().fromJson(fromServer);
							logger.info("Successfully unsubscribed. Found " + response.getResultSize() + " results.");
							run = false;
						} else if (fromServer.contains("error")) {
							Response response = new Response().fromJson(fromServer);
							logger.error("Error: " + response.getErrorMessage());
							run = false;
						}
					}
				} catch (IOException e) {
					logger.error(e.getClass().getName() + " " + e.getMessage());
				}
			}
		};
		subscriptionThread.start();

		try {
			// "When the user presses ENTER, i.e. a line is read from standard
			// input..."
			System.in.read();

			// "... then the client will UNSUBSCRIBE and can then terminate."
			String[] unsubscribeArgs = { "-unsubscribe" };
			ClientArgs unsubArgs = new ClientArgs(unsubscribeArgs);
			Command unsubscribeCommand = new Command(unsubArgs);
			this.submitCommand(unsubscribeCommand, outToServer);

			subscriptionThread.join();
		} catch (IOException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		} catch (InterruptedException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}

	/**
	 * Sends a string to the given DataOutputStream
	 * 
	 * @param string
	 *            The string to be sent
	 * @param output
	 *            The DataOutputStream to send the string to via writeUTF()
	 * @throws IOException
	 */
	private void sendString(String string, DataOutputStream output) throws IOException {
		output.writeUTF(string);
		output.flush();
		logger.debug("SENT: " + string);
	}

	/**
	 * Receives a string from the given DataInputStream and logs it
	 * 
	 * @param input
	 *            The DataInputStream to read the string from
	 * @return The read string
	 * @throws IOException
	 */
	private String receiveString(DataInputStream input) throws IOException {
		String message = input.readUTF();
		logger.debug("RECEIVED: " + message);
		return message;
	}

	/**
	 * It downloads a file from a socket InputStream and BUF_SIZE at a time and
	 * writes the output to a FileOutputStream.
	 * 
	 * @param fileName
	 *            the name of the file to write to disk
	 * @param fileSize
	 *            the number of bytes the expected file is going to be
	 * @throws IOException
	 */
	private void receiveFile(String fileName, long fileSize) throws IOException {
		logger.info("Downloading " + fileName);
		InputStream in = this.socket.getInputStream();
		FileOutputStream out = new FileOutputStream(fileName);
		byte[] bytes = new byte[BUF_SIZE];
		int count, totalRead = 0;
		long bytesToRead = 0;
		// stop reading only when have read bytes equal to the fileSize
		logger.info("...");
		while (totalRead < fileSize) {
			// determine how many more bytes to read
			bytesToRead = Math.min(bytes.length, fileSize - totalRead);
			// read bytesToRead from the InputStream
			count = in.read(bytes, 0, (int) bytesToRead);
			totalRead += count;
			// write bytes to file
			out.write(bytes, 0, count);
			this.logger.debug("Downloaded: " + count + " bytes, remaining: " + (fileSize - totalRead) + " bytes");
		}
		out.close();

		File file = new File(fileName);
		logger.info("Download complete!");
		logger.info("Your file is located at " + file.getAbsolutePath());
	}
}
