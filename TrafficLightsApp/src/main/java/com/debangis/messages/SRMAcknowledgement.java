package com.debangis.messages;


import java.util.Objects;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;

public class SRMAcknowledgement extends V2xMessage {
	 private static final long serialVersionUID = 1L;
	 
	 private final EncodedPayload payload;
	 private final SRMAcknowledgementContent srmAckContent;
	 
	@Override
	public EncodedPayload getPayload() {
		// TODO Auto-generated method stub
		return payload;
	}
	public EncodedPayload getPayLoad() {
		// TODO Auto-generated method stub
		return this.payload;
	}
	
	public SRMAcknowledgement(final MessageRouting routing, SRMAcknowledgementContent srmAckCnt,  long minimalPayloadLength) {
		// TODO Auto-generated constructor stub
		super(routing);
		Objects.requireNonNull(srmAckCnt);
		this.srmAckContent = srmAckCnt;
		if ((EtsiPayloadConfiguration.getPayloadConfiguration()).encodePayloads) {
			payload = new EncodedPayload(srmAckCnt, minimalPayloadLength);	
		}else {
			payload = new EncodedPayload(0L, minimalPayloadLength);
		}
	}

	public SRMAcknowledgement(final MessageRouting routing, SRMAcknowledgement srmAck, long minimalPayloadLength) {
		this(routing, srmAck.srmAckContent, minimalPayloadLength);
	}
	
	public String getJunctionID() {
		return this.srmAckContent.getJunctionID();
	}
	public String getSender() {
		return this.srmAckContent.getSender();
	}
	public String getPath() {
		return this.srmAckContent.getPath();
	}
	public int getStatus() {
		return this.srmAckContent.getStatus();
	}
	public int getMsgType() {
		return this.srmAckContent.getMsgType();
	}

}
