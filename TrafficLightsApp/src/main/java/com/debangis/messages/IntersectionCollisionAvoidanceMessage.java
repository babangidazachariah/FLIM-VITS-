package com.debangis.messages;

import java.util.Objects;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;

public class IntersectionCollisionAvoidanceMessage extends V2xMessage {
	 private static final long serialVersionUID = 1L;
	 
	 private final EncodedPayload payload;
	 private final IntersectionCollisionAvoidanceMessageContent icaContent;


	 public EncodedPayload getPayLoad() {
		// TODO Auto-generated method stub
		return this.payload;
	 }
	 public EncodedPayload getPayload() {
		// TODO Auto-generated method stub
		return payload;
	 }
	public IntersectionCollisionAvoidanceMessage(final MessageRouting routing, IntersectionCollisionAvoidanceMessage ica, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		this(routing, ica.icaContent, minimalPayloadLength);
	}

	public IntersectionCollisionAvoidanceMessage(final MessageRouting routing, IntersectionCollisionAvoidanceMessageContent icaCnt, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		
		super(routing);
		Objects.requireNonNull(icaCnt);
		this.icaContent = icaCnt;
		
		if ((EtsiPayloadConfiguration.getPayloadConfiguration()).encodePayloads) {
			payload = new EncodedPayload(icaCnt, minimalPayloadLength);	
		}else {
			payload = new EncodedPayload(0L, minimalPayloadLength);
		}
	}
	
	public long getTimeStamp() {
		return this.icaContent.getTimeStamp();
	}
	public String getVehicleID() {
		return this.icaContent.getVehicleID();
	}
	
	public String getPath() {
		return this.icaContent.getPath();
	}
	
	public int getVehicleType() {
		return this.icaContent.getVehicleType();
	}
	public float getSteeringAngle() {
		return this.icaContent.getSteeringAngle();
	}
	public float getAcceleration() {
		return this.icaContent.getAcceleration();
	}
	public float getSpeed() {
		return this.icaContent.getSpeed();
	}
	public float getHeading() {
		return this.icaContent.getHeading();
	}
	public float getLongitude() {
		return this.icaContent.getLongitude();
	}
	public float getLatitude() {
		return this.icaContent.getLatitude();
	}

}
