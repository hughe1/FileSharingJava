package EZShare;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
	private static Logger logger;
	private static Socket socket;
	
	public static void main(String[] args) {

		Client client = new Client(args);


		// Configure logger
		if (client.clientArgs.hasOption(Constants.debugOption)) {
			System.setProperty("log4j.configurationFile", "../logging-config-debug.xml");
		} else {
			System.setProperty("log4j.configurationFile", "../logging-config-default.xml");
		}
		logger = LogManager.getRootLogger();

		logger.debug("Debugger enabled");
				
		Command command = client.parseCommand();
		
		ServerInfo serverInfo = client.parseServerInfo();

		logger.debug("Publishing to " + serverInfo);

		
		try {
			logger.info("Connecting to host "+serverInfo.getHostname()+" at port "+serverInfo.getPort());
			socket = new Socket(serverInfo.getHostname(), serverInfo.getPort());
			socket.setSoTimeout(TIME_OUT_LIMIT); // wait for 5 seconds
			DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

			outToServer.writeUTF(command.toJson());
			logger.info("Sending "+command.command+" command... ");
			outToServer.flush();
			logger.info(command.command+" command sent. Waiting for response.. ");

			logger.debug("SENT: " + command.toJson());

			// TODO processing the responses in a better way
			// TODO: Implement timeout
			boolean run = false;

			ArrayList<String> responses = new ArrayList<String>();
			do {
				String fromServer = inFromServer.readUTF();
				

				if (fromServer.contains("success") && command.command.equals("QUERY")
						|| command.command.equals("FETCH"))
					run = true;
				
				if (fromServer.contains("resultSize"))
					run = false;
				logger.debug("RECEIVED: " + fromServer);
				//client.processServerResponse(fromServer);

				String response = fromServer;
				responses.add(response);

				
				//logger.info("Server response successfully processed ");
			} while (run);
			
			client.processServerResponse(command, responses, inFromServer);

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
	public void processServerResponse(Command command, ArrayList<String> responses, DataInputStream input) {
		try 
		{	

			
			int number_responses = responses.size();
			int number_correct_responses = 0;
			for (int i = 0;i<number_responses;i++) {
				Response response = (new Response()).fromJson(responses.get(i));
				if (!parseResponseForErrors(response, input)) {
					 number_correct_responses +=1;
				}
			}
			
			logger.info("Server response received ("+number_correct_responses+" out of "+number_responses+" responses well-formed)");
			
			if (number_correct_responses != number_responses) {
				logger.error("Incorretly formed server response");
			}
			else {
				try {
				switch (command.command) {
					case Constants.queryCommand:
						processQueryResponse(responses, input);
						break;
					case Constants.fetchCommand:
						processFetchResponse(responses, input);
						break;
					case Constants.publishCommand:
						processPublishResponse(responses, input);
						break;
					case Constants.shareCommand:
						processShareResponse(responses, input);
						break;
					case Constants.removeCommand:
						processRemoveResponse(responses, input);
						break;
					case Constants.invalidCommand:
						processInvalidResponse(responses, input);
						break;
					default:
						processMissingOrInvalidResponse(responses, input);
						break;
				}
				}
				catch (Exception e) {
					logger.error("Error processing response");
				}
			}
			
		}
		catch (Exception e) {
			logger.error("Error processing Server response");
		}
	
}

	private static void processMissingOrInvalidResponse(ArrayList<String> responses, DataInputStream input) {
		logger.error("Incorrectly formed server response");
		
	}

	private static void processInvalidResponse(ArrayList<String> responses, DataInputStream input) {
		logger.error("Incorrectly formed server response");
		
	}

	private static void processRemoveResponse(ArrayList<String> responses, DataInputStream input) {
		genericResponse(responses, "Remove");
		
	}

	private static void processShareResponse(ArrayList<String> responses, DataInputStream input) {
		genericResponse(responses, "Share");
		
	}

	private static void processPublishResponse(ArrayList<String> responses, DataInputStream input) {
		genericResponse(responses, "Publish");
	}
	
	private static void genericResponse(ArrayList<String> responses, String commandName) {
		Response response = (new Response()).fromJson(responses.get(0));
		if (response.response.equals("success")) {
			logger.info(commandName+" successful");
		}
		else {
			logger.info(commandName+" unsuccessful: "+response.errorMessage);
		}
	}

	private static void processFetchResponse(ArrayList<String> responses, DataInputStream input) {
		// TODO Auto-generated method stub
		
	}

	private static void processQueryResponse(ArrayList<String> responses, DataInputStream input) {
		Response firstResponse = (new Response()).fromJson(responses.get(0));
		Response lastResponse = (new Response()).fromJson(responses.get(responses.size()-1));

		if (firstResponse.response.equals("success")) {
			logger.info(lastResponse.resultSize+" results returned: ");
			for (int i =1; i<responses.size()-1;i++) {
				logger.info("    "+responses.get(i));
			}
		}
		else {
			logger.info("Query unsuccessful: "+firstResponse.errorMessage);
		}
		
	}
	
	// Skeleton method to be used if we need to implement any error checking
	private static boolean parseResponseForErrors(Response response, DataInputStream input) {
		
		boolean errorFound = false;
		return errorFound;
	}
	

	/**
	 * 
	 * @param fileName
	 * @param fileSize
	 * @param socket
	 * @throws IOException
	 */
	public void receiveFile(String fileName, int fileSize, Socket socket) throws IOException {
		InputStream is = socket.getInputStream();
		FileOutputStream fos = new FileOutputStream(fileName);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		// create a bytes array + 1 byte.. otherwise it will hang
		byte[] bytes  = new byte [fileSize+1];
	    int bytesRead = is.read(bytes,0,bytes.length);
	    // System.out.println("bytes read: " + bytesRead);
	    int current = bytesRead;
	    do {
	    	bytesRead = is.read(bytes, current, (bytes.length-current));
	    	// System.out.println("bytes read: " + bytesRead);
	    	if(bytesRead >= 0) current += bytesRead;
	    } while (bytesRead > -1);
	    // bytes left over on the buffer
	    bos.write(bytes);
	    bos.flush();
	    // close in and out streams
	    fos.close();
	    bos.close();
	}
}
