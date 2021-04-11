package message;

public class MessagePayload {
	/*
	 * choke, unchoke, interested, not_interested message has no payload
	 * have, request message has a payload of 4-byte piece index field
	 * bitfield message has a paylaod of a bit field
	 * piece message has a payload of 4-byte piece index field and the content of the piece
	 */
	public int pieceIndex;
	public BitField bitField;
	public byte[] content;
	
	public MessagePayload() {}
	
	public MessagePayload(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}
	
	public MessagePayload(BitField bitField) {
		this.bitField = bitField;
	}
	
	public MessagePayload(int pieceIndex, byte[] content) {
		this.pieceIndex = pieceIndex;
		this.content = content;
	}
	
	@Override
	public String toString() {
		return "piece index: " + Integer.toString(pieceIndex) + "\n"
				+ "bit field: " + bitField.toString() + "\n"
				+ "content: " + content.toString();
	}
	
}
