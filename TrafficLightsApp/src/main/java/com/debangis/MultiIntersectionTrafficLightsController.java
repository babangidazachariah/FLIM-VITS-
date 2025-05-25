package com.debangis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLight;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightGroup;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.routing.RoutingParameters;
import org.eclipse.mosaic.lib.routing.RoutingPosition;
import org.eclipse.mosaic.lib.routing.RoutingResponse;
import org.eclipse.mosaic.lib.routing.util.ReRouteSpecificConnectionsCostFunction;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import com.debangis.controllers.BenchmarkZachariahEtAl;
import com.debangis.controllers.FuzzyModel;
import com.debangis.controllers.QueueDependentController;
import com.debangis.controllers.QueueLengthDistanceSpeedWaitingTimeController;
import com.debangis.controllers.QueueLengthWaitingTimeDistanceController;

import com.debangis.messages.SPaT;
import com.debangis.messages.SRMAcknowledgement;
import com.debangis.messages.SRMAcknowledgementContent;
import com.debangis.messages.SignalRequestMessage;
import com.debangis.messages.ICNMAcknowledgement;
import com.debangis.messages.ICNMAcknowledgementContent;
import com.debangis.messages.IntersectionCoordinationNotificationMessage;
import com.debangis.messages.IntersectionCoordinationNotificationMessageContent;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import org.eclipse.mosaic.fed.application.ambassador.navigation.INavigationModule;
import org.eclipse.mosaic.fed.application.ambassador.simulation.TrafficLightGroupUnit;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightProgram;

public class MultiIntersectionTrafficLightsController extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {

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
	  String chosenController = "fisModel";
	  
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
	  Table adhocTrafficRecords, previousAdhocTrafficRecords;
	  
	  //Radius of the Earth in kilometers
	  private static final double EARTH_RADIUS = 6371; // in kilometers
	  private GeoPoint intersectionLocation;
		
	  //Fuzzy Logic input parameters
	  long maxWaitingTime  = 0;
	  float maxSpeed = 0, maxDistance = 0;
	  int maxQueueLength = 0;
	  long remTime, lastDepartTime;
	  boolean watchDeparture = false;
	  
	  String[] myControlledLanes, myImportantLanes;
	  
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
    	AdHocModuleConfiguration configuration = new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(17)
                .distance(1000)
                .create();
        getOs().getAdHocModule().enable(configuration);
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
		 previousAdhocTrafficRecords = adhocTrafficRecords; //So they have same schema
		 
		 fisModel = new FuzzyModel();
		 qdswFisModel = new QueueLengthDistanceSpeedWaitingTimeController();
		 qwdFisModel = new QueueLengthWaitingTimeDistanceController();
		 bzFisModel = new BenchmarkZachariahEtAl();
		 queueDependent = new QueueDependentController();
		 //setYellowRedPhasesTime();
		 
		

		 /*
		 * Simcoe-Taunton: 53.546101, 9.965058
		 * Simcoe-Rossland: 53.546152, 9.968502
		 * Rossland-Mary: 53.547693, 9.968449
		 * Taunton-Mary: 53.547767, 9.965096
		 */
		 if (tlsControllerName.equals("SimcoeRosslandJunction")) {
	    		String[] lanes = {"RosslandSimcoeSouthApproach", "SimcoeRosslandWestDepart",
								"RosslandSimcoeSouthApproach", "RosslandSimcoeEastDepart",
								"RosslandSimcoeSouthApproach", "SimcoeRosslandNorthDepart",
								
								"RosslandSimcoeNorthApproach", "RosslandSimcoeEastDepart",
								"RosslandSimcoeNorthApproach", "SimcoeRosslandWestDepart",
								"RosslandSimcoeNorthApproach", "SimcoeRosslandSouthDepart",
								
								"SimcoeRosslandEastApproach", "SimcoeRosslandSouthDepart",
								"SimcoeRosslandEastApproach", "SimcoeRosslandNorthDepart",
								"SimcoeRosslandEastApproach", "SimcoeRosslandWestDepart",
								
								"SimcoeRosslandWestApproach", "SimcoeRosslandNorthDepart",
								"SimcoeRosslandWestApproach", "RosslandSimcoeEastDepart",
								"SimcoeRosslandWestApproach", "SimcoeRosslandSouthDepart"};
								
	    		myControlledLanes = lanes;	
	    		intersectionLocation = GeoPoint.latLon(53.546152, 9.968502);

	    		String[] impLanes = {"SimcoeRosslandWestApproach", "RosslandSimcoeNorthApproach"};
	    		myImportantLanes = impLanes;
	    		
	    		
	    	}else if (tlsControllerName.equals("MaryRosslandJuction")) {
	    		String[] lanes  = {"RosslandMarySouthApproach", "RosslandMaryWestDepart",
							"RosslandMarySouthApproach", "RosslandMaryEastDepart",
							"RosslandMarySouthApproach", "MaryRosslandNorthDepart",
							
							"RosslandMaryNorthApproach", "RosslandMaryEastDepart",
							"RosslandMaryNorthApproach", "RosslandMarySouthDepart",
							"RosslandMaryNorthApproach", "RosslandMaryWestDepart",
							
							
							"MaryRosslandEastApproach", "RosslandMarySouthDepart",
							"MaryRosslandEastApproach", "MaryRosslandNorthDepart",
							"MaryRosslandEastApproach", "RosslandMaryWestDepart",
							
							"MaryRosslandWestApproach", "MaryRosslandNorthDepart",
							"MaryRosslandWestApproach", "RosslandMarySouthDepart",
							"MaryRosslandWestApproach", "RosslandMaryEastDepart"};
	    		
	    		myControlledLanes = lanes;		
	    		intersectionLocation = GeoPoint.latLon(53.547693, 9.968449);
	    		
	    		String[] impLanes = {"RosslandMarySouthApproach", "MaryRosslandWestApproach"};
	    		myImportantLanes = impLanes;
	    		
	    	}else if (tlsControllerName.equals("MaryTauntonJunction")) {
	    		String[] lanes  = {"MaryTauntonEastApproach", "MaryTauntonSouthDepart",
							"MaryTauntonEastApproach", "MaryTauntonNorthDepart",
							"MaryTauntonEastApproach", "MaryTauntonWestDepart",
							
							"MaryTauntonWestApproach", "MaryTauntonNorthDepart",
							"MaryTauntonWestApproach", "TauntonMaryEastDepart",
							"MaryTauntonWestApproach", "MaryTauntonSouthDepart",
							
							
							"TauntonMaryNorthApproach", "TauntonMaryEastDepart",
							"TauntonMaryNorthApproach", "MaryTauntonWestDepart",
							"TauntonMaryNorthApproach", "MaryTauntonSouthDepart",
							
							"SimcoeTauntonSouthApproach", "MaryTauntonWestDepart",
							"SimcoeTauntonSouthApproach", "TauntonMaryEastDepart",
							"SimcoeTauntonSouthApproach", "MaryTauntonNorthDepart"};
	    		myControlledLanes = lanes;
	    		intersectionLocation = GeoPoint.latLon(53.547767, 9.965096);
	    		
	    		String[] impLanes = {"MaryTauntonEastApproach", "SimcoeTauntonSouthApproach"};
	    		myImportantLanes = impLanes;
	    		
			}else if (tlsControllerName.equals("SimcoeTauntonJunction")) {
				String[] lanes  = {"SimcoeTauntonEastApproach", "SimcoeTauntonSouthDepart",
								"SimcoeTauntonEastApproach", "SimcoeTauntonNorthDepart",
								"SimcoeTauntonEastApproach", "SimcoeTauntonWestDepart",
							
								"SimcoeTauntonWestApproach", "SimcoeTauntonNorthDepart",
								"SimcoeTauntonWestApproach", "SimcoeTauntonEastDepart",
								"SimcoeTauntonWestApproach", "SimcoeTauntonSouthDepart",
								
								
								"TauntonSimcoeNorthApproach", "SimcoeTauntonEastDepart",
								"TauntonSimcoeNorthApproach", "SimcoeTauntonWestDepart",
								"TauntonSimcoeNorthApproach", "SimcoeTauntonSouthDepart",
								
								"TauntonSimcoeSouthApproach", "SimcoeTauntonWestDepart",
								"TauntonSimcoeSouthApproach", "SimcoeTauntonEastDepart",
								"TauntonSimcoeSouthApproach", "SimcoeTauntonNorthDepart"};
				myControlledLanes = lanes;	
	    		intersectionLocation = GeoPoint.latLon(53.546101, 9.965058);			

	    		String[] impLanes = {"TauntonSimcoeNorthApproach", "SimcoeTauntonEastApproach"};
	    		myImportantLanes = impLanes;
	    		
			}
		 
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

	  
	private String[] getNextProposedSchedule(String[] lanes){
		
		System.out.println(lanes[0] + " " + lanes[1] + " || " + lanes[6]+ " " + lanes[7]);
		
		System.out.println(lanes[2] + " " + lanes[3] + " && " + lanes[4]+ " " + lanes[5] + " Also " + lanes[8] + " " + lanes[9] + " && " + lanes[10]+ " " + lanes[11]);
		
		Table nsLeftTurn = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[0]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[1]))
				.or(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[6]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[7])) ) );
		
		Table neStraightRightTurn = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[2]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[3]))
				.or(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[4]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[5])) )
				.or(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[8]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[9])) )
				.or(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[10]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[11])) ));
		
		double nsLeftWT = getAveragePhaseDelay(nsLeftTurn);
		double nsStraightRightWT = getAveragePhaseDelay(neStraightRightTurn);
		
		Table weLeftTurn = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[12]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[13]))
				.or(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[18]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[19])) ) );
		
		Table weStraightRightTurn = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[14]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[15]))
				.or(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[16]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[17])) )
				.or(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[20]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[21])) )
				.or(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(lanes[22]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(lanes[23])) ));
		
		double weLeftWT = getAveragePhaseDelay(weLeftTurn);
		double weStraightRightWT = getAveragePhaseDelay(weStraightRightTurn);
		String[] apps = {lanes[0], lanes[2], lanes[12], lanes[14]};
		String[] depts = {lanes[1], lanes[3], lanes[13], lanes[15]};
		double[] wts = {nsLeftWT, nsStraightRightWT, weLeftWT, weStraightRightWT}; 
		int maxAvgIndex = 0;
		double avgWT = 0;
		for (int i = 0; i < wts.length - 1; i++) {
			if (wts[i] > wts[i+1]) {
				maxAvgIndex = i;
				avgWT = wts[i];
			}else {
				maxAvgIndex = i + 1;
				avgWT = wts[i+1];
			}
		}
		
		String[] nextSchedule = {Double.toString(avgWT), apps[maxAvgIndex], depts[maxAvgIndex]};
		return nextSchedule;
	}
	
	
	private double getAveragePhaseDelay(Table phaseVehs){
		/*
		 * https://www.mathworks.com/products/connections/product_detail/Ibm-ilog-cplex.html
         * Higher priority approach convert to objective function
         */
		 
		double avgWT = 0.0;
		
        LongColumn longColumn = phaseVehs.longColumn("arrivaltime");
        DoubleColumn differenceColumn = longColumn.subtract(getOs().getSimulationTime());

        differenceColumn = differenceColumn.multiply(-1);
        avgWT = differenceColumn.mean();
        avgWT = avgWT > 0 ? avgWT : 0;
        System.out.println("avgWT  " + avgWT);
		return avgWT;
	}
	
	private String determineVehiculeTurn(String appLeg, String deptLeg) {
		String turn = "";
		if (((appLeg.endsWith("northApproach")) && (deptLeg.endsWith("westDepart"))) || ((appLeg.endsWith("NorthApproach")) && (deptLeg.endsWith("WestDepart"))) ){
			turn = "right";
			
			  
		}else if (((appLeg.endsWith("northApproach")) && (deptLeg.endsWith("southDepart"))) || ((appLeg.endsWith("NorthApproach")) && (deptLeg.endsWith("SouthDepart"))) ) {
			turn = "straight";
			
			  
		}else if (((appLeg.endsWith("northApproach")) && (deptLeg.endsWith("eastDepart"))) || ((appLeg.endsWith("NorthApproach")) && (deptLeg.endsWith("EastDepart")))) {
			turn = "left";
			
			
			  
		}else if (((appLeg.endsWith("southApproach")) && (deptLeg.endsWith("northDepart"))) || ((appLeg.endsWith("SouthApproach")) && (deptLeg.endsWith("NorthDepart"))) ){
			turn = "straight";
			  
		}else if (((appLeg.endsWith("southApproach")) && (deptLeg.endsWith("westDepart"))) || ((appLeg.endsWith("SouthApproach")) && (deptLeg.endsWith("WestDepart"))) ){
			turn = "left";
			
			  
		}else if ( ((appLeg.endsWith("southApproach")) && (deptLeg.endsWith("eastDepart"))) || ((appLeg.endsWith("SouthApproach")) && (deptLeg.endsWith("EastDepart")))) {
			turn = "right";
			
		}else if ( ((appLeg.endsWith("westApproach")) && (deptLeg.endsWith("northDepart"))) || ((appLeg.endsWith("WestApproach")) && (deptLeg.endsWith("NorthDepart")))) {
			turn = "left";

			  
		}else if (((appLeg.endsWith("westApproach")) && (deptLeg.endsWith("eastDepart"))) || ((appLeg.endsWith("WestApproach")) && (deptLeg.endsWith("EastDepart"))) ){
			turn = "straight";
			
			  
		}else if (((appLeg.endsWith("westApproach")) && (deptLeg.endsWith("southDepart"))) || ((appLeg.endsWith("WestApproach")) && (deptLeg.endsWith("SouthDepart")))) {
			turn = "right";
			
			
		}else if (((appLeg.endsWith("eastApproach")) && (deptLeg.endsWith("northDepart"))) || ((appLeg.endsWith("EastApproach")) && (deptLeg.endsWith("NorthDepart"))) ){
			turn = "right";
			
			  
		}else if (((appLeg.endsWith("eastApproach")) && (deptLeg.endsWith("southDepart"))) || ((appLeg.endsWith("EastApproach")) && (deptLeg.endsWith("SouthDepart"))) ){
			turn = "left";

			  
			  
		}else if (((appLeg.endsWith("eastApproach")) && (deptLeg.endsWith("westDepart"))) || ((appLeg.endsWith("EastApproach")) && (deptLeg.endsWith("WestDepart"))) ){
			turn = "straight";

		}	
		return turn;	  
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
				  /*
				  if (CompareTrafficStates(previousAdhocTrafficRecords, tmpRecord)) {
					  roundRobinControl();
					  System.out.println("DEADLOCK DETECTED");
				  
				  }else {
					  //*/
					  Table vehRec,vehRecd;
					  String appLeg = "", deptLeg = "";
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
					  boolean executePriority = false;
					  boolean conflictingPriority = false; 
					  
					  
					  if (nextPhasePriority > 1) { //Meaning high priority vehicle is around this intersection
						  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
							  System.out.println("HIGH PRIORITY Prev " + nextPhasePriority + "  @  " + tlsControllerName);
						  }
						  //Get vehicles with the highest priority
						  vehRec = tmpRecord.where(vehPrtyCol.isEqualTo(nextPhasePriority));
						  
						  if (vehRec.rowCount() > 1) {
							  appLeg = vehRec.stringColumn("approachleg").getString(0);
							  for (int i = 1; i < vehRec.rowCount(); i++) {
								  if (vehRec.stringColumn("approachleg").getString(0).equals(appLeg)) { //Check to see if there conflicting priority request at this intersection
									  conflictingPriority = true;
									  break;
								  }
							  }
						  }
						  
						//Get approach with a vehicle that has the earliest arrival
						  phaseVehArrival = vehRec.longColumn("arrivaltime");
						  vehRecd = vehRec.where(phaseVehArrival.isEqualTo(phaseVehArrival.min()));
						  
						  //System.out.println("Veh With Max Waiting: " + vehRec.printAll());
						  appLeg = vehRecd.stringColumn("approachleg").getString(0);
						  
						  if ((conflictingPriority == true) || (tmpRecord.where(tmpRecord.stringColumn("approachleg").isEqualTo(appLeg)).rowCount() >= 25)){
							  
							  
							  deptLeg = ""; //vehRecd.stringColumn("departleg").getString(0);
							  maxWaitingTime = (getOs().getSimulationTime() - (long)phaseVehArrival.min());
							  executePriority = true;
						  }
						  //System.out.println("HIGH PRIORITY Now Schedule: " + appLeg + "   and  "+ deptLeg + "  @  " + tlsControllerName);
						  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
							  System.out.println("HIGH PRIORITY FOR  " + vehRecd.stringColumn("vehicleid").getString(0)+ "   ON " + appLeg + "  @  " + tlsControllerName);
						  }
						  
					  }
					  if (executePriority == false) {
						  int impLane1 =  getQueueLength(myImportantLanes[0], ""), impLane2 = getQueueLength(myImportantLanes[1], "");
						  if ((impLane1 >= 30) || (impLane2 >= 30)){ // If there is high density on approach between two intersections
							  	if (impLane1 > impLane2) {
							  		appLeg = myImportantLanes[0];
							  	}else {
							  		appLeg = myImportantLanes[1];
							  	}
							  	//Query the app and determine turnings of vehicles
							  	Table veh = tmpRecord.where(tmpRecord.stringColumn("approachleg").isEqualTo(appLeg));
							  	Table cat = veh.countBy("departleg");
							  	NumericColumn<?> ct = cat.numberColumn("Count");
							  	StringColumn dept = cat.stringColumn("Category").where(cat.intColumn("Count").isEqualTo(ct.max()));
							  	deptLeg = ""; //dept.getString(0);
							  	//System.out.println("CHOSEN Dept: " + deptLeg);
							  	nextPhasePriority = 2;
							  	phaseVehArrival = veh.longColumn("arrivaltime");
								//Earliest arriving time
								maxWaitingTime = (getOs().getSimulationTime() - (long)phaseVehArrival.min());
							  	if (tlsControllerName.equals("SimcoeTauntonJunction")) {
									  System.out.println("IMPORTANTLANES Prev " + nextPhasePriority + "  @  " + tlsControllerName);
								 }
						  }else { //Vehicles have default priority of 1
							//*
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
							  
							  //*/
							  /*
							  String[] nextSchedule = getNextProposedSchedule(myControlledLanes);
							  maxWaitingTime = (long)  Double.parseDouble(nextSchedule[0]);
							  appLeg = nextSchedule[1];
							  deptLeg = nextSchedule[2];
							  //*/
							  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
								  System.out.println("NORMAL SCHEDULE Prev " + nextPhasePriority + "  @  " + tlsControllerName);
							  }
						  }
					  }
					  /*
					  if (tlsControllerName.equals("SimcoeTauntonJunction")) {
						  System.out.println(vehRec.stringColumn("vehicleid").getString(0) + appLeg + "  " + deptLeg);
					  }
					  */
					  //nextPhase = getNextPhaseProgram(appLeg, deptLeg);
					  //System.out.println("APPROACH: " + appLeg + " NEXT PHASE " + nextPhase);
					  ArrayPair concFlows = getConcurrentFlows(appLeg, deptLeg);
					  
					  //System.out.println("TRAFFIC Scheduled for: " + appLeg + "   and  "+ deptLeg + "  @  " + tlsControllerName + "  is  " + nextPhase);
					  
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
				  //}
				  previousAdhocTrafficRecords = tmpRecord;
				  
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
	  
	  private int getApproachQueueLength(String appLeg) {
			 //Returns the number of vehicles on a given approach (appLeg). The result may be used to calculate density 
			  Table phaseVeh = adhocTrafficRecords, catCount;
			  int vehCount = 0;
			  
			  phaseVeh = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(appLeg));
			  catCount = phaseVeh.countBy("departleg"); //Use to group and count vehicles based on their approach lanes.
			  NumericColumn<?> ct = catCount.numberColumn("Count");
			  vehCount = (int) ct.max(); //we take the approach lane with max number of vehicles
		  
			  
			  
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
		  //System.out.println(getOs().getSimulationTime());
		  //This function is used to set traffic lights and send messages that needs to be send 
		  long runningTime = (getOs().getSimulationTime() - startTime); // 1000000000

		  remTime = ((phaseDuration*1000000000L) - runningTime) ;
		  //System.out.println("remTime : " + remTime);
		  remTime = remTime > 0 ? remTime : 0;
		  //System.out.println(tlsControllerName + "   phaseDuration :  " + phaseDuration + "  runningTime : " + runningTime + " remTime : " + remTime + " startTime : " + startTime);
			if ((remTime >= (3*1000000000L)) && (remTime < (4*1000000000L))) {// Schedule Next Vehicles/Flows and the end of the current phase (Set Yellow and Clearance Phases)
				setNextSchedule();
				//System.out.println(tlsControllerName + "  NEXT PHASE = " + nextPhase);
				//setYellowRedPhasesTime();
				if (!nextPhase.equals(currentPhase)) { //if nextPhase is same as the currentPhase, then no need to set yellow and red-clearcance phases
					setYellowPhase();
				}
				
			}else if ((remTime >= (1*1000000000L)) && (remTime < (2*1000000000L))) {
				
				if (!nextPhase.equals(currentPhase)) { //if nextPhase is same as the currentPhase, then no need to set yellow and red-clearcance phases
					setAllRedPhase();
				}
				
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
			
			/*
			if (watchDeparture) {
				if ((((getOs().getSimulationTime() - lastDepartTime)/1000000000L) > 7) && ((remTime)/1000000000L) > 5) {
					//Reset phase duration as there are no vehicles passing
					phaseDuration = 5; //so that the current phase is set to yellow phase
					startTime = getOs().getSimulationTime(); //Reset the start an modify the phase duration
					System.out.println("EARLY DEADLOCK DETECTED");
	    		}
			}
			//*/
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
		  /*
		  if ((tlsControllerName.equals("SimcoeTauntonJunction")) || (tlsControllerName.equals("SimcoeRosslandJunction"))) {
			  System.out.println(tlsControllerName + "  SET PHASE = " + phase);
		  }
		  */
		  //Event setNextScheduleEvent = new Event(startTime  + (phaseDuration - 5)* 1000000000L, this::setNextSchedule);
		  //getOs().getEventManager().addEvent(setNextScheduleEvent);
		  
		  //Event setGreenEvent = new Event(startTime + (phaseDuration * 1000000000L), this::setGreenPhase);
		  //getOs().getEventManager().addEvent(setGreenEvent);
		    
	  }
	  
	  public void roundRobinControl(){
		  
		  String trafficLight = "";
		  
		  //while(true){ //Loop forever 
		  //String[] greenPhaseSequence = {"northSouthLeft", "northSouthStraightRight", "eastWestLeft", "eastWestStraightRight", "allOutNorth", "allOutSouth", "allOutWest", "allOutEast"};
			  if(phasePointer == 0){
				  trafficLight = "northSouthLeft";
				  phasePointer += 1;
			  }else if(phasePointer == 1){
				  trafficLight = "northSouthStraightRight";
				  phasePointer += 1;
			  }else if(phasePointer == 2){
				  trafficLight = "eastWestLeft";
				  phasePointer += 1;
			  }else if(phasePointer >= 3){
				  trafficLight = "eastWestStraightRight";
				  phasePointer = 0;
			  }else {
				  
			  }
			  //
			  //((TrafficLightOperatingSystem)getOs()).switchToProgram(trafficLight);
			  long dur = ((TrafficLightOperatingSystem) getOs()).getSimulationTime() + 1000000L;
			  //while (((TrafficLightOperatingSystem) getOs()).getSimulationTime() < dur){

			  getLog().infoSimTime(this, "Roundrobin: Traffic Lights Assigned to " + trafficLight );
			  nextPhase = trafficLight;
			  getControlledApproaches(nextPhase);
			  //System.out.println("ROUNDROBIN:  " + tlsControllerName + " phasePointer: " + phasePointer + "  NEXT PHASE = " + nextPhase);
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
			  //System.out.println("@" +tlsControllerName+ "getControlledApproaches " + phase + "   " + approaches[0] + " " + approaches[1] + " "+ approaches[2]);
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
		  //System.out.println("@" +tlsControllerName+ "getControlledApproaches for " + nextPhase + "  are: " + approaches[0] + " " + approaches[1] + " "+ approaches[2]);
		  
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
    	int qLength, pDuration = 0, minInterIntersectionTime = 0;
    	//long remTime;
    	boolean vehPathScheduled;
	    if (msg instanceof SignalRequestMessage) {
		    	
		    SignalRequestMessage srm = (SignalRequestMessage) msg;
		    //System.out.println(getOs().getControlledLanes());// Returns the lanes ([SimcoeTauntonEastApproach_0, SimcoeTauntonEastApproach_1, SimcoeTauntonEastApproach_2, TauntonSimcoeSouthApproach_0, ....) controlled by traffic light
		    
		    if (srm.getJunctionId().equals(tlsControllerName)) { //Check if the SRM is for this controller
		    	
				getLog().info("MSG FOR {} FROM: {} at lat {} and lon {} MSGType {}", srm.getJunctionId(), srm.getVehicleId(), srm.getLocation().getLatitude(),srm.getLocation().getLongitude(), srm.getMsgType());
				//System.out.println("MSG   FOR : " + srm.getJunctionId() + " FROM : "  + srm.getVehicleId() + " type: " + srm.getMsgType() );
				trfRcdng = new TrafficRecording();
				trfRcdng.setTable(adhocTrafficRecords);
				adhocTrafficRecords = trfRcdng.ProcessSRM(adhocTrafficRecords, srm);
				    
				//getLog().info("TRAFFIC RECORD: {}", adhocTrafficRecords.printAll());
			
				
			
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
			    
			    int msgtype = srm.getMsgType();
			    //Set last received departure notification
			    if (msgtype == 2) {
			    	lastDepartTime = getOs().getSimulationTime();
			    	watchDeparture = true;
			    }
			    
			    //Check if vehicle is a VIP Car, execute phase Recall and notify other intersections
			    if ((srm.getPriority() >= 2) && (msgtype == 1)) {
			    	//Check if current phase is different from vehicle's approach 
			    	vehPathScheduled = currentlyHasGreenWave(appLeg);
			    	wTime = getMaxWaitingTime(appLeg, "");
			    	qLength = getQueueLength(appLeg, "");
			    	pDuration = (int)getPhaseDuration(wTime, qLength, 0, 0);
			    	//remTime =  (getOs().getSimulationTime() - startTime);
			    	//getConcurrentFlows(appLeg, ""); //If we needed to list vehicles that should be scheduled, we would have assign getConcurrentFlows to a value
			    	if (vehPathScheduled) { //If vehicle's approach path is currently being served, then compared remaining time and estimated 
			    		
			    		phaseDuration = ((int)remTime/2000000000) + pDuration; //Extending the phase duration
			    		System.out.println(tlsControllerName + "  SRM: Extending phase Duration Rem: " + remTime + " by "+ pDuration + " to "+ phaseDuration);
			    		
			    	}else {// we have to reduce the phase  duration so it transit to yellow and red.
			    		if (phaseDuration > 5) {
			    			phaseDuration = 5; //so that the current phase is set to yellow phase
			    		}
			    		startTime = getOs().getSimulationTime(); //Reset the start an modify the phase duration 
			    		
			    		//nextPhaseDuration = pDuration; //this line is not needed as it is recalculated in scheduleNextTraffic
			    		System.out.println(tlsControllerName + "  SRM: Terminating phase Duration");
			    		
			    	}
			    	Table appVol = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(appLeg));
			    	icnmCnt = new IntersectionCoordinationNotificationMessageContent(getOs().getSimulationTime(), tlsControllerName, srm.getVehicleId(), srm.getPriority(), 
							srm.getApproachLeg(), srm.getPath(), srm.getApproachLeg(), srm.getSpeed(), srm.getLocation(), appVol.rowCount());

			    	sendIcnm = true; //Start IntersectionCoordinationNotificationMessageContent
			    	
			   // }else if ((srm.getPriority() >= 2) && (srm.getMsgType() == 2) && (!srm.getApproachLeg().equals(srm.getDepartLeg()))) { //Meaning Priority vehicle is departing the intersection. Thus, send intersection coordination notification message
			    	//Above was commented because ICNM should be sent immediately a high priority vehicle sends an SRM
			    	
			    																		//long tmeStmp, String intstnID, String vehID, int vehPrty, String vehOriging, String vehDest, String vehApp, double vehSpd
			    	
			    	
			    	//System.out.println("SRM MESSAGE @: "+ tlsControllerName +" VEH PATH : " + srm.getApproachLeg() + " VEH DEPT: ..." );    
			    }
			}
		
	    }else  if (msg instanceof IntersectionCoordinationNotificationMessage) { //If this controller receives the IntersectionCoordinationNotificationMessageContent
	    	/*
	    	 * One of the advantages of ICNM is that it can be deployed to regions where packing/stopping is prohibited but traffic congestion can be high
	    	 * 
	    	 * If this controller receives an intersection coordination notification message from another controller:
	    	 * 		Check if the high priority vehicle is likely to pass through this intersection using getPath() of the message
	    	 * 		if high priority vehicle would pass through this intersection, 
	    	 * 			Check traffic scenario on the approach of the high priority vehicle and determine time requirement (phase duration) for the current situation
	    	 * 			if phase duration requirement of priority vehicle approach >= threshold value (say 10s) OR the remaining phase duration of the current phase >= threshold value
	    	 * 				Perform traffic light control preemption and reassign the right-of-way to the approach of the high priority vehicle 
	    	 * 			else
	    	 * 				Do not preempt but schedule the priority vehicle approach next (add vehicle to adhocTrafficRecords using addPriorityVehicle(String vehid, String appLeg, String depLeg, float spd, int prty))
	    	 * 
	    	 * Threshold value is dependent on the estimated time value for a vehicle under ideal speed to travel from previous intersection to this intersection
	    	 */
	    	//System.out.println("ICNM Received");
	    	IntersectionCoordinationNotificationMessage icnm = (IntersectionCoordinationNotificationMessage)msg;
	    	
	    	minInterIntersectionTime = getInterIntersectionTimeThreshold(icnm.getIntersectioID());
	    	if (minInterIntersectionTime > 0) { //If true, it means vehicle may pass this intersection
	    		//System.out.println("ICNM Received by " + tlsControllerName);
		    	String priorityVehPath[] = icnm.getVehiclePath().split(";");
		    	Collection<String> myControlledLanes = getOs().getControlledLanes();
		    	boolean comingToThisIntersection = false;
		    	String vehApproach = "";
		    	//Check if priority vehicle path passes through this intersection
		    	
		    	String vehPth = "", vehDepart = "";
		    	for (int i = 0; i < priorityVehPath.length; i++) {
		    		vehPth = priorityVehPath[i].trim();
		    		/*
		    		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		    		System.out.println("ICNM MESSAGE : VEH PATH : " + vehPth);
			    	System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			    	*/
		    		if (vehPth.contains("Approach")) {
		    			
		    			for (String element : myControlledLanes) {
		    				
		    			      if(element.contains(vehPth)){
		    			    	  //System.out.println(tlsControllerName + ": " + element + " && " + vehPth);
		    			    	  
		    			    	  comingToThisIntersection = true;
		    			    	  vehApproach = vehPth;//.substring(0,  vehPth.indexOf("_"));
		    			    	  vehDepart = priorityVehPath[i+1];//.substring(0, priorityVehPath[i+1].indexOf("_"));
		    			    	  break;
		    			      }
			            }
		    		}
		    	}
		    	//System.out.println("ICNM MESSAGE @: "+ tlsControllerName +" VEH PATH : " + vehApproach + " VEH DEPT: " + vehDepart);
		    	if (comingToThisIntersection) {
		    		//Send Acknowledgement to sender intersection.
		    		ICNMAcknowledgementContent icnmAckCnt = new ICNMAcknowledgementContent(getOs().getSimulationTime(), tlsControllerName, icnm.getIntersectioID(), 1);
		    		
		    		final MessageRouting adhocIcnmAckRouting = getOperatingSystem()
					          .getAdHocModule()
					          .createMessageRouting()
					          .topoBroadCast();
		    		ICNMAcknowledgement adhocIcnmAck = new ICNMAcknowledgement(adhocIcnmAckRouting, icnmAckCnt, 200L);
					getOs().getAdHocModule().sendV2xMessage(adhocIcnmAck);
					
					qLength = getApproachQueueLength(vehApproach);
					
		    		wTime = getMaxWaitingTime(vehApproach, "");
			    	/*
			    	pDuration = (int)getPhaseDuration(wTime, qLength, 0, 0);
			    	int otherVeh = (int)getPhaseDuration(50, ((icnm.getApproachVolume())/3), 0, 0);
			    	
			    	if ((pDuration > 25) || (otherVeh <25)){
					    	vehPathScheduled = currentlyHasGreenWave(icnm.getVehicleApproach());
					    	
					    	//if (((pDuration >= minInterIntersectionTime) || ((remTime/1000000000) >= ((int)(minInterIntersectionTime/2)))) && (vehPathScheduled == false)){ //Meaning time required to clear the current traffic density on the priority vehicle's approach  is greater than the time the priority vehicle requires to arrive this intersection
					    	if (vehPathScheduled == false){	
					    		if (phaseDuration > 5) {
					    			phaseDuration = 5; //Reset phaseDuration to 4s so that the current phase end and
					    		}
					    		
					    		startTime = getOs().getSimulationTime(); //Reset the start an modify the phase duration 
					    		//System.out.println(tlsControllerName + "  ICNM: Terminating phase Duration  @: " + tlsControllerName );
						    }
					    	
					    	//To add high priority vehicle information sent by another intersection, consider traffic density at this intersection
					    	
					    	//Add instance of high priority vehicle to Traffic Record, so its approach is scheduled next
					    	addPriorityVehicle(icnm.getVehicleID(), vehApproach,  vehDepart, ((float)icnm.getVehicleSpeed()), icnm.getVehiclePriority());
					}
					//*/

			    	//Add instance of high priority vehicle to Traffic Record, so its approach is scheduled next
			    	addPriorityVehicle(icnm.getVehicleID(), vehApproach,  vehDepart, ((float)icnm.getVehicleSpeed()), icnm.getVehiclePriority());
			    	//System.out.println("ICNM MESSAGE @: "+ tlsControllerName +" VEH PATH : " + vehApproach + " VEH DEPT: " + vehDepart);
		    	}else { //Vehicle will not be passing through this intersection. Thus, continue scheduling based on traffic situation at this intersection
		    		
		    	}
	    	}
	    	
	    }else  if (msg instanceof ICNMAcknowledgement) { //Intersection Coordination Notification Message Acknowledgement
	    	ICNMAcknowledgement icnmAck = (ICNMAcknowledgement) msg;
	    	if (icnmAck.getReceiverIntersectionID().equals(tlsControllerName) && icnmAck.getStatus() == 1) {
	    		sendIcnm = false; //Stop sending IntersectionCoordinationNotificationMessage
	    	} 
	    }
	}

	private double getPhaseDuration(double wT, double qL, double mS, double mD) {
		int pDuration = 0;
		//pDuration = (int) queueDependent.EvaluateFIS(wTime, qLength, 0, 0.0)[0];
    	if (chosenController.equals("fisModel")){
			  double[] fisOutput = fisModel.EvaluateFIS(wT, qL,  mS, mD);
			  pDuration = (int) fisOutput[0];
			  
		  }else if(chosenController.equals("qdswFisModel")){
			  double[] fisOutput = qdswFisModel.EvaluateFIS(wT, qL,  mS, mD);
			  pDuration = (int) fisOutput[0];
			  
		  }else if(chosenController.equals("qwdFisModel")){
			  
			  double[] fisOutput = qwdFisModel.EvaluateFIS(wT, qL, mS, mD);
			  pDuration = (int) fisOutput[0];
			  
		  }else if(chosenController.equals("bzFisModel")){
			  
			  double[] fisOutput = bzFisModel.EvaluateFIS(wT, qL,  mS, mD);
			  pDuration = (int) fisOutput[0];
		  }else if(chosenController.equals("queueDependent")){
	  
			  double[] fisOutput = queueDependent.EvaluateFIS(wT, qL,  mS, mD);
			  pDuration = (int) fisOutput[0];
		  }
    	return pDuration;
	}
	private boolean currentlyHasGreenWave(String appLeg) {
		boolean vehPathScheduled = false;
		/*
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println(tlsControllerName + "    SCHEDULED LANES LENGTH: " +  scheduledApproaches.length + "    " + appLeg + " in " + scheduledApproaches[0] + ", " + scheduledApproaches[1] + ", "+ scheduledApproaches[2]);
    	//*/
    	for (int i = 0; i < scheduledApproaches.length; i++) {
    		//System.out.println(scheduledApproaches[i]);
    		if (scheduledApproaches[i].equals(appLeg)){
    			//System.out.println(tlsControllerName + "    SCHEDULED LENGTH: " +  scheduledApproaches.length + "  APPLEG :  " + appLeg + "" + );
    			vehPathScheduled = true;
    			break;
    		}
    	}
    	//System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
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
