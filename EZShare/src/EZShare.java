import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import EZShare.Command;
import EZShare.Resource;

/**
 * todo 
 *
 * make command, resource -> sendable
 * command, resource will have validate self methods
 * create client/server
 * create ResponseManager class 
 * 
 */

public class EZShare {

	public static void main(String[] args) {
		
//		Command exchange = new Command();
//		
//		exchange.command = "EXCHANGE";
//		exchange.serverList = new ArrayList<ServerInfo>();
//		exchange.serverList.add(new ServerInfo("115.146.85.165",3780));
//		exchange.serverList.add(new ServerInfo("115.146.85.24",3781));
//		
//		Command copyExchange = new Command().fromJson(exchange.toJson());
//		
//		System.out.println(exchange.toJson());
//		System.out.println(copyExchange.toJsonPretty());
		
//		ConcurrentHashMap<String,Integer> map = new ConcurrentHashMap<>();
//		
//		Thread t1 = new Thread(new Method(map));
//		Thread t2 = new Thread(new Method(map));
//		Thread t3 = new Thread(new Method(map));
//		
//		t1.start();
//		t2.start();
//		t3.start();
//		
//		try {
//			Thread.sleep(1);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
//		try {
//			t1.join();
//			t2.join();
//			t3.join();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		System.out.println("size:" + map.size());
//		for(String key : map.keySet()) {			
//			System.out.println(map.get(key));
//		}
		
//		new Thread(() -> doSomething("hello steve")).start();
//		new Thread(() -> doSomething("hello bobby")).start();	
		
		// sunrise.cis.unimelb.edu.au:3780
		try {
			String host = "sunrise.cis.unimelb.edu.au";
			int port = 3780;
			
			Socket clientSocket = new Socket(host, 3780);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
			
			Command q = new Command();
			q.command = "QUERY";
			q.resourceTemplate = new Resource();
			q.resourceTemplate.name = "uni";
			
			outToServer.writeUTF(q.toJson());			
			
			String line;
			try {
				while((line = inFromServer.readUTF())!=null) {
					System.out.println(line);
				}
			} catch (IOException e) {
				
			}
			
			clientSocket.close();
			
			System.exit(1);
			
			Thread.sleep(1000);
			
			clientSocket = new Socket(host,port);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new DataInputStream(clientSocket.getInputStream());
			
			q = new Command();
			q.command = "FETCH";
			q.resourceTemplate = new Resource();
			q.resourceTemplate.channel = "";
			
			try {
				q.resourceTemplate.uri = new URI("http://www.unimelb.edu.au").toString();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println(q.toJson());
			outToServer.writeUTF(q.toJson());
			
			try {
				while((line = inFromServer.readUTF())!=null) {
					System.out.println(line);
				}
			} catch (IOException e) {
				
			}
						
			clientSocket.close();			
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
	}
	
	public static void doSomething(String str) {
		System.out.println(str);
	}

}
