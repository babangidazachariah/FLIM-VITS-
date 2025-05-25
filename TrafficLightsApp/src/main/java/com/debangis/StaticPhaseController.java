package com.debangis;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.util.scheduling.Event;

public class StaticPhaseController extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {

	int lightSequence = 0;

	String[] greenPhaseSequence = {"northsouthleft", "northsouthstraightright", "eastwestleft", "eastweststraightright"};
	String[] yellowPhaseSequence = {"northsouthleftyellow", "northsouthstraightrightyellow", "eastwestleftyellow", "eastweststraightrightyellow"};
	String allRed = "allred";
	 int phasePointer = 1;
	 int phaseDuration = 25; 
  
	public StaticPhaseController() {
		// TODO Auto-generated constructor stub
	}
	
	public void roundRobinControl(Event event){
		  
		  String trafficLight = "";
		 
		  //while(true){ //Loop forever 
			  if(phasePointer == 0){
				  
				  phasePointer += 1;
			  }else if(phasePointer == 1){
				  
				  phasePointer += 1;
			  }else if(phasePointer == 2){
				  
				  phasePointer += 1;
			  }else if(phasePointer == 3){
				  
				  phasePointer = 0;
			  }
			  String phase = greenPhaseSequence[phasePointer];
			  ((TrafficLightOperatingSystem)getOs()).switchToProgram(phase);
			  long dur = ((TrafficLightOperatingSystem) getOs()).getSimulationTime() + 1000000L;
			  //while (((TrafficLightOperatingSystem) getOs()).getSimulationTime() < dur){

				getLog().infoSimTime(this, "Traffic Lights Assigned to " + trafficLight );
				Event setRedEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 2)* 1000000000L, this::setAllRedPhase);
				getOs().getEventManager().addEvent(setRedEvent);
				    
				    
				Event setYellowEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 5)* 1000000000L, this::setYellowPhase);
				getOs().getEventManager().addEvent(setYellowEvent);
				
				Event setGreenEvent = new Event(getOs().getSimulationTime() + (phaseDuration * 1000000000L), this::roundRobinControl);
				getOs().getEventManager().addEvent(setGreenEvent);
	  }
	public void onShutdown() {
		// TODO Auto-generated method stub
		getLog().info("Hello World! Traffic Light Controller Shuting down...");
	}

	
	public void onStartup() {
		// TODO Auto-generated method stub
		getLog().info("Hello World! Traffic Light Controller Starting...");
		Event setGreenEvent = new Event(getOs().getSimulationTime() + (1 * 1000000000L), this::roundRobinControl);
	    getOs().getEventManager().addEvent(setGreenEvent);
	    System.out.println("StaticPhaseController: " + phaseDuration);
	}


	  private void setAllRedPhase(Event event) {
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(allRed);
		  getLog().infoSimTime(this, "Traffic Lights Assigned to " + allRed );
	  }
	  

	  private void setYellowPhase(Event event) {
		//This function traffic lights 
		  String phase = yellowPhaseSequence[phasePointer];
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(phase);
		  getLog().infoSimTime(this, "Traffic Lights Assigned to " + phase );
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
	public void onMessageReceived(ReceivedV2xMessage arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}

	

}
