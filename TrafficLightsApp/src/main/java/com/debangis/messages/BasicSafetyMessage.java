package com.debangis.messages;

import java.util.Objects;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;



public class BasicSafetyMessage extends V2xMessage {
	 private static final long serialVersionUID = 1L;
	 
	 private final EncodedPayload payload;
	 private final BasicSafetyMessageContent bsmContent;


	 public EncodedPayload getPayLoad() {
		// TODO Auto-generated method stub
		return this.payload;
	 }
	 public EncodedPayload getPayload() {
		// TODO Auto-generated method stub
		return payload;
	 }
	public BasicSafetyMessage(final MessageRouting routing, BasicSafetyMessage bsm, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		this(routing, bsm.bsmContent, minimalPayloadLength);
	}

	public BasicSafetyMessage(final MessageRouting routing, BasicSafetyMessageContent bsmCnt, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		
		super(routing);
		Objects.requireNonNull(bsmCnt);
		this.bsmContent = bsmCnt;
		
		if ((EtsiPayloadConfiguration.getPayloadConfiguration()).encodePayloads) {
			payload = new EncodedPayload(bsmCnt, minimalPayloadLength);	
		}else {
			payload = new EncodedPayload(0L, minimalPayloadLength);
		}
	}
	
	public long getTimeStamp() {
		return this.bsmContent.getTimeStamp();
	}
	public String getVehicleID() {
		return this.bsmContent.getVehicleID();
	}
	
	public int getVehicleType() {
		return this.bsmContent.getVehicleType();
	}
	public float getSteeringAngle() {
		return this.bsmContent.getSteeringAngle();
	}
	public float getAcceleration() {
		return this.bsmContent.getAcceleration();
	}
	public float getSpeed() {
		return this.bsmContent.getSpeed();
	}
	public float getHeading() {
		return this.bsmContent.getHeading();
	}
	public float getLongitude() {
		return this.bsmContent.getLongitude();
	}
	public float getLatitude() {
		return this.bsmContent.getLatitude();
	}
}
