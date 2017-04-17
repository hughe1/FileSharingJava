package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
<<<<<<< HEAD
import java.util.Arrays;

=======
>>>>>>> Client_Refactor
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
<<<<<<< HEAD
 * The Client class represents EZShare clients. 
 * The class contains functionality to parse command-line arguments according 
 * to the assignment specifications. Additionally, it can also communicate with
 * and handles responses/file transfer from an EZShare server that is listening 
 * for EZShare client, according to the protocol defined in the assignment 
 * specifications.
 *  
 */
public class Client {
	public static final int TIME_OUT_LIMIT = 10*1000;
	
	private ClientArgs clientArgs;
	private static Logger logger;
	private static Socket socket;
	
	public static void main(String[] args) {
		Client client = new Client(args);
		client.configLogger();
		
		// Parse the command and server info from command-line arguments
		Command command = client.parseCommand();
		ServerInfo serverInfo = client.parseServerInfo();
		logger.debug(command.getCommand() + " to " + serverInfo);
				
		try {
			//Establish the socket for sending and receiving
			logger.debug("Connecting to host " + serverInfo.getHostname() +	" at port " + serverInfo.getPort());
			socket = new Socket(serverInfo.getHostname(), serverInfo.getPort());
			socket.setSoTimeout(TIME_OUT_LIMIT); // wait for 5 seconds
			
			client.sendCommand(command);
			
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			
			//Populate the response's fields according to the first message received
			Response first_response = client.getResponse(inputStream);
			
			//Deal with subsequent responses, if relevant to the command
			client.processResponses(command, first_response, inputStream);
			
			//Done
=======
 * The class contains functionality to parse command-line arguments according to
 * the assignment specifications. Additionally, it can also communicate with an
 * EZShare.Server that is listening for EZShare.Client, according to the
 * protocol defined in the assignment specifications.
 * 
 * Aaron's server: sunrise.cis.unimelb.edu.au:3780
 */
public class Client {

	public static final int TIME_OUT_LIMIT = 5000;
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
	}
	
	/**
	 * The only method that should be called after creating a client object.
	 * It is responsible for doing the client work. Furthermore, it establishes
	 * a connection with the server, sends a command and processes the response.
	 */
	public void run() {
		try {
			logger.info("Connecting to host "+serverInfo.getHostname()+" at port "+serverInfo.getPort());
			this.socket = new Socket(serverInfo.getHostname(), serverInfo.getPort());
			this.socket.setSoTimeout(TIME_OUT_LIMIT); // wait for TIME_OUT_LIMIT seconds
			
			DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
			Command command = this.parseCommand();
			this.submitCommand(command, outToServer);

			// block call, wait for client response
			String fromServer = inFromServer.readUTF();
			if(fromServer.contains("error")) {
				// onError
				Response response = new Response().fromJson(fromServer);
				logger.error(response.toJson());
			}
			else if(fromServer.contains("success")) {
				// onSuccess
				Response response = new Response().fromJson(fromServer);
				logger.info(response.toJson());
				this.onSuccess(command,inFromServer);
			}
			else {
				// something went wrong
				logger.error("Something went wrong when receiving reponse from Server");
			}
>>>>>>> Client_Refactor
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
<<<<<<< HEAD
	 * Constructor for Client based on the command-line arguments
	 * @param args
	 *            String[] command-line arguments
=======
	 * Submits a command to a DataOutPutStream
	 * 
	 * @param command the object containing the instructions to the server
	 * @param outToServer is the output stream
	 * @throws IOException if method cannot write to the output stream
>>>>>>> Client_Refactor
	 */
	private void submitCommand(Command command, DataOutputStream outToServer) 
			throws IOException {
		outToServer.writeUTF(command.toJson());
		logger.info("Sending "+command.command+" command... ");
		outToServer.flush();
		logger.info(command.command+" command sent. Waiting for response.. ");
		logger.debug("SENT: " + command.toJson());
	}
	
	public void configLogger() {
		if (clientArgs.hasOption(ClientArgs.DEBUG_OPTION)) {
			System.setProperty("log4j.configurationFile", "../logging-config-debug.xml");
		} else {
			System.setProperty("log4j.configurationFile", "../logging-config-default.xml");
		}
		logger = LogManager.getRootLogger();
		logger.debug("Setting debug on");
	}
	
	/**
	 * The parseCommand method constructs the Server Command that is to be sent from 
	 * the client based on the command-line arguments
	 * @return a Command object constructed based on the command-line arguments
	 */
	private Command parseCommand() {
		return new Command(clientArgs);
	}
	
<<<<<<< HEAD
=======
	/**
	 * 
	 */
	private void configLogger() {
		// Configure logger
		System.out.println(System.getProperty("user.dir"));
		if (this.clientArgs.hasOption(Constants.debugOption)) {
			System.setProperty("log4j.configurationFile", "../logging-config-debug.xml");
		} else {
			System.setProperty("log4j.configurationFile", "../logging-config-default.xml");
		}
		logger = LogManager.getRootLogger();
		logger.debug("Debugger enabled");
		this.serverInfo = this.parseServerInfo();
		logger.debug("Publishing to " + serverInfo);
	}

>>>>>>> Client_Refactor
	/**
	 * The parseServerInfo method constructs the destination Server's information
	 * based on the command-line arguments. 
	 * 
	 * @return a ServerInfo object, with host name and port specified by the 
	 * command-line argument. If no arguments are provided, the 
	 * default value is returned.
	 */
	private ServerInfo parseServerInfo() {
		return new ServerInfo(clientArgs.getSafeHost(), clientArgs.getSafePort());
	}
	
<<<<<<< HEAD
	/**
	 * The sendCommand methods sends a command using the bound socket
	 * @param command
	 */
	public void sendCommand(Command command) throws SocketTimeoutException, IOException{
		logger.info("Sending "+command.getCommand()+" command... ");
		
		DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
		outToServer.writeUTF(command.toJson());
		outToServer.flush();
		
		logger.info(command.getCommand()+" command sent. ");
		logger.debug("SENT: " + command.toJson());
		logger.info("Waiting for response...");
	}
	
	/**
	 * The getResponse method receives a response message from the bound socket.
	 * @return a Response object where the fields reflect the message received
	 */
	public Response getResponse(DataInputStream inputStream) throws SocketTimeoutException, IOException{
		String serverMessage = inputStream.readUTF();
		logger.debug("RECEIVED: " + serverMessage);
		
		//Should be a success or error response, parse it to check for sure
		Response response = new Response().fromJson(serverMessage);
		return response;
	}
	

	/**
	 * The processResponses method handles subsequent responses from the server, if any
	 * is expected for the given command (i.e. QUERY and FETCH)
	 * @param command
	 * 			command sent to the server
	 * @param first_response
	 * 			the first response received from the server
	 * @param inputStream
	 * 			the DataInputStream used by the socket
	 */
	public void processResponses(Command command, Response first_response, 
									DataInputStream inputStream) 
									throws SocketTimeoutException, 
											IOException{
		if(first_response.isSuccess()){ //no further responses expected if error
			switch (command.getCommand()) {
			case Command.QUERY_COMMAND:
				getQueryResponses(inputStream);
				break;
			case Command.FETCH_COMMAND:
				getFetchResponses(inputStream);
				break;
			default:
				// Other commands only expects one response from the server
				break;
			}
			logger.info("End of expected server responses.");
		}
	}
	
	/**
	 * The getQueryResponses method receives and logs all subsequent resource
	 * responses from the server as well as the final resultSize response
	 * @param inputStream
	 * 			the DataInputStream used by the socket
	 */
	public void getQueryResponses(DataInputStream inputStream) 
									throws SocketTimeoutException, IOException{
		while(true){
			String serverMessage = inputStream.readUTF();
			logger.debug("RECEIVED: " + serverMessage);
			
			// Check if response is a resource response or a resultsSize response
			Response response = new Response().fromJson(serverMessage);
			if(response.isResultSize()){
				//Done, no more responses expected
				return;
				
			} else {
				//Should be a resource, parse it to check for sure
				Resource resource = new Resource().fromJson(serverMessage);
				if(!resource.isValidResourceResponse()) {
					logger.error("Bad resource response from the server.");
					return;
				}
			}
		}
	}
	
	/**
	 * The getFetchResponses method receives and logs the resourceSize response, 
	 * the bytes of the resources and the final resultSize response.
	 * @param inputStream
	 * 			the DataInputStream used by the socket
	 */
	public void getFetchResponses(DataInputStream inputStream) 
									throws SocketTimeoutException, IOException{
		
		String resourceSizeMessage = inputStream.readUTF();
		logger.debug("RECEIVED: " + resourceSizeMessage);
		
		// Check if response is a resourceSize response and get the name & size
		Resource resource = new Resource().fromJson(resourceSizeMessage);
		Long resourceSize = resource.getResourceSize();
		String fileURI = resource.getURI();
		
		if(resourceSize == null || fileURI == null){
			logger.error("Bad resourceSize response from the server.");
			return;
			
		} else {
			//Replicate the file name for download
			File resourceFile = new File(fileURI);
			String fileName = resourceFile.getName();
			
			receiveFile(fileName, resourceSize, inputStream);
			getResponse(inputStream); //wait for the final resultSize response
		}
	}
	
	/**
	 * The receiveFile method receives a file from the inputStream and saves it
	 * in a file in the local directory. The method is adapted from the code
	 * example in Tutorial 7.
	 * 
	 * @param fileName
	 * @param fileSize
	 * @param inputStream
	 * 			the DataInputStream created using the client socket
	 */
	public void receiveFile(String fileName, Long fileSize, DataInputStream inputStream) 
							throws SocketTimeoutException, IOException {
		// Create a RandomAccessFile to read and write the output file.
		RandomAccessFile downloadingFile = new RandomAccessFile(fileName, "rw");
		
		long fileSizeRemaining = fileSize;
		int chunkSize = setChunkSize(fileSizeRemaining);
		
		// Represents the receiving buffer
		byte[] receiveBuffer = new byte[chunkSize];
		
		// Variable used to read if there are remaining size left to read.
		int num;
		
		logger.info("Downloading "+fileName+" of size "+fileSizeRemaining);
		while((num = inputStream.read(receiveBuffer))>0){
			// Write the received bytes into the RandomAccessFile
			downloadingFile.write(Arrays.copyOf(receiveBuffer, num));
			
			// Reduce the file size left to read
			fileSizeRemaining -= num;
			
			// Update ChunkSize
			chunkSize = setChunkSize(fileSizeRemaining);
			receiveBuffer = new byte[chunkSize];
			
			if(fileSizeRemaining==0){
				break; //Done
			}
		}
		logger.info("Download completed.");
		downloadingFile.close();
=======
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
	 * @throws IOException 
	 */
	private void processFetch(DataInputStream inFromServer) throws IOException {
		// TODO Auto-generated method stub
		Resource resource = new Resource().fromJson(inFromServer.readUTF());
		this.receiveFile("test.jpg", resource.resourceSize);
		this.logger.info(inFromServer.readUTF());
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
	 * It downloads a file from a socket InputStream and BUF_SIZE at a time and
	 * writes the output to a FileOutputStream.
	 * @param fileName the name of the file to write to disk
	 * @param fileSize the number of bytes the expected file is going to be
	 * @throws IOException
	 */
	private void receiveFile(String fileName, int fileSize) throws IOException {
		InputStream in = this.socket.getInputStream();
		FileOutputStream out = new FileOutputStream(fileName);
		byte[] bytes  = new byte [BUF_SIZE];		
		int count, totalRead = 0, bytesToRead = 0;
		// stop reading only when have read bytes equal to the fileSize
		while(totalRead < fileSize) {
			// determine how many more bytes to read
			bytesToRead = Math.min(bytes.length, fileSize-totalRead);
			// read bytesToRead from the InputStream
			count = in.read(bytes,0,bytesToRead);
			totalRead += count;
			// write bytes to file
			out.write(bytes,0,count);
			this.logger.debug("downloaded: " + count + " bytes, remaining: " + 
					(fileSize - totalRead) + " bytes");
		}
		out.close();
>>>>>>> Client_Refactor
	}

	public static int setChunkSize(long fileSizeRemaining){
		int chunkSize = 1024*1024;
		
		// If the file size remaining is less than the chunk size
		// then set the chunk size to be equal to the file size.
		if(fileSizeRemaining<chunkSize){
			chunkSize = (int) fileSizeRemaining;
		}
		
		return chunkSize;
	}

}
