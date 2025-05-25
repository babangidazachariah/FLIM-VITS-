package com.debangis;

import com.debangis.messages.*;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageAcknowledgement;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.addressing.SourceAddressContainer;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.util.scheduling.Event;



import tech.tablesaw.api.*;

public class AdhocTrafficRecordingServer extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {
	/*
	public AdhocTrafficRecordingServer() {
		
	}
	*/
	TrafficRecording trfRcdng;
	Table adhocTrafficRecords;
	//Coordinate of intersection: 53.547718, 9.966288
	 public void onStartup() {
	    getLog().infoSimTime((OperatingSystemAccess)this, "Initialize Adhoc Communication Application", new Object[0]);
	    ((RoadSideUnitOperatingSystem)getOs()).getAdHocModule().enable();
	    getLog().infoSimTime((OperatingSystemAccess)this, "Activated Adhoc Communication Wifi Module", new Object[0]);
	    
	    Event event = new Event(getOs().getSimulationTime() + 3000000000L, this::initializeParameters);
		getOs().getEventManager().addEvent(event);
	    
	  }

	 private void initializeParameters(Event event) {
		 trfRcdng = new TrafficRecording();
		 adhocTrafficRecords = trfRcdng.CreateTrafficRecording("adhocTrafficRecords");
	 }
	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		adhocTrafficRecords.printAll();
		getLog().infoSimTime((OperatingSystemAccess)this, "Shutdown Adhoc Traffic Recording Server Application", new Object[0]);
		
	}

	@Override
	public void processEvent(Event arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCamBuilding(CamBuilder arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		// TODO Auto-generated method stub
		getLog().info("Adhoc SRM Message is received");
		//*
		V2xMessage msg = receivedV2xMessage.getMessage();
	    if (!(msg instanceof SignalRequestMessage)) {
	    	getLog().infoSimTime((OperatingSystemAccess)this, "Ignoring message of type: {}", new Object[] { msg.getSimpleClassName() });
	    	return;
	    }
	    SignalRequestMessage srm = (SignalRequestMessage) msg;
	    getLog().info("MSG FROM: {} at lat {} and lon {}", srm.getVehicleId(), srm.getLocation().getLatitude(),srm.getLocation().getLongitude() );
	    trfRcdng = new TrafficRecording();
	    trfRcdng.setTable(adhocTrafficRecords);
		adhocTrafficRecords = trfRcdng.ProcessSRM(adhocTrafficRecords, srm);
	    
		getLog().info("TRAFFIC RECORD: {}", adhocTrafficRecords.printAll());
	    //Send Adhoc V2xMessage Acknowledgement
	    int status = trfRcdng.getStatus(); //GET MSGTYPE from Traffic recording to report successful recording of vehicle data
	    SRMAcknowledgementContent srmAckCnt = new SRMAcknowledgementContent("Junction", srm.getVehicleId(), srm.getApproachLeg(), srm.getMsgType(), status);
	    final MessageRouting adhocRouting = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoBroadCast();
	    SRMAcknowledgement adhocSrm = new SRMAcknowledgement(adhocRouting,srmAckCnt, 200L);
        getOs().getAdHocModule().sendV2xMessage(adhocSrm);
	    
	    //*/
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}
	  
	public Table getAhocTrafficRecords() {
		return this.adhocTrafficRecords;
	}

}
