package main;

import java.util.Date;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class Log {

	String LogFileName;

	public Log(int peerID) {
		this.LogFileName = "log_peer_" + peerID + ".log";
	}

	public String GetTime() {
		SimpleDateFormat DateFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return DateFormat.format(new Date());
	}

	// TCP connection
	public void ConnectionLog(int PeerID, int ServerPeerID) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" connects to Peer " + ServerPeerID + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	public void BeingConnectedLog(int PeerID, int ClientPeerID) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" is connected from Peer " + ClientPeerID + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	// Change of preferred neighbors
	public void PrefeerredNeighborsLog(int PeerID, List<Integer> PreferredList) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" has the preferred neighbors: ";
		for (Integer id : PreferredList)
			s = s + "Peer " + id + ", ";
		s = s.substring(0, s.length() - 1);
		s = s + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	// Change of optimistically unchoked neighbor.
	public void OptimisticallyUnchokedNeighborLog(int PeerID, int UnchokedID) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" has the optimistically unchoked neighbor: Peer " + UnchokedID + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	// Unchoking
	public void UnchokingLog(int PeerID, int PeerID2) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" is unchoked by Peer " + PeerID2 + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	// Choking
	public void ChokingLog(int PeerID, int PeerID2) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" is choked by Peer " + PeerID2 + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	// Receiving have message
	public void ReceiveHaveMessageLog(int PeerID, int PeerID2, int PieceIndex) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" received the 'have' message from Peer " + PeerID2 + " for the piece [" + PieceIndex + "].\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	// Receiving interested message
	public void ReceiveInterestedMessageLog(int PeerID, int PeerID2) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" received the 'interested' message from Peer " + PeerID2 + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	// Receiving not interested message
	public void ReceiveNotInterestedMessageLog(int PeerID, int PeerID2) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" received the 'not interested' message from Peer " + PeerID2 + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}

	// Downloading a piece
	public void DownloadPieceLog(int PeerID, int PeerID2, int PieceIndex, int PieceNum) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID +
				" has downloaded the piece " + PieceIndex + " from " + PeerID2 + ".\n";
		String s_ = "[" + GetTime() + "] Now the number of pieces it has is " + PieceNum + ".\n";

		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.write(s_);
		writer.close();
		return;
	}

	// Completion of download
	public void CompletionLog(int PeerID) throws IOException {
		String s = "[" + GetTime() + "] Peer " + PeerID + " has downloaded the complete file.\n";
		FileWriter writer = new FileWriter(LogFileName, true);
		writer.write(s);
		writer.close();
		return;
	}
}
