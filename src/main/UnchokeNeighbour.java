package main;

import java.util.List;
import java.util.Set;

public abstract class UnchokeNeighbour {
    PeerInfo thisInfo;

    volatile TransmissionStatus peerStatus;
    volatile List<Connection> connectionList;
    volatile Set<Integer> interestedSet;
    volatile NeighborManager neighborManager;

    public UnchokeNeighbour(peerProcess peer) {
        this.thisInfo = peer.info;
        this.peerStatus = peer.peerStatus;
        this.connectionList = peer.connectionList;
        this.interestedSet = peer.interestedSet;
        this.neighborManager = peer.neighborManager;
    }
}
