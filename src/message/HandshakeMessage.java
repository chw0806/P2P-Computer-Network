package message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HandshakeMessage {//implements Externalizable {
	
	public String handshakerHeader;
	public byte[] zeroBits;
	public int peerID;                               						   //4-byte peer ID
	
	public HandshakeMessage() {
		zeroBits = new byte[Constant.ZERO_BITS.length];
	}
	
	public HandshakeMessage(int peerID) {
		this.handshakerHeader = Constant.HANDSHAKER_HEADER;
		this.zeroBits = Constant.ZERO_BITS;
		this.peerID = peerID;
	}
	
	public boolean checkReceivedHandshake(int serverID) {
		return this.handshakerHeader == Constant.HANDSHAKER_HEADER
				&& this.peerID == serverID;
	}
	
	@Override
	public String toString() {
		String bitFieldString = new String();
		for(byte b : zeroBits)
			bitFieldString += Integer.toString(b & 0x00FF) + ',';
		bitFieldString = bitFieldString.substring(0, bitFieldString.length()-1);
		return handshakerHeader + "\n"
				+ bitFieldString + "\n"
				+ Integer.toString(peerID);
	}
	
	public void readHandshakeMessage(InputStream in) throws IOException, InterruptedException {
		byte[] byteArray = new byte[Constant.HANDSHAKER_LENGTH];
		while (in.available() != Constant.HANDSHAKER_LENGTH) {
			Thread.sleep(Constant.SHORT_INTERVAL);
		}
		synchronized(in) {
			in.read(byteArray, 0, Constant.HANDSHAKER_LENGTH);
		}
		getFromByteArray(byteArray);
		return;
	}
	
	private boolean getFromByteArray(byte[] byteArray) throws IOException {
		//check if the length of handshake message is correct
		if(byteArray.length != Constant.HANDSHAKER_LENGTH)
			return false;
		handshakerHeader = new String(Arrays.copyOfRange(byteArray, 0, Constant.HANDSHAKER_HEADER_LENGTH));
		zeroBits = Arrays.copyOfRange(byteArray, Constant.HANDSHAKER_HEADER_LENGTH, Constant.HANDSHAKER_HEADER_LENGTH + Constant.ZERO_BITS.length);
		peerID = convertBytesToInteger(Arrays.copyOfRange(byteArray, Constant.HANDSHAKER_LENGTH - Integer.BYTES, Constant.HANDSHAKER_LENGTH));
		return true;
	}
	
	private int convertBytesToInteger(byte[] byteArray) {
		ByteBuffer wrapped = ByteBuffer.wrap(byteArray);
		return wrapped.getInt();
	}
	
	public void writeHandshakeMessage(OutputStream out) throws IOException {
		byte[] byteArray = convertToByteArray();
		synchronized(out) {
			out.write(byteArray, 0, Constant.HANDSHAKER_LENGTH);
		}
		return;
	}

	private byte[] convertToByteArray() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream (bos);
		
		dos.writeBytes(handshakerHeader);
		dos.write(zeroBits);
		dos.writeInt(peerID);
		
		bos.flush();
		byte[] byteArray = bos.toByteArray();
		bos.close();
		return byteArray;
	}
	
}
