package file;

import java.util.List;

import main.ReadData;

import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System;

public class BlockReaderWriter {

	public static String filePath;
	public volatile List<BlockNode> fileBlockList = new ArrayList<>();
	
	public BlockReaderWriter(String filepath) {
		this(filepath, false);
	}
	
	public BlockReaderWriter(String filepath, boolean hasFile) {
		filePath = filepath;
		if (!hasFile) {
			return;
		}
		int curFileSize = ReadData.fileSize;
		int eachPieceSize = ReadData.pieceSize;
		int tailSize = curFileSize % eachPieceSize;
		int blockNum = (tailSize == 0) ? (curFileSize / eachPieceSize) : (curFileSize / eachPieceSize + 1);
		tailSize = (tailSize == 0) ? eachPieceSize : tailSize;
		for (int index = 0; index < blockNum - 1; ++index) {
			fileBlockList.add(new BlockNode(index, eachPieceSize));
		}
		fileBlockList.add(new BlockNode(blockNum - 1, tailSize));
	}
	
	public int insertPiece(int pieceIndex, byte[] bytesInserted) {
		BlockNode newNode = new BlockNode(pieceIndex, bytesInserted.length);
		int insertOffset = findInsertOffset(newNode);
		if(insertOffset == -1) return insertOffset;
		try {
			File file = new File(filePath);
			if(file.exists()) {
				BufferedInputStream  bis  = new BufferedInputStream (new FileInputStream(filePath));
				byte[] byteStream = new byte[(int)file.length()];
				bis.read(byteStream);
				bis.close();
				
				byte[] concentratedArray = insertByteArray(byteStream, bytesInserted, insertOffset);
				if(concentratedArray == null) {
					return -1;
				}
				
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
				bos.write(concentratedArray);
				bos.close();
			}
			else {
				file.createNewFile();
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
				bos.write(bytesInserted);
				bos.close();
			}
		}catch (IOException e) {
			System.out.println("Fail add new block");
			e.printStackTrace();
		}
		return pieceIndex;
	}
	
	private int findInsertOffset(BlockNode newNode) {
		int insertOffset = 0;
		int blockIndex = 0;
		for(BlockNode node : fileBlockList) {
			if(node.index < newNode.index) {
				insertOffset += node.length;
				blockIndex++;
			}else if(node.index == newNode.index) {
				return -1;
			}else {
				break;
			}
		}
		synchronized(fileBlockList) {
			fileBlockList.add(blockIndex, newNode);
		}
		return insertOffset;
	}
	
	private byte[] insertByteArray(byte[] originBytes, byte[] targetBytes, int insertIndex) {
		int originLength = originBytes.length;
		if(insertIndex > originLength)
			return null;
		int lengthInserted = targetBytes.length;
		byte[] concentratedBytes = new byte[originLength + lengthInserted];
		System.arraycopy(originBytes, 0, concentratedBytes, 0, insertIndex);
		System.arraycopy(targetBytes, 0, concentratedBytes, insertIndex, lengthInserted);
		System.arraycopy(originBytes, insertIndex, concentratedBytes, insertIndex + lengthInserted, originLength - insertIndex);
		return concentratedBytes;
	}
	
	public byte[] getPiece(int pieceIndex) throws IOException{
		int offset = 0;
		for(BlockNode node : fileBlockList) {
			if(node.index == pieceIndex) {
				byte[] piece = null;
				File file = new File(filePath);
				FileInputStream fileInputStream = new FileInputStream(filePath);
				BufferedInputStream  bufferedInputStream  = new BufferedInputStream (fileInputStream);
				byte[] byteStream = new byte[(int)file.length()];
				bufferedInputStream.read(byteStream);
				bufferedInputStream.close();
				piece = Arrays.copyOfRange(byteStream, offset, offset + node.length);
				return piece;
			}
			offset += node.length;
		}
		return null;
	}
	class BlockNode {

		int index;
		int length;

		BlockNode(int index, int length){
			this.index = index;
			this.length = length;
		}

	}
}
