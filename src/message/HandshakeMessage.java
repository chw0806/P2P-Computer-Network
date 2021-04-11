package message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HandshakeMessage {//implements Externalizable {
	public int peerID;
	public byte[] zeroBits;
	public String handshakerHeader;

	public HandshakeMessage() {
		zeroBits = new byte[Constant.ZERO_BITS.length];
	}
	
	public HandshakeMessage(int peerID) {
		this.peerID = peerID;
		this.zeroBits = Constant.ZERO_BITS;
		this.handshakerHeader = Constant.HANDSHAKER_HEADER;
	}
	
	public boolean checkReceivedHandshake(int serverID) {
		boolean result1 = this.handshakerHeader == Constant.HANDSHAKER_HEADER;
		boolean result2 = this.peerID == serverID;
		return result1 && result2;
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

	private int convertBytesToInteger(byte[] byteArray) {
		ByteBuffer wrapped = ByteBuffer.wrap(byteArray);
		return wrapped.getInt();
	}

	private boolean getFromByteArray(byte[] byteArray) throws IOException {
		//check if the length of handshake message is correct
		if(byteArray.length != Constant.HANDSHAKER_LENGTH)
			return false;
		peerID = convertBytesToInteger(Arrays.copyOfRange(byteArray, Constant.HANDSHAKER_LENGTH - Integer.BYTES, Constant.HANDSHAKER_LENGTH));
		zeroBits = Arrays.copyOfRange(byteArray, Constant.HANDSHAKER_HEADER_LENGTH, Constant.HANDSHAKER_HEADER_LENGTH + Constant.ZERO_BITS.length);
		handshakerHeader = new String(Arrays.copyOfRange(byteArray, 0, Constant.HANDSHAKER_HEADER_LENGTH));
		return true;
	}

	public void readHandshakeMessage(InputStream input) throws IOException, InterruptedException {
		byte[] byteArray;
		byteArray = new byte[Constant.HANDSHAKER_LENGTH];
		while (input.available() != Constant.HANDSHAKER_LENGTH) {
			Thread.sleep(Constant.SHORT_INTERVAL);
		}
		synchronized(input) {
			input.read(byteArray, 0, Constant.HANDSHAKER_LENGTH);
		}
		getFromByteArray(byteArray);
		return;
	}

	private byte[] convertToByteArray() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream (bos);
		
		dos.writeBytes(handshakerHeader);
		dos.write(zeroBits);
		dos.writeInt(peerID);
		
		bos.flush();
		byte[] byte_Array;
		byte_Array = bos.toByteArray();
		bos.close();
		return byte_Array;
	}

	public void writeHandshakeMessage(OutputStream out) throws IOException {
		byte[] byte_Array;
		byte_Array = convertToByteArray();
		synchronized(out) {
			out.write(byte_Array, 0, Constant.HANDSHAKER_LENGTH);
		}
		return;
	}

}
