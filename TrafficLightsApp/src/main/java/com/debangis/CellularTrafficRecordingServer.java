package com.debangis;

import com.debangis.messages.*;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.os.ServerOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import tech.tablesaw.api.*;

public class CellularTrafficRecordingServer extends AbstractApplication<ServerOperatingSystem>   implements CommunicationApplication{
	/*
	public CellularTrafficRecordingServer() {
		
	}
	*/
	TrafficRecording trfRcdng = new TrafficRecording();
	Table cellularTrafficRecords;
	
	//Coordinate of intersection: 53.547718, 9.966288
	@Override
	public void onStartup() {
		
	    getLog().infoSimTime((OperatingSystemAccess)this, "Initialize Cellular Communication Application Unit", new Object[0]);
	    ((ServerOperatingSystem)getOs()).getCellModule().enable();
	    getLog().infoSimTime((OperatingSystemAccess)this, "Setup Cellular Communication Application Server {} at time {}", new Object[] { ((ServerOperatingSystem)getOs()).getId(), Long.valueOf(((ServerOperatingSystem)getOs()).getSimulationTime()) });
	    getLog().infoSimTime((OperatingSystemAccess)this, "Activated Cellular Communication Application Cell Module", new Object[0]);
	    
	}
	
	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		V2xMessage msg = receivedV2xMessage.getMessage();
		/*
		getLog().info("SRM Message Receieve and being processed");
	    V2xMessage msg = receivedV2xMessage.getMessage();
	    if (!(msg instanceof SignalRequestMessage)) {
	    	getLog().infoSimTime((OperatingSystemAccess)this, "Ignoring message of type: {}", new Object[] { msg.getSimpleClassName() });
	    	return;
	    }
	    
	    SignalRequestMessage srm = (SignalRequestMessage) msg;
	    trfRcdng.ProcessSRM(cellularTrafficRecords, srm);
	    getLog().info("Done Processing SRM Message");
	    */
		if (msg instanceof LoopDataMessage) {
	    	//getLog().infoSimTime((OperatingSystemAccess)this, "Ignoring message of type: {}", new Object[] { msg.getSimpleClassName() });
	    	//return;
			
			System.out.println("LDM Message Receieve ");
	    }
	 } 
	
	@Override  
	public void onShutdown() {
		getLog().infoSimTime((OperatingSystemAccess)this, "Shutdown Cellular Traffic Recording Server Application", new Object[0]);
	}

	@Override
	public void processEvent(Event arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public Table getCellularTrafficRecords() {
		return this.cellularTrafficRecords;
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
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}
}
