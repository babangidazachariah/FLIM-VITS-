package com.debangis;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import com.debangis.controllers.BenchmarkZachariahEtAl;
import com.debangis.controllers.FuzzyModel;
import com.debangis.controllers.QueueDependentController;
import com.debangis.controllers.QueueLengthDistanceSpeedWaitingTimeController;
import com.debangis.controllers.QueueLengthWaitingTimeDistanceController;
import com.debangis.messages.LoopDataMessage;
import com.debangis.messages.SRMAcknowledgement;
import com.debangis.messages.SRMAcknowledgementContent;
import com.debangis.messages.SignalRequestMessage;

public class LoopandV2IIntersectionController extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
	  
	
	  int lightSequence = 0;
	  
	  FuzzyModel fisModel;// = new FuzzyModel();
	  QueueLengthDistanceSpeedWaitingTimeController qdswFisModel;
	  QueueLengthWaitingTimeDistanceController qwdFisModel;
	  BenchmarkZachariahEtAl bzFisModel;
	  QueueDependentController queueDependent;
	  //CHOOSE CONTROLLER TO BE USED using the declaration above
	  String chosenController = "queueDependent";
	  
	  int minPhaseDuration = 7;
	  int maxPhaseDuration = 25;
	  int phaseDuration = 9; //Temp value (15) for testing, set to 1
	  int nextPhaseDuration = 0;
	  String nextPhase;
	  String currentPhase;
	  int phasePointer = 1; 
	  long startTime = 0;
	  
	  String[] greenPhaseSequence = {"northsouthleft", "northsouthstraightright", "eastwestleft", "eastweststraightright"};
	  String[] yellowPhaseSequence = {"northsouthleftyellow", "northsouthstraightrightyellow", "eastwestleftyellow", "eastweststraightrightyellow"};
	  String allRed = "allred";
	  //String trfData =  northApproachLeft + ", " + northApproachStraight + ", " +  northApproachRight + ", " +  southApproachLeft + ", " + southApproachStraight + ", " +  southApproachRight + ", " +  westApproachLeft + ", " +  westApproachStraight + ", " +  westApproachRight + ", " +  eastApproachLeft + ", " +  eastApproachStraight + ", " +  eastApproachRight;
	  double[] trfData = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	  
	  //Radius of the Earth in kilometers
	  private static final double EARTH_RADIUS = 6371; // in kilometers
	  private GeoPoint intersectionLocation;
	  String junctionID = "Junction";
	  GeoPoint junctionPoint;
		
	  //Fuzzy Logic input parameters
	  long maxWaitingTime  = 0;
	  float maxSpeed = 0, maxDistance = 0;
	  int maxQueueLength = 0;
	  
	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onStartup() {
		// TODO Auto-generated method stub
		// This for Adhoc network Communication. 
	    getLog().infoSimTime((OperatingSystemAccess)this, "Initialize application", new Object[0]);
	    ((TrafficLightOperatingSystem)getOs()).getAdHocModule().enable();
	    getLog().infoSimTime((OperatingSystemAccess)this, "Activated Wifi Module", new Object[0]);
	    //*/
		  
	    Event setGreenEvent = new Event(getOs().getSimulationTime() + (phaseDuration * 1000000000L), this::setGreenPhase);
	    getOs().getEventManager().addEvent(setGreenEvent);
	    Event event = new Event(getOs().getSimulationTime() + 3000000000L, this::initializeParameters);
		getOs().getEventManager().addEvent(event);
	}
	
	private void initializeParameters(Event event) {
		 
		 intersectionLocation = GeoPoint.latLon(53.547745, 9.966246);
		 fisModel = new FuzzyModel();
		 qdswFisModel = new QueueLengthDistanceSpeedWaitingTimeController();
		 qwdFisModel = new QueueLengthWaitingTimeDistanceController();
		 bzFisModel = new BenchmarkZachariahEtAl();
		 queueDependent = new QueueDependentController();
		 
		 System.out.println("initializeParameters Done");
		 
	 }
	
	 private void setNextSchedule(Event event) {
		  
		 
	 }
	 
	 private void setYelloRedPhasesTime() {
		  /*/This function determines if current phase equals next phase, 
		   * it ensures that traffic lights does not change to yellow and all red phase
		   * It is called from the setNextSchedule() 
		   */
		  if (!(currentPhase.equals(nextPhase))) {
			  Event setRedEvent = new Event(startTime + (phaseDuration - 1)* 1000000000L, this::setAllRedPhase);
			  getOs().getEventManager().addEvent(setRedEvent);
			    
			    
			  Event setYellowEvent = new Event(startTime + (phaseDuration - 3)* 1000000000L, this::setYellowPhase);
			  getOs().getEventManager().addEvent(setYellowEvent);
		  }
	 }
	 

		
	  private ArrayPair getConcurrentFlows(String appLeg, String deptLeg) {
		  //"northsouthleft", "northsouthstraightright", "eastwestleft", "eastweststraightright"
		  //This function sets the nextPhase program name and returns arrays of concurrent flows for the next phase
		  
		  String[] approaches= new String[4];
		  String[] departs = new String[4];
		  ArrayPair arrayPair = new ArrayPair();
		  
		  if ((appLeg.equals("northApproach")) && (deptLeg.equals("westDepart"))) {
				nextPhase = "northsouthstraightright";
				approaches[0] = "northApproach";
				  approaches[1] = "northApproach";
				  approaches[2] = "southApproach";
				  approaches[3] = "southApproach";
				  departs[0] = "southDepart";
				  departs[1] = "westDepart";
				  departs[2] = "northDepart";
				  departs[3] = "eastDepart";
				  
			}else if ((appLeg.equals("northApproach")) && (deptLeg.equals("southDepart"))) {
				nextPhase = "northsouthstraightright";
				approaches[0] = "northApproach";
				  approaches[1] = "northApproach";
				  approaches[2] = "southApproach";
				  approaches[3] = "southApproach";
				  departs[0] = "southDepart";
				  departs[1] = "westDepart";
				  departs[2] = "northDepart";
				  departs[3] = "eastDepart";
				  
			}else if ((appLeg.equals("northApproach")) && (deptLeg.equals("eastDepart"))) {
				nextPhase = "northsouthleft";
				
				approaches = new String[2];
				  departs = new String[2];
				approaches[0] = "northApproach";
				  approaches[1] = "southApproach";
				  departs[0] = "eastDepart";
				  departs[1] = "westDepart";
				  
			}else if ((appLeg.equals("southApproach")) && (deptLeg.equals("northDepart"))) {
				nextPhase = "northsouthstraightright";
				approaches[0] = "northApproach";
				  approaches[1] = "northApproach";
				  approaches[2] = "southApproach";
				  approaches[3] = "southApproach";
				  departs[0] = "southDepart";
				  departs[1] = "westDepart";
				  departs[2] = "northDepart";
				  departs[3] = "eastDepart";
				  
			}else if ((appLeg.equals("southApproach")) && (deptLeg.equals("westDepart"))) {
				nextPhase = "northsouthleft";
				
				approaches = new String[2];
				  departs = new String[2];
				approaches[0] = "northApproach";
				  approaches[1] = "southApproach";
				  departs[0] = "eastDepart";
				  departs[1] = "westDepart";
				  
			}else if ((appLeg.equals("southApproach")) && (deptLeg.equals("eastDepart"))) {
				nextPhase = "northsouthstraightright";
				approaches[0] = "northApproach";
				  approaches[1] = "northApproach";
				  approaches[2] = "southApproach";
				  approaches[3] = "southApproach";
				  departs[0] = "southDepart";
				  departs[1] = "westDepart";
				  departs[2] = "northDepart";
				  departs[3] = "eastDepart";
				
			}else if ((appLeg.equals("westApproach")) && (deptLeg.equals("northDepart"))) {
				nextPhase = "eastwestleft";

				  approaches = new String[2];
				  departs = new String[2];
				  approaches[0] = "eastApproach";
				  approaches[1] = "westApproach";
				  departs[0] = "southDepart";
				  departs[1] = "northDepart";
				  
			}else if ((appLeg.equals("westApproach")) && (deptLeg.equals("eastDepart"))) {
				nextPhase = "eastweststraightright";
				approaches[0] = "eastApproach";
				  approaches[1] = "eastApproach";
				  approaches[2] = "westApproach";
				  approaches[3] = "westApproach";
				  departs[0] = "westDepart";
				  departs[1] = "northDepart";
				  departs[2] = "southDepart";
				  departs[3] = "eastDepart";
				  
			}else if ((appLeg.equals("westApproach")) && (deptLeg.equals("southDepart"))) {
				nextPhase = "eastweststraightright";
				approaches[0] = "eastApproach";
				  approaches[1] = "eastApproach";
				  approaches[2] = "westApproach";
				  approaches[3] = "westApproach";
				  departs[0] = "westDepart";
				  departs[1] = "northDepart";
				  departs[2] = "southDepart";
				  departs[3] = "eastDepart";
				
			}else if ((appLeg.equals("eastApproach")) && (deptLeg.equals("northDepart"))) {
				nextPhase = "eastweststraightright";
				approaches[0] = "eastApproach";
				  approaches[1] = "eastApproach";
				  approaches[2] = "westApproach";
				  approaches[3] = "westApproach";
				  departs[0] = "westDepart";
				  departs[1] = "northDepart";
				  departs[2] = "southDepart";
				  departs[3] = "eastDepart";
				  
			}else if ((appLeg.equals("eastApproach")) && (deptLeg.equals("southDepart"))) {
				nextPhase = "eastwestleft";

				  approaches = new String[2];
				  departs = new String[2];
				  approaches[0] = "eastApproach";
				  approaches[1] = "westApproach";
				  departs[0] = "southDepart";
				  departs[1] = "northDepart";
				  
			}else if ((appLeg.equals("eastApproach")) && (deptLeg.equals("westDepart"))) {
				nextPhase = "eastweststraightright";

				approaches[0] = "eastApproach";
				  approaches[1] = "eastApproach";
				  approaches[2] = "westApproach";
				  approaches[3] = "westApproach";
				  departs[0] = "westDepart";
				  departs[1] = "northDepart";
				  departs[2] = "southDepart";
				  departs[3] = "eastDepart";
				  
			}
		  //System.out.println("SETTING NEXT PHASE Program TO: " + nextPhase);
		  return arrayPair.makeArrayPair(approaches, departs);
	  }
	  
	  
	  
	//Convert degrees to radians
	  private static double toRadians(double degrees) {
	      return degrees * Math.PI / 180.0;
	  }
	  
		private float DistanceToIntersection(float lat2, float lon2) {
			/*
			 * Calculates distance of a vehicle, given its GeoPoint coordinate, from the
			 * intersection whose coordinates Coordinate of intersection: 53.547718, 9.966288
			 */
			float lat1 = (float) intersectionLocation.getLatitude();
			float lon1 = (float) intersectionLocation.getLongitude();
			
			
			
			double dLat = toRadians(lat2 - lat1);
	      double dLon = toRadians(lon2 - lon1);
	      double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
	      double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	      return (float) (EARTH_RADIUS * c);
	      
			//return (float)(Math.sqrt(Math.pow((vehPos.getLatitude() - intersectionLocation.getLatitude()),2) + Math.pow((vehPos.getLongitude() - intersectionLocation.getLongitude()),2)));
			
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
	  
	  private void setPhasePointer(String phase) {
		  for (int i = 0; i < greenPhaseSequence.length; i++) {
			  if (phase == greenPhaseSequence[i]) {
				  phasePointer = i;
				  break;
			  }
		  }
		  
	  }
	  private void setGreenPhase(Event event) {
		  //This function traffic lights 
		  if (nextPhaseDuration != 0 ) {
			  
			  phaseDuration = nextPhaseDuration;
		  }else {
			  currentPhase = greenPhaseSequence[phasePointer];
			  phaseDuration = 9;
		  }
		  if (nextPhase == null) {
			  currentPhase = greenPhaseSequence[phasePointer];
		  }else {
			  currentPhase = nextPhase;
		  }
		  
		  System.out.println("SETTING GREEN PHASE PROGRAM WITH: "+ nextPhase + " PD " + phaseDuration);
		  setPhasePointer(currentPhase);
		  String phase = greenPhaseSequence[phasePointer];
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(phase);
		  getLog().infoSimTime(this, "Traffic Lights Assigned to " + phase );
		  //event = new Event(getOs().getSimulationTime() + (phaseDuration * 1000000000L), this::roundRobinControl);10 000 000 000L
		  //getOs().getEventManager().addEvent(event);
		  startTime = getOs().getSimulationTime();
		  Event setNextScheduleEvent = new Event(startTime  + (phaseDuration - 5)* 1000000000L, this::setNextSchedule);
		  getOs().getEventManager().addEvent(setNextScheduleEvent);
		  
		  Event setGreenEvent = new Event(startTime + (phaseDuration * 1000000000L), this::setGreenPhase);
		  getOs().getEventManager().addEvent(setGreenEvent);
		    
	  }
	  
	  public void roundRobinControl(){
		  
		  String trafficLight = "";
		  
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
		  	  phasePointer = lightSequence;
			  //}
			  
		  //}
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
	
	public static double[] convertToDoubleArray(String trfData) {
        // Split the concatenated string by ", " to get individual string values
        String[] stringValues = trfData.split(", ");
        
        // Create a double array to store the converted values
        double[] doubleValues = new double[stringValues.length];
        
        // Convert each string value to a double and store it in the double array
        for (int i = 0; i < stringValues.length; i++) {
            doubleValues[i] = Double.parseDouble(stringValues[i]);
        }
        
        return doubleValues;
    }
	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		  
		  
	  	getLog().info("Adhoc SRM Message is received");
		//*
		V2xMessage msg = receivedV2xMessage.getMessage();
	    if (!(msg instanceof LoopDataMessage)) {
	    	getLog().infoSimTime((OperatingSystemAccess)this, "Ignoring message of type: {}", new Object[] { msg.getSimpleClassName() });
	    	return;
	    }
	    LoopDataMessage ldm = (LoopDataMessage) msg;
	    String ldmTrfData = ldm.getTrafficData();
	    getLog().info("TimeStamp: {}  Traffic Data: {}", ldm.getTimeStamp(), ldmTrfData);
	    //LoopDataMessage from LoopDataProcessing RSU is not acknowledge since it sends updates at regular intervals.
	    trfData = convertToDoubleArray(ldmTrfData);
	    
  }
  
	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}

}
