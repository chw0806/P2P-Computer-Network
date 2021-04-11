package main;

import file.BlockReaderWriter;
import java.util.List;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public class peerProcess {
	
	volatile ReadData data;
	volatile PeerInfo info;
	volatile BlockReaderWriter blockReaderWriter;
	volatile TransmissionStatus peerStatus;
	volatile NeighborManager neighborManager;
	
	volatile List<Connection> connectionList;
	volatile Map<Integer, Integer> downloadMap;
	volatile Map<Integer, Long> startTimeMap;
	volatile Set<Integer> interestedSet;
	volatile Set<Integer> inFlightSet;
	volatile Set<Integer> hasFileSet;
	
	volatile OptimisticallyUnchokedNeighbor optimisticallyUnchokedNeighbor;
	volatile PreferredNeighbors preferredNeighbors;
	
	public peerProcess(int peerIndex) {
		data = new ReadData(peerIndex);
		info = data.myInfo;
		blockReaderWriter = new BlockReaderWriter("src/" + info.peerID + "/" + ReadData.fileName, info.hasFile);
		peerStatus = new TransmissionStatus(info);
		neighborManager = new NeighborManager();
	}
	
	public void init() {
		connectionList = new ArrayList<Connection>();
		downloadMap = new HashMap<Integer, Integer>();
		startTimeMap = new HashMap<Integer, Long>();
		interestedSet = new HashSet<Integer>();
		inFlightSet = new HashSet<Integer>();
		hasFileSet = new HashSet<Integer>();
		
		for(PeerInfo otherInfo : data.peerList) {
			if (otherInfo.hasFile) {
				hasFileSet.add(otherInfo.peerID);
			}
			if(!downloadMap.containsKey(otherInfo.peerID)) {
				downloadMap.put(otherInfo.peerID, 0);
			}
			
			if(otherInfo != info) {
				Connection con = new Connection(this, otherInfo);
				try {
					con.initialize();
				}
				catch (UnknownHostException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				connectionList.add(con);
//				downloadMap.put(otherInfo.peerID, 0);
				new Thread(con).start();
			}
		}
		
		//When all the connections are running, start checking preferred neighbors and optimistically unchoked neighbor
		optimisticallyUnchokedNeighbor = new OptimisticallyUnchokedNeighbor(this);
		preferredNeighbors = new PreferredNeighbors(this);
		
		new Thread(optimisticallyUnchokedNeighbor).start();
		new Thread(preferredNeighbors).start();
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		int peerID = 1001;
		peerProcess peer = new peerProcess(peerID);
		peer.init();
	}
	
}
