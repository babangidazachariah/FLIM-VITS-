package com.debangis;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import com.debangis.messages.SRMAcknowledgement;
import com.debangis.messages.SRMAcknowledgementContent;
import com.debangis.messages.SignalRequestMessage;

import tech.tablesaw.api.Table;

public class Playground extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
	String tlsControllerName;
	Table adhocTrafficData;
	 //Traffic Recording Variables
	  TrafficRecording trfRcdng;
	  Table adhocTrafficRecords, previousAdhocTrafficRecords;
	  String nextPhase = "";
	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
	
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
		 
		 if (tlsControllerName.equals("SimcoeTauntonJunction")) {
			 ((TrafficLightOperatingSystem)getOs()).switchToProgram("allOutWest");
		 }else if (tlsControllerName.equals("SimcoeRosslandJunction")) {
			 ((TrafficLightOperatingSystem)getOs()).switchToProgram("allOutWest");
		 }else if (tlsControllerName.equals("MaryTauntonJunction")) {
			 ((TrafficLightOperatingSystem)getOs()).switchToProgram("allOutSouth");
			 
		 }else if (tlsControllerName.equals("MaryRosslandJuction")) {
			 ((TrafficLightOperatingSystem)getOs()).switchToProgram("allOutSouth");
		 }
	}
	
	@Override
	public void onStartup() {
		// TODO Auto-generated method stub
		getLog().infoSimTime((OperatingSystemAccess)this, "Initialize application", new Object[0]);
    	AdHocModuleConfiguration configuration = new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(17)
                .distance(1000)
                .create();
        getOs().getAdHocModule().enable(configuration);
        Event event = new Event(getOs().getSimulationTime() + 3000000000L, this::initializeParameters);
    	getOs().getEventManager().addEvent(event);
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
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		// TODO Auto-generated method stub
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
				System.out.println("MSG   FOR : " + srm.getJunctionId() + " FROM : "  + srm.getVehicleId() + " type: " + srm.getMsgType() + " Time: " + srm.getTimeStamp());
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
			    getConcurrentFlows(appLeg, "");
			    ((TrafficLightOperatingSystem)getOs()).switchToProgram(nextPhase);
			}
	    }
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
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
	  
	

}
