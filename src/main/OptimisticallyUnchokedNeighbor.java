package main;

import java.io.IOException;

public class OptimisticallyUnchokedNeighbor extends UnchokeNeighbour implements Runnable {

	private static int randomChooseInterval;
	
	OptimisticallyUnchokedNeighbor(peerProcess peer){
		super(peer);
		randomChooseInterval = ReadData.optimisticUnchokingInterval * 1000;
	}
	
	@Override
	public void run() {
		try {
			//Wait for all the connection is set up
			Thread.sleep(randomChooseInterval);
			while(!peerStatus.checkCompleted()) {
				int selectPeer = getOptNeighbor();
				Connection connection = null;
				for(Connection cn : connectionList) {
					if(cn.peerInfo.peerID == selectPeer) {
						connection=cn;
					}
				}
				//If all the interested peers have been chosen as preferred neighbors, skip this round
				//If the peerID got is wrong, skip this round
				if(connection == null) {
					Thread.sleep(randomChooseInterval);
					continue;
				}
				connection.server.sendChokeMessage(false);
				Thread.sleep(randomChooseInterval);
				if(interestedSet.contains(selectPeer) || neighborManager.optimisticallyUnchokedNeighbor == selectPeer) {
					connection.server.sendChokeMessage(true);
				}
			}
		}catch(InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Optimistic neighbor end!");
	}
	
	private int getOptNeighbor() throws IOException {
		int selectPeer = randomSelectPeer();
		while((!neighborManager.preferredNeighborsSet.contains(selectPeer)) || (selectPeer == -1)) {
			if(neighborManager.preferredNeighborsSet.containsAll(interestedSet)) {
				return -1;
			}
			if (interestedSet.size() == 0) {
				return -1;
			}
			selectPeer = randomSelectPeer();
			if (selectPeer == -1)
				return -1;
		}
		neighborManager.updateOptimisticallyUnchokedNeighbor(selectPeer);
		
		(new Log(thisInfo.peerID)).OptimisticallyUnchokedNeighborLog(thisInfo.peerID, selectPeer);
		
		return selectPeer;
	}
	
	private int randomSelectPeer() {
		Integer[] peers;
		synchronized(interestedSet) {
			peers = (Integer[])interestedSet.toArray(new Integer[interestedSet.size()]);
			if (peers.length == 0)
				return -1;
		}
		int index = (int)(Math.random()*(peers.length));
		return peers[index].intValue();
	}
}
