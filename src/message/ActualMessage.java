package message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ActualMessage {

	public static enum MessageType {
		choke,					// 0, no payload
		unchoke,				// 1, no payload
		interested,				// 2, no payload
		not_interested,			// 3, no payload
		have,					// 4, 4-byte piece index field
		bitfield,				// 5, a bit field
		request,				// 6, 4-byte piece index field
		piece					// 7, 4-byte piece index field and the content of the piece
	}

	public int messageLength;
	public MessageType messageType;
	public MessagePayload payload;
	
	public ActualMessage() {}
	
	public ActualMessage(int messageLength, MessageType messageType) {
		this.messageLength = messageLength;
		this.messageType = messageType;
	}
	
	public ActualMessage(int messageLength, MessageType messageType, MessagePayload payload) {
		this.messageLength = messageLength;
		this.messageType = messageType;
		this.payload = payload;
	}
	
	@Override
	public String toString() {
		return Integer.toString(messageLength) + " "
				+ messageType.name() + "\n"
				+ payload.toString();
	}
	
	public void readActualMessage(InputStream in) throws IOException, InterruptedException {
		while(in.available() < Constant.MINIMUM_SIZE) {
			Thread.sleep(Constant.SHORT_INTERVAL);
		}
		//The input stream has got enough information
		byte[] intArray = new byte[Integer.BYTES];
		in.read(intArray, 0, Integer.BYTES);
		
		messageLength = convertBytesToInteger(intArray);
		messageType = MessageType.values()[in.read()];
		
		System.out.print(messageType.ordinal() + " ");
		
		switch(messageType) {
			case have: case request: {
				payload = new MessagePayload();
				byte[] indexArray = new byte[Integer.BYTES];
				in.read(indexArray, 0, Integer.BYTES);

				payload.pieceIndex = convertBytesToInteger(indexArray);
			}
			break;
			case bitfield: {
				payload = new MessagePayload();
				byte[] bitFieldArray = new byte[messageLength - Constant.MINIMUM_SIZE];
				in.read(bitFieldArray, 0, bitFieldArray.length);
				BitField bitField = new BitField(bitFieldArray);
				payload.bitField = bitField;
			}
			break;
			case piece: {
				payload = new MessagePayload();
				byte[] indexArray = new byte[Integer.BYTES];
				in.read(indexArray, 0, Integer.BYTES);
				
				payload.pieceIndex = convertBytesToInteger(indexArray);
				byte[] contentArray = new byte[messageLength - Integer.BYTES - Constant.MINIMUM_SIZE];
				while (in.available() < contentArray.length) {
					Thread.sleep(10);
				}
				in.read(contentArray, 0, contentArray.length);
				payload.content = contentArray;
			}
			break;
			default: {
				//for choke, unchoke, interested, not_interested message
				//skip checking the payload field
			}
			break;
		}
	}
	
	private int convertBytesToInteger(byte[] byteArray) {
		ByteBuffer wrapped = ByteBuffer.wrap(byteArray);
		return wrapped.getInt();
	}
	
	public void writeActualMessage(OutputStream out) throws IOException {
		byte[] byteArray = convertToByteArray();
		synchronized(out) {
			out.write(byteArray);
		}
	}
	
	
	private byte[] convertToByteArray() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream (bos);
		
		dos.writeInt(messageLength);
		dos.writeByte((byte)messageType.ordinal());
		//Write down the payload field according to the message type 
		switch(messageType) {
			case have: case request: {
				dos.writeInt(payload.pieceIndex);
			}
			break;
			case bitfield: {
				dos.write(payload.bitField.bitFieldArray);
			}
			break;
			case piece: {
				dos.writeInt(payload.pieceIndex);
				dos.write(payload.content);
			}
			break;
			default: {
				//for choke, unchoke, interested, not_interested message
				//skip writing the payload field
			}
			break;
		}
		
		bos.flush();
		byte[] byteArray = bos.toByteArray();
		bos.close();
		return byteArray;
	}
	
}
