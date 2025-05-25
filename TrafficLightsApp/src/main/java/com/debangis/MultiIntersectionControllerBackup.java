package com.debangis;
/*
 * Copyright: ©2024
 * Author: Babangida Zachariah
 * @ Ontario Tech University
 * Backup on 3rd June, 2024
 */
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
import com.debangis.messages.SRMAcknowledgement;
import com.debangis.messages.SRMAcknowledgementContent;
import com.debangis.messages.SignalRequestMessage;

import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.Table;

public class MultiIntersectionControllerBackup extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {

	
	int lightSequence = 0;
	  
	  FuzzyModel fisModel;// = new FuzzyModel();
	  QueueLengthDistanceSpeedWaitingTimeController qdswFisModel;
	  QueueLengthWaitingTimeDistanceController qwdFisModel;
	  BenchmarkZachariahEtAl bzFisModel;
	  QueueDependentController queueDependent;
	  //CHOOSE CONTROLLER TO BE USED using the declaration above
	  String chosenController = "queueDependent";
	  
	  Table adhocTrafficData;
	  Table cellularTrafficData;
	  //AdhocTrafficRecordingServer atrcdng = new AdhocTrafficRecordingServer();
	  //CellularTrafficRecordingServer ctrcdng = new CellularTrafficRecordingServer();
	  //TrafficRecording trcdng = new TrafficRecording();
	  
	  //Table trafficData = trcdng.CreateTrafficRecording("trafficData");
	  
	  int minPhaseDuration = 7;
	  int maxPhaseDuration = 25;
	  int phaseDuration = 9; //Temp value (15) for testing, set to 1
	  int nextPhaseDuration = 0;

	  String tlsControllerName;
	  String nextPhase;
	  String currentPhase;
	  int phasePointer = 1; 
	  long startTime = 0;
	  
	  String[] greenPhaseSequence = {"northSouthLeft", "northSouthStraightRight", "eastWestLeft", "eastWestStraightRight"};
	  String[] yellowPhaseSequence = {"northSouthLeftYellow", "northSouthStraightRightYellow", "eastWestLeftYellow", "eastWestStraightRightYellow"};
	  String allRed = "allRed";
	  
	  //Traffic Recording Variables
	  TrafficRecording trfRcdng;
	  Table adhocTrafficRecords;
	  
	  //Radius of the Earth in kilometers
	  private static final double EARTH_RADIUS = 6371; // in kilometers
	  private GeoPoint intersectionLocation;
		
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
		 //using lambda expression to define a specific method for processing this event
        //this.getOs().getEventManager().newEvent(getOs().getSimulationTime() + 2 * TIME.SECOND, this::initializeParameters)
        //        .withResource("A message for my specific method!")
        //        .schedule();
        
        getLog().infoSimTime((OperatingSystemAccess)this, "Initialize application", new Object[0]);
        ((TrafficLightOperatingSystem)getOs()).getAdHocModule().enable();
        Event setGreenEvent = new Event(getOs().getSimulationTime() + (2 * 1000000000L), this::setGreenPhase);
        getOs().getEventManager().addEvent(setGreenEvent);
        Event event = new Event(getOs().getSimulationTime() + 3000000000L, this::initializeParameters);
    	getOs().getEventManager().addEvent(event);
    	//nextPhase = "northSouthLeft";
    	//currentPhase = "eastWestStraightRight";
    	//phasePointer = 0;
    	//setYelloRedPhasesTime();
        
	}

	private void initializeParameters(Event event) {
		/*
		//Event for each intersection should have:
		 * 
		 * junctionID: used to retrieve list of vehicles waiting at the intersection
		 * phaseDuration: Default/minimum phase duration  or a newly computed phase duration
		 */
		//  ((TrafficLightOperatingSystem)getOs()).switchToProgram(phase);
		//getOs().getAllTrafficLights(): Retrieves  all traffic lights in the network
		
		tlsControllerName = getOs().getGroup(); //Set this controller's name
		trfRcdng = new TrafficRecording();
		 adhocTrafficRecords = trfRcdng.CreateTrafficRecording("adhocTrafficRecords");
		 intersectionLocation = GeoPoint.latLon(53.547745, 9.966246);
		 fisModel = new FuzzyModel();
		 qdswFisModel = new QueueLengthDistanceSpeedWaitingTimeController();
		 qwdFisModel = new QueueLengthWaitingTimeDistanceController();
		 bzFisModel = new BenchmarkZachariahEtAl();
		 queueDependent = new QueueDependentController();
		 setYelloRedPhasesTime();
		 System.out.println("initializeParameters Done");
		 
		/*
		if (event.getResource() instanceof String) {
            String message = (String)event.getResource();
            System.out.println("Received message: "  + message);
        }
		final List<TrafficLight> trafficLights = getOs().getTrafficLightGroup().getTrafficLights();
		final Collection<TrafficLight> trafficControllers = getOs().getAllTrafficLights();
		//System.out.println(getOs().getAllPrograms().toArray());
		final Object[] trafficLightControllers = getOs().getAllPrograms().toArray();
		System.out.println("THIS TRAFFIC LIGHT IS:" + getOs().getId());
		System.out.println("GROUP IS:" + getOs().getGroup());
		/*
		for (int i = 0; i < trafficLightControllers.length; i++) {
			System.out.println(trafficLightControllers[i].toString());
			
		}
		
	
		Simcoe-Taunton: 53.546101, 9.965058
		 * Simcoe-Rossland: 53.546152, 9.968502
		 * Rossland-Mary: 53.547693, 9.968449
		 * Taunton-Mary: 53.547767, 9.965096
		 * 
		for (TrafficLight trafficLight : trafficLights) {
	         getLog().infoSimTime(this, "Traffic light: {}", trafficLight.toString());
		}
		
		for (TrafficLight trafficLight : trafficControllers) {
		    // Process each traffic light
		    // For example:
		    System.out.println("Traffic light ID: " + trafficLight.getId());
		    System.out.println("Traffic light : " + trafficLight.toString());
		    // Add more processing as needed
		}
		//*/
	
	}

	  private void setNextSchedule(Event event) {
		  
		  //Set next phase
		  /*
		  adhocTrafficData = atrcdng.getAhocTrafficRecords();
		  cellularTrafficData = ctrcdng.getCellularTrafficRecords();
		  trafficData = trcdng.Append(cellularTrafficData, adhocTrafficData);
		  
		  
		  roundRobinControl();
		  */
		  if (adhocTrafficRecords.rowCount() > 0) {
			  
			  Table tmpRecord = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("junctionid").isEqualTo(tlsControllerName));
			  
			  if(tmpRecord.rowCount() > 0) {

				  //System.out.println("TABLE DETAILS " + tlsControllerName);
				  //System.out.println(tmpRecord.printAll());
				  
				  tmpRecord = tmpRecord.sortAscendingOn("arrivaltime");
				  maxWaitingTime  = 0;
				  maxSpeed = 0;
				  maxDistance = 0;
				  maxQueueLength = 0;
				  //Get approach with a vehicle that has the earliest arrival
				  LongColumn phaseVehArrival = tmpRecord.longColumn("arrivaltime");
				  //Earliest arriving time
				  maxWaitingTime = (getOs().getSimulationTime() - (long)phaseVehArrival.min());
				  //System.out.println("MAX Waiting Time = " + maxWaitingTime);
				  
				  Table vehRec = tmpRecord.where(phaseVehArrival.isEqualTo(phaseVehArrival.min()));
				  //System.out.println("Veh With Max Waiting: " + vehRec.printAll());
				  String appLeg = vehRec.stringColumn("approachleg").getString(0);
				  
				  String deptLeg = vehRec.stringColumn("departleg").getString(0);
				  //System.out.println("Veh With Max Waiting: " + vehRec.print(1));
				  
				  //nextPhase = getNextPhaseProgram(appLeg, deptLeg);
				  //System.out.println("APPROACH: " + appLeg + " NEXT PHASE " + nextPhase);
				  ArrayPair concFlows = getConcurrentFlows(appLeg, deptLeg);
				  String[] approaches = concFlows.getFirstArray();
				  String[] departs = concFlows.getSecondArray();
				  for (int i = 0; i < approaches.length; i++) {
					  setNextPahseFuzzyInputValues(approaches[i], departs[i]);
				  }
				  //double wT, double qL, double sP, double dS
				  maxWaitingTime = (maxWaitingTime/1000000000L);
				  maxDistance = (maxDistance*100);
				  maxSpeed = maxSpeed * 3.6f;
				  
				  if (chosenController.equals("fisModel")){
					  double[] fisOutput = fisModel.EvaluateFIS((double)(maxWaitingTime), (double)maxQueueLength,  (double)maxSpeed, (double)maxDistance);
					  nextPhaseDuration = (int) fisOutput[0];
					  
				  }else if(chosenController.equals("qdswFisModel")){
					  double[] fisOutput = qdswFisModel.EvaluateFIS((double)(maxWaitingTime), (double)maxQueueLength,  (double)maxSpeed, (double)maxDistance);
					  nextPhaseDuration = (int) fisOutput[0];
					  
				  }else if(chosenController.equals("qwdFisModel")){
					  
					  double[] fisOutput = qwdFisModel.EvaluateFIS((double)(maxWaitingTime), (double)maxQueueLength,  (double)maxSpeed, (double)maxDistance);
					  nextPhaseDuration = (int) fisOutput[0];
					  
				  }else if(chosenController.equals("bzFisModel")){
					  
					  double[] fisOutput = bzFisModel.EvaluateFIS((double)(maxWaitingTime), (double)maxQueueLength,  (double)maxSpeed, (double)maxDistance);
					  nextPhaseDuration = (int) fisOutput[0];
				  }else if(chosenController.equals("queueDependent")){
			  
					  double[] fisOutput = queueDependent.EvaluateFIS((double)(maxWaitingTime), (double)maxQueueLength,  (double)maxSpeed, (double)maxDistance);
					  nextPhaseDuration = (int) fisOutput[0];
				  }
			  }else {
				  roundRobinControl();
			  }
			  
			  //nextPhaseDuration += 5;
			  //System.out.println("NEXT PHASE Program is: " + nextPhase + " WT: " + maxWaitingTime + " QL: " + maxQueueLength + " SPD: " + maxSpeed + " DST: " + maxDistance + " PD: " + nextPhaseDuration);
		  }else {
			  roundRobinControl();
		  }
		  //if nextPhase is same currentPhase, no need to call this function, setYelloRedPhasesTime(), so that the phase continue with interruption
		  setYelloRedPhasesTime();
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
	 
	  private void setNextPahseFuzzyInputValues(String appLeg, String deptLeg) {
		  FloatColumn phaseMaxVehSpeed = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(appLeg)
					.and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(deptLeg))).floatColumn("speed");
			float mxSpd = (float)phaseMaxVehSpeed.max();
			if (maxSpeed < mxSpd) {
				maxSpeed = mxSpd;
			}
			
		  Table phaseVeh = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(appLeg)
					.and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(deptLeg)));
		  int vehCount = phaseVeh.rowCount();
		  if (maxQueueLength < vehCount) {
			  maxQueueLength = vehCount;
		  }
		  
		  float lat, lon, dst;
		  for (int i = 0; i < phaseVeh.rowCount(); i++) {
			  lat = phaseVeh.floatColumn("latitude").get(i);
			  lon = phaseVeh.floatColumn("longitude").get(i);
			  dst = DistanceToIntersection(lat, lon);
			  if (dst > maxDistance) {
				  maxDistance = dst;
			  }
		  }
		  //return maxDistance;
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
			  nextPhase = currentPhase;
		  }else {
			  currentPhase = nextPhase;
		  }
		  
		  //System.out.println(tlsControllerName + " SETTING GREEN PHASE PROGRAM WITH: "+ nextPhase + " PD " + phaseDuration);
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
				  trafficLight = "northSouthLeft";
				  lightSequence += 1;
			  }else if(lightSequence == 1){
				  trafficLight = "northSouthStraightRight";
				  lightSequence += 1;
			  }else if(lightSequence == 2){
				  trafficLight = "eastWestLeft";
				  lightSequence += 1;
			  }else if(lightSequence == 3){
				  trafficLight = "eastWestStraightRight";
				  lightSequence = 0;
			  }
			  
			  //((TrafficLightOperatingSystem)getOs()).switchToProgram(trafficLight);
			  long dur = ((TrafficLightOperatingSystem) getOs()).getSimulationTime() + 1000000L;
			  //while (((TrafficLightOperatingSystem) getOs()).getSimulationTime() < dur){

			  getLog().infoSimTime(this, "Traffic Lights Assigned to " + trafficLight );
			  nextPhase = trafficLight;
		  	  //phasePointer = lightSequence;
			  //}
			  
		  //}
	  }
	  

	  private ArrayPair getConcurrentFlows(String appLeg, String deptLeg) {
		  //"northSouthLeft", "northSouthStraightRight", "eastWestLeft", "eastWestStraightRight"
		  //This function sets the nextPhase program name and returns arrays of concurrent flows for the next phase
		  
		  String[] approaches= new String[4];
		  String[] departs = new String[4];
		  ArrayPair arrayPair = new ArrayPair();
		  
		  //SIMCOE-TAUNTON:  SimcoeTauntonJunction
		  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
				if (((appLeg.equals("TauntonSimcoeSouthApproach")) && (deptLeg.equals("SimcoeTauntonNorthDepart"))) || ((appLeg.equals("TauntonSimcoeSouthApproach")) && (deptLeg.equals("SimcoeTautonEastDepart"))) || ((appLeg.equals("TauntonSimcoeNorthApproach")) && (deptLeg.equals("SimcoeTauntonWestDepart"))) || ((appLeg.equals("TauntonSimcoeNorthApproach")) && (deptLeg.equals("SimcoeTauntonSouthDepart")))) { //NORTH-SOUTH : Right turn or Straight-Through
					nextPhase = "northSouthStraightRight";
					approaches[0] = "TauntonSimcoeNorthApproach";
					  approaches[1] = "TauntonSimcoeNorthApproach";
					  approaches[2] = "TauntonSimcoeSouthApproach";
					  approaches[3] = "TauntonSimcoeSouthApproach";
					  departs[0] = "SimcoeTauntonSouthDepart";
					  departs[1] = "SimcoeTauntonWestDepart";
					  departs[2] = "SimcoeTauntonNorthDepart";
					  departs[3] = "SimcoeTautonEastDepart";
					  
					  
					  
				}else if (((appLeg.equals("TauntonSimcoeNorthApproach")) && (deptLeg.equals("SimcoeTautonEastDepart"))) || ((appLeg.equals("TauntonSimcoeSouthApproach")) && (deptLeg.equals("SimcoeTauntonWestDepart")))) { //NORTH-SOUTH : Left-Turn
					nextPhase = "northSouthLeft";
					
					
					approaches[0] = "TauntonSimcoeNorthApproach";
					approaches[1] = "TauntonSimcoeSouthApproach";
					approaches[2] = "SimcoeTautonEastApproach";
					approaches[3] = "SimcoeTautonWestApproach";
					
					departs[0] = "SimcoeTautonEastDepart";
					departs[1] = "SimcoeTauntonWestDepart";
					departs[2] = "SimcoeTauntonNorthDepart";
					departs[3] = "SimcoeTauntonSouthDepart";
					
					
					
					
				}else if (((appLeg.equals("SimcoeTautonEastApproach")) && (deptLeg.equals("SimcoeTauntonNorthDepart"))) || ((appLeg.equals("SimcoeTautonEastApproach")) && (deptLeg.equals("SimcoeTauntonWestDepart"))) || ((appLeg.equals("SimcoeTauntonWestApproach")) && (deptLeg.equals("SimcoeTauntonSouthDepart"))) || ((appLeg.equals("SimcoeTauntonWestApproach")) && (deptLeg.equals("SimcoeTautonEastDepart")))) { //WEST-EAST : Right turn or Straight-Through
					nextPhase = "eastWestStraightRight";
					approaches[0] = "SimcoeTautonEastApproach";
					  approaches[1] = "SimcoeTautonEastApproach";
					  approaches[2] = "SimcoeTauntonWestApproach";
					  approaches[3] = "SimcoeTauntonWestApproach";
					  departs[0] = "SimcoeTauntonNorthDepart";
					  departs[1] = "SimcoeTauntonWestDepart";
					  departs[2] = "SimcoeTauntonSouthDepart";
					  departs[3] = "SimcoeTautonEastDepart";
					  
					  //{"northSouthLeft", "northSouthStraightRight", "eastWestLeft", "eastWestStraightRight"};
				}else if (((appLeg.equals("SimcoeTautonEastApproach")) && (deptLeg.equals("SimcoeTauntonSouthDepart"))) || ((appLeg.equals("SimcoeTauntonWestApproach")) && (deptLeg.equals("SimcoeTauntonNorthDepart")))) { //WEST-EAST : Left-Turn
					nextPhase = "eastWestLeft";
					
					
					
					approaches[0] = "SimcoeTautonEastApproach";
					approaches[1] = "SimcoeTauntonWestApproach";
					approaches[2] = "TauntonSimcoeNorthApproach";
					approaches[3] = "TauntonSimcoeSouthApproach";
					
					departs[0] = "SimcoeTauntonSouthDepart";
					departs[1] = "SimcoeTauntonNorthDepart";
					departs[2] = "SimcoeTauntonWestDepart";
					departs[3] = "SimcoeTautonEastDepart";
					  
					
					  
				}
			
			
			
		  }else if (tlsControllerName.equals("SimcoeRosslandJunction")) {
			//SIMCOE-ROSSLAND: SimcoeRosslandJunction

				if (((appLeg.equals("RosslandSimcoeSouthApproach")) && (deptLeg.equals("SimcoeRosslandNorthDepart"))) || ((appLeg.equals("RosslandSimcoeSouthApproach")) && (deptLeg.equals("RosslandSimcoeEastDepart"))) || ((appLeg.equals("RosslandSimcoeNorthApproach")) && (deptLeg.equals("SimcoeRosslandWestDepart"))) || ((appLeg.equals("RosslandSimcoeNorthApproach")) && (deptLeg.equals("SimcoeRosslandSouthDepart")))) { //NORTH-SOUTH : Right turn or Straight-Through
					
					nextPhase = "northSouthStraightRight";
					approaches[0] = "RosslandSimcoeNorthApproach";
					  approaches[1] = "RosslandSimcoeNorthApproach";
					  approaches[2] = "RosslandSimcoeSouthApproach";
					  approaches[3] = "RosslandSimcoeSouthApproach";
					
					  departs[0] = "SimcoeRosslandSouthDepart";
					  departs[1] = "SimcoeRosslandWestDepart";
					  departs[2] = "SimcoeRosslandNorthDepart";
					  departs[3] = "RosslandSimcoeEastDepart";
					  
				
					  
				}else if (((appLeg.equals("RosslandSimcoeNorthApproach")) && (deptLeg.equals("RosslandSimcoeEastDepart"))) || ((appLeg.equals("RosslandSimcoeSouthApproach")) && (deptLeg.equals("SimcoeRosslandWestDepart")))) { //NORTH-SOUTH : Left-Turn
					nextPhase = "northSouthLeft";
					
					
					approaches[0] = "RosslandSimcoeNorthApproach";
					approaches[1] = "RosslandSimcoeSouthApproach";
					approaches[2] = "SimcoeRosslandEastApproach";
					approaches[3] = "SimcoeRosslandWestApproach";
					
					departs[0] = "RosslandSimcoeEastDepart";
					departs[1] = "SimcoeRosslandWestDepart";
					departs[2] = "SimcoeRosslandNorthDepart";
					departs[3] = "SimcoeRosslandSouthDepart";
					
					
					
					
				}else if (((appLeg.equals("SimcoeRosslandEastApproach")) && (deptLeg.equals("SimcoeRosslandNorthDepart"))) || ((appLeg.equals("SimcoeRosslandEastApproach")) && (deptLeg.equals("SimcoeRosslandWestDepart"))) || ((appLeg.equals("SimcoeRosslandWestApproach")) && (deptLeg.equals("SimcoeRosslandSouthDepart"))) || ((appLeg.equals("SimcoeRosslandWestApproach")) && (deptLeg.equals("RosslandSimcoeEastDepart")))) { //WEST-EAST : Right turn or Straight-Through
					
					nextPhase = "eastWestStraightRight";
					approaches[0] = "SimcoeRosslandEastApproach";
					  approaches[1] = "SimcoeRosslandEastApproach";
					  approaches[2] = "SimcoeRosslandWestApproach";
					  approaches[3] = "SimcoeRosslandWestApproach";
					  departs[0] = "SimcoeRosslandNorthDepart";
					  departs[1] = "SimcoeRosslandWestDepart";
					  departs[2] = "SimcoeRosslandSouthDepart";
					  departs[3] = "RosslandSimcoeEastDepart";
					  
				
				}else if (((appLeg.equals("SimcoeRosslandEastApproach")) && (deptLeg.equals("SimcoeRosslandSouthDepart"))) || ((appLeg.equals("SimcoeRosslandWestApproach")) && (deptLeg.equals("SimcoeRosslandNorthDepart")))) { //WEST-EAST : Left-Turn
					nextPhase = "eastWestLeft";
					
					
					
					approaches[0] = "SimcoeRosslandEastApproach";
					approaches[1] = "SimcoeRosslandWestApproach";
					approaches[2] = "RosslandSimcoeNorthApproach";
					approaches[3] = "RosslandSimcoeSouthApproach";
					
					departs[0] = "SimcoeRosslandSouthDepart";
					departs[1] = "SimcoeRosslandNorthDepart";
					departs[2] = "SimcoeRosslandWestDepart";
					departs[3] = "RosslandSimcoeEastDepart";
					  
					
					  
				}
			
			
		  }else if (tlsControllerName.equals("MaryRosslandJuction")) {
			  //MARY-ROSSLAND: MaryRosslandJuction
		  
				if (((appLeg.equals("RosslandMarySouthApproach")) && (deptLeg.equals("MaryRosslandNorthDepart"))) || ((appLeg.equals("RosslandMarySouthApproach")) && (deptLeg.equals("RosslandMaryEastDepart"))) || ((appLeg.equals("RosslandMaryNorthApproach")) && (deptLeg.equals("RosslandMaryWestDepart"))) || ((appLeg.equals("RosslandMaryNorthApproach")) && (deptLeg.equals("RosslandMarySouthDepart")))) { //NORTH-SOUTH : Right turn or Straight-Through
					nextPhase = "northSouthStraightRight";
					approaches[0] = "RosslandMaryNorthApproach";
					  approaches[1] = "RosslandMaryNorthApproach";
					  approaches[2] = "RosslandMarySouthApproach";
					  approaches[3] = "RosslandMarySouthApproach";
					
					  departs[0] = "RosslandMarySouthDepart";
					  departs[1] = "RosslandMaryWestDepart";
					  departs[2] = "MaryRosslandNorthDepart";
					  departs[3] = "RosslandMaryEastDepart";
					  
				
					  
				}else if (((appLeg.equals("RosslandMaryNorthApproach")) && (deptLeg.equals("RosslandMaryEastDepart"))) || ((appLeg.equals("RosslandMarySouthApproach")) && (deptLeg.equals("RosslandMaryWestDepart")))) { //NORTH-SOUTH : Left-Turn
					nextPhase = "northSouthLeft";
					
					
					approaches[0] = "RosslandMaryNorthApproach";
					approaches[1] = "RosslandMarySouthApproach";
					approaches[2] = "MaryRosslandEastApproach";
					approaches[3] = "MaryRosslandWestApproach";
					
					departs[0] = "RosslandMaryEastDepart";
					departs[1] = "RosslandMaryWestDepart";
					departs[2] = "MaryRosslandNorthDepart";
					departs[3] = "RosslandMarySouthDepart";
					
					
					
					
				}else if (((appLeg.equals("MaryRosslandEastApproach")) && (deptLeg.equals("MaryRosslandNorthDepart"))) || ((appLeg.equals("MaryRosslandEastApproach")) && (deptLeg.equals("RosslandMaryWestDepart"))) || ((appLeg.equals("MaryRosslandWestApproach")) && (deptLeg.equals("RosslandMarySouthDepart"))) || ((appLeg.equals("MaryRosslandWestApproach")) && (deptLeg.equals("RosslandMaryEastDepart")))) { //WEST-EAST : Right turn or Straight-Through
					nextPhase = "eastWestStraightRight";
					approaches[0] = "MaryRosslandEastApproach";
					  approaches[1] = "MaryRosslandEastApproach";
					  approaches[2] = "MaryRosslandWestApproach";
					  approaches[3] = "MaryRosslandWestApproach";
					  departs[0] = "MaryRosslandNorthDepart";
					  departs[1] = "RosslandMaryWestDepart";
					  departs[2] = "RosslandMarySouthDepart";
					  departs[3] = "RosslandMaryEastDepart";
					  
				
				}else if (((appLeg.equals("MaryRosslandEastApproach")) && (deptLeg.equals("RosslandMarySouthDepart"))) || ((appLeg.equals("MaryRosslandWestApproach")) && (deptLeg.equals("MaryRosslandNorthDepart")))) { //WEST-EAST : Left-Turn
					nextPhase = "eastWestLeft";
					
					
					
					approaches[0] = "MaryRosslandEastApproach";
					approaches[1] = "MaryRosslandWestApproach";
					approaches[2] = "RosslandMaryNorthApproach";
					approaches[3] = "RosslandMarySouthApproach";
					
					departs[0] = "RosslandMarySouthDepart";
					departs[1] = "MaryRosslandNorthDepart";
					departs[2] = "RosslandMaryWestDepart";
					departs[3] = "RosslandMaryEastDepart";
					  
					
					  
				}
				
			
				
		  }else if (tlsControllerName.equals("MaryTauntonJunction")) {
			  //MARY-TAUNTON: MaryTauntonJunction
		  
			
				if (((appLeg.equals("SimcoeTauntonSouthApproach")) && (deptLeg.equals("MaryTauntonNorthDepart"))) || ((appLeg.equals("SimcoeTauntonSouthApproach")) && (deptLeg.equals("TauntonMaryEastDepart"))) || ((appLeg.equals("TauntonMaryNorthApproach")) && (deptLeg.equals("MaryTauntonWestDepart"))) || ((appLeg.equals("TauntonMaryNorthApproach")) && (deptLeg.equals("MaryTauntonSouthDepart")))) { //NORTH-SOUTH : Right turn or Straight-Through
					nextPhase = "northSouthStraightRight";
					approaches[0] = "TauntonMaryNorthApproach";
					  approaches[1] = "TauntonMaryNorthApproach";
					  approaches[2] = "SimcoeTauntonSouthApproach";
					  approaches[3] = "SimcoeTauntonSouthApproach";
					
					  departs[0] = "MaryTauntonSouthDepart";
					  departs[1] = "MaryTauntonWestDepart";
					  departs[2] = "MaryTauntonNorthDepart";
					  departs[3] = "TauntonMaryEastDepart";
					  
				
					  
				}else if (((appLeg.equals("TauntonMaryNorthApproach")) && (deptLeg.equals("TauntonMaryEastDepart"))) || ((appLeg.equals("SimcoeTauntonSouthApproach")) && (deptLeg.equals("MaryTauntonWestDepart")))) { //NORTH-SOUTH : Left-Turn
					nextPhase = "northSouthLeft";
					
					
					approaches[0] = "TauntonMaryNorthApproach";
					approaches[1] = "SimcoeTauntonSouthApproach";
					approaches[2] = "MaryTauntonEastApproach";
					approaches[3] = "MaryTauntonWestApproach";
					
					departs[0] = "TauntonMaryEastDepart";
					departs[1] = "MaryTauntonWestDepart";
					departs[2] = "MaryTauntonNorthDepart";
					departs[3] = "MaryTauntonSouthDepart";
					
					
					
					
				}else if (((appLeg.equals("MaryTauntonEastApproach")) && (deptLeg.equals("MaryTauntonNorthDepart"))) || ((appLeg.equals("MaryTauntonEastApproach")) && (deptLeg.equals("MaryTauntonWestDepart"))) || ((appLeg.equals("MaryTauntonWestApproach")) && (deptLeg.equals("MaryTauntonSouthDepart"))) || ((appLeg.equals("MaryTauntonWestApproach")) && (deptLeg.equals("TauntonMaryEastDepart")))) { //WEST-EAST : Right turn or Straight-Through
					nextPhase = "eastWestStraightRight";
					approaches[0] = "MaryTauntonEastApproach";
					  approaches[1] = "MaryTauntonEastApproach";
					  approaches[2] = "MaryTauntonWestApproach";
					  approaches[3] = "MaryTauntonWestApproach";
					  departs[0] = "MaryTauntonNorthDepart";
					  departs[1] = "MaryTauntonWestDepart";
					  departs[2] = "MaryTauntonSouthDepart";
					  departs[3] = "TauntonMaryEastDepart";
					  
				
				}else if (((appLeg.equals("MaryTauntonEastApproach")) && (deptLeg.equals("MaryTauntonSouthDepart"))) || ((appLeg.equals("MaryTauntonWestApproach")) && (deptLeg.equals("MaryTauntonNorthDepart")))) { //WEST-EAST : Left-Turn
					nextPhase = "eastWestLeft";
					
					
					
					approaches[0] = "MaryTauntonEastApproach";
					approaches[1] = "MaryTauntonWestApproach";
					approaches[2] = "TauntonMaryNorthApproach";
					approaches[3] = "SimcoeTauntonSouthApproach";
					
					departs[0] = "MaryTauntonSouthDepart";
					departs[1] = "MaryTauntonNorthDepart";
					departs[2] = "MaryTauntonWestDepart";
					departs[3] = "TauntonMaryEastDepart";
					  
					
					  
				}
		  }
		  //System.out.println("SETTING NEXT PHASE Program TO: " + nextPhase);
		  return arrayPair.makeArrayPair(approaches, departs);
	  }
	  
	@Override
	public void processEvent(final Event arg0) throws Exception {
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
	    
	    
		    getLog().info("MSG FOR {} FROM: {} at lat {} and lon {} MSGType {}", srm.getJunctionId(), srm.getVehicleId(), srm.getLocation().getLatitude(),srm.getLocation().getLongitude(), srm.getMsgType() );
		    //System.out.println("MSG   FOR : " + srm.getJunctionId() + " FROM : "  + srm.getVehicleId() + " type: " + srm.getMsgType() );
		    trfRcdng = new TrafficRecording();
		    trfRcdng.setTable(adhocTrafficRecords);
			adhocTrafficRecords = trfRcdng.ProcessSRM(adhocTrafficRecords, srm);
		    
			getLog().info("TRAFFIC RECORD: {}", adhocTrafficRecords.printAll());
			
		if (srm.getJunctionId().equals(tlsControllerName)) { //Check if the SRM is for this controller
		    	
		    //Send Adhoc V2xMessage Acknowledgement
		    int status = trfRcdng.getStatus(); //GET MSGTYPE from Traffic recording to report successful recording of vehicle data
		    SRMAcknowledgementContent srmAckCnt = new SRMAcknowledgementContent(tlsControllerName, srm.getVehicleId(), srm.getApproachLeg(), srm.getMsgType(), status);
		    final MessageRouting adhocRouting = getOperatingSystem()
		          .getAdHocModule()
		          .createMessageRouting()
		          .topoBroadCast();
		    SRMAcknowledgement adhocSrm = new SRMAcknowledgement(adhocRouting,srmAckCnt, 200L);
		    getOs().getAdHocModule().sendV2xMessage(adhocSrm);
		    
	    }
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}

}
