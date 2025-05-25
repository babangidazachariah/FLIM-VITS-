package com.debangis.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.ToDataOutput;
import org.eclipse.mosaic.lib.util.SerializationUtils;

public class SignalRequestMessageContent implements ToDataOutput, Serializable{
	 private static final long serialVersionUID = 1L;
	private final long timeStamp;
	private final String junctionID;
	private final String vehicleID;
	private final String approachLeg;
	private final String departLeg;
	private final String path;
	private final long arrivalTime;
	private final int vehicleType;
	private final GeoPoint location;
	private final float speed;
	private final int priority;
	private final int msgType; //1: approach, 2: Depart

	public SignalRequestMessageContent(long time, String jnctnId, String vehId, String appLeg, String deptLeg, String pth, long arvltme, int vehType, GeoPoint loc, float spd, int prty, int msgtp) {
		// TODO Auto-generated constructor stub
		this.timeStamp = time;
		this.junctionID = jnctnId;
		this.vehicleID = vehId;
		this.approachLeg = appLeg;
		this.departLeg = deptLeg;
		this.path = pth;
		this.arrivalTime = arvltme;
		this.vehicleType = vehType;
		this.location = loc;
		this.speed = spd;
		this.priority = prty;
		this.msgType = msgtp;
				
	}
	
	public SignalRequestMessageContent(long time, String jnctnId, String vehId, String appLeg, String deptLeg, String pth, long arvltme, int vehType, GeoPoint loc, float spd, int mstp) {
		this(time, jnctnId, vehId, appLeg, deptLeg, pth, arvltme, vehType, loc, spd, 1, mstp);
	}

	public SignalRequestMessageContent(SignalRequestMessageContent srm) throws IOException{
		this(srm.getTimeStamp(), 
				srm.getJunction(),
				srm.getVehicleId(),
				srm.getApproachLeg(),
				srm.getDepartLeg(),
				srm.getPath(),
				srm.getArrivalTime(),
				srm.getVehicleType(),
				srm.getLocation(),
				srm.getSpeed(),
				srm.getPriority(),
				srm.getMsgType()
			);
				
				
	}

	
	public SignalRequestMessageContent(DataInput din) throws IOException{
		// TODO Auto-generated constructor stub
		this.timeStamp = din.readLong();
		this.junctionID = din.readUTF();
		this.vehicleID = din.readUTF();
		this.approachLeg = din.readUTF();
		this.departLeg = din.readUTF();
		this.path = din.readUTF();
		this.arrivalTime = din.readLong();
		this.vehicleType = din.readInt();
		this.location = SerializationUtils.decodeGeoPoint(din);
		this.speed = din.readFloat();
		this.priority = din.readInt();
		this.msgType = din.readInt();
	}
	
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		// TODO Auto-generated method stub
		dataOutput.writeLong(this.timeStamp);
		dataOutput.writeUTF(this.junctionID);
		dataOutput.writeUTF(this.vehicleID);
		dataOutput.writeUTF(this.approachLeg);
		dataOutput.writeUTF(this.departLeg);
		dataOutput.writeUTF(this.path);
		dataOutput.writeLong(this.arrivalTime);
		dataOutput.writeInt(this.vehicleType);
		SerializationUtils.encodeGeoPoint(dataOutput, this.location); 
		dataOutput.writeFloat(this.speed);
		dataOutput.writeInt(this.priority);
		dataOutput.write(this.msgType);
		
	}
	
	//Functions to retrieve individual values
	
	public long getTimeStamp() {
		return this.timeStamp;
		
	}

	public String getJunction() {
		return this.junctionID;
	}
	
	public String getVehicleId() {
		return this.vehicleID;
	}
	
	public String getApproachLeg() {
		return this.approachLeg;
	}

	public String getDepartLeg() {
		return this.departLeg;
	}
	

	public String getPath() {
		return this.path;
	}
	
	public long getArrivalTime() {
		return this.arrivalTime;
	}

	public int getVehicleType() {
		return this.vehicleType;
	}
	
	public GeoPoint getLocation() {
		return this.location;
	}
	
	public  float getSpeed() {
		return this.speed;
		
	}
	
	public int getPriority() {
		return this.priority;
	}
	public int getMsgType() {
		return this.msgType;
	}
}
