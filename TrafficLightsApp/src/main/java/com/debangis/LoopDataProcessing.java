package com.debangis;

import java.util.Collection;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.ambassador.simulation.tmc.InductionLoop;
import org.eclipse.mosaic.fed.application.ambassador.simulation.tmc.LaneAreaDetector;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.TrafficManagementCenterApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficManagementCenterOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import com.debangis.messages.LoopDataMessage;
import com.debangis.messages.LoopDataMessageContent;
import com.debangis.messages.SRMAcknowledgement;
import com.debangis.messages.SRMAcknowledgementContent;

public class LoopDataProcessing extends AbstractApplication<TrafficManagementCenterOperatingSystem> implements TrafficManagementCenterApplication, CommunicationApplication  {

	//double northApproachLeft, northApproachStraight, northApproachRight, southApproachLeft, southApproachStraight, southApproachRight, westApproachLeft, westApproachStraight, westApproachRight, eastApproachLeft, eastApproachStraight, eastApproachRight;
	String trfData = "";
	  @Override
	  public void onInductionLoopUpdated(Collection<InductionLoop> updatedInductionLoops) {
	    // is called whenever an induction loop has new values
		  
		  //EAST APPROACH
		  InductionLoop detector = getOs().getInductionLoop("eastApproachLoop2");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double eastApproachLeft = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  detector = getOs().getInductionLoop("eastApproachLoop1");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double eastApproachStraight = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  detector = getOs().getInductionLoop("eastApproachLoop0");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double eastApproachRight = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  

		  //WEST APPROACH
		  detector = getOs().getInductionLoop("westApproachLoop2");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double westApproachLeft = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  detector = getOs().getInductionLoop("westApproachLoop1");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double westApproachStraight = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  detector = getOs().getInductionLoop("westApproachLoop0");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double westApproachRight = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates


		  //NORTH APPROACH
		  detector = getOs().getInductionLoop("northApproachLoop2");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double northApproachLeft = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  detector = getOs().getInductionLoop("northApproachLoop1");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double northApproachStraight = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  detector = getOs().getInductionLoop("northApproachLoop0");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double northApproachRight = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  


		  //SOUTH APPROACH
		  detector = getOs().getInductionLoop("southApproachLoop2");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double southApproachLeft = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  detector = getOs().getInductionLoop("southApproachLoop1");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double southApproachStraight = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  detector = getOs().getInductionLoop("southApproachLoop0");
		  //double avgSpeed = detector.getAverageSpeedMs();
		  double southApproachRight = detector.getTrafficFlowVehPerHour(); // in veh/h aggregated over the last 1500 updates
		  
		  trfData =  northApproachLeft + ", " + northApproachStraight + ", " +  northApproachRight + ", " +  southApproachLeft + ", " + southApproachStraight + ", " +  southApproachRight + ", " +  westApproachLeft + ", " +  westApproachStraight + ", " +  westApproachRight + ", " +  eastApproachLeft + ", " +  eastApproachStraight + ", " +  eastApproachRight;
	  }

	 

	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStartup() {
		// TODO Auto-generated method stub
		getLog().infoSimTime((OperatingSystemAccess)this, "Initialize application", new Object[0]);
		((TrafficManagementCenterOperatingSystem)getOs()).getAdHocModule().enable();
	    getLog().infoSimTime((OperatingSystemAccess)this, "Activated Wifi Module", new Object[0]);

	    //*/Schedule the start of sending traffic data
	    Event event = new Event(getOs().getSimulationTime() + 3000000000L, this::sendTrafficData);
		getOs().getEventManager().addEvent(event);
		  
	}

	public void sendTrafficData(Event event) {
		
		LoopDataMessageContent ldmCnt = new LoopDataMessageContent(getOs().getSimulationTime(), trfData );
	    final MessageRouting adhocRouting = getOperatingSystem()
	          .getAdHocModule()
	          .createMessageRouting()
	          .topoBroadCast();
	    LoopDataMessage adhocldm = new LoopDataMessage(adhocRouting,ldmCnt, 200L);
	    getOs().getAdHocModule().sendV2xMessage(adhocldm);
	    
	    //Schedule when to send the next data
	    event = new Event(getOs().getSimulationTime() + 3000000000L, this::sendTrafficData);
		getOs().getEventManager().addEvent(event);
	}
	@Override
	public void processEvent(Event arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onLaneAreaDetectorUpdated(Collection<LaneAreaDetector> arg0) {
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
	public void onMessageReceived(ReceivedV2xMessage arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}	

}
