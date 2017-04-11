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

			// block call
			String fromServer = inFromServer.readUTF();
			if(fromServer.contains("error")) {
				// onError
				Response response = new Response().fromJson(fromServer);
				logger.error(response.toJson());
			}
			else if(fromServer.contains("success")) {
				// onSuccess()
				Response response = new Response().fromJson(fromServer);
				logger.info(response.toJson());
				client.onSuccess(command,inFromServer);
			}
			else {
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
	 * @param command
	 * @param inFromServer
	 * @throws IOException 
	 * @throws SocketTimeoutException 
	 */
	public void onSuccess(Command command, DataInputStream inFromServer) throws 
		SocketTimeoutException, IOException {
		// publish, remove, share, exchange -> only print the response
		// query, fetch -> have to deal with these dynamically
		switch (command.command) {
		case Constants.queryCommand:
			this.processQuery(inFromServer);
			break;
		case Constants.fetchCommand:
			this.processFetch(inFromServer);
			break;
		default:
			// logger.info(command.toJson());
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
	 */
	private void processFetch(DataInputStream inFromServer) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Responsible for processing a query command. By the time this method is
	 * called, the client has already called readUTF once. This method should
	 * process anything after the initial JSON object from the server.
	 * 
	 * @param inFromServer
	 * @throws SocketTimeoutException, IOException 
	 */
	private void processQuery(DataInputStream inFromServer) throws 
		SocketTimeoutException, IOException {
		// TODO Auto-generated method stub
		boolean run = true;
		while(run) {
			String fromServer = inFromServer.readUTF();
			if(fromServer.contains("resultSize")) {
				run = false;
				logger.info(new Response().fromJson(fromServer).toJson());
			}
			else {
				logger.info(new Resource().fromJson(fromServer).toJson());
			}			
		}
	}

	/**
	 * 
	 * @param fileName
	 * @param fileSize
	 * @param socket
	 * @throws IOException
	 */
	public void receiveFile(String fileName, int fileSize) throws IOException {
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
