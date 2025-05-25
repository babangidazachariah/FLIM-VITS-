package com.debangis;

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
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import com.debangis.controllers.*;

import com.debangis.messages.*;


import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class UrgencyBasedAlgorithm extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
	  

	/**
	 * This is platoon-based controller, which schedule and publishes individual vehicles 
	 * that are allowed to utilised the intersection 
	 */
	
	int minPhaseDuration = 7;
	int maxPhaseDuration = 25;
	int nextPhaseDuration = 0, phaseDuration = minPhaseDuration; 
	
	//Traffic Recording Variables
	TrafficRecording trfRcdng;
	Table adhocTrafficRecords, previousTrafficRecords;
	FuzzyModel fisModel;// = new FuzzyModel();
	UrgencyController urgCtrler;
	UrgencyBasedPhaseDurationController urgPhaseDuration;
	
	//Radius of the Earth in kilometers
	private static final double EARTH_RADIUS = 6371; // in kilometers
	private GeoPoint intersectionLocation;
	private GeoCircle geoCircle;
	private  MessageRouting routing;
	
	String nextPhase;
	String currentPhase;
	int previousPhasePointer = 0, phasePointer = 0; 
	String nextAppLegs, nextDepartLegs, nextVehList;
	
	String[] greenPhaseSequence = {"northsouthleft", "northsouthstraightright", "eastwestleft", "eastweststraightright"};
	String[] yellowPhaseSequence = {"northsouthleftyellow", "northsouthstraightrightyellow", "eastwestleftyellow", "eastweststraightrightyellow"};
	String allRed = "allred";
	
	SPaTContent spatContent, nextSpatContent;
	SPaT spat, nextSpat;
	long startTime;
	String trafficLightProgram;
	String junctionID = "Junction";
	GeoPoint junctionPoint;
	
	

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
		  
	    
	    Event event = new Event(getOs().getSimulationTime() + 2000000000L, this::initializeParameters);
		getOs().getEventManager().addEvent(event);
		
		event = new Event(getOs().getSimulationTime() + 5000000000L, this::publishScheduledVehicles);
		getOs().getEventManager().addEvent(event);
		
		event = new Event(getOs().getSimulationTime() + 3000000000L, this::scheduleVehicles);
		getOs().getEventManager().addEvent(event);
		
		event = new Event(getOs().getSimulationTime() + 4000000000L, this::setTrafficProgram);
		getOs().getEventManager().addEvent(event);
	}

	private void publishScheduledVehicles(Event event) {
		
		if (spat != null) {
			//System.out.println("SPaT is not Null");
			//Send Adhoc V2xMessage
			final MessageRouting adhocRouting = getOperatingSystem()
	                .getAdHocModule()
	                .createMessageRouting()
	                .topoBroadCast();
				
			spat = new SPaT(adhocRouting, spatContent, 200L);
			
			getOs().getAdHocModule().sendV2xMessage(spat);
			//getLog().info("SPaT Sent {}, {}",  spat.getScheduledVehicles(), spat.getApproachLegs());
			//System.out.println("publishScheduledVehicles Executed");
		}
		
		event = new Event(getOs().getSimulationTime() + 1000000000L, this::publishScheduledVehicles);
		getOs().getEventManager().addEvent(event);
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
	private void initializeParameters(Event event) {
		trfRcdng = new TrafficRecording();
		adhocTrafficRecords = trfRcdng.CreateTrafficRecording("adhocTrafficRecords");
		previousTrafficRecords = trfRcdng.CreateTrafficRecording("previousTrafficRecords");
		intersectionLocation = GeoPoint.latLon(53.547745, 9.966246);
		junctionPoint = intersectionLocation;
		fisModel = new FuzzyModel();
		urgCtrler = new UrgencyController();
		urgPhaseDuration = new UrgencyBasedPhaseDurationController();
		
		final MessageRouting adhocRouting = getOperatingSystem()
              .getAdHocModule()
              .createMessageRouting()
              .topoBroadCast();
		spat = new SPaT(adhocRouting, new SPaTContent (getOs().getSimulationTime(), 7), 200);
		//System.out.println("initializeParameters Done Start Time: " + spat.getStartTime());
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

	private void setTrafficProgram(Event event) {
		/*
		 * rrrgrrrrrrrgrrrr east west left
		 * gggrrrrrgggrrrrr east west straight right
		 * rrrrrrrgrrrrrrrg north south left
		 * rrrryyyrrrrryyyr north south straight right
		 */
		
		String prog = "";
		phaseDuration = nextPhaseDuration;
		//strtTime = getOs().getSimulationTime();
		nextSpatContent = new SPaTContent(junctionID, junctionPoint, nextVehList,getOs().getSimulationTime() , phaseDuration, nextAppLegs, nextDepartLegs);
		
		//System.out.println("SPaT is set for dur: " + phaseDuration);
		//
		event = new Event(getOs().getSimulationTime() + ((int)(phaseDuration - 1 ) * 1000000000L), this::scheduleVehicles);
		getOs().getEventManager().addEvent(event);// setTrafficProgram
		event = new Event(getOs().getSimulationTime() + ((int)(phaseDuration - 5 ) * 1000000000L), this::setYellowPhase);
		getOs().getEventManager().addEvent(event);
		event = new Event(getOs().getSimulationTime() + ((int)(phaseDuration - 2 ) * 1000000000L), this::setAllRedPhase);
		getOs().getEventManager().addEvent(event);
		event = new Event(getOs().getSimulationTime() + ((int)(phaseDuration) * 1000000000L), this::setTrafficProgram);
		getOs().getEventManager().addEvent(event); 
		spatContent = nextSpatContent;
		
		 setPhasePointer(nextPhase);
		 previousPhasePointer = phasePointer;
		 System.out.println("SECOND previousPhasePointer " + previousPhasePointer);
		 //Set Traffic lights
		 ((TrafficLightOperatingSystem)getOs()).switchToProgram(nextPhase);
		 
	}
	
	private void setPhasePointer(String phase) {
		  for (int i = 0; i < greenPhaseSequence.length; i++) {
			  if (phase == greenPhaseSequence[i]) {
				  phasePointer = i;
				  break;
			  }
		  }
		  
	}
	
	private double[] DeterminePhaseUrgencyAndQueue(String approach, String departure) {
		double urg = 0, wT = 0;
		int qL = 0;
		Table vehs = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(approach).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo (departure)));
		wT = (double)(getOs().getSimulationTime() - vehs.longColumn("arrivaltime").min());
		qL = vehs.rowCount();
		if (Double.isNaN(wT)) {
			wT = 0.0;
		}
		urg = urgCtrler.EvaluateFIS(wT, qL);
		double[] uergencyAndQueue = {urg, qL};
		return uergencyAndQueue;
	}
	
	private void scheduleVehicles(Event event) {
		//System.out.println("Scheduled Vehs executed " );
		long t = 0; //Determine phase Duration
		
		String vehList = " ", applegs = " ", deptlegs = " ";
		
		if (adhocTrafficRecords.rowCount() > 0) {
			double chosenUrgency = 0, otherUrgency = 0;
			int chosenQueue = 0;
			double[] urgQueue;
			String[] phaseApproaches = {"northApproach", "northApproach", "northApproach", "southApproach", "southApproach", "southApproach", "westApproach", "westApproach", "westApproach", "eastApproach", "eastApproach", "eastApproach"};
			String[] phaseDepartures = {"eastDepart",    "southDepart",    "westDepart",    "westDepart",    "northDepart",   "eastDepart",    "northDepart",  "eastDepart",  "southDepart",   "southDepart",  "westDepart",   "northDepart"};
			Table vehs;
			String appLeg = "", deptLeg = "";
			
			if (CompareTrafficStates(previousTrafficRecords, adhocTrafficRecords)) {
				
				//Get the next phase in the Dual-Ring Phase sequence
				//String[] greenPhaseSequence = {"northsouthleft", "northsouthstraightright", "eastwestleft", "eastweststraightright"};
				
				//System.out.println("Previous and Current Traffic States are the same");
				System.out.println("FIRST previousPhasePointer " + previousPhasePointer);
				
				double urg1 = 0.0, urg2 = 0.0, urg3 = 0.0, urg4 = 0.0;
				int ql1 = 0, ql2 = 0, ql3 = 0, ql4 = 0;
				if(previousPhasePointer == 3){
					  
					  phasePointer += 1;
					  applegs = "northApproach, southApproach";
					  deptlegs = "eastDepart, westDepart";
					  appLeg = "northApproach";
					  deptLeg = "eastDepart";
					  
					  urgQueue = DeterminePhaseUrgencyAndQueue("northApproach", "eastDepart");
					  urg1 = urgQueue[0];
					  ql1 = (int) urgQueue[1];
					  urgQueue = DeterminePhaseUrgencyAndQueue("southApproach", "westDepart");
					  urg2 = urgQueue[0];
					  ql2 = (int) urgQueue[1];
					  chosenUrgency = ((urg1 + urg2)/2); //chosenUrgency of North and South Left turning movement
					  otherUrgency = ((DeterminePhaseUrgencyAndQueue("northApproach", "southDepart")[0] + DeterminePhaseUrgencyAndQueue("northApproach", "westDepart")[0] +
							  DeterminePhaseUrgencyAndQueue("southApproach", "northDepart")[0]  + DeterminePhaseUrgencyAndQueue("southApproach", "eastDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("westApproach", "northDepart")[0] + DeterminePhaseUrgencyAndQueue("westApproach", "eastDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("westApproach", "southDepart")[0] + DeterminePhaseUrgencyAndQueue("eastApproach", "southDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("eastApproach", "westDepart")[0] + DeterminePhaseUrgencyAndQueue("eastApproach", "northDepart")[0])/10);
					  chosenQueue = Math.max(ql1, ql2);
						
				 }else if(previousPhasePointer == 0){
					  
					  phasePointer += 1;
					  applegs = "northApproach, southApproach, northApproach, southApproach";
					  deptlegs = "southDepart, northDepart, westDepart, eastDepart";
					  appLeg = "northApproach";
					  deptLeg = "southDepart";
					  
					  
					  urgQueue = DeterminePhaseUrgencyAndQueue("northApproach", "southDepart");
					  urg1 = urgQueue[0];
					  ql1 = (int) urgQueue[1];
					  urgQueue = DeterminePhaseUrgencyAndQueue("southApproach", "northDepart");
					  urg2 = urgQueue[0];
					  ql2 = (int) urgQueue[1];
					  
					  urgQueue = DeterminePhaseUrgencyAndQueue("northApproach", "westDepart");
					  urg3 = urgQueue[0];
					  ql3 = (int) urgQueue[1];
					  urgQueue = DeterminePhaseUrgencyAndQueue("southApproach", "eastDepart");
					  urg4 = urgQueue[0];
					  ql4 = (int) urgQueue[1];
					  
					  
					  chosenUrgency = ((urg1 + urg2 + urg3 + urg4)/4); //chosenUrgency of North and South Straight-Through and Right turning movement
					  otherUrgency = ((DeterminePhaseUrgencyAndQueue("northApproach", "eastDepart")[0] + DeterminePhaseUrgencyAndQueue("southApproach", "westDepart")[0]  + 
							  DeterminePhaseUrgencyAndQueue("westApproach", "northDepart")[0] + DeterminePhaseUrgencyAndQueue("westApproach", "eastDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("westApproach", "southDepart")[0] + DeterminePhaseUrgencyAndQueue("eastApproach", "southDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("eastApproach", "westDepart")[0]  + DeterminePhaseUrgencyAndQueue("eastApproach", "northDepart")[0] )/8);
					  chosenQueue = Math.max(Math.max(ql1, ql2), Math.max(ql3, ql4));
						
				 }else if(previousPhasePointer == 1){
					
					  phasePointer += 1;
					  applegs = "eastApproach, westApproach";
					  deptlegs = "southDepart, northDepart";
					  appLeg = "eastApproach";
					  deptLeg = "southDepart";
					  
					  
					  urgQueue = DeterminePhaseUrgencyAndQueue("eastApproach", "southDepart");
					  urg1 = urgQueue[0];
					  ql1 = (int) urgQueue[1];
					  urgQueue = DeterminePhaseUrgencyAndQueue("westApproach", "northDepart");
					  urg2 = urgQueue[0];
					  ql2 = (int) urgQueue[1];
					  
					
					  chosenUrgency = ((urg1 + urg2)/2); //chosenUrgency of North and South Left turning movement ((urgency[0] + urgency[1] + urgency[2] + urgency[3] + urgency[4]  + urgency[5] + urgency[7] + urgency[8] +  urgency[10] + urgency[11]
					  otherUrgency = ((DeterminePhaseUrgencyAndQueue("northApproach", "eastDepart")[0] + DeterminePhaseUrgencyAndQueue("northApproach", "southDepart")[0] +
							  DeterminePhaseUrgencyAndQueue("northApproach", "westDepart")[0]  + DeterminePhaseUrgencyAndQueue("southApproach", "westDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("southApproach", "northDepart")[0] + DeterminePhaseUrgencyAndQueue("southApproach", "eastDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("westApproach", "eastDepart")[0] + DeterminePhaseUrgencyAndQueue("westApproach", "southDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("eastApproach", "westDepart")[0] + DeterminePhaseUrgencyAndQueue("eastApproach", "northDepart")[0])/10);
					  chosenQueue = Math.max(ql1, ql2);
				 }else if(previousPhasePointer == 2){
					  
					  phasePointer = 0;
					  applegs = "eastApproach, westApproach, eastApproach, westApproach";
					  deptlegs = "westDepart, eastDepart, northDepart, southDepart";
					  appLeg = "eastApproach";
					  deptLeg = "westDepart";
					  
					  
					  urgQueue = DeterminePhaseUrgencyAndQueue("eastApproach", "westDepart");
					  urg1 = urgQueue[0];
					  ql1 = (int) urgQueue[1];
					  urgQueue = DeterminePhaseUrgencyAndQueue("westApproach", "eastDepart");
					  urg2 = urgQueue[0];
					  ql2 = (int) urgQueue[1];
					  
					  urgQueue = DeterminePhaseUrgencyAndQueue("eastApproach", "northDepart");
					  urg3 = urgQueue[0];
					  ql3 = (int) urgQueue[1];
					  urgQueue = DeterminePhaseUrgencyAndQueue("westApproach", "southDepart");
					  urg4 = urgQueue[0];
					  ql4 = (int) urgQueue[1];
					  
					//String[] phaseApproaches = {"northApproach", "northApproach", "northApproach", "southApproach", "southApproach", "southApproach", "westApproach", "westApproach", "westApproach", "eastApproach", "eastApproach", "eastApproach"};
					//String[] phaseDepartures = {"eastDepart",    "southDepart",    "westDepart",    "westDepart",    "northDepart",   "eastDepart",    "northDepart",  "eastDepart",  "southDepart",   "southDepart",  "westDepart",   "northDepart"};
							
					  chosenUrgency = ((urg1 + urg2 + urg3 + urg4)/4); //chosenUrgency of North and South Straight-Through and Right turning movement urgency[0] + urgency[1] + urgency[2] + urgency[3] + urgency[4]  + urgency[5] + urgency[6] + urgency[9]
					  otherUrgency = ((DeterminePhaseUrgencyAndQueue("northApproach", "eastDepart")[0] + DeterminePhaseUrgencyAndQueue("northApproach", "southDepart")[0]  + 
							  DeterminePhaseUrgencyAndQueue("northApproach", "westDepart")[0] + DeterminePhaseUrgencyAndQueue("southApproach", "westDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("southApproach", "northDepart")[0] + DeterminePhaseUrgencyAndQueue("southApproach", "eastDepart")[0] + 
							  DeterminePhaseUrgencyAndQueue("westApproach", "northDepart")[0]  + DeterminePhaseUrgencyAndQueue("eastApproach", "southDepart")[0] )/8);
					  chosenQueue = Math.max(Math.max(ql1, ql2), Math.max(ql3, ql4));
					  
				 }
				
				System.out.println("MOVEMENT : " + appLeg + " TO " + deptLeg);
			}else {
			//*/
				//Store waiting times of phases
				int[] queueLengths = new int[12]; //queueLength[0] = nsLeftQL, queueLength[1] = nsRightQL, queueLength[2] = weLeftQL, queueLength[3] = weRightQL
				double[] urgency = new double[12]; //Stores the urgency requirements of each phase
				
				double urg = 0, maxUrg = 0, wT = 0;
				int maxUrgIdx = 0, qL = 0;
				//Create Phase Approaches and Departures
				
				 
				//Get max queue lengths and max waiting times of the various phases
				for(int i = 0; i < phaseApproaches.length; i++) {
					urgQueue = DeterminePhaseUrgencyAndQueue(phaseApproaches[i],phaseDepartures[i]);
					urg = urgQueue[0];
					urgency[i] = urg;
					queueLengths[i] = (int) urgQueue[1];
					//System.out.println("URG: " + urgQueue[0] + "" + urgQueue[1]);
					if (urg > maxUrg) {
						maxUrg = urg;
						maxUrgIdx = i;
						appLeg = phaseApproaches[i];
						deptLeg = phaseDepartures[i]; 
					}
				}
				//Determine the chosenUrgency as an average
				if (maxUrgIdx ==0 || maxUrgIdx ==3) {
					chosenUrgency = ((urgency[0] + urgency[3])/2); //chosenUrgency of North and South Left turning movement
					otherUrgency = ((urgency[1] + urgency[2] + urgency[4]  + urgency[5] + urgency[6] + urgency[7] + urgency[8] + urgency[9] + urgency[10] + urgency[11])/10);
					chosenQueue = Math.max(queueLengths[0], queueLengths[3]);
					
				}else if(maxUrgIdx ==6 || maxUrgIdx ==9) {
					chosenUrgency = ((urgency[6] + urgency[9])/2); //chosenUrgency of West and East Left turning movement
					otherUrgency = ((urgency[1] + urgency[2] + urgency[4]  + urgency[5] + urgency[0] + urgency[7] + urgency[8] + urgency[3] + urgency[10] + urgency[11])/10);
					chosenQueue = Math.max(queueLengths[6], queueLengths[9]);
					
				}else if(maxUrgIdx ==7 || maxUrgIdx == 8 || maxUrgIdx ==10 || maxUrgIdx ==11) {
					chosenUrgency = ((urgency[7] + urgency[8] + urgency[10] + urgency[11])/4); //chosenUrgency of West and East Straight-Through and Right turning movement
					otherUrgency = ((urgency[0] + urgency[1] + urgency[2] + urgency[3] + urgency[4]  + urgency[5] + urgency[6] + urgency[9])/8);
					chosenQueue = Math.max(Math.max(queueLengths[7], queueLengths[8]), Math.max(queueLengths[10], queueLengths[11]));
					
				}else if(maxUrgIdx ==1 || maxUrgIdx == 2 || maxUrgIdx ==4 || maxUrgIdx ==5) {
					chosenUrgency = ((urgency[1] + urgency[2] + urgency[4] + urgency[5])/4); //chosenUrgency of North and South Straight-Through and Right turning movement
					otherUrgency = ((urgency[0] + urgency[3] + urgency[6] + urgency[7] + urgency[8]  + urgency[9] + urgency[10] + urgency[11])/8);
					chosenQueue = Math.max(Math.max(queueLengths[1], queueLengths[2]), Math.max(queueLengths[4], queueLengths[5]));
				}
		
			}
			//*/
			ArrayPair concFlows = getConcurrentFlows(appLeg, deptLeg);
			String[] approaches = concFlows.getFirstArray();
			String[] departs = concFlows.getSecondArray();
			//Retrieve Scheduled Vehicles IDs for publishing.
			for (int i = 0; i < approaches.length; i++) {
			  
				vehs = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(approaches[i]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo (departs[i])));
				for (Row veh: vehs) {
					if (vehList.length() ==0) {
						vehList = veh.getString("vehicleid");
						applegs = veh.getString("approachleg");
						deptlegs = veh.getString("departleg");
					}else {
					  
						vehList = vehList + "," + veh.getString("vehicleid");
						applegs = applegs  + "," +  veh.getString("approachleg");
						deptlegs = deptlegs  + "," + veh.getString("departleg");
					}
					
				}
			}
			//Determine Phase Duration using UrgencyBasedPhaseDurationController
			t = (long) urgPhaseDuration.EvaluateFIS(chosenUrgency, otherUrgency, chosenQueue);
			previousTrafficRecords = adhocTrafficRecords;
			//System.out.println("APP LEGS: " + applegs);
			//System.out.println("DPT LEGS: " + deptlegs);
			System.out.println("SCH VEHs: " + vehList);
		}else {
			//Run round-robin algorithm
			
			 if(phasePointer == 0){
				  
				  phasePointer += 1;
				  applegs = "northApproach, southApproach";
				  deptlegs = "eastDepart, westDepart";
			 }else if(phasePointer == 1){
				  
				  phasePointer += 1;
				  applegs = "northApproach, southApproach, northApproach, southApproach";
				  deptlegs = "southDepart, northDepart, westDepart, eastDepart";
			 }else if(phasePointer == 2){
				
				  phasePointer += 1;
				  applegs = "eastApproach, westApproach";
				  deptlegs = "southDepart, northDepart";
				  
			 }else if(phasePointer == 3){
				  
				  phasePointer = 0;
				  applegs = "eastApproach, westApproach, eastApproach, westApproach";
				  deptlegs = "westDepart, eastDepart, northDepart, southDepart";
				  
			 }
			 nextPhase = greenPhaseSequence[phasePointer];
			t = 7;
			
		}
		if (t <= 0) {
			t = 7;
		}
		nextPhaseDuration =  (int) Math.floor(t);
		
		
		nextVehList = vehList;
		nextAppLegs = applegs;
		nextDepartLegs = deptlegs;
		
		
	}

	private void setAllRedPhase(Event event) {
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(allRed);
		  getLog().infoSimTime(this, "Traffic Lights Assigned to " + allRed );
	  }
	  

	private void setYellowPhase(Event event) {
		//This function traffic lights 
		  String phase = yellowPhaseSequence[previousPhasePointer];
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(phase);
		  getLog().infoSimTime(this, "Traffic Lights Assigned to " + phase );
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
	  
	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {

	  
	  	getLog().info("Adhoc SRM Message is received");
		//*
		V2xMessage msg = receivedV2xMessage.getMessage();
	    if (!(msg instanceof SignalRequestMessage)) {
	    	getLog().infoSimTime((OperatingSystemAccess)this, "Ignoring message of type: {}", new Object[] { msg.getSimpleClassName() });
	    	return;
	    }
	    SignalRequestMessage srm = (SignalRequestMessage) msg;
	    getLog().info("MSG FROM: {} at lat {} and lon {} MSGType {}", srm.getVehicleId(), srm.getLocation().getLatitude(),srm.getLocation().getLongitude(), srm.getMsgType() );
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
	    
		
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}

}
