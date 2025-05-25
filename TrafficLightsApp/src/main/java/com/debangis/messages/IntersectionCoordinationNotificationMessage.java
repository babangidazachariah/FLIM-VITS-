package com.debangis.messages;

import java.util.Objects;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;

public class IntersectionCoordinationNotificationMessage  extends V2xMessage {
	 private static final long serialVersionUID = 1L;
	 
	 private final EncodedPayload payload;
	 private final IntersectionCoordinationNotificationMessageContent icnmCnt;
	 
	
	public EncodedPayload getPayLoad() {
		// TODO Auto-generated method stub
		return this.payload;
	}
	public EncodedPayload getPayload() {
		// TODO Auto-generated method stub
		return payload;
	}

	public IntersectionCoordinationNotificationMessage(final MessageRouting routing, IntersectionCoordinationNotificationMessageContent icnmCont, long minimalPayloadLength) {
		super(routing);
		Objects.requireNonNull(icnmCont);
		this.icnmCnt = icnmCont;
		
		if ((EtsiPayloadConfiguration.getPayloadConfiguration()).encodePayloads) {
			payload = new EncodedPayload(icnmCont, minimalPayloadLength);	
		}else {
			payload = new EncodedPayload(0L, minimalPayloadLength);
		}
	}
	
	public IntersectionCoordinationNotificationMessage(final MessageRouting routing, IntersectionCoordinationNotificationMessage icnm, long minimalPayloadLength) {
		this(routing, icnm.icnmCnt, minimalPayloadLength);
	}
	
	
	public long getTimeStamp() {
		return icnmCnt.getTimeStamp();
	}
	public String getIntersectioID() {
		return icnmCnt.getIntersectioID();
	}
	public String getVehicleID() {
		return icnmCnt.getVehicleID();
	}
	public int getVehiclePriority() {
		return icnmCnt.getVehiclePriority();
	}
	
	public String getVehicleOriging() {
		return icnmCnt.getVehicleOriging();
	}
	
	public String getVehiclePath () {
		return icnmCnt.getVehiclePath () ;
	}
	
	
	public String getVehicleApproach() {
		return icnmCnt.getVehicleApproach();
	}
	
	public double getVehicleSpeed() {
		return icnmCnt.getVehicleSpeed() ;
	}
	
	public GeoPoint getLocation() {
		return icnmCnt.getLocation();
	}
	
	public int getApproachVolume() {
		return icnmCnt.getApproachVolume();
	}
}
