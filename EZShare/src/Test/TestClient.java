package Test;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class TestClient {

	public static void main(String[] args) throws IOException {
		try {
			System.out.println("Connecting...");
		    Socket sock = new Socket("127.0.0.1", 3780);
		    DataInputStream inFromServer = new DataInputStream(sock.getInputStream());
		    int fileSize = Integer.parseInt(inFromServer.readUTF());
		    System.out.println("expecting fileSize: " + fileSize);
		    receiveFile("new.png",fileSize,sock);
		    System.out.println("finished downloading");
		    sock.close();		    
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void receiveFile(String fileName, int fileSize, Socket socket) throws IOException {
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
