package com.debangis.messages;

import java.util.Objects;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;

public class ICNMAcknowledgement  extends V2xMessage {
	 private static final long serialVersionUID = 1L;
	 
	 private final EncodedPayload payload;
	 private final ICNMAcknowledgementContent icnmAckCnt;
	 
	
	public EncodedPayload getPayLoad() {
		// TODO Auto-generated method stub
		return this.payload;
	}
	public EncodedPayload getPayload() {
		// TODO Auto-generated method stub
		return payload;
	}
	
	
	public ICNMAcknowledgement(final MessageRouting routing, ICNMAcknowledgementContent icnmackCont, long minimalPayloadLength) {
		super(routing);
		Objects.requireNonNull(icnmackCont);
		this.icnmAckCnt = icnmackCont;
		
		if ((EtsiPayloadConfiguration.getPayloadConfiguration()).encodePayloads) {
			payload = new EncodedPayload(icnmackCont, minimalPayloadLength);	
		}else {
			payload = new EncodedPayload(0L, minimalPayloadLength);
		}
	}
	
	public ICNMAcknowledgement(final MessageRouting routing, ICNMAcknowledgement icnmAck, long minimalPayloadLength) {
		this(routing, icnmAck.icnmAckCnt, minimalPayloadLength);
	}
	
	
	public long getTimeStamp() {
		 return icnmAckCnt.getTimeStamp();
	 }
	 
	 public String getSenderIntersectionID() {
		 return icnmAckCnt.getSenderIntersectionID() ;
	 }
	 
	 public String getReceiverIntersectionID() {
		 return icnmAckCnt.getReceiverIntersectionID();
	 }
	 
	 public int getStatus() {
		 return icnmAckCnt.getStatus();
	 }
	 
}
