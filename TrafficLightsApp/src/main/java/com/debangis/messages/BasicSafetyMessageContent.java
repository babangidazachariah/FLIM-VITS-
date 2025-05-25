package com.debangis.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class BasicSafetyMessageContent implements ToDataOutput, Serializable{
	 private static final long serialVersionUID = 1L;
	/*
	 * BSM Part 1: Core data elements such as vehicle position, heading, speed, acceleration, 
	 * steering wheel angle, and vehicle size. It’s transmitted approximately 10 times per second.
	 * BSM Part 2: A variable set of data elements from an extensive list of optional elements
	 */
	String vehicleID;
	long timeStamp;
	float latitude;
	float longitude;
	float heading;
	float speed;
	float accel; //Acceleration
	float steerAngle;
	int vehType;
	
	public BasicSafetyMessageContent(String vehid, long tmeStamp, float lat, float lon, float head, float spd, float acc, float strangle,int vtype) {
		// TODO Auto-generated constructor stub
		this.vehicleID = vehid;
		this.timeStamp = tmeStamp;
		this.latitude = lat;
		this.longitude = lon;
		this.heading = head;
		this.speed = spd;
		this.accel = acc;
		this.steerAngle =strangle;
		this.vehType=vtype;
				
	}

	public BasicSafetyMessageContent(String vehid, long tme, float lat, float lon, float head,  float strangle,int vtype) {
		this(vehid, tme, lat, lon, head, 0.0f, 0.0f, strangle, vtype);
	}


	public BasicSafetyMessageContent(BasicSafetyMessageContent bsmCnt) {
		this(bsmCnt.getVehicleID(),
				bsmCnt.getTimeStamp(),
				bsmCnt.getLatitude(),
				bsmCnt.getLongitude(),
				bsmCnt.getHeading(),
				bsmCnt.getSpeed(),
				bsmCnt.getAcceleration(),
				bsmCnt.getSteeringAngle(),
				bsmCnt.getVehicleType()
			);
	}
	public BasicSafetyMessageContent(DataInput din) throws IOException{
		this.vehicleID = din.readUTF();
		this.timeStamp = din.readLong();
		this.latitude = din.readFloat();
		this.longitude = din.readFloat();
		this.heading = din.readFloat();
		this.speed = din.readFloat();
		this.accel = din.readFloat();
		this.steerAngle = din.readFloat();
		this.vehType = din.readInt();
	}
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		// TODO Auto-generated method stub
		dataOutput.writeUTF(this.vehicleID);
		dataOutput.writeLong(this.timeStamp);
		dataOutput.writeFloat(this.latitude);
		dataOutput.writeFloat(this.longitude);
		dataOutput.writeFloat(this.heading);
		dataOutput.writeFloat(this.speed);
		dataOutput.writeFloat(this.accel);
		dataOutput.writeFloat(this.steerAngle);
		dataOutput.writeInt(this.vehType);
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}
	public String getVehicleID() {
		return this.vehicleID;
	}
	
	public int getVehicleType() {
		return this.vehType;
	}
	public float getSteeringAngle() {
		return this.steerAngle;
	}
	public float getAcceleration() {
		return this.accel;
	}
	public float getSpeed() {
		return this.speed;
	}
	public float getHeading() {
		return this.heading;
	}
	public float getLongitude() {
		return this.longitude;
	}
	public float getLatitude() {
		return this.latitude;
	}
}
