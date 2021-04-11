package main;

import message.HandshakeMessage;
import message.ActualMessage;
import message.ActualMessage.MessageType;
import message.MessagePayload;

import main.PeerInfo;
import file.BlockReaderWriter;

import java.net.*;
import java.util.Map;
import java.util.Set;
import java.io.*;

public class Client implements Runnable {
	
	public static enum ClientStatus {
		not_initialized,
		interested,
		not_interested,
		choked,
		unchoked,
		completed
	}
	
	public static final int INT_SIZE = 4;
	public static final int MESSAGE_TYPE_SIZE = 1;
	public static final int DEFAULT_SLEEP_TIME = 1000;
	
	private static int pieceGetNum = 0;

	Socket requestSocket;           //socket connect to the server
	
	OutputStream out;         //stream write to the socket
 	InputStream in;           //stream read from the socket
	
	volatile PeerInfo thisInfo;		//Info of this side
	volatile PeerInfo serverInfo;	//Info of the other side
	volatile BlockReaderWriter clientReaderWriter;
	volatile TransmissionStatus connectionStatus;
	
	volatile Map<Integer, Integer> downloadMap;
	volatile Map<Integer, Long> startTimeMap;
	volatile Set<Integer> inFlightSet;
	
	private ClientStatus status;
	
	public Client(Connection connection) {
		this.thisInfo = connection.thisInfo;
		this.serverInfo = connection.peerInfo;
		this.clientReaderWriter = connection.readerWriter;
		this.connectionStatus = connection.connectionStatus;
		this.requestSocket = connection.connectionSocket;
		this.downloadMap = connection.downloadMap;
		this.startTimeMap = connection.startTimeMap;
		this.inFlightSet = connection.inFlightSet;
		this.in = connection.in;
		this.out = connection.out;
		this.status = ClientStatus.not_initialized;
	}
	
	@Override
	public void run() {
		System.out.println("Client of peer " + serverInfo.peerID + " is running");
		long currentTime = System.currentTimeMillis();
		startTimeMap.put(serverInfo.peerID, currentTime);
		try {
			while(!connectionStatus.getDownloadCompleted()) {
				Thread.sleep(DEFAULT_SLEEP_TIME);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void initialize() throws IOException, ClassNotFoundException, InterruptedException {
		(new Log(thisInfo.peerID)).ConnectionLog(thisInfo.peerID, serverInfo.peerID);
		
		System.out.println("Client of peer " + thisInfo.peerID + " connects " + serverInfo.peerID);
		exchangeHandshakeMessage();
		sendBitfieldMessage();
		System.out.println("The connection between server of peer " + thisInfo.peerID 
				+ " and client of peer " + serverInfo.peerID + " has been initialized");
	}
	
	private void exchangeHandshakeMessage() throws IOException, InterruptedException {
		HandshakeMessage firstHandshake = new HandshakeMessage(thisInfo.peerID);
		firstHandshake.writeHandshakeMessage(out);
		System.out.println("Send a handshake message to peer " + serverInfo.peerID);
		HandshakeMessage secondHandshake = new HandshakeMessage();
		secondHandshake.readHandshakeMessage(in);
		System.out.println("Get a handshake message from peer " + serverInfo.peerID);
	}
	
	private void sendBitfieldMessage() throws IOException {
		//Send bit field message to the server
		MessagePayload clientPayLoad = new MessagePayload(thisInfo.bitField);
		ActualMessage clientBitFieldMessage = new ActualMessage(INT_SIZE + MESSAGE_TYPE_SIZE + 
				clientPayLoad.bitField.bitFieldArray.length, MessageType.bitfield, clientPayLoad);
		clientBitFieldMessage.writeActualMessage(out);
		System.out.println("Send a bit field message to peer " + serverInfo.peerID);
	}
	
	public void handleBitFieldMessage(ActualMessage bitfieldMessage) throws ClassNotFoundException, IOException {
		serverInfo.setBitField(bitfieldMessage.payload.bitField);
		if(thisInfo.initBefore(serverInfo)) {
			sendBitfieldMessage();
		}
		boolean interested = thisInfo.checkInterested(serverInfo);
		status = interested ? ClientStatus.interested : ClientStatus.not_interested;
		sendInterestedMessage(interested);
	}
	
	private void sendInterestedMessage(boolean interested) throws IOException {
		ActualMessage clientInterestedMessage = new ActualMessage(INT_SIZE + MESSAGE_TYPE_SIZE,
				interested ? MessageType.interested : MessageType.not_interested);
		clientInterestedMessage.writeActualMessage(out);
		System.out.println("Send a " + (interested ? "interested" : "not interested") + " message to peer " + serverInfo.peerID);
		if(!interested) {
			status = ClientStatus.not_interested;
			checkCompleted();
		}
	}
	
	public void handleHaveMessage(ActualMessage haveMessage) throws IOException {
		(new Log(thisInfo.peerID)).ReceiveHaveMessageLog(thisInfo.peerID, serverInfo.peerID, haveMessage.payload.pieceIndex);
		serverInfo.bitField.updateBitField(haveMessage.payload.pieceIndex);
		synchronized(downloadMap) {
			downloadMap.replace(serverInfo.peerID, downloadMap.get(serverInfo.peerID), downloadMap.get(serverInfo.peerID) + 1);
		}
		if(status == ClientStatus.not_interested) {
			boolean interested = thisInfo.checkInterested(serverInfo);
			if(interested) {
				sendInterestedMessage(true);
			}
		}
	}
	
	public void handleChokeMessage(ActualMessage chokeMessage, boolean choked) throws IOException, InterruptedException {
		if(choked){
			if(!thisInfo.checkInterested(serverInfo)) {
				sendInterestedMessage(false);
				return;
			}
			status = ClientStatus.choked;
			(new Log(thisInfo.peerID)).ChokingLog(thisInfo.peerID, serverInfo.peerID);
		}else{
			status = ClientStatus.unchoked;
			sendRequestMessage();
			(new Log(thisInfo.peerID)).UnchokingLog(thisInfo.peerID, serverInfo.peerID);
		}
	}
	
	private void sendRequestMessage() throws IOException {
		if(!thisInfo.checkInterested(serverInfo, inFlightSet)) {
			sendInterestedMessage(false);
			return;
		}
		int piece = thisInfo.bitField.askForRequest(serverInfo.bitField);
		while (inFlightSet.contains(piece)) {
			piece = thisInfo.bitField.askForRequest(serverInfo.bitField);
		}
		MessagePayload payload = new MessagePayload(piece);
		ActualMessage requestMessage = new ActualMessage(2*INT_SIZE + MESSAGE_TYPE_SIZE, MessageType.request, payload);
		requestMessage.writeActualMessage(out);

		synchronized (inFlightSet) {
			inFlightSet.add(piece);
		}
	}
	
	public int handlePieceMessage(ActualMessage pieceMessage) throws IOException, InterruptedException {
		int getPiece;
		synchronized(clientReaderWriter) {
			getPiece = clientReaderWriter.insertPiece(pieceMessage.payload.pieceIndex, pieceMessage.payload.content);
		}
		if(getPiece != -1) {
			synchronized (inFlightSet) {
				//inFlightSet.remove(pieceNum);
				inFlightSet.remove(getPiece);
			}
			thisInfo.updateBitField(pieceMessage.payload.pieceIndex);
			if (thisInfo.bitField.checkCompleted()) {
				connectionStatus.setDownloadCompleted();
			}
		}
		boolean interested = thisInfo.checkInterested(serverInfo, inFlightSet);
		if(interested) {
			sendRequestMessage();
		}else {
			sendInterestedMessage(false);
		}
		
		(new Log(thisInfo.peerID)).DownloadPieceLog(thisInfo.peerID, serverInfo.peerID, 
				pieceMessage.payload.pieceIndex, ++pieceGetNum);
		return getPiece;
	}

	private void checkCompleted() throws IOException {
		if(thisInfo.bitField.checkCompleted()) {
			status = ClientStatus.completed;
			connectionStatus.setDownloadCompleted();
			(new Log(thisInfo.peerID)).CompletionLog(thisInfo.peerID);
		}
	}
}