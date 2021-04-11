package main;

import java.util.Set;

import message.BitField;

public class PeerInfo {

	public int peerID;
	public String hostName;
	public int listeningPort;
	public boolean hasFile;
	public volatile BitField bitField;
	public int initSeq;
	
	public boolean initBefore(PeerInfo otherSide) {
		return initSeq < otherSide.initSeq;
	}
	
	public void setBitField(BitField bitField) {
		synchronized(this.bitField) {
			this.bitField = bitField;
		}
	}
	
	public void updateBitField(int pieceIndex) {
		synchronized(this.bitField) {
			this.bitField.updateBitField(pieceIndex);
		}
	}
	
	public boolean checkInterested(PeerInfo otherSide) {
		boolean interested;
		synchronized(bitField) {
			interested = bitField.checkIfInterested(otherSide.bitField);;
		}
		return interested;
	}
	
	public boolean checkInterested(PeerInfo otherSide, Set<Integer> inFlightSet) {
		boolean interested;
		synchronized(bitField) {
			interested = bitField.checkIfInterested(otherSide.bitField, inFlightSet);
		}
		return interested;
	}
	
	@Override
	public String toString() {
		String peerInfoString = "peerID = " + peerID + "\n"
								+ "hostName = " + hostName + "\n"
								+ "listeningPort = " + listeningPort + "\n"
								+ "bitField = " + bitField.toString() + "\n"
								+ "initSeq = " + initSeq + "\n";
		return peerInfoString;
	}
	
}
