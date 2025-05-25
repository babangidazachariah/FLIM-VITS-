package com.debangis.messages;

import java.util.Objects;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;

public class LoopDataMessage extends V2xMessage {
	 private static final long serialVersionUID = 1L;
	 
	 private final EncodedPayload payload;
	 private final LoopDataMessageContent ldmContent;


	 public EncodedPayload getPayLoad() {
		// TODO Auto-generated method stub
		return this.payload;
	 }
	 public EncodedPayload getPayload() {
		// TODO Auto-generated method stub
		return payload;
	 }
	public LoopDataMessage(final MessageRouting routing, LoopDataMessage ldm, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		this(routing, ldm.ldmContent, minimalPayloadLength);
	}

	public LoopDataMessage(final MessageRouting routing, LoopDataMessageContent icaCnt, long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		
		super(routing);
		Objects.requireNonNull(icaCnt);
		this.ldmContent = icaCnt;
		
		if ((EtsiPayloadConfiguration.getPayloadConfiguration()).encodePayloads) {
			payload = new EncodedPayload(icaCnt, minimalPayloadLength);	
		}else {
			payload = new EncodedPayload(0L, minimalPayloadLength);
		}
	}
	
	public long getTimeStamp() {
		return this.ldmContent.getTimeStamp();
	}
	public String getTrafficData() {
		return this.ldmContent.getTrafficData();
	}
}
