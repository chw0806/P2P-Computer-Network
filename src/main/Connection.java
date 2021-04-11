package main;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.ServerSocket;

import file.BlockReaderWriter;
import message.ActualMessage;

public class Connection implements Runnable {
	
	private static final int SMALL_INTERVAL = 100;
	private static final int END_INTERVAL = 10000;
	
	Socket connectionSocket;           				//socket created by this connection
	
	OutputStream out;         				//stream write to the socket
 	InputStream in;          					//stream read from the socket
	
	volatile PeerInfo thisInfo;						//The peer information of this side
	volatile PeerInfo peerInfo;						//The peer information of the other side
	
	volatile BlockReaderWriter readerWriter;
	volatile TransmissionStatus connectionStatus;
	
	Client client;									//
	Server server;									//
	
	volatile List<Connection> connectionList;		//Save the info of other connection
	
	volatile Map<Integer, Integer> downloadMap;		//Updated in client thread when receiving piece message
	volatile Map<Integer, Long> startTimeMap;		//Set by each client thread
	volatile Set<Integer> interestedSet;			//Updated in server thread when receiving interest message
	volatile Set<Integer> inFlightSet;
	volatile NeighborManager neighborManager;	//

	public Connection(peerProcess peer, PeerInfo info) {
		this.thisInfo = peer.info;
		this.peerInfo = info;
		this.readerWriter = peer.blockReaderWriter;
		this.connectionStatus = peer.peerStatus;
		this.connectionList = peer.connectionList;
		this.downloadMap = peer.downloadMap;
		this.startTimeMap = peer.startTimeMap;
		this.interestedSet = peer.interestedSet;
		this.inFlightSet = peer.inFlightSet;
		this.neighborManager = peer.neighborManager;
	}
	
	public void initialize() throws UnknownHostException, IOException {
		if(!thisInfo.initBefore(peerInfo)) {
			//Create the socket if the other side is initialized earlier
			connectionSocket = new Socket(peerInfo.hostName, peerInfo.listeningPort);
		}else {
			//Waiting for connection
			ServerSocket listener = new ServerSocket(thisInfo.listeningPort);
			connectionSocket = listener.accept();
			listener.close();
		}
		//initialize inputStream and outputStream
		out = connectionSocket.getOutputStream();
		out.flush();
		in = connectionSocket.getInputStream();
	}
	
	private void handshake() throws IOException, ClassNotFoundException, InterruptedException {
		if(thisInfo.initBefore(peerInfo)) {
			server.initialize();
		}else {
			client.initialize();
		}
	}
	
	@Override
	public void run() {
		try {
			System.out.println("Connection to peer " + peerInfo.peerID + " is running");
			//Start a client thread to send request to the other side
			client = new Client(this);
			//Start a server thread to handle request from the other side
			server = new Server(this);
			
			//Exchange handshake message
			//In this process, the side initialized first will also send a bit field message to the other side
			handshake();
			
			new Thread(client).start();
			new Thread(server).start();
			Thread.sleep(3000);
			//Handle actual message followed
			while(!connectionStatus.checkCompleted()) {
				ActualMessage actualMessage = new ActualMessage();
				boolean completed = false;
				while (in.available() == 0) {
					if (connectionStatus.checkCompleted()) {
						completed = true;
						Thread.sleep(END_INTERVAL);
						break;
					}
					Thread.sleep(SMALL_INTERVAL);
				}
				
				if (completed) {
					break;
				}
					
				actualMessage.readActualMessage(in);

//				System.out.println("Get a " + actualMessage.messageType.toString() + " message from " + peerInfo.peerID);
				switch(actualMessage.messageType) {
					case bitfield: {
						//Receiving bit field message, send it to the client to handle it
						client.handleBitFieldMessage(actualMessage);
						System.out.println("Get a " + actualMessage.messageType.toString() + " message from " + peerInfo.peerID);
					}
					break;
					
					case choke: {
						//Receiving choke message, send it to the client to handle it
						client.handleChokeMessage(actualMessage, true);
						System.out.println("Get a " + actualMessage.messageType.toString() + " message from " + peerInfo.peerID);
					}
					break;
					
					case have: {
						//Receiving have message, send it to the client to handle it
						client.handleHaveMessage(actualMessage);
						
					}
					break;
					
					case interested: {
						//Receiving interested message, send it to the server to handle it
						server.handleInterestedMessage(true);
						System.out.println("Get a " + actualMessage.messageType.toString() + " message from " + peerInfo.peerID);
					}
					break;
					
					case not_interested: {
						//Receiving not interested message, send it to the server to handle it
						server.handleInterestedMessage(false);
						System.out.println("Get a " + actualMessage.messageType.toString() + " message from " + peerInfo.peerID);
					}
					break;
					
					case piece: {
						//Receiving piece message, send it to the client to handle it
						//The client will set the bit field and update the file
						int getPiece = client.handlePieceMessage(actualMessage);
						//All the servers in this peer will send a have message to the other side
						if(getPiece != -1) {
							for(Connection connection : connectionList) {
								connection.server.handlePieceMessage(getPiece);
							}
						}
					}
					break;
					
					case request: {
						//Receiving request message, send it to the server to handle it
						server.handleRequestMessage(actualMessage);
					}
					break;
					
					case unchoke: {
						//Receiving unchoke message, send it to the client to handle it
						client.handleChokeMessage(actualMessage, false);
						System.out.println("Get a " + actualMessage.messageType.toString() + " message from " + peerInfo.peerID);
					}
					break;
					
					default: {
						//Receiving wrong message, skip it
					}
					break;
				}
			}
			
		}
		catch (ConnectException e) {
    		System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch (ClassNotFoundException e ) {
        	System.err.println("Class not found");
    	} 
		catch(UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally {
			//Close connections
			try {
				in.close();
				out.close();
				connectionSocket.close();
			}
			catch(IOException ioException) {
				ioException.printStackTrace();
			}
		}
		System.out.println("Connection end!");
	}
	
}
