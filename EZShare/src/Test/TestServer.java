package Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer {

	public static void main(String[] args) {
		try {
			ServerSocket serverSocket = new ServerSocket(3780);
			System.out.println("Waiting for client on port " + 
		               serverSocket.getLocalPort() + "...");
			Socket server = serverSocket.accept();
			System.out.println("Just connected to " + server.getRemoteSocketAddress());
			
			DataInputStream inFromClient = new DataInputStream(server.getInputStream());
			DataOutputStream outToClient = new DataOutputStream(server.getOutputStream());
			
			System.out.println(inFromClient.readUTF());
			// Thread.sleep(10000);
			
			server.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
