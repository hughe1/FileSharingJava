package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
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
 */
public class Client {

	public static final int TIME_OUT_LIMIT = 10000;
	public static final int BUF_SIZE = 1024 * 4;
	private Logger logger;
	private ClientArgs clientArgs;
	private Socket socket;
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
	private Client(String[] args) {
		this.clientArgs = new ClientArgs(args);
		this.configLogger();
		this.serverInfo = this.parseServerInfo();
	}
	
	/**
	 * The only method that should be called after creating a client object.
	 * It is responsible for doing the client work. Furthermore, it establishes
	 * a connection with the server, sends a command and processes the response.
	 */
	public void run() {
		try {
			logger.info("Connecting to host " + serverInfo.getHostname() + " at port " + serverInfo.getPort());
			this.socket = new Socket(serverInfo.getHostname(), serverInfo.getPort());
			this.socket.setSoTimeout(TIME_OUT_LIMIT); // wait for TIME_OUT_LIMIT seconds
			
			DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
			Command command = this.parseCommand();
			this.submitCommand(command, outToServer);

			// block call, wait for client response
			String fromServer = inFromServer.readUTF();
			logger.debug("RECEIVED: " + fromServer);
			if(fromServer.contains("error")) {
				// onError
				Response response = new Response().fromJson(fromServer);
				logger.error("Error: " + response.getErrorMessage());
			}
			else if(fromServer.contains("success")) {
				// onSuccess
				logger.info("Success!");
				this.onSuccess(command,inFromServer);
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
	 * Submits a command to a DataOutPutStream
	 * 
	 * @param command the object containing the instructions to the server
	 * @param outToServer is the output stream
	 * @throws IOException if method cannot write to the output stream
	 */
	private void submitCommand(Command command, DataOutputStream outToServer) 
			throws IOException {
		outToServer.writeUTF(command.toJson());
		logger.info("Sending " + command.getCommand() + " command...");
		outToServer.flush();
		logger.debug("SENT: " + command.toJson());
		logger.info(command.getCommand() + " command sent. Waiting for response..");
	}

	/**
	 * 
	 * @return
	 */
	private Command parseCommand() {
		return new Command(clientArgs);
	}
	
	/**
	 * 
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
	 * 
	 * @return
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
	private void onSuccess(Command command, DataInputStream inFromServer) throws 
		SocketTimeoutException, IOException {
		// publish, remove, share, exchange -> only print the response
		// query, fetch -> have to deal with these dynamically
		switch (command.getCommand()) {
		case Command.QUERY_COMMAND:
			this.processQuery(inFromServer);
			break;
		case Command.FETCH_COMMAND:
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
	 * @throws IOException 
	 */
	private void processFetch(DataInputStream inFromServer) throws IOException {
		String resourceString = inFromServer.readUTF();
		this.logger.debug("RECEIVED: " + resourceString);
		Resource resource = new Resource().fromJson(resourceString);
		
		// Get file name
		String[] strings = resource.getURI().split("/");
		String fileName = strings[strings.length - 1];
		
		this.receiveFile(fileName, resource.getResourceSize());
		this.logger.debug("RECEIVED: " + inFromServer.readUTF());
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
		boolean run = true;
		while (run) {
			String fromServer = inFromServer.readUTF();
			logger.debug("RECEIVED: " + fromServer);
			if (fromServer.contains("resultSize")) {
				Response response = new Response().fromJson(fromServer);
				run = false;
				logger.info("Query finished. Found " + response.getResultSize() +  " results.");
			} else {
				logger.info(fromServer);
			}			
		}
	}

	
	/**
	 * It downloads a file from a socket InputStream and BUF_SIZE at a time and
	 * writes the output to a FileOutputStream.
	 * @param fileName the name of the file to write to disk
	 * @param fileSize the number of bytes the expected file is going to be
	 * @throws IOException
	 */
	private void receiveFile(String fileName, long fileSize) throws IOException {
		logger.info("Downloading " + fileName);
		InputStream in = this.socket.getInputStream();
		FileOutputStream out = new FileOutputStream(fileName);
		byte[] bytes  = new byte [BUF_SIZE];		
		int count, totalRead = 0;
		long bytesToRead = 0;
		// stop reading only when have read bytes equal to the fileSize
		logger.info("...");
		while(totalRead < fileSize) {
			// determine how many more bytes to read
			bytesToRead = Math.min(bytes.length, fileSize-totalRead);
			// read bytesToRead from the InputStream
			count = in.read(bytes,0,(int) bytesToRead);
			totalRead += count;
			// write bytes to file
			out.write(bytes,0,count);
			this.logger.debug("Downloaded: " + count + " bytes, remaining: " + 
					(fileSize - totalRead) + " bytes");
		}
		out.close();

		File file = new File(fileName);
		logger.info("Download complete!");
		logger.info("Your file is located at " + file.getAbsolutePath());
	}
}
