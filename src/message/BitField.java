package message;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Set;

public class BitField {

	public byte[] bitFieldArray;
	public static int pieceNum;
	
	public BitField(int pieceNum) {
		this(pieceNum, false);
	}
	
	public BitField(byte[] bitFieldArray) {
		this.bitFieldArray = bitFieldArray;
	}
	
	public BitField(int pieceNumber, boolean hasFile) {
		pieceNum = pieceNumber;
		int offset, size, offset_size;

		offset = pieceNum % Constant.BYTE_SIZE;
		//offset = (offset == 0) ? Constant.BYTE_SIZE : offset;
		if(offset == 0) offset = Constant.BYTE_SIZE;
		else offset = offset;

		//size = pieceNum/Constant.BYTE_SIZE + (offset==Constant.BYTE_SIZE ? 0 : 1);
		offset_size = (offset == Constant.BYTE_SIZE) ? 0 : 1;
		size = pieceNum/Constant.BYTE_SIZE + offset_size;

		bitFieldArray = new byte[size];
		if(hasFile) {
			//Initialize the bit field array to -1
			for(int i = 0; i < size - 1; i++)
				bitFieldArray[i] = Constant.MAX_BYTE;
		}else { // !hasFile
			//Initialize the bit field array to 0
			for(int i = 0; i < size - 1; i++)
				bitFieldArray[i] = Constant.MIN_BYTE;
		}
		//Set the value of last byte
		for(int i = 0; i < Constant.BYTE_SIZE; i++) {
			if(hasFile) {
				bitFieldArray[size-1] = (byte)((int)bitFieldArray[size-1] * 2 + (i < offset ? 1 : 0));
			}else {
				bitFieldArray[size-1] = Constant.MIN_BYTE;
			}
		}	
	}
	
	public boolean checkIfInterested(BitField serverBitField, Set<Integer> inFlightSet) {
		String clientBitString = this.toString();
		String serverBitString = serverBitField.toString();
		for(int i=0; i<clientBitString.length(); i++) {
			if(clientBitString.charAt(i) == '0' && serverBitString.charAt(i) == '1' && !inFlightSet.contains(i))
				return true;
		}
		return false;
	}

	public boolean checkIfInterested(BitField serverBitField) {
		String clientBitString = this.toString();
		String serverBitString = serverBitField.toString();
		for(int i=0; i<clientBitString.length(); i++) {
			if(clientBitString.charAt(i) == '0' && serverBitString.charAt(i) == '1')
				return true;
		}
		return false;
	}
	
	public boolean checkCompleted() {
		for(int i = 0; i < bitFieldArray.length - 1; i++) {
			if(bitFieldArray[i] != Constant.MAX_BYTE)
				return false;
		}
		if(bitFieldArray.length > 0) {
			int offset = pieceNum % Constant.BYTE_SIZE;
			offset = (offset == 0) ? Constant.BYTE_SIZE : offset;
			int checkSum = 0;
			for(int i = 0; i < Constant.BYTE_SIZE; i++) {
				checkSum = (byte)(checkSum * 2 + (i < offset ? 1 : 0));
			}
			if(bitFieldArray[bitFieldArray.length - 1] != checkSum)
				return false;
		}
		return true;
	}
	
	public int askForRequest(BitField serverBitfield) {
		String clientString = this.toString();
		String serverString = serverBitfield.toString();
		ArrayList<Integer> bitArray = new ArrayList<>();
		for(int i = 0; i < clientString.length(); i++) {
			if(clientString.charAt(i) == '0' && serverString.charAt(i) == '1')
				bitArray.add(i);
		}
		if(bitArray.size() == 0)
			return -1;
		return bitArray.get((int)(Math.random()*(bitArray.size())));
	}
	
	public void updateBitField(int offset) {
		int index, remain;
		index = offset / Constant.BYTE_SIZE;
		remain = offset % Constant.BYTE_SIZE;
		bitFieldArray[index] += (int)Math.pow(2, (7-remain));
	}

	private String byteToString(byte b) {
		if(b>=0) {
			char[] chars = Integer.toBinaryString(b ^ 0x00FF).toCharArray();
			for(int i=0;i<chars.length;i++) {
				if(chars[i]=='0') chars[i]='1';
				else chars[i]='0';
			}
			return new String(chars);
		}else {
			return Integer.toBinaryString(b & 0x00FF);
		}
	}

	public boolean checkBitField(int offset) {
		int index = offset / Constant.BYTE_SIZE;
		int remain = offset % Constant.BYTE_SIZE;
		String s = byteToString(bitFieldArray[index]);
		return s.charAt(remain) == '1';
	}
	
	@Override
	public String toString() {
		String bitFieldString = new String();
		for(byte b : bitFieldArray)
			bitFieldString += byteToString(b);
		return bitFieldString;
	}
	


}
