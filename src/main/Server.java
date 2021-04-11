package main;

import message.HandshakeMessage;
import message.ActualMessage;
import message.ActualMessage.MessageType;
import message.MessagePayload;

import file.BlockReaderWriter;

import java.net.*;
import java.util.List;
import java.util.Set;
import java.io.*;


public class Server implements Runnable {
	
	public static final int INT_SIZE = 4;
	public static final int MESSAGE_TYPE_SIZE = 1;
	public static final int DEFAULT_SLEEP_TIME = 1000;

	Socket responseSocket;           //socket connect to the server
	
	OutputStream out;         //stream write to the socket
 	InputStream in;           //stream read from the socket
	
	volatile PeerInfo thisInfo;		//Info of this side
	volatile PeerInfo clientInfo;	//Info of the other side
	volatile BlockReaderWriter serverReaderWriter;
	volatile TransmissionStatus connectionStatus;
	
	volatile List<Connection> connectionList;
	
	volatile Set<Integer> interestedSet;
	volatile NeighborManager unchokedNeighbors;
	
	public Server(Connection connection) {
		this.thisInfo = connection.thisInfo;
		this.clientInfo = connection.peerInfo;
		this.serverReaderWriter = connection.readerWriter;
		this.connectionStatus = connection.connectionStatus;
		this.connectionList = connection.connectionList;
		this.interestedSet = connection.interestedSet;
		this.unchokedNeighbors = connection.neighborManager;
		this.responseSocket = connection.connectionSocket;
		this.in = connection.in;
		this.out = connection.out;
	}
	
	@Override
	public void run() {
		try {
			System.out.println("Server of peer " + thisInfo.peerID + " is running");
			while(!connectionStatus.checkCompleted()) {
				Thread.sleep(DEFAULT_SLEEP_TIME);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void initialize() throws ClassNotFoundException, IOException, InterruptedException {
		(new Log(thisInfo.peerID)).BeingConnectedLog(thisInfo.peerID, clientInfo.peerID);
		
		System.out.println("Server of peer " + thisInfo.peerID + " is being connected");
		exchangeHandshakeMessage();
		System.out.println("The connection between server of peer " + thisInfo.peerID 
				+ " and client of peer " + clientInfo.peerID + " has been initialized");
	}
	
	private void exchangeHandshakeMessage() throws IOException, ClassNotFoundException, InterruptedException {
		HandshakeMessage secondHandshake = new HandshakeMessage();
		secondHandshake.readHandshakeMessage(in);
		System.out.println("Get a handshake message from peer " + clientInfo.peerID);
		HandshakeMessage firstHandshake = new HandshakeMessage(thisInfo.peerID);
		firstHandshake.writeHandshakeMessage(out);
		System.out.println("Send a handshake message to peer " + clientInfo.peerID);
	}
	
	public void handleInterestedMessage(boolean interested) throws IOException {
		if(interested){
			if(!interestedSet.contains(clientInfo.peerID)) {
				synchronized(interestedSet) {
					interestedSet.add(clientInfo.peerID);
				}
			}
			(new Log(thisInfo.peerID)).ReceiveInterestedMessageLog(thisInfo.peerID, clientInfo.peerID);
		}else{
			if(interestedSet.contains(clientInfo.peerID)) {
				synchronized(interestedSet) {
					interestedSet.remove(clientInfo.peerID);
				}
			}
			if(unchokedNeighbors.optimisticallyUnchokedNeighbor == clientInfo.peerID) {
				synchronized(unchokedNeighbors) {
					unchokedNeighbors.optimisticallyUnchokedNeighbor = -1;
				}
			}else if(unchokedNeighbors.preferredNeighborsSet.contains(clientInfo.peerID)) {
				synchronized(unchokedNeighbors) {
					unchokedNeighbors.preferredNeighborsSet.remove(clientInfo.peerID);
				}
			}
			(new Log(thisInfo.peerID)).ReceiveNotInterestedMessageLog(thisInfo.peerID, clientInfo.peerID);
		}
	}
	
	public void handlePieceMessage(int pieceIndex) throws IOException {
		sendHaveMessage(pieceIndex);
	}
	
	private void sendHaveMessage(int pieceIndex) throws IOException {
		MessagePayload havePayload = new MessagePayload(pieceIndex);
		ActualMessage haveMessage = new ActualMessage(INT_SIZE*2 + MESSAGE_TYPE_SIZE, MessageType.have, havePayload);
		haveMessage.writeActualMessage(out);
	}
	
	public void handleRequestMessage(ActualMessage requestMessage) throws IOException {
		byte[] content;
		int pieceIndex = requestMessage.payload.pieceIndex;
		synchronized(serverReaderWriter) {
			content = serverReaderWriter.getPiece(pieceIndex);
		}
		if(content != null) {
			sendPieceMessage(pieceIndex, content);
		}
	}
	
	private void sendPieceMessage(int pieceIndex, byte[] content) throws IOException {
		MessagePayload piecePayload = new MessagePayload(pieceIndex, content);
		ActualMessage pieceMessage = new ActualMessage(INT_SIZE*2 + MESSAGE_TYPE_SIZE + content.length, MessageType.piece, piecePayload);
		pieceMessage.writeActualMessage(out);
	}
	
	public void sendChokeMessage(boolean choked) throws IOException {
		if(choked){
			ActualMessage pieceMessage = new ActualMessage(INT_SIZE + MESSAGE_TYPE_SIZE, MessageType.choke);
			pieceMessage.writeActualMessage(out);
			System.out.println("Send a choke message to peer " + clientInfo.peerID);
		}else{
			ActualMessage pieceMessage = new ActualMessage(INT_SIZE + MESSAGE_TYPE_SIZE, MessageType.unchoke);
			pieceMessage.writeActualMessage(out);
			System.out.println("Send a unchoke message to peer " + clientInfo.peerID);
		}
	}
}