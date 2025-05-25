package com.debangis.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class ICNMAcknowledgementContent implements ToDataOutput, Serializable{
	 private static final long serialVersionUID = 1L;
	 long timeStamp;
	 String senderIntersectionID; // The intersection that received the IntersectionCoordinationNotificationMessage and now sending the acknowledgement
	 String receiverIntersectionID; //The communication initiator and sender of IntersectionCoordinationNotificationMessage
	 int status; //The status of IntersectionCoordinationNotificationMessage reception by this intersection. 1: Successfully received; 0: unsuccessful reception
	 
	 public ICNMAcknowledgementContent(long tme, String sender, String receiver, int stus) {
		 this.timeStamp =tme;
		 this.senderIntersectionID = sender;
		 this.receiverIntersectionID = receiver;
		 this.status = stus;
	 }
	 
	 public ICNMAcknowledgementContent(long tme, String sender, String receiver) {
		 this(tme, sender, receiver, 0);
	 }
	 
	 public ICNMAcknowledgementContent(ICNMAcknowledgementContent icnmAckCnt) {
		 this(icnmAckCnt.getTimeStamp(),
				 icnmAckCnt.getSenderIntersectionID(),
				 icnmAckCnt.getReceiverIntersectionID(),
				 icnmAckCnt.getStatus()
			);
		 
	 }
	 
	 public ICNMAcknowledgementContent(DataInput din) throws IOException{
		 this.timeStamp = din.readLong();
		 this.senderIntersectionID = din.readUTF();
		 this.receiverIntersectionID = din.readUTF();
		 this.status = din.readInt();
	 }
	 
	 @Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
			// TODO Auto-generated method stub
			dataOutput.writeLong(this.timeStamp);
			dataOutput.writeUTF(this.senderIntersectionID);
			dataOutput.writeUTF(this.receiverIntersectionID);
			dataOutput.writeInt(this.status);
	 }
	 
	 public long getTimeStamp() {
		 return this.timeStamp;
	 }
	 
	 public String getSenderIntersectionID() {
		 return this.senderIntersectionID;
	 }
	 
	 public String getReceiverIntersectionID() {
		 return this.receiverIntersectionID;
	 }
	 
	 public int getStatus() {
		 return this.status;
	 }
	 

}
