package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import message.BitField;

public class ReadData {

	public static int numberOfPreferredNeighbors;
	public static int unchokingInterval;
	public static int optimisticUnchokingInterval;
	public static int fileSize;
	public static int pieceSize;
	public static String fileName;
	
	public volatile List<PeerInfo> peerList = new ArrayList<PeerInfo>();
	public volatile PeerInfo myInfo;
	
	ReadData(int peerIndex) {
		ReadCommon();
		ReadPeerInfo(peerIndex);
	}
	
	//Read Common Info
	private void ReadCommon() {
		try {
			File file = new File("src/Common.cfg");
			if (file.isFile()) {
				InputStreamReader read = new InputStreamReader(new FileInputStream(file));
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineInfo;

				for(int i = 0; (lineInfo = bufferedReader.readLine()) != null; ++i) {
					String[] value_list = lineInfo.split(" ");
					if (i==0){
						numberOfPreferredNeighbors = Integer.parseInt(value_list[1]);
					}
					if (i==1){
						unchokingInterval = Integer.parseInt(value_list[1]);
					}
					if (i==2){
						optimisticUnchokingInterval = Integer.parseInt(value_list[1]);
					}
					if (i==3){
						fileName = value_list[1];
					}
					if (i==4){
						fileSize = Integer.parseInt(value_list[1]);
					}
					if (i==5){
						pieceSize = Integer.parseInt(value_list[1]);
					}
				}
			}else {
				System.out.println("Cannot find Common.cfg file!");
			}
		} catch (Exception e) {
			System.out.println("Read error!");
		}
	}
	
	//Read Peer Info
	private void ReadPeerInfo(int peerIndex) {
		try {
			//String encoding = "UTF-8";
			File file = new File("src/PeerInfo.cfg");
			if (file.isFile()) {
				InputStreamReader read = new InputStreamReader(new FileInputStream(file));
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineInfo;

				for (int initSequence = 0;(lineInfo = bufferedReader.readLine()) != null;initSequence++) {
					String value_list[] = lineInfo.split(" ");
					PeerInfo peerInfo = new PeerInfo();
					int pieceNumber;
					if(fileSize%pieceSize == 0){
						pieceNumber=(fileSize/pieceSize);
					}
					else{
						pieceNumber=(fileSize/pieceSize + 1);
					}
					peerInfo.peerID = Integer.parseInt(value_list[0]);
					if (peerInfo.peerID == peerIndex) {
						myInfo = peerInfo;
					}
					peerInfo.hostName = value_list[1];
					peerInfo.listeningPort = Integer.parseInt(value_list[2]);

					peerInfo.hasFile = (Integer.parseInt(value_list[3]) == 1);
					peerInfo.bitField = new BitField(pieceNumber, peerInfo.hasFile ? true : false);
					peerInfo.initSeq = initSequence;
					peerList.add(peerInfo);
				}
			}
			else {
				System.out.println("Cannot find PeerInfo.cfg file!");
			}
		}
		catch (Exception e) {
			System.out.println("Read error!");
		}
	}
	
	@Override
	public String toString() {
		String sharedDataString = "numberOfPreferredNeighbors = " + numberOfPreferredNeighbors + "\n"
								+ "unchokingInterval = " + unchokingInterval + "\n"
								+ "optimisticUnchokingInterval = " + optimisticUnchokingInterval + "\n"
								+ "fileSize = " + fileSize + "\n"
								+ "pieceSize = " + pieceSize + "\n"
								+ "fileName = " + fileName + "\n";
		for(PeerInfo info : peerList) {
			sharedDataString += "\n" + info.toString();
		}
		return sharedDataString;
	}
	
}
