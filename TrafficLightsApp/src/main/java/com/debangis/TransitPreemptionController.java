package com.debangis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
import com.debangis.messages.ICNMAcknowledgement;
import com.debangis.messages.ICNMAcknowledgementContent;
import com.debangis.messages.IntersectionCoordinationNotificationMessage;
import com.debangis.messages.IntersectionCoordinationNotificationMessageContent;
import com.debangis.messages.SRMAcknowledgement;
import com.debangis.messages.SRMAcknowledgementContent;
import com.debangis.messages.SignalRequestMessage;

import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

/*
 * This control approach allows for antennas to be installed at 50m - 250m range from an intersection so that public transit vehicles (or other high priority) are detected and traffic control preemption is done
 * to allow the vehicle utilize the intersection with minimal delay. This is to serve as a benchmark for Multi-Intersection Traffic Control scheme based on the utilization of C-V2X
 */


public class TransitPreemptionController extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {

	//https://www.toronto.ca/services-payments/streets-parking-transportation/traffic-management/traffic-signals-street-signs/traffic-signal-operations/traffic-signal-prioritization/
	//https://nacto.org/publication/transit-street-design-guide/intersections/intersection-design/right-turn-pocket/
	/*
	 * https://www.itskrs.its.dot.gov/2018-sc00401
	 * Cellular-V2X (C-V2X) communication modules that use wireless wide-area network (WWAN) cellular modems to broadcast and receive basic safety messages (BSM) can be supplied to automobile manufacturers for less than $222 per unit.
	 */
	
	int lightSequence = 0;
	  
	  FuzzyModel fisModel;// = new FuzzyModel();
	  QueueLengthDistanceSpeedWaitingTimeController qdswFisModel;
	  QueueLengthWaitingTimeDistanceController qwdFisModel;
	  BenchmarkZachariahEtAl bzFisModel;
	  QueueDependentController queueDependent;
	  //CHOOSE CONTROLLER TO BE USED using the declaration above
	  String chosenController = "qdswFisModel";
	  
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
	  int phasePriority = 0, nextPhasePriority = 0; //Meaning lowest priority used for all vehicles at intersection. The higher the value (int), the higher the priority. This is used for vehicles whose priority is 1 or less.
	  String[] scheduledApproaches = {}, nextScheduledApproaches = {}, scheduledDepartures = {}, nextScheduledDepartures = {};
	  
	  String[] greenPhaseSequence = {"northSouthLeft", "northSouthStraightRight", "eastWestLeft", "eastWestStraightRight", "allOutNorth", "allOutSouth", "allOutWest", "allOutEast"};
	  String[] yellowPhaseSequence = {"northSouthLeftYellow", "northSouthStraightRightYellow", "eastWestLeftYellow", "eastWestStraightRightYellow", "allOutNorthYellow", "allOutSouthYellow", "allOutWestYellow", "allOutEastYellow"};
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
	  long remTime;
	  
	  IntersectionCoordinationNotificationMessageContent icnmCnt;
	  boolean sendIcnm = false;
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
        Event startTrafficControl = new Event(getOs().getSimulationTime() + (4 * 1000000000L), this::ResetTrafficControl);
        getOs().getEventManager().addEvent(startTrafficControl);
        Event event = new Event(getOs().getSimulationTime() + 3000000000L, this::initializeParameters);
    	getOs().getEventManager().addEvent(event);
    	//nextPhase = "northSouthLeft";
    	//currentPhase = "eastWestStraightRight";
    	//phasePointer = 0;
    	//setYellowRedPhasesTime();
    	startTime = getOs().getSimulationTime() + 1000000000L;
    	
        
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
		 //setYellowRedPhasesTime();
		 
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

	  
	private void setNextSchedule() {
		  
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
				  Table vehRec,vehRecd;
				  String appLeg, deptLeg;
				  LongColumn phaseVehArrival;
				  //System.out.println("TABLE DETAILS " + tlsControllerName);
				  //System.out.println(tmpRecord.printAll());
				  
				  tmpRecord = tmpRecord.sortAscendingOn("arrivaltime");
				  
				  IntColumn vehPrtyCol = tmpRecord.intColumn("priority");
				  nextPhasePriority = (int)vehPrtyCol.max(); //Set Priority for the next phase
				  
				  maxWaitingTime  = 0;
				  maxSpeed = 0;
				  maxDistance = 0;
				  maxQueueLength = 0;
				  if (nextPhasePriority > 1) { //Meaning high priority vehicle is around this intersection
					  System.out.println("HIGH PRIORITY " + nextPhasePriority + "  @  " + tlsControllerName);
					  //Get vehicles with the highest priority
					  vehRec = tmpRecord.where(vehPrtyCol.isEqualTo(nextPhasePriority));
					  //Get approach with a vehicle that has the earliest arrival
					  phaseVehArrival = vehRec.longColumn("arrivaltime");
					  vehRecd = vehRec.where(phaseVehArrival.isEqualTo(phaseVehArrival.min()));
					  
					  //System.out.println("Veh With Max Waiting: " + vehRec.printAll());
					  appLeg = vehRecd.stringColumn("approachleg").getString(0);
					  
					  deptLeg = vehRecd.stringColumn("departleg").getString(0);
					  //System.out.println("Veh With Max Waiting: " + vehRec.print(1));
					//Earliest arriving time
					  maxWaitingTime = (getOs().getSimulationTime() - (long)phaseVehArrival.min());
					  
				  }else { //Vehicles have default priority of 1
					//Get approach with a vehicle that has the earliest arrival
					  phaseVehArrival = tmpRecord.longColumn("arrivaltime");
					  //Earliest arriving time
					  maxWaitingTime = (getOs().getSimulationTime() - (long)phaseVehArrival.min());
					  //System.out.println("MAX Waiting Time = " + maxWaitingTime);
					  
					  vehRec = tmpRecord.where(phaseVehArrival.isEqualTo(phaseVehArrival.min()));
					  //System.out.println("Veh With Max Waiting: " + vehRec.printAll());
					  
					  appLeg = vehRec.stringColumn("approachleg").getString(0);
					  
					  deptLeg = vehRec.stringColumn("departleg").getString(0);
					  //System.out.println("Veh With Max Waiting: " + vehRec.print(1));
					  
				  }
				  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
					  System.out.println(vehRec.stringColumn("vehicleid").getString(0) + appLeg + "  " + deptLeg);
				  }
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
		  //if nextPhase is same currentPhase, no need to call this function, setYellowRedPhasesTime(), so that the phase continue with interruption
		  //setYellowRedPhasesTime();
	  }

	  
	
	private boolean CompareTrafficStates(Table prevState, Table curState) {
		/*This function compares traffic state to validate that from the previous traffic light state and now there has being change in traffic state.
		 * That is, that some vehicles have moved
		 */
        // Retrieve the name columns from both tables
        StringColumn prevVehs = prevState.stringColumn("vehicleid");
        StringColumn curVehs = curState.stringColumn("vehicleid");

        // Check if the number of names in both tables is the same
        if (prevVehs.size() != curVehs.size()) {
            return false;
        }

        // Convert the names to sets for comparison
        Set<String> prevSet = new HashSet<>(prevVehs.asList());
        Set<String> curSet = new HashSet<>(curVehs.asList());

        // Compare the sets of names
        return prevSet.equals(curSet);
    }
	
	  private void setYellowRedPhasesTime() {
		  /*/This function determines if current phase equals next phase, 
		   * it ensures that traffic lights does not change to yellow and all red phase
		   * It is called from the setNextSchedule() 
		   */
		  System.out.println(" SET YELLOW: "+ (startTime + (phaseDuration - 1)* 1000000000L) + " CURRENT TIME: " +getOs().getSimulationTime()); 
		  if (!(currentPhase.equals(nextPhase))) {
			 // Event setRedEvent = new Event((startTime + ((phaseDuration - 1)* 1000000000L)), this::setAllRedPhase);
			  //getOs().getEventManager().addEvent(setRedEvent);
			    
			  System.out.println(" SET YELLOW: "+startTime + (phaseDuration - 3)* 1000000000L); 
			  //Event setYellowEvent = new Event(startTime + (phaseDuration - 3)* 1000000000L, this::setYellowPhase);
			  //getOs().getEventManager().addEvent(setYellowEvent);
		  }
	  }
	  
	  
	  private double getMaxWaitingTime(String appLeg, String deptLeg) {
		  //If deptLeg is not provided, then the max waiting time of the whole approach without regard to turnings is returned.
		  double wTime = 0.0;
		  Table phaseVeh = adhocTrafficRecords;
		  if (deptLeg.equals("")) {
			  phaseVeh = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(appLeg));
		  }else {
			  phaseVeh = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(appLeg)
						.and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(deptLeg)));
			  
		  }
		  LongColumn phaseVehArrival = phaseVeh.longColumn("arrivaltime");
		  wTime = (getOs().getSimulationTime() - (long)phaseVehArrival.min());
		  return wTime;
	  }
	  
	  private int getQueueLength(String appLeg, String deptLeg) {
		 //If deptLeg is not provided, then the queue length of the whole approach without regard to turnings is returned.
		  Table phaseVeh = adhocTrafficRecords, catCount;
		  int vehCount = 0;
		  if (deptLeg.equals("")) {
			  phaseVeh = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(appLeg));
			  catCount = phaseVeh.countBy("departleg"); //Use to group and count vehicles based on their approach lanes.
			  NumericColumn<?> ct = catCount.numberColumn("Count");
			  vehCount = (int) ct.max(); //we take the approach lane with max number of vehicles
		  }else {
			  phaseVeh = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(appLeg)
						.and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(deptLeg)));
			  vehCount = phaseVeh.rowCount();
		  }
		 
		  
		  
		  return vehCount;
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
	  
	  
	  private void ResetTrafficControl(Event event) {
		  /*
		  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
			  System.out.println("ResetTrafficControl");
		  }
		  //*/
		  //System.out.println("ResetTrafficControl");
		  //This function is used to set traffic lights and send messages that needs to be send 
		  long runningTime = (getOs().getSimulationTime() - startTime); // 1000000000

		  remTime = ((phaseDuration*1000000000L) - runningTime) ;
		  //System.out.println("remTime : " + remTime);
		  remTime = remTime > 0 ? remTime : 0;
		  //System.out.println(tlsControllerName + "   phaseDuration :  " + phaseDuration + "  runningTime : " + runningTime + " remTime : " + remTime + " startTime : " + startTime);
			if ((remTime >= (4*1000000000L)) && (remTime < (5*1000000000L))) {// Schedule Next Vehicles/Flows and the end of the current phase (Set Yellow and Clearance Phases)
				setNextSchedule();
				//System.out.println(tlsControllerName + "  NEXT PHASE = " + nextPhase);
				//setYellowRedPhasesTime();
				setYellowPhase();
			}else if ((remTime >= (1*1000000000L)) && (remTime < (2*1000000000L))) {	
				setAllRedPhase();
			}else if (remTime == 0) {
				setGreenPhase();
				
				
			}
			
			//Check if sendIcnm
			if (sendIcnm == true) {
				final MessageRouting adhocIcnmRouting = getOperatingSystem()
				          .getAdHocModule()
				          .createMessageRouting()
				          .topoBroadCast();
		    	 IntersectionCoordinationNotificationMessage adhocIcnm = new IntersectionCoordinationNotificationMessage(adhocIcnmRouting,icnmCnt, 200L);
				 getOs().getAdHocModule().sendV2xMessage(adhocIcnm);
			}
			
			event = new Event(getOs().getSimulationTime() + 1000000000L, this::ResetTrafficControl);
			getOs().getEventManager().addEvent(event);
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
		
		
	  
	  private void setAllRedPhase() {
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(allRed);
		  getLog().infoSimTime(this, "Traffic Lights Assigned to " + allRed );
	  }
	  

	  
	  private void setYellowPhase() {
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
	  
	  
	  private void setGreenPhase() {
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
		  //Set scheduled Approaches and Departures
		  scheduledApproaches = nextScheduledApproaches;
		  scheduledDepartures = nextScheduledDepartures;
		  //System.out.println("SCHEDULED @ : " + tlsControllerName + "  SCHEDULED APP"+ scheduledApproaches[0]);
		  phasePriority = nextPhasePriority;
		  //System.out.println(tlsControllerName + " SETTING GREEN PHASE PROGRAM WITH: "+ nextPhase + " PD " + phaseDuration);
		  setPhasePointer(currentPhase);
		  String phase = greenPhaseSequence[phasePointer];
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(phase);
		  getLog().infoSimTime(this, "Traffic Lights Assigned to " + phase );
		  //event = new Event(getOs().getSimulationTime() + (phaseDuration * 1000000000L), this::roundRobinControl);10 000 000 000L
		  //getOs().getEventManager().addEvent(event);
		  startTime = getOs().getSimulationTime();
		  if ((tlsControllerName.equals("SimcoeTauntonJunction")) || (tlsControllerName.equals("SimcoeRosslandJunction"))) {
			  System.out.println(tlsControllerName + "  SET PHASE = " + phase);
		  }
		  //Event setNextScheduleEvent = new Event(startTime  + (phaseDuration - 5)* 1000000000L, this::setNextSchedule);
		  //getOs().getEventManager().addEvent(setNextScheduleEvent);
		  
		  //Event setGreenEvent = new Event(startTime + (phaseDuration * 1000000000L), this::setGreenPhase);
		  //getOs().getEventManager().addEvent(setGreenEvent);
		    
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
			  getControlledApproaches(nextPhase);
		  	  //phasePointer = lightSequence;
			  //}
			  
		  //}
	  }
	  

	
	  private void getControlledApproaches(String phase) {
			  //"northSouthLeft", "northSouthStraightRight", "eastWestLeft", "eastWestStraightRight"
			  //This function sets the phase program name and returns arrays of concurrent flows for the next phase
			  
			  String[] approaches= new String[4];
			  String[] departs = new String[4];
			  
			  //SIMCOE-TAUNTON:  SimcoeTauntonJunction
			  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
					if (phase.equals("northSouthStraightRight")){ //NORTH-SOUTH : Right turn or Straight-Through
						
						approaches[0] = "TauntonSimcoeNorthApproach";
						  approaches[1] = "TauntonSimcoeNorthApproach";
						  approaches[2] = "TauntonSimcoeSouthApproach";
						  approaches[3] = "TauntonSimcoeSouthApproach";
						  
						  departs[0] = "SimcoeTauntonSouthDepart";
						  departs[1] = "SimcoeTauntonWestDepart";
						  departs[2] = "SimcoeTauntonNorthDepart";
						  departs[3] = "SimcoeTauntonEastDepart";
						  
						  
						  
					}else if (phase.equals("northSouthLeft")) { //NORTH-SOUTH : Left-Turn
						
						approaches[0] = "TauntonSimcoeNorthApproach";
						approaches[1] = "TauntonSimcoeSouthApproach";
						approaches[2] = "SimcoeTauntonEastApproach";
						approaches[3] = "SimcoeTauntonWestApproach";
						
						departs[0] = "SimcoeTauntonEastDepart";
						departs[1] = "SimcoeTauntonWestDepart";
						departs[2] = "SimcoeTauntonNorthDepart";
						departs[3] = "SimcoeTauntonSouthDepart";
						
						
						
						
					}else if (phase.equals("eastWestStraightRight")) { //WEST-EAST : Right turn or Straight-Through
						
						approaches[0] = "SimcoeTauntonEastApproach";
						  approaches[1] = "SimcoeTauntonEastApproach";
						  approaches[2] = "SimcoeTauntonWestApproach";
						  approaches[3] = "SimcoeTauntonWestApproach";
						  
						  departs[0] = "SimcoeTauntonNorthDepart";
						  departs[1] = "SimcoeTauntonWestDepart";
						  departs[2] = "SimcoeTauntonSouthDepart";
						  departs[3] = "SimcoeTauntonEastDepart";
						  
						  
					}else if (phase.equals("eastWestLeft")) { //WEST-EAST : Left-Turn
						
						approaches[0] = "SimcoeTauntonEastApproach";
						approaches[1] = "SimcoeTauntonWestApproach";
						approaches[2] = "TauntonSimcoeNorthApproach";
						approaches[3] = "TauntonSimcoeSouthApproach";
						
						departs[0] = "SimcoeTauntonSouthDepart";
						departs[1] = "SimcoeTauntonNorthDepart";
						departs[2] = "SimcoeTauntonWestDepart";
						departs[3] = "SimcoeTauntonEastDepart";
						  
						
						
					}else if(phase.equals("allOutNorth")) {
						
						approaches = new String[3];
						departs = new String[3];
						
						approaches[0] = "TauntonSimcoeNorthApproach";
						approaches[1] = "TauntonSimcoeNorthApproach";
						approaches[2] = "TauntonSimcoeNorthApproach";
						
						
						departs[0] = "SimcoeTauntonSouthDepart";
						departs[1] = "SimcoeTauntonWestDepart";
						departs[2] = "SimcoeTauntonEastDepart";
						
					}else if(phase.equals("allOutSouth")){
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "TauntonSimcoeSouthApproach";
						approaches[1] = "TauntonSimcoeSouthApproach";
						approaches[2] = "TauntonSimcoeSouthApproach";
						
						
						departs[0] = "SimcoeTauntonNorthDepart";
						departs[1] = "SimcoeTauntonWestDepart";
						departs[2] = "SimcoeTauntonEastDepart";
						
					}else if(phase.equals("allOutWest")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "SimcoeTauntonWestApproach";
						approaches[1] = "SimcoeTauntonWestApproach";
						approaches[2] = "SimcoeTauntonWestApproach";
						
						
						departs[0] = "SimcoeTauntonNorthDepart";
						departs[1] = "SimcoeTauntonSouthDepart";
						departs[2] = "SimcoeTauntonEastDepart";
						
					}else if(phase.equals("allOutEast")){
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "SimcoeTauntonEastApproach";
						approaches[1] = "SimcoeTauntonEastApproach";
						approaches[2] = "SimcoeTauntonEastApproach";
						
						
						departs[0] = "SimcoeTauntonNorthDepart";
						departs[1] = "SimcoeTauntonSouthDepart";
						departs[2] = "SimcoeTauntonWestDepart";
						
					}
				
				
				
			  }else if (tlsControllerName.equals("SimcoeRosslandJunction")) {
				//SIMCOE-ROSSLAND: SimcoeRosslandJunction

					if (phase.equals("northSouthStraightRight")) { //NORTH-SOUTH : Right turn or Straight-Through
						
						approaches[0] = "RosslandSimcoeNorthApproach";
						  approaches[1] = "RosslandSimcoeNorthApproach";
						  approaches[2] = "RosslandSimcoeSouthApproach";
						  approaches[3] = "RosslandSimcoeSouthApproach";
						
						  departs[0] = "SimcoeRosslandSouthDepart";
						  departs[1] = "SimcoeRosslandWestDepart";
						  departs[2] = "SimcoeRosslandNorthDepart";
						  departs[3] = "RosslandSimcoeEastDepart";
						  
					
						  
					}else if (phase.equals("northSouthLeft")) { //NORTH-SOUTH : Left-Turn
						
						approaches[0] = "RosslandSimcoeNorthApproach";
						approaches[1] = "RosslandSimcoeSouthApproach";
						approaches[2] = "SimcoeRosslandEastApproach";
						approaches[3] = "SimcoeRosslandWestApproach";
						
						departs[0] = "RosslandSimcoeEastDepart";
						departs[1] = "SimcoeRosslandWestDepart";
						departs[2] = "SimcoeRosslandNorthDepart";
						departs[3] = "SimcoeRosslandSouthDepart";
						
						
						
						
					}else if (phase.equals("eastWestStraightRight")) { //WEST-EAST : Right turn or Straight-Through
						
						approaches[0] = "SimcoeRosslandEastApproach";
						  approaches[1] = "SimcoeRosslandEastApproach";
						  approaches[2] = "SimcoeRosslandWestApproach";
						  approaches[3] = "SimcoeRosslandWestApproach";
						  
						  departs[0] = "SimcoeRosslandNorthDepart";
						  departs[1] = "SimcoeRosslandWestDepart";
						  departs[2] = "SimcoeRosslandSouthDepart";
						  departs[3] = "RosslandSimcoeEastDepart";
						  
					
					}else if (phase.equals("eastWestLeft")) { //WEST-EAST : Left-Turn
						
						
						approaches[0] = "SimcoeRosslandEastApproach";
						approaches[1] = "SimcoeRosslandWestApproach";
						approaches[2] = "RosslandSimcoeNorthApproach";
						approaches[3] = "RosslandSimcoeSouthApproach";
						
						departs[0] = "SimcoeRosslandSouthDepart";
						departs[1] = "SimcoeRosslandNorthDepart";
						departs[2] = "SimcoeRosslandWestDepart";
						departs[3] = "RosslandSimcoeEastDepart";
						  
						
					}else if(phase.equals("allOutNorth")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "RosslandSimcoeNorthApproach";
						approaches[1] = "RosslandSimcoeNorthApproach";
						approaches[2] = "RosslandSimcoeNorthApproach";
						
						departs[0] = "SimcoeRosslandSouthDepart";
						departs[1] = "SimcoeRosslandWestDepart";
						departs[2] = "RosslandSimcoeEastDepart";
						
					}else if(phase.equals("allOutSouth")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "RosslandSimcoeSouthApproach";
						approaches[1] = "RosslandSimcoeSouthApproach";
						approaches[2] = "RosslandSimcoeSouthApproach";
						
						departs[0] = "SimcoeRosslandNorthDepart";
						departs[1] = "SimcoeRosslandWestDepart";
						departs[2] = "RosslandSimcoeEastDepart";
						
						
					}else if(phase.equals("allOutWest")) {

						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "SimcoeRosslandWestApproach";
						approaches[1] = "SimcoeRosslandWestApproach";
						approaches[2] = "SimcoeRosslandWestApproach";
						
						departs[0] = "SimcoeRosslandNorthDepart";
						departs[1] = "SimcoeRosslandSouthDepart";
						departs[2] = "RosslandSimcoeEastDepart";
						
						
					}else if(phase.equals("allOutEast")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "RosslandSimcoeEastDepart";
						approaches[1] = "RosslandSimcoeEastDepart";
						approaches[2] = "RosslandSimcoeEastDepart";
						
						departs[0] = "SimcoeRosslandNorthDepart";
						departs[1] = "SimcoeRosslandWestDepart";
						departs[2] = "SimcoeRosslandSouthDepart";
						  
					}
				
				
			  }else if (tlsControllerName.equals("MaryRosslandJuction")) {
				  //MARY-ROSSLAND: MaryRosslandJuction
			  
					if (phase.equals("northSouthStraightRight")) { //NORTH-SOUTH : Right turn or Straight-Through
						
						approaches[0] = "RosslandMaryNorthApproach";
						  approaches[1] = "RosslandMaryNorthApproach";
						  approaches[2] = "RosslandMarySouthApproach";
						  approaches[3] = "RosslandMarySouthApproach";
						
						  departs[0] = "RosslandMarySouthDepart";
						  departs[1] = "RosslandMaryWestDepart";
						  departs[2] = "MaryRosslandNorthDepart";
						  departs[3] = "RosslandMaryEastDepart";
						  
					
						  
					}else if (phase.equals("northSouthLeft")) { //NORTH-SOUTH : Left-Turn
						
						approaches[0] = "RosslandMaryNorthApproach";
						approaches[1] = "RosslandMarySouthApproach";
						approaches[2] = "MaryRosslandEastApproach";
						approaches[3] = "MaryRosslandWestApproach";
						
						departs[0] = "RosslandMaryEastDepart";
						departs[1] = "RosslandMaryWestDepart";
						departs[2] = "MaryRosslandNorthDepart";
						departs[3] = "RosslandMarySouthDepart";
						
						
						
						
					}else if (phase.equals("eastWestStraightRight")) { //WEST-EAST : Right turn or Straight-Through
						 approaches[0] = "MaryRosslandEastApproach";
						  approaches[1] = "MaryRosslandEastApproach";
						  approaches[2] = "MaryRosslandWestApproach";
						  approaches[3] = "MaryRosslandWestApproach";
						  
						  departs[0] = "MaryRosslandNorthDepart";
						  departs[1] = "RosslandMaryWestDepart";
						  departs[2] = "RosslandMarySouthDepart";
						  departs[3] = "RosslandMaryEastDepart";
						  
					
					}else if (phase.equals("eastWestLeft")) { //WEST-EAST : Left-Turn
						
						approaches[0] = "MaryRosslandEastApproach";
						approaches[1] = "MaryRosslandWestApproach";
						approaches[2] = "RosslandMaryNorthApproach";
						approaches[3] = "RosslandMarySouthApproach";
						
						departs[0] = "RosslandMarySouthDepart";
						departs[1] = "MaryRosslandNorthDepart";
						departs[2] = "RosslandMaryWestDepart";
						departs[3] = "RosslandMaryEastDepart";
						  
						
						  
						
					}else if(phase.equals("allOutNorth")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "RosslandMaryNorthApproach";
						approaches[1] = "RosslandMaryNorthApproach";
						approaches[2] = "RosslandMaryNorthApproach";
						
						departs[0] = "RosslandMarySouthDepart";
						departs[1] = "RosslandMaryEastDepart";
						departs[2] = "RosslandMaryWestDepart";
						
					}else if(phase.equals("allOutSouth")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "RosslandSimcoeSouthApproach";
						approaches[1] = "RosslandSimcoeSouthApproach";
						approaches[2] = "RosslandSimcoeSouthApproach";
						
						departs[0] = "RosslandMaryEastDepart";
						departs[1] = "MaryRosslandNorthDepart";
						departs[2] = "RosslandMaryWestDepart";
						
						
					}else if(phase.equals("allOutWest")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "SimcoeRosslandWestApproach";
						approaches[1] = "SimcoeRosslandWestApproach";
						approaches[2] = "SimcoeRosslandWestApproach";
						
						departs[0] = "RosslandMarySouthDepart";
						departs[1] = "MaryRosslandNorthDepart";
						departs[2] = "RosslandMaryEastDepart";
						
						
					}else if(phase.equals("allOutEast")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "MaryRosslandEastApproach";
						approaches[1] = "MaryRosslandEastApproach";
						approaches[2] = "MaryRosslandEastApproach";
						
						departs[0] = "RosslandMarySouthDepart";
						departs[1] = "MaryRosslandNorthDepart";
						departs[2] = "RosslandMaryWestDepart";
						  
					}
				
					
					
			  }else if (tlsControllerName.equals("MaryTauntonJunction")) {
				  //MARY-TAUNTON: MaryTauntonJunction
			  
				
					if (phase.equals("northSouthStraightRight")) { //NORTH-SOUTH : Right turn or Straight-Through
						
						approaches[0] = "TauntonMaryNorthApproach";
						  approaches[1] = "TauntonMaryNorthApproach";
						  approaches[2] = "SimcoeTauntonSouthApproach";
						  approaches[3] = "SimcoeTauntonSouthApproach";
						
						  departs[0] = "MaryTauntonSouthDepart";
						  departs[1] = "MaryTauntonWestDepart";
						  departs[2] = "MaryTauntonNorthDepart";
						  departs[3] = "TauntonMaryEastDepart";
						  
					
						  
					}else if (phase.equals("northSouthLeft")) { //NORTH-SOUTH : Left-Turn
						
						approaches[0] = "TauntonMaryNorthApproach";
						approaches[1] = "SimcoeTauntonSouthApproach";
						approaches[2] = "MaryTauntonEastApproach";
						approaches[3] = "MaryTauntonWestApproach";
						
						departs[0] = "TauntonMaryEastDepart";
						departs[1] = "MaryTauntonWestDepart";
						departs[2] = "MaryTauntonNorthDepart";
						departs[3] = "MaryTauntonSouthDepart";
						
						
						
						
					}else if (phase.equals("eastWestStraightRight")) { //WEST-EAST : Right turn or Straight-Through
						
						approaches[0] = "MaryTauntonEastApproach";
						  approaches[1] = "MaryTauntonEastApproach";
						  approaches[2] = "MaryTauntonWestApproach";
						  approaches[3] = "MaryTauntonWestApproach";
						  departs[0] = "MaryTauntonNorthDepart";
						  departs[1] = "MaryTauntonWestDepart";
						  departs[2] = "MaryTauntonSouthDepart";
						  departs[3] = "TauntonMaryEastDepart";
						  
					
					}else if (phase.equals("eastWestLeft")) { //WEST-EAST : Left-Turn
						
						approaches[0] = "MaryTauntonEastApproach";
						approaches[1] = "MaryTauntonWestApproach";
						approaches[2] = "TauntonMaryNorthApproach";
						approaches[3] = "SimcoeTauntonSouthApproach";
						
						departs[0] = "MaryTauntonSouthDepart";
						departs[1] = "MaryTauntonNorthDepart";
						departs[2] = "MaryTauntonWestDepart";
						departs[3] = "TauntonMaryEastDepart";
						  
						

					}else if(phase.equals("allOutNorth")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "TauntonMaryNorthApproach";
						approaches[1] = "TauntonMaryNorthApproach";
						approaches[2] = "TauntonMaryNorthApproach";
						
						departs[0] = "MaryTauntonSouthDepart";
						departs[1] = "TauntonMaryEastDepart";
						departs[2] = "MaryTauntonWestDepart";
						
					}else if(phase.equals("allOutSouth")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "SimcoeTauntonSouthApproach";
						approaches[1] = "SimcoeTauntonSouthApproach";
						approaches[2] = "SimcoeTauntonSouthApproach";
						
						departs[0] = "TauntonMaryEastDepart";
						departs[1] = "MaryTauntonNorthDepart";
						departs[2] = "MaryTauntonWestDepart";
						
					}else if(phase.equals("allOutWest")) {

						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "MaryTauntonWestApproach";
						approaches[1] = "MaryTauntonWestApproach";
						approaches[2] = "MaryTauntonWestApproach";
						
						departs[0] = "MaryTauntonSouthDepart";
						departs[1] = "MaryTauntonNorthDepart";
						departs[2] = "TauntonMaryEastDepart";
						
					}else if(phase.equals("allOutEast")) {
						
						approaches= new String[3];
						departs = new String[3];
						
						approaches[0] = "MaryTauntonEastApproach";
						approaches[1] = "MaryTauntonEastApproach";
						approaches[2] = "MaryTauntonEastApproach";
						
						departs[0] = "MaryTauntonSouthDepart";
						departs[1] = "MaryTauntonNorthDepart";
						departs[2] = "MaryTauntonWestDepart";
						  
					}
			  }
			  nextScheduledApproaches = approaches;
			  nextScheduledDepartures = departs;
			/*
			  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
				  System.out.println("getControlledApproaches " + phase + "   " + approaches[0] + " " + approaches[1] + " "+ approaches[2]);
			  }
			  //*/
		  }
		  
		
	  private ArrayPair getConcurrentFlows(String appLeg, String deptLeg) {
		  //"northSouthLeft", "northSouthStraightRight", "eastWestLeft", "eastWestStraightRight"
		  //This function sets the nextPhase program name and returns arrays of concurrent flows for the next phase
		  
		  String[] approaches= new String[4];
		  String[] departs = new String[4];
		  ArrayPair arrayPair = new ArrayPair();
		  
		  //SIMCOE-TAUNTON:  SimcoeTauntonJunction
		  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
				if (((appLeg.equals("TauntonSimcoeSouthApproach")) && (deptLeg.equals("SimcoeTauntonNorthDepart"))) || ((appLeg.equals("TauntonSimcoeSouthApproach")) && (deptLeg.equals("SimcoeTauntonEastDepart"))) || ((appLeg.equals("TauntonSimcoeNorthApproach")) && (deptLeg.equals("SimcoeTauntonWestDepart"))) || ((appLeg.equals("TauntonSimcoeNorthApproach")) && (deptLeg.equals("SimcoeTauntonSouthDepart")))) { //NORTH-SOUTH : Right turn or Straight-Through
					nextPhase = "northSouthStraightRight";
					approaches[0] = "TauntonSimcoeNorthApproach";
					  approaches[1] = "TauntonSimcoeNorthApproach";
					  approaches[2] = "TauntonSimcoeSouthApproach";
					  approaches[3] = "TauntonSimcoeSouthApproach";
					  departs[0] = "SimcoeTauntonSouthDepart";
					  departs[1] = "SimcoeTauntonWestDepart";
					  departs[2] = "SimcoeTauntonNorthDepart";
					  departs[3] = "SimcoeTauntonEastDepart";
					  
					  
					  
				}else if (((appLeg.equals("TauntonSimcoeNorthApproach")) && (deptLeg.equals("SimcoeTauntonEastDepart"))) || ((appLeg.equals("TauntonSimcoeSouthApproach")) && (deptLeg.equals("SimcoeTauntonWestDepart")))) { //NORTH-SOUTH : Left-Turn
					nextPhase = "northSouthLeft";
					
					
					approaches[0] = "TauntonSimcoeNorthApproach";
					approaches[1] = "TauntonSimcoeSouthApproach";
					approaches[2] = "SimcoeTauntonEastApproach";
					approaches[3] = "SimcoeTauntonWestApproach";
					
					departs[0] = "SimcoeTauntonEastDepart";
					departs[1] = "SimcoeTauntonWestDepart";
					departs[2] = "SimcoeTauntonNorthDepart";
					departs[3] = "SimcoeTauntonSouthDepart";
					
					
				}else if (((appLeg.equals("SimcoeTauntonEastApproach")) && (deptLeg.equals("SimcoeTauntonNorthDepart"))) || ((appLeg.equals("SimcoeTauntonEastApproach")) && (deptLeg.equals("SimcoeTauntonWestDepart"))) || ((appLeg.equals("SimcoeTauntonWestApproach")) && (deptLeg.equals("SimcoeTauntonSouthDepart"))) || ((appLeg.equals("SimcoeTauntonWestApproach")) && (deptLeg.equals("SimcoeTauntonEastDepart")))) { //WEST-EAST : Right turn or Straight-Through
					nextPhase = "eastWestStraightRight";
					approaches[0] = "SimcoeTauntonEastApproach";
					  approaches[1] = "SimcoeTauntonEastApproach";
					  approaches[2] = "SimcoeTauntonWestApproach";
					  approaches[3] = "SimcoeTauntonWestApproach";
					  departs[0] = "SimcoeTauntonNorthDepart";
					  departs[1] = "SimcoeTauntonWestDepart";
					  departs[2] = "SimcoeTauntonSouthDepart";
					  departs[3] = "SimcoeTauntonEastDepart";
					  
					  //{"northSouthLeft", "northSouthStraightRight", "eastWestLeft", "eastWestStraightRight"};
				}else if (((appLeg.equals("SimcoeTauntonEastApproach")) && (deptLeg.equals("SimcoeTauntonSouthDepart"))) || ((appLeg.equals("SimcoeTauntonWestApproach")) && (deptLeg.equals("SimcoeTauntonNorthDepart")))) { //WEST-EAST : Left-Turn
					nextPhase = "eastWestLeft";
					
					
					
					approaches[0] = "SimcoeTauntonEastApproach";
					approaches[1] = "SimcoeTauntonWestApproach";
					approaches[2] = "TauntonSimcoeNorthApproach";
					approaches[3] = "TauntonSimcoeSouthApproach";
					
					departs[0] = "SimcoeTauntonSouthDepart";
					departs[1] = "SimcoeTauntonNorthDepart";
					departs[2] = "SimcoeTauntonWestDepart";
					departs[3] = "SimcoeTauntonEastDepart";
					  
					
					
				}else if((appLeg.equals("TauntonSimcoeNorthApproach")) && (deptLeg.equals(""))) {
					nextPhase = "allOutNorth";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "TauntonSimcoeNorthApproach";
					approaches[1] = "TauntonSimcoeNorthApproach";
					approaches[2] = "TauntonSimcoeNorthApproach";
					
					
					departs[0] = "SimcoeTauntonSouthDepart";
					departs[1] = "SimcoeTauntonWestDepart";
					departs[2] = "SimcoeTauntonEastDepart";
					
				}else if((appLeg.equals("TauntonSimcoeSouthApproach")) && (deptLeg.equals(""))) {
					nextPhase = "allOutSouth";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "TauntonSimcoeSouthApproach";
					approaches[1] = "TauntonSimcoeSouthApproach";
					approaches[2] = "TauntonSimcoeSouthApproach";
					
					
					departs[0] = "SimcoeTauntonNorthDepart";
					departs[1] = "SimcoeTauntonWestDepart";
					departs[2] = "SimcoeTauntonEastDepart";
					
				}else if((appLeg.equals("SimcoeTauntonWestApproach")) && (deptLeg.equals(""))) {
					
					nextPhase = "allOutWest";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "SimcoeTauntonWestApproach";
					approaches[1] = "SimcoeTauntonWestApproach";
					approaches[2] = "SimcoeTauntonWestApproach";
					
					
					departs[0] = "SimcoeTauntonNorthDepart";
					departs[1] = "SimcoeTauntonSouthDepart";
					departs[2] = "SimcoeTauntonEastDepart";
					
				}else if((appLeg.equals("SimcoeTauntonEastApproach")) && (deptLeg.equals(""))) {


					nextPhase = "allOutEast";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "SimcoeTauntonEastApproach";
					approaches[1] = "SimcoeTauntonEastApproach";
					approaches[2] = "SimcoeTauntonEastApproach";
					
					
					departs[0] = "SimcoeTauntonNorthDepart";
					departs[1] = "SimcoeTauntonSouthDepart";
					departs[2] = "SimcoeTauntonWestDepart";
					
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
					  
					
				}else if((appLeg.equals("RosslandSimcoeNorthApproach")) && (deptLeg.equals(""))) {
					nextPhase = "allOutNorth";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "RosslandSimcoeNorthApproach";
					approaches[1] = "RosslandSimcoeNorthApproach";
					approaches[2] = "RosslandSimcoeNorthApproach";
					
					departs[0] = "SimcoeRosslandSouthDepart";
					departs[1] = "SimcoeRosslandWestDepart";
					departs[2] = "RosslandSimcoeEastDepart";
					
				}else if((appLeg.equals("RosslandSimcoeSouthApproach")) && (deptLeg.equals(""))) {
					nextPhase = "allOutSouth";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "RosslandSimcoeSouthApproach";
					approaches[1] = "RosslandSimcoeSouthApproach";
					approaches[2] = "RosslandSimcoeSouthApproach";
					
					departs[0] = "SimcoeRosslandNorthDepart";
					departs[1] = "SimcoeRosslandWestDepart";
					departs[2] = "RosslandSimcoeEastDepart";
					
					
				}else if((appLeg.equals("SimcoeRosslandWestApproach")) && (deptLeg.equals(""))) {

					nextPhase = "allOutWest";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "SimcoeRosslandWestApproach";
					approaches[1] = "SimcoeRosslandWestApproach";
					approaches[2] = "SimcoeRosslandWestApproach";
					
					departs[0] = "SimcoeRosslandNorthDepart";
					departs[1] = "SimcoeRosslandSouthDepart";
					departs[2] = "RosslandSimcoeEastDepart";
					
					
				}else if((appLeg.equals("SimcoeRosslandEastApproach")) && (deptLeg.equals(""))) {


					nextPhase = "allOutEast";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "RosslandSimcoeEastDepart";
					approaches[1] = "RosslandSimcoeEastDepart";
					approaches[2] = "RosslandSimcoeEastDepart";
					
					departs[0] = "SimcoeRosslandNorthDepart";
					departs[1] = "SimcoeRosslandWestDepart";
					departs[2] = "SimcoeRosslandSouthDepart";
					  
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
					  
					
					  
					
				}else if((appLeg.equals("RosslandMaryNorthApproach")) && (deptLeg.equals(""))) {
					nextPhase = "allOutNorth";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "RosslandMaryNorthApproach";
					approaches[1] = "RosslandMaryNorthApproach";
					approaches[2] = "RosslandMaryNorthApproach";
					
					departs[0] = "RosslandMarySouthDepart";
					departs[1] = "RosslandMaryEastDepart";
					departs[2] = "RosslandMaryWestDepart";
					
				}else if((appLeg.equals("RosslandMarySouthApproach")) && (deptLeg.equals(""))) {
					nextPhase = "allOutSouth";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "RosslandSimcoeSouthApproach";
					approaches[1] = "RosslandSimcoeSouthApproach";
					approaches[2] = "RosslandSimcoeSouthApproach";
					
					departs[0] = "RosslandMaryEastDepart";
					departs[1] = "MaryRosslandNorthDepart";
					departs[2] = "RosslandMaryWestDepart";
					
					
				}else if((appLeg.equals("MaryRosslandWestApproach")) && (deptLeg.equals(""))) {

					nextPhase = "allOutWest";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "SimcoeRosslandWestApproach";
					approaches[1] = "SimcoeRosslandWestApproach";
					approaches[2] = "SimcoeRosslandWestApproach";
					
					departs[0] = "RosslandMarySouthDepart";
					departs[1] = "MaryRosslandNorthDepart";
					departs[2] = "RosslandMaryEastDepart";
					
					
				}else if((appLeg.equals("MaryRosslandEastApproach")) && (deptLeg.equals(""))) {


					nextPhase = "allOutEast";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "MaryRosslandEastApproach";
					approaches[1] = "MaryRosslandEastApproach";
					approaches[2] = "MaryRosslandEastApproach";
					
					departs[0] = "RosslandMarySouthDepart";
					departs[1] = "MaryRosslandNorthDepart";
					departs[2] = "RosslandMaryWestDepart";
					  
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
					  
					

				}else if((appLeg.equals("TauntonMaryNorthApproach")) && (deptLeg.equals(""))) {
					nextPhase = "allOutNorth";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "TauntonMaryNorthApproach";
					approaches[1] = "TauntonMaryNorthApproach";
					approaches[2] = "TauntonMaryNorthApproach";
					
					departs[0] = "MaryTauntonSouthDepart";
					departs[1] = "TauntonMaryEastDepart";
					departs[2] = "MaryTauntonWestDepart";
					
				}else if((appLeg.equals("SimcoeTauntonSouthApproach")) && (deptLeg.equals(""))) {
					nextPhase = "allOutSouth";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "SimcoeTauntonSouthApproach";
					approaches[1] = "SimcoeTauntonSouthApproach";
					approaches[2] = "SimcoeTauntonSouthApproach";
					
					departs[0] = "TauntonMaryEastDepart";
					departs[1] = "MaryTauntonNorthDepart";
					departs[2] = "MaryTauntonWestDepart";
					
				}else if((appLeg.equals("MaryTauntonWestApproach")) && (deptLeg.equals(""))) {

					nextPhase = "allOutWest";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "MaryTauntonWestApproach";
					approaches[1] = "MaryTauntonWestApproach";
					approaches[2] = "MaryTauntonWestApproach";
					
					departs[0] = "MaryTauntonSouthDepart";
					departs[1] = "MaryTauntonNorthDepart";
					departs[2] = "TauntonMaryEastDepart";
					
				}else if((appLeg.equals("MaryTauntonEastApproach")) && (deptLeg.equals(""))) {


					nextPhase = "allOutEast";
					
					approaches= new String[3];
					departs = new String[3];
					
					approaches[0] = "MaryTauntonEastApproach";
					approaches[1] = "MaryTauntonEastApproach";
					approaches[2] = "MaryTauntonEastApproach";
					
					departs[0] = "MaryTauntonSouthDepart";
					departs[1] = "MaryTauntonNorthDepart";
					departs[2] = "MaryTauntonWestDepart";
					  
				}
		  }
		  nextScheduledApproaches = approaches;
		  nextScheduledDepartures = departs;
		  
		  
		  /*
		  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
			  System.out.println(nextPhase + "   APPLEG: " + appLeg + "  DPETLEG: " + deptLeg + approaches[0] + " " + approaches[1] + " "+ approaches[2]);
		  }
		  //*/
		  //System.out.println("SETTING NEXT PHASE Program TO: " + nextPhase);
		  //System.out.println(tlsControllerName + "  :  APP LEG " + appLeg + " DEPT LEG "+ deptLeg + " NEXT  PHASE " + nextPhase);
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
		double wTime;
    	int qLength, pDuration, minInterIntersectionTime = 0;
    	//long remTime;
    	boolean vehPathScheduled;
	    if (msg instanceof SignalRequestMessage) {
		    	
		    SignalRequestMessage srm = (SignalRequestMessage) msg;
		    //System.out.println(getOs().getControlledLanes());// Returns the lanes ([SimcoeTauntonEastApproach_0, SimcoeTauntonEastApproach_1, SimcoeTauntonEastApproach_2, TauntonSimcoeSouthApproach_0, ....) controlled by traffic light
		    
		    
			getLog().info("MSG FOR {} FROM: {} at lat {} and lon {} MSGType {}", srm.getJunctionId(), srm.getVehicleId(), srm.getLocation().getLatitude(),srm.getLocation().getLongitude(), srm.getMsgType() );
			//System.out.println("MSG   FOR : " + srm.getJunctionId() + " FROM : "  + srm.getVehicleId() + " type: " + srm.getMsgType() );
			trfRcdng = new TrafficRecording();
			trfRcdng.setTable(adhocTrafficRecords);
			adhocTrafficRecords = trfRcdng.ProcessSRM(adhocTrafficRecords, srm);
			    
			//getLog().info("TRAFFIC RECORD: {}", adhocTrafficRecords.printAll());
			
				
			if (srm.getJunctionId().equals(tlsControllerName)) { //Check if the SRM is for this controller
			    	
				String appLeg = srm.getApproachLeg();
			    //Send Adhoc V2xMessage Acknowledgement
			    int status = trfRcdng.getStatus(); //GET MSGTYPE from Traffic recording to report successful recording of vehicle data
			    SRMAcknowledgementContent srmAckCnt = new SRMAcknowledgementContent(tlsControllerName, srm.getVehicleId(), srm.getApproachLeg(), srm.getMsgType(), status);
			    final MessageRouting adhocRouting = getOperatingSystem()
			          .getAdHocModule()
			          .createMessageRouting()
			          .topoBroadCast();
			    SRMAcknowledgement adhocSrm = new SRMAcknowledgement(adhocRouting,srmAckCnt, 200L);
			    getOs().getAdHocModule().sendV2xMessage(adhocSrm);
			    
			    
			    //Check if vehicle is a VIP Car, execute phase Recall and notify other intersections
			    if ((srm.getPriority() >= 2) && (srm.getMsgType() == 1)) {
			    	//Check if current phase is different from vehicle's approach 
			    	vehPathScheduled = currentlyHasGreenWave(appLeg);
			    	wTime = getMaxWaitingTime(appLeg, "");
			    	qLength = getQueueLength(appLeg, "");
			    	pDuration = (int) queueDependent.EvaluateFIS(wTime, qLength, 0, 0.0)[0];
			    	//remTime =  (getOs().getSimulationTime() - startTime);
			    	getConcurrentFlows(appLeg, ""); //If we needed to list vehicles that should be scheduled, we would have assign getConcurrentFlows to a value
			    	if (vehPathScheduled) { //If vehicle's approach path is currently being served, then compared remaining time and estimated 
			    		
			    		phaseDuration = ((int)remTime/2000000000) + pDuration; //Extending the phase duration
			    		System.out.println(tlsControllerName + "  SRM: Extending phase Duration");
			    		
			    	}else {// we have to reduce the phase  duration so it transit to yellow and red.
			    		if (phaseDuration > 5) {
			    			phaseDuration = 5; //so that the current phase is set to yellow phase
			    		}
			    		startTime = getOs().getSimulationTime(); //Reset the start an modify the phase duration 
			    		
			    		//nextPhaseDuration = pDuration; //this line is not needed as it is recalculated in scheduleNextTraffic
			    		System.out.println(tlsControllerName + "  SRM: Terminating phase Duration");
			    		
			    	}
			    	
			    	
			   // }else if ((srm.getPriority() >= 2) && (srm.getMsgType() == 2) && (!srm.getApproachLeg().equals(srm.getDepartLeg()))) { //Meaning Priority vehicle is departing the intersection. Thus, send intersection coordination notification message
			    	//Above was commented because ICNM should be sent immediately a high priority vehicle sends an SRM
			    	
			    																		//long tmeStmp, String intstnID, String vehID, int vehPrty, String vehOriging, String vehDest, String vehApp, double vehSpd
			    	
			    	
			    	System.out.println("SRM MESSAGE @: "+ tlsControllerName +" VEH PATH : " + srm.getApproachLeg() + " VEH DEPT: ..." );    
			    }
			}
		
	    }
	}

	private boolean currentlyHasGreenWave(String appLeg) {
		boolean vehPathScheduled = false;
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println(tlsControllerName + "    SCHEDULED LANES LENGTH: " +  scheduledApproaches.length + "    " + appLeg + " in " + scheduledApproaches[0] + ", " + scheduledApproaches[1] + ", "+ scheduledApproaches[2]);
    	for (int i = 0; i < scheduledApproaches.length; i++) {
    		System.out.println(scheduledApproaches[i]);
    		if (scheduledApproaches[i].equals(appLeg)){
    			//System.out.println(tlsControllerName + "    SCHEDULED LENGTH: " +  scheduledApproaches.length + "  APPLEG :  " + appLeg + "" + );
    			vehPathScheduled = true;
    			break;
    		}
    	}
    	System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    	return vehPathScheduled;
    	
	}
	
	private int getInterIntersectionTimeThreshold(String otherIntersection) {
		/*
		 * The function, getInterIntersectionTimeThreshold(String otherIntersection) accepts the name of other intersection
		 * and returns the minimum time require for a vehicle, at certain speed, to travel between this and other intersection.
		 * 
		 * This can be calculated as the ratio of distance (between the two intersections) to the ideal speed.
		 */
		
		int minTime = 0; //Minimum time require by a vehicle to travel from otherIntersection to this Intersection
		if (((tlsControllerName.equals("SimcoeTauntonJunction")) && (otherIntersection.equals("SimcoeRosslandJunction"))) || ((tlsControllerName.equals("SimcoeRosslandJunction")) && (otherIntersection.equals("SimcoeTauntonJunction")))) {
			minTime = 18;
		}else if (((tlsControllerName.equals("SimcoeTauntonJunction")) && (otherIntersection.equals("MaryTauntonJunction"))) || ((tlsControllerName.equals("MaryTauntonJunction")) && (otherIntersection.equals("SimcoeTauntonJunction")))) {
			minTime = 16;
		}else if (((tlsControllerName.equals("MaryRosslandJuction")) && (otherIntersection.equals("MaryTauntonJunction"))) || ((tlsControllerName.equals("MaryTauntonJunction")) && (otherIntersection.equals("MaryRosslandJuction")))) {
			minTime = 16;
		}else if (((tlsControllerName.equals("MaryTauntonJunction")) && (otherIntersection.equals("MaryTauntonJunction"))) || ((tlsControllerName.equals("MaryTauntonJunction")) && (otherIntersection.equals("MaryTauntonJunction")))) {
			minTime = 18;
		}
		return minTime;
		
	}
	
	private  void addPriorityVehicle(String vehid, String appLeg, String depLeg, float spd, int prty) {
		Row row = adhocTrafficRecords.appendRow();
		row.setLong("timestamp",getOs().getSimulationTime());
		row.setString("junctionid",tlsControllerName);
		row.setString("vehicleid", vehid);
		row.setString("approachleg",appLeg);
		row.setString("departleg",depLeg);
		row.setFloat("latitude", (float) intersectionLocation.getLatitude());
		row.setFloat("longitude", (float) intersectionLocation.getLongitude());
		row.setLong("arrivaltime",getOs().getSimulationTime());
		row.setInt("vehicletype",1);
		row.setFloat("speed",spd);
		row.setInt("priority",prty);
		row.setInt("msgType", 1);
		adhocTrafficRecords.addRow(row);
	}
	
	
	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}

}

