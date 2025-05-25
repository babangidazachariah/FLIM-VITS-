package com.debangis.messages;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class SPaTContent  implements ToDataOutput, Serializable{
	 private static final long serialVersionUID = 1L;
	 private String scheduledVehicles; //List of scheduled vehicles IDs seperated by comma
	 private long startTime;
	 private int duration;
	 private String approachLegs; //List of approach legs of scheduled vehicles
	 private String departLegs; //List of depart legs of scheduled vehicles
	 private String junctionID; //Stores the name of the junction
	 private GeoPoint junctionPoint; //Stores names of junctions and their geocoordinate points: 34.56, 34.92 

	public SPaTContent(String jncID, GeoPoint jncPoint, String schdledVehs, long strtTime, int dur, String appLegs, String deptLegs) {
		// TODO Auto-generated constructor stub
		this.junctionID = jncID;
		this.junctionPoint = jncPoint;
		this.scheduledVehicles = schdledVehs;
		this.startTime = strtTime;
		this.duration = dur;
		this.approachLegs = appLegs;
		this.departLegs = deptLegs;
	}
	
	public SPaTContent(long strtTime, int dur) {
		//Constructor for Scenario where there are no vehicles
		this.junctionID = " ";
		this.junctionPoint = GeoPoint.latLon(0.0, 0.0);
		this.scheduledVehicles = " ";
		this.startTime = strtTime;
		this.duration = dur;
		this.approachLegs = " ";
		this.departLegs = " ";
	}

	public SPaTContent(SPaTContent spatCntnt)  throws IOException{
		this(spatCntnt.getJunctionID(),
				spatCntnt.getJunctionPoint(),
				spatCntnt.getScheduledVehicles(),
				spatCntnt.getStartTime(),
				spatCntnt.getDuration(),
				spatCntnt.getApproachLegs(),
				spatCntnt.getDepartLegs()
			);
	}
	public SPaTContent(String schdledVehs, long strtTime, int dur) {
		this("", GeoPoint.latLon(0.0, 0.0), schdledVehs, strtTime, dur, "", "");
	}
	
	public GeoPoint getJunctionPoint() {
		return junctionPoint;
	}
	public String getJunctionID() {
		return junctionID;
	}
	public int getDuration() {
		return duration;
	}
	public String getDepartLegs() {
		return departLegs;
	}
	public String getApproachLegs() {
		return approachLegs;
	}
	public String getScheduledVehicles(){
		return scheduledVehicles;
	}
	public long getStartTime() {
		return startTime;
	}
	public SPaTContent(DataInput din) throws IOException{
		this.scheduledVehicles = din.readUTF();
		this.startTime = din.readLong();
		this.duration = din.readInt();
		this.departLegs = din.readUTF();
		this.departLegs = din.readUTF();
	}
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		// TODO Auto-generated method stub
		dataOutput.writeUTF(scheduledVehicles);
		dataOutput.writeLong(startTime);
		dataOutput.writeInt(duration);
		dataOutput.writeUTF(approachLegs);
		dataOutput.writeUTF(departLegs);
	}

}
