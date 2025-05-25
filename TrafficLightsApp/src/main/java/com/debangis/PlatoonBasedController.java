package com.debangis;

import com.debangis.messages.*;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CellModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.DATA;

import com.debangis.controllers.FuzzyModel;

import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
//public class PlatoonBasedController extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
public class PlatoonBasedController extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
	  

	/**
	 * This is platoon-based controller, which schedule and publishes individual vehicles 
	 * that are allowed to utilised the intersection 
	 */
	
	int minPhaseDuration = 7;
	int maxPhaseDuration = 25;
	int nextPhaseDuration = 0, phaseDuration = minPhaseDuration; 
	
	//Traffic Recording Variables
	TrafficRecording trfRcdng;
	Table adhocTrafficRecords;
	FuzzyModel fisModel;// = new FuzzyModel();
	
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

	public PlatoonBasedController() {
		// TODO Auto-generated constructor stub
	}
	

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
			System.out.println("SPaT is not Null");
			//Send Adhoc V2xMessage
			final MessageRouting adhocRouting = getOperatingSystem()
	                .getAdHocModule()
	                .createMessageRouting()
	                .topoBroadCast();
				
			spat = new SPaT(adhocRouting, spatContent, 200L);
			
			getOs().getAdHocModule().sendV2xMessage(spat);
			//getLog().info("SPaT Sent {}, {}",  spat.getScheduledVehicles(), spat.getApproachLegs());
			System.out.println("publishScheduledVehicles Executed");
		}
		
		event = new Event(getOs().getSimulationTime() + 1000000000L, this::publishScheduledVehicles);
		getOs().getEventManager().addEvent(event);
	}

	private void initializeParameters(Event event) {
		trfRcdng = new TrafficRecording();
		adhocTrafficRecords = trfRcdng.CreateTrafficRecording("adhocTrafficRecords");
		intersectionLocation = GeoPoint.latLon(53.547745, 9.966246);
		junctionPoint = intersectionLocation;
		fisModel = new FuzzyModel();
		
		final MessageRouting adhocRouting = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoBroadCast();
		spat = new SPaT(adhocRouting, new SPaTContent (getOs().getSimulationTime(), 7), 200);
		System.out.println("initializeParameters Done Start Time: " + spat.getStartTime());
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
		
		System.out.println("SPaT is set for dur: " + phaseDuration);
		//
		event = new Event(getOs().getSimulationTime() + ((int)(phaseDuration - 1 ) * 1000000000L), this::scheduleVehicles);
		getOs().getEventManager().addEvent(event);// setTrafficProgram
		event = new Event(getOs().getSimulationTime() + ((int)(phaseDuration - 5 ) * 1000000000L), this::setYellowPhase);
		getOs().getEventManager().addEvent(event);
		event = new Event(getOs().getSimulationTime() + ((int)(phaseDuration - 5 ) * 1000000000L), this::setAllRedPhase);
		getOs().getEventManager().addEvent(event);
		event = new Event(getOs().getSimulationTime() + ((int)(phaseDuration) * 1000000000L), this::setTrafficProgram);
		getOs().getEventManager().addEvent(event); 
		spatContent = nextSpatContent;
		
		 setPhasePointer(nextPhase);
		 previousPhasePointer = phasePointer;
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
	
	private void scheduleVehicles(Event event) {
		System.out.println("Scheduled Vehs executed " );
		long t = 0, t0 = 0, strtTime; //arrival time of vehicle
		float v = 0; //speed of vehicle
		float d = 0; //estimated distance
		float D = 0; //vehicle distance from from intersection point
		int dur = 0;
		String vehList = " ", applegs = " ", deptlegs = " ";
		
		if (adhocTrafficRecords.rowCount() > 0) {
			//Get approach with a vehicle that has the earliest arrival
			  LongColumn phaseVehArrival = adhocTrafficRecords.longColumn("arrivaltime");
			 
			  //System.out.println("MAX Waiting Time = " + maxWaitingTime);
			  
			  Table vehRec = adhocTrafficRecords.where(phaseVehArrival.isEqualTo(phaseVehArrival.min()));
			  //System.out.println("Veh With Max Waiting: " + vehRec.printAll());
			  String appLeg = vehRec.stringColumn("approachleg").getString(0);
			  
			  String deptLeg = vehRec.stringColumn("departleg").getString(0);
			  ArrayPair concFlows = getConcurrentFlows(appLeg, deptLeg);
			  String[] approaches = concFlows.getFirstArray();
			  String[] departs = concFlows.getSecondArray();
			  
			  for (int i = 0; i < approaches.length; i++) {
				  
				  Table vehs = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(approaches[i]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo (departs[i])));
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
					  t0 = getOs().getSimulationTime() - veh.getLong("arrivaltime");
					  v = veh.getFloat("speed");
					  D = (DistanceToIntersection( veh.getFloat("latitude"), veh.getFloat("longitude")) * 100); //distance of vehicle at arrival.
					  d = v * t0; //Estimated distance of vehicle from its known arrival point
					  
					  t0 = (long) ((D - d)/ v) + 3; //Time require for vehicle to arrive stop line and cross the intersection in 3s
					  if (t0 > t) {
						  t = t0;
					  }
					  
				  }
			  }
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
			t = 8;
			
		}
		if (t <= 0) {
			t = 8;
		}
		nextPhaseDuration =  (int) Math.ceil(t);
		
		
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
