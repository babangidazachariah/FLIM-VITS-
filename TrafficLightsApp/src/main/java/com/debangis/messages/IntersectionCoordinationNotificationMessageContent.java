package com.debangis.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.ToDataOutput;
import org.eclipse.mosaic.lib.util.SerializationUtils;

public class IntersectionCoordinationNotificationMessageContent implements ToDataOutput, Serializable{
	 private static final long serialVersionUID = 1L;
	 long timeStamp;
	 String intersectionID;
	 String vehicleID;
	 int vehiclePriority;
	 String vehicleOriging;
	 String vehiclePath;
	 String vehicleApproach; //Vehicle's current approach
	 double vehicleSpeed; //Vehicle's last known Speed.
	 private final GeoPoint location;
	 private int appVolume;

	 public IntersectionCoordinationNotificationMessageContent(long tmeStmp, String intstnID, String vehID, int vehPrty, String vehOriging, String vehPth, String vehApp, double vehSpd, GeoPoint loc, int appVol) {
		 this.timeStamp = tmeStmp;
		 this.intersectionID = intstnID;
		 this.vehicleID = vehID;
		 this.vehiclePriority = vehPrty;
		 this.vehicleOriging = vehOriging;
		 this.vehiclePath = vehPth;
		 this.vehicleApproach = vehApp;
		 this.vehicleSpeed = vehSpd;
		 this.location = loc;
		 this.appVolume = appVol;
		 
	 }
	 
	 public IntersectionCoordinationNotificationMessageContent( String intstnID, String vehID, int vehPrty, String vehOriging, String vehPth, String vehApp, double vehSpd, GeoPoint loc, int appVol) {
		 this(0l, intstnID, vehID, vehPrty, vehOriging, vehPth, vehApp, vehSpd, loc, appVol);
	 }
	 
	 
	 public IntersectionCoordinationNotificationMessageContent(IntersectionCoordinationNotificationMessageContent icnmCnt) {
		 this(icnmCnt.getTimeStamp(),
				 icnmCnt.getIntersectioID(),
				 icnmCnt.getVehicleID(),
				 icnmCnt.getVehiclePriority(),
				 icnmCnt.getVehicleOriging(),
				 icnmCnt.getVehiclePath(),
				 icnmCnt.getVehicleApproach(),
				 icnmCnt.getVehicleSpeed(),
				 icnmCnt.getLocation(),
				 icnmCnt.getApproachVolume()
			);
		 
	 }
	 
	 public IntersectionCoordinationNotificationMessageContent(DataInput din) throws IOException{
		 this.timeStamp = din.readLong();
		 this.intersectionID = din.readUTF();
		 this.vehicleID = din.readUTF();
		 this.vehiclePriority = din.readInt();
		 this.vehicleOriging = din.readUTF();
		 this.vehiclePath = din.readUTF();
		 this.vehicleApproach = din.readUTF();
		 this.vehicleSpeed = din.readDouble();
		 this.location = SerializationUtils.decodeGeoPoint(din);
		 this.appVolume = din.readInt();
	 }
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		// TODO Auto-generated method stub
		dataOutput.writeLong(this.timeStamp);
		dataOutput.writeUTF(this.intersectionID);
		dataOutput.writeUTF(this.vehicleID);
		dataOutput.writeInt(this.vehiclePriority);
		dataOutput.writeUTF(this.vehicleOriging);
		dataOutput.writeUTF(this.vehiclePath);
		dataOutput.writeUTF(this.vehicleApproach);
		dataOutput.writeDouble(this.vehicleSpeed);
		SerializationUtils.encodeGeoPoint(dataOutput, this.location); 
		dataOutput.writeInt(this.appVolume);
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}
	
	public int getVehiclePriority() {
		return this.vehiclePriority;
	}
	
	public String getIntersectioID() {
		return this.intersectionID;
	}
	public String getVehicleID() {
		return this.vehicleID;
	}
	public String getVehicleOriging() {
		return this.vehicleOriging;
	}
	
	public String getVehiclePath () {
		return this.vehiclePath;
	}
	
	
	public String getVehicleApproach() {
		return this.vehicleApproach;
	}
	
	public double getVehicleSpeed() {
		return this.vehicleSpeed;
	}
	
	public GeoPoint getLocation() {
		return this.location;
	}
	
	public int getApproachVolume() {
		return this.appVolume;
	}
}
