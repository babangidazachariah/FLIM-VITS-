package com.debangis.messages;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;


public class SignalRequestMessage extends V2xMessage {
	 private static final long serialVersionUID = 1L;
	 
	 private final EncodedPayload payload;
	 private final SignalRequestMessageContent srmContent;
	 
	
	public EncodedPayload getPayLoad() {
		// TODO Auto-generated method stub
		return this.payload;
	}
	public EncodedPayload getPayload() {
		// TODO Auto-generated method stub
		return payload;
	}
	 
	public SignalRequestMessage(final MessageRouting routing, SignalRequestMessageContent srmCnt, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		
		super(routing);
		Objects.requireNonNull(srmCnt);
		this.srmContent = srmCnt;
		
		if ((EtsiPayloadConfiguration.getPayloadConfiguration()).encodePayloads) {
			payload = new EncodedPayload(srmCnt, minimalPayloadLength);	
		}else {
			payload = new EncodedPayload(0L, minimalPayloadLength);
		}
	}
	
	public SignalRequestMessage(final MessageRouting routing, final SignalRequestMessage srm, long minimalPayloadLength) {
	  
		this(routing, srm.srmContent, minimalPayloadLength);
	  }
	
	public long getTimeStamp() {
		return this.srmContent.getTimeStamp();
	}
	
	public String getJunctionId() {
		return this.srmContent.getJunction();
	}
	
	public String getVehicleId() {
		return this.srmContent.getVehicleId();
	}
	
	public String getApproachLeg() {
		return this.srmContent.getApproachLeg();
	}
	
	public String getDepartLeg() {
		return this.srmContent.getDepartLeg();
	}
	public String getPath() {
		return this.srmContent.getPath();
	}
	public long getArrivalTime() {
		return this.srmContent.getArrivalTime();
	}
	
	public int getVehicleType() {
		return this.srmContent.getVehicleType();
	}
	
	public GeoPoint getLocation() {
		return this.srmContent.getLocation();
	}
	
	public float getSpeed() {
		return this.srmContent.getSpeed();
	}
	
	public int getPriority() {
		return this.srmContent.getPriority();
	}
	public int getMsgType() {
		return this.srmContent.getMsgType();
	}
	

	
}
