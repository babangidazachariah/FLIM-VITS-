package com.debangis;

import org.apache.commons.lang3.Validate;
//import org.eclipse.mosaic.app.tutorial.message.GreenWaveMsg;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.geo.Point;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;

import com.debangis.FuzzyModel;
//import com.debangis.MyClass;

public final class TrafficLightsController extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
  public static final String SECRET = "open sesame!";
  
  private static final short GREEN_DURATION = 10;
  
  private static final String DEFAULT_PROGRAM = "1";
  
  private static final String GREEN_PROGRAM = "0";
  
  private static final Integer MIN_DISTANCE = Integer.valueOf(15);
	
  int lightSequence = 0;
  
  FuzzyModel fisModel = new FuzzyModel();
  //MyClass mycls = new MyClass();
  
  public void onStartup() {
    getLog().infoSimTime((OperatingSystemAccess)this, "Initialize application", new Object[0]);
    ((TrafficLightOperatingSystem)getOs()).getAdHocModule().enable();
    getLog().infoSimTime((OperatingSystemAccess)this, "Activated Wifi Module", new Object[0]);
    Event event = new Event(getOs().getSimulationTime() + 10000000000L, this::roundRobinControl);
    getOs().getEventManager().addEvent(event);
}
  
	
  public void roundRobinControl(Event event){
	  
	  String trafficLight = "";
	  String otpt = fisModel.EvaluateFIS(10.000, 20.000);
	  //String mcls = mycls.ReturnString();
	  getLog().infoSimTime(this, "Fuzzy Logic Output: " + otpt + " MYCLASS: " );
	  //while(true){ //Loop forever
		  if(lightSequence == 0){
			  trafficLight = "northsouthleft";
			  lightSequence += 1;
		  }else if(lightSequence == 1){
			  trafficLight = "northsouthstraightright";
			  lightSequence += 1;
		  }else if(lightSequence == 2){
			  trafficLight = "eastwestleft";
			  lightSequence += 1;
		  }else if(lightSequence == 3){
			  trafficLight = "eastweststraightright";
			  lightSequence = 0;
		  }
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(trafficLight);
		  long dur = ((TrafficLightOperatingSystem) getOs()).getSimulationTime() + 1000000L;
		  //while (((TrafficLightOperatingSystem) getOs()).getSimulationTime() < dur){

			getLog().infoSimTime(this, "Traffic Lights Assigned to " + trafficLight );
	  		event = new Event(getOs().getSimulationTime() + 10000000000L, this::roundRobinControl);
    		getOs().getEventManager().addEvent(event);

		  //}
		  
	  //}
  }
  
  public void onShutdown() {
    getLog().infoSimTime((OperatingSystemAccess)this, "Shutdown application", new Object[0]);
  }
  /*
  private void setGreen() {
    ((TrafficLightOperatingSystem)getOs()).switchToProgram("0");
    getLog().infoSimTime((OperatingSystemAccess)this, "Setting traffic lights to GREEN", new Object[0]);
    ((TrafficLightOperatingSystem)getOs()).getEventManager().addEvent(((TrafficLightOperatingSystem)
        getOs()).getSimulationTime() + 10000000000L, new EventProcessor[] { e -> setRed() });
  }
  
  private void setRed() {
    ((TrafficLightOperatingSystem)getOs()).switchToProgram("1");
    getLog().infoSimTime((OperatingSystemAccess)this, "Setting traffic lights to RED", new Object[0]);
  }
  */
  public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
  /*
    if (!(receivedV2xMessage.getMessage() instanceof GreenWaveMsg))
      return; 
    getLog().infoSimTime((OperatingSystemAccess)this, "Received GreenWaveMsg", new Object[0]);
    if (!((GreenWaveMsg)receivedV2xMessage.getMessage()).getMessage().equals("open sesame!"))
      return; 
    getLog().infoSimTime((OperatingSystemAccess)this, "Received correct passphrase: {}", new Object[] { "open sesame!" });
    Validate.notNull(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition(), "The source position of the sender cannot be null", new Object[0]);
    if (receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition()
      .distanceTo((Point)((TrafficLightOperatingSystem)getOs()).getPosition()) > MIN_DISTANCE.intValue()) {
      getLog().infoSimTime((OperatingSystemAccess)this, "Vehicle that sent message is too far away.", new Object[0]);
      return;
    } 
    if ("1".equals(((TrafficLightOperatingSystem)getOs()).getCurrentProgram().getProgramId()))
      setGreen(); 
     */
  }
  
  public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {}
  
  public void onCamBuilding(CamBuilder camBuilder) {}
  
  public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {}
  
  public void processEvent(Event event) throws Exception {}
  
}
