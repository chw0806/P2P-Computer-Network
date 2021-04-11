package main;

public class TransmissionStatus {

	public volatile boolean downloadCompleted;
	public volatile boolean uploadCompleted;
	
	TransmissionStatus(PeerInfo info){
		downloadCompleted = info.bitField.checkCompleted();
		uploadCompleted = false;
	}
	
	public boolean getDownloadCompleted() {
		synchronized(this){
			return downloadCompleted;
		}
	}
	
	public boolean getUploadCompleted() {
		synchronized(this){
			return uploadCompleted;
		}
	}
	
	public void setDownloadCompleted() {
		synchronized(this){
			downloadCompleted = true;
		}
	}
	
	public void setUploadCompleted() {
		synchronized(this){
			uploadCompleted = true;
		}
	}
	
	public boolean checkCompleted() {
		return getDownloadCompleted() && getUploadCompleted();
	}
	
}
