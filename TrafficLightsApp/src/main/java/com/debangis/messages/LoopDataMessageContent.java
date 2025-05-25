package com.debangis.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class LoopDataMessageContent implements ToDataOutput, Serializable{
	 private static final long serialVersionUID = 1L;
	/*
	 * Loop Data for an Isolated Intersection is formatted in the following manner:
	 * northApproachLeft, northApproachStraight, northApproachRight, southApproachLeft, southApproachStraight, southApproachRight
	 * westApproachLeft, westApproachStraight, westApproachRight, eastApproachLeft, eastApproachStraight, eastApproachRight
	 * as a single string. Thus, when received, it has to be splitted in the format
	 */
	long timeStamp;
	String trafficData;
	
	
	public LoopDataMessageContent(long tmpStamp, String trfData) {
		this.timeStamp = tmpStamp;
		this.trafficData = trfData;
	}
	
	public LoopDataMessageContent(String trfData) {
		this(0L, trfData);
	}
	
	public LoopDataMessageContent(LoopDataMessageContent ldmCnt) {
		this(ldmCnt.getTimeStamp(),ldmCnt.getTrafficData());
	}
	
	public LoopDataMessageContent(DataInput din) throws IOException {
		
		this.timeStamp = din.readLong();
		this.trafficData = din.readUTF();
	}
	
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		// TODO Auto-generated method stub
		
		dataOutput.writeLong(this.timeStamp);
		dataOutput.writeUTF(this.trafficData);
		
		
	}
	
	public long getTimeStamp() {
		return this.timeStamp;
	}
	public String getTrafficData() {
		return this.trafficData;
	}
}
