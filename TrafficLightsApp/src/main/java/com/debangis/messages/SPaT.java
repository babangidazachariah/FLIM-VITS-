package com.debangis.messages;


import java.util.ArrayList;
import java.util.Objects;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;

public class SPaT extends V2xMessage {
	 private static final long serialVersionUID = 1L;
	 
	 private final EncodedPayload payload;
	 private final SPaTContent spatContent;
	 
	
	public EncodedPayload getPayLoad() {
		// TODO Auto-generated method stub
		return this.payload;
	}
	public EncodedPayload getPayload() {
		// TODO Auto-generated method stub
		return payload;
	}
	public SPaT(final MessageRouting routing, SPaTContent spatCnt, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		
		super(routing);
		Objects.requireNonNull(spatCnt);
		this.spatContent = spatCnt;
		
		if ((EtsiPayloadConfiguration.getPayloadConfiguration()).encodePayloads) {
			payload = new EncodedPayload(spatCnt, minimalPayloadLength);	
		}else {
			payload = new EncodedPayload(0L, minimalPayloadLength);
		}
	}
	
	public SPaT(final MessageRouting routing, final SPaT spat, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		this(routing, spat.spatContent, minimalPayloadLength);
	}
	
	public GeoPoint getJunctionPoint() {
		return spatContent.getJunctionPoint();
	}
	public String getJunctionID() {
		return spatContent.getJunctionID();
	}
	public String getScheduledVehicles(){
		return spatContent.getScheduledVehicles();
	}
	
	public String getDepartLegs() {
		return spatContent.getDepartLegs();
	}
	
	public String getApproachLegs() {
		return spatContent.getApproachLegs();
	}
	
	public int getDuration() {
		return spatContent.getDuration();
	}

	public long getStartTime() {
		return spatContent.getStartTime();
	}
}
