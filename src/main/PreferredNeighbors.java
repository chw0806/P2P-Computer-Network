package main;

import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;

public class PreferredNeighbors extends UnchokeNeighbour implements Runnable {
	
	private static int unchokedSelectInterval;
	private static int pieceNum;

	volatile Map<Integer, Integer> downloadMap;
	volatile Map<Integer, Long> startTimeMap;
	volatile Set<Integer> hasFileSet;
	
	PreferredNeighbors(peerProcess peer) {
		super(peer);
		unchokedSelectInterval = ReadData.unchokingInterval * 1000;
		pieceNum = (int)Math.ceil(((double) ReadData.fileSize / (double) ReadData.pieceSize));
		this.downloadMap = peer.downloadMap;
		this.startTimeMap = peer.startTimeMap;
		this.hasFileSet = peer.hasFileSet;
	}
	
	// Test the current download speed.
	private List<Integer> testDownloadSpeed() {
		long currentTime = System.currentTimeMillis();
		TreeMap<Double, Integer> speedMap = new TreeMap<Double, Integer>(Collections.reverseOrder());
		List<Integer> highRateList = new ArrayList<>();
		
		System.out.print("Speed map: ");
		for (Integer selectPeer : interestedSet) {
			double downloadSpeed;
			if (downloadMap.containsKey(selectPeer) && startTimeMap.containsKey(selectPeer)){
				downloadSpeed = (downloadMap.get(selectPeer) * 1000) * 1.0 / (currentTime - startTimeMap.get(selectPeer));
			}else{
				downloadSpeed = 0;
			}
			speedMap.put(downloadSpeed, selectPeer);
			System.out.print("Current speed of " + selectPeer + " is: " + downloadSpeed);
		}
		int count = 0;
		for (Integer id : speedMap.values()) {
			if (count == ReadData.numberOfPreferredNeighbors + 1)
				break;
			highRateList.add(id);
			count++;
		}
		
		if (highRateList.isEmpty()) 
			return highRateList;
		
		Integer key = neighborManager.optimisticallyUnchokedNeighbor;
		if (highRateList.contains(key)) {
			highRateList.remove(key);
		}
		if(highRateList.size() == ReadData.numberOfPreferredNeighbors + 1){
			highRateList.remove(highRateList.size() - 1);
		}
		
		return highRateList;
	}
	
	private void handlePreferredNeighbor() throws IOException {
		System.out.println("Start selecting the preferred neighbors");
		List<Integer> highRateList = testDownloadSpeed();
		System.out.println("Finish the selection and four selected neighbors are below: ");

		synchronized (neighborManager.preferredNeighborsSet) {
			for (Integer key : highRateList) {
				neighborManager.preferredNeighborsSet.add(key);
				System.out.println(key);
			}
			
			(new Log(thisInfo.peerID)).PrefeerredNeighborsLog(thisInfo.peerID, highRateList);
			
			for (Connection connection : connectionList) {
				if (neighborManager.preferredNeighborsSet.contains(connection.peerInfo.peerID)) {
					connection.server.sendChokeMessage(false);
				}
			}
		}
	}
	
	private void clearPreferredNeighbors() throws IOException {
		synchronized (neighborManager.preferredNeighborsSet) {
			for (Connection connection : connectionList) {
				if (neighborManager.preferredNeighborsSet.contains(connection.peerInfo.peerID)) {
					connection.server.sendChokeMessage(true);
				}
			}
			neighborManager.preferredNeighborsSet.clear();
		}
	}
	
	private boolean checkAllPeersDownload() {
		Set<Integer> keySet = downloadMap.keySet();
		for(Integer selectPeer : keySet) {
			int peerPieceNum = downloadMap.get(selectPeer);
			if(peerPieceNum != pieceNum) {
				if(hasFileSet.contains(selectPeer)) {
					continue;
				}
				if (selectPeer == thisInfo.peerID) {
					continue;
				}
				return false;
			}
		}
		if (!peerStatus.getDownloadCompleted()) {
			return false;
		}
		return true;
	}
	
	@Override
	public void run() {
		try {
			while (!peerStatus.checkCompleted()) {
				handlePreferredNeighbor();
				Thread.sleep(unchokedSelectInterval);
				clearPreferredNeighbors();
				if(checkAllPeersDownload()) {
					synchronized(peerStatus) {
						peerStatus.setUploadCompleted();
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("The procedure of PreferredNeighbors ends!");
	}

}