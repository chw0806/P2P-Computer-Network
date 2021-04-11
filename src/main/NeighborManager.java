package main;

import java.util.HashSet;
import java.util.Set;

public class NeighborManager {

	volatile Set<Integer> preferredNeighborsSet;
	volatile int optimisticallyUnchokedNeighbor;
	
	NeighborManager() {
		preferredNeighborsSet = new HashSet<Integer>();
	}
	
	public void updateOptimisticallyUnchokedNeighbor(int peerID) {
		optimisticallyUnchokedNeighbor = peerID;
	}
	
}
