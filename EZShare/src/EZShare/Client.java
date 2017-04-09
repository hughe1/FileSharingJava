package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The class contains functionality to parse command-line arguments according to
 * the assignment specifications. Additionally, it can also communicate with an
 * EZShare.Server that is listening for EZShare.Client, according to the
 * protocol defined in the assignment specifications.
 * 
 * Aaron's server: sunrise.cis.unimelb.edu.au:3780
 * 
 * @author Koteski, B
 *
 */
public class Client {

	private ClientArgs clientArgs;
	public static final int TIME_OUT_LIMIT = 5000;

	public static void main(String[] args) {
		// TODO: Remove if -- solely for testing purposes
		if (args.length == 0) {
			// QUERY
			String[] args2 = { "-" + Constants.queryOption, "-" + Constants.channelOption, "mydamnchannel",
					"-" + Constants.descriptionOption, "jar", "-" + Constants.uriOption, "http://www.unimelb.edu.au",
					"-" + Constants.debugOption };

			// PUBLISH
			// String[] args2 = { "-" + Constants.publishOption, "-" +
			// Constants.nameOption, "Unimelb website",
			// "-" + Constants.descriptionOption, "The main page for the
			// University of Melbourne",
			// "-" + Constants.uriOption, "http://www.unimelb.edu.au", "-" +
			// Constants.tagsOption, "web,html",
			// "-" + Constants.ownerOption, "Alex", "-" + Constants.debugOption,
			// "-" + Constants.debugOption
			// };

			// REMOVE
			// String[] args2 = { "-" + Constants.removeOption, "-" +
			// Constants.uriOption, "http://www.unimelb.edu.au", "-" +
			// Constants.debugOption };

			// SHARE
			// String[] args2 = { "-" + Constants.shareOption, "-" +
			// Constants.uriOption,
			// "file:///Users/alexandrafritzen/ezshare.jar", "-" +
			// Constants.nameOption, "EZShare JAR",
			// "-" + Constants.descriptionOption, "The jar file for EZShare. Use
			// with caution.",
			// "-" + Constants.tagsOption, "jar", "-" + Constants.channelOption,
			// "myprivatechannel",
			// "-" + Constants.ownerOption, "aaron010", "-" +
			// Constants.secretOption, "1234",
			// "-" + Constants.debugOption };

			args = args2;
		}

		// String[] args2 = { "-exchange", "-servers", "host1:sadf"};
		Client client = new Client(args);

		// Configure logger
		if (client.clientArgs.hasOption(Constants.debugOption)) {
			System.setProperty("log4j.configurationFile", "logging-config-debug.xml");
		} else {
			System.setProperty("log4j.configurationFile", "logging-config-default.xml");
		}
		Logger logger = LogManager.getRootLogger();
		logger.debug("Debugger enabled");

		Command command = client.parseCommand();
		ServerInfo serverInfo = client.parseServerInfo();

		logger.debug("Publishing to " + serverInfo);

		try {
			Socket socket = new Socket(serverInfo.getHostname(), serverInfo.getPort());
			socket.setSoTimeout(TIME_OUT_LIMIT); // wait for 5 seconds
			DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

			outToServer.writeUTF(command.toJson());
			outToServer.flush();

			// Doesn't work with the logger due to multiple lines being
			// generated
			// logger.info(command.toJsonPretty());
			logger.debug("SENT: " + command.toJson());

			// TODO processing the responses in a better way
			// TODO: Implement timeout
			boolean run = false;
			do {
				String fromServer = inFromServer.readUTF();
				if (fromServer.contains("success") && command.command.equals("QUERY")
						|| command.command.equals("FETCH"))
					run = true;
				if (fromServer.contains("resultSize"))
					run = false;
				logger.debug("RECEIVED: " + fromServer);
				processServerResponse(fromServer);
			} while (run);

			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			logger.error(e.getClass().getName() + " " + e.getMessage());
		} catch (SocketTimeoutException e) {
			logger.error(e.getClass().getName() + " " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getClass().getName() + " " + e.getMessage());
		}
	}

	/**
	 * Constructor for Client
	 * 
	 * @param args
	 *            String[] command line arguments
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

	/**
	 * 
	 */
	public static void processServerResponse(String response) {
		// TODO ?
	}
}
