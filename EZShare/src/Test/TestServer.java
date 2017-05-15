package Test;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer {

	public static void main(String[] args) throws IOException {	    
		try {
			ServerSocket servsock = new ServerSocket(3780);
			
			Socket sock = servsock.accept();
			DataOutputStream outToClient = new DataOutputStream(sock.getOutputStream());
			System.out.println("Accepted connection : " + sock);
			
			File myFile = new File ("tree.png");
			// send the file size
			outToClient.writeUTF(Long.toString(myFile.length()));
			
			sendFile(myFile,sock);
			System.out.println("Done.");
			
//			byte [] mybytearray  = new byte [(int)myFile.length()];
//			FileInputStream fis = new FileInputStream(myFile);
//			BufferedInputStream bis = new BufferedInputStream(fis);
//			bis.read(mybytearray,0,mybytearray.length);
//			
//			OutputStream os = sock.getOutputStream();
//			os.write(mybytearray,0,mybytearray.length);
//			os.flush();
			
			servsock.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void sendFile(File file, Socket socket) throws IOException {
		// define an array as the length of the file
		byte[] bytes  = new byte [(int)file.length()];
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);
		// read the file into the bytes array
		bis.read(bytes);
		// define an output stream
		OutputStream os = socket.getOutputStream();
		// write the bytes array onto the stream
		os.write(bytes);
		os.flush();
		// close bis and os - no longer needed
		bis.close();
		os.close();
	}

}
