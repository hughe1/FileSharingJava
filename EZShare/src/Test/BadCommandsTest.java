package Test;

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

import EZShare.Resource;
import EZShare.Response;


public class BadCommandsTest {

	private static Logger logger;
	private static String host = "130.56.251.227";
	private static int port = 2222;
	private static Socket socket;

	public static void main(String[] args) {
		
		String command = "QUERY";
		//String bad_command = "{\"command\":\"PUBLISH\",\"resource\":{\"name\":\"reddit\",\"tags\":[],\"description\":\"\",\"uri\":\"http://www.reddit.com\",\"channel\":\"\",\"owner\":\"\",\"ezserver\":\"\"}}";
		//String bad_command = "{\"command\":\"EXCHANGE\",\"serverList\":[{\"hostname\":\"115.146.85.165\",\"port\":\"not_number\"}]}";

		String bad_command = "{\"command\":\"SUBSCRIBE\",\"id\":\"test\",\"relay\":true,\"resourceTemplate\":{\"name\":\"\",\"tags\":[],\"description\":\"\",\"uri\":\"\",\"channel\":\"\",\"owner\":\"*\",\"ezserver\":\"\"}}";
	
		
		System.out.println(bad_command);
		
		System.setProperty("log4j.configurationFile", "logging-config-debug.xml");
		logger = LogManager.getRootLogger();
		logger.info("Connecting to host " + host + " at port " + port);
		
		try {
			socket = new Socket(host, port);
			DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
			submitCommand(bad_command, outToServer);

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
				onSuccess(command,inFromServer);
			}
			else {
				// something went wrong
				logger.error("Something went wrong when receiving reponse from Server");
			}
			socket.close();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void submitCommand(String command, DataOutputStream outToServer) 
			throws IOException {
		outToServer.writeUTF(command);
		outToServer.flush();
		logger.debug("SENT: " + command);
	}
	
	private static void onSuccess(String command, DataInputStream inFromServer) throws 
	SocketTimeoutException, IOException {
	// publish, remove, share, exchange -> only print the response
	// query, fetch -> have to deal with these dynamically
	switch (command) {
		case "QUERY":
			processQuery(inFromServer);
			break;
		case "FETCH":
			processFetch(inFromServer);
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
	private static void processFetch(DataInputStream inFromServer) throws IOException {
		String resourceString = inFromServer.readUTF();
		logger.debug("RECEIVED: " + resourceString);
		Resource resource = new Resource().fromJson(resourceString);
		
		// Get file name
		String[] strings = resource.getURI().split("/");
		String fileName = strings[strings.length - 1];
		
		receiveFile(fileName, resource.getResourceSize());
		logger.debug("RECEIVED: " + inFromServer.readUTF());
	}
	
	/**
	 * Responsible for processing a query command. By the time this method is
	 * called, the client has already called readUTF once. This method should
	 * process anything after the initial JSON object from the server.
	 * 
	 * @param inFromServer
	 * @throws SocketTimeoutException, IOException 
	 */
	private static void processQuery(DataInputStream inFromServer) throws 
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
	private static void receiveFile(String fileName, long fileSize) throws IOException {
		logger.info("Downloading " + fileName);
		InputStream in = socket.getInputStream();
		FileOutputStream out = new FileOutputStream(fileName);
		byte[] bytes  = new byte [1024 * 4];		
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
			logger.debug("Downloaded: " + count + " bytes, remaining: " + 
					(fileSize - totalRead) + " bytes");
		}
		out.close();

		File file = new File(fileName);
		logger.info("Download complete!");
		logger.info("Your file is located at " + file.getAbsolutePath());
	}
}
