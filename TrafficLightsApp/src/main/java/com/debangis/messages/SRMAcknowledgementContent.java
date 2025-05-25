package com.debangis.messages;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class SRMAcknowledgementContent implements ToDataOutput, Serializable {

	private static final long serialVersionUID = 1L;
	
	private String junctionID = "Junction";
	private String sender; //The Originator of Signal Request Message
	private String path; //The current path (roadID of the originator of SRM
	private int msgType;
	private int status; //2 ==> Approach Message successfully received and recorded; 4 ==> Depart Message successfully received and process
						//0 ==> implies unsuccessful processing
	
	public SRMAcknowledgementContent(String jncID, String vehID, String pth, int mstpe, int stus) {
		// TODO Auto-generated constructor stub
		this.junctionID = jncID;
		this.sender = vehID;
		this.path = pth;
		this.msgType = mstpe;
		this.status = stus;
	}
	public SRMAcknowledgementContent() {
		this("Junction", "", "", 1, 0);
	}

	public SRMAcknowledgementContent(String jncID, String vehID, String pth, int mstpe) {
		this(jncID, vehID, pth, mstpe, 0);
	}
	public SRMAcknowledgementContent(SRMAcknowledgementContent srmAckCnt)throws IOException{
		this(srmAckCnt.getJunctionID(), srmAckCnt.getSender(), srmAckCnt.getPath(), srmAckCnt.getMsgType(),srmAckCnt.getStatus());
	}
	public SRMAcknowledgementContent(DataInput din) throws IOException{
		this.junctionID = din.readUTF();
		this.sender = din.readUTF();
		this.path = din.readUTF();
		this.msgType = din.readInt();
		this.status = din.readInt();
	}
		// TODO Auto-generated constructor stub
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		// TODO Auto-generated method stub
		dataOutput.writeUTF(this.junctionID);
		dataOutput.writeUTF(this.sender);
		dataOutput.writeUTF(this.path);
		dataOutput.writeInt(this.msgType);
		dataOutput.writeInt(this.status);
	}
	
	public String getJunctionID() {
		return this.junctionID;
	}
	public String getSender() {
		return this.sender;
	}
	public String getPath() {
		return this.path;
	}
	public int getStatus() {
		return this.status;
	}
	public int getMsgType() {
		return this.msgType;
	}
}
