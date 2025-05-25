package com.debangis;

import com.debangis.messages.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class VehicleOnBoardUnit  extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication{
	
	// Radius of the Earth in kilometers
    private static final double EARTH_RADIUS = 6371; // in kilometers
    
	private GeoPoint intersectionLocation;
	
	private float MAXDISTOINTERSECION = 0.1f; //Distance from which vehicle should make SRM request
	private float departDistance = 0.009f;
	private boolean setMaxDstToInt = false;
	
	//Vehicles SRM Parameters Definition
	private long timeStamp;
	private String intersectionName = "Simcoe-Taunton";;
	private String vehicleID;
	private String approachID;
	private String departID;
	private long arrivalTime = 0;
	private int vehType = 1;
	private GeoPoint vehLocation;
	private float speed = 0;
	private int vehPriority = 1;
	private int msgType;
	private float dstToIntersection;
	
	private int srmAck = 0;
	private final long SRMINTERVAL = 2 * TIME.SECOND; //Intervals between SRM Requests until acknowledgement is received 
	
	//Communication parameters
	private GeoCircle geoCircle;
	private  MessageRouting routing;
	private SignalRequestMessageContent srmContent;
	private SignalRequestMessage srm;

	private boolean gonePastIntersection = false;
	private boolean setArrivalTime = false;
	
	public VehicleOnBoardUnit() {
		// TODO Auto-generated constructor stub
		//System.out.println("VehicleOnBoardUnit Constructor Executed");
	}

	@Override
	public void processEvent(Event arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onStartup() {
		 
		//System.out.println("VehicleOnBoardUnit onStartup Executing");
		// TODO Auto-generated method stub
		 getLog().infoSimTime((OperatingSystemAccess)this, "Initialize Vehicle OnBoard Cellular Communication Application", new Object[0]);
	    ((VehicleOperatingSystem)getOs()).getCellModule().enable();
	    getLog().infoSimTime((OperatingSystemAccess)this, "Setup Vehicle OnBoard Cellular Communication {} at time {}", new Object[] { ((VehicleOperatingSystem)getOs()).getId(), Long.valueOf(((VehicleOperatingSystem)getOs()).getSimulationTime()) });
	    getLog().infoSimTime((OperatingSystemAccess)this, "Activated Vehicle OnBoarcd Cellular Communication Cell Module", new Object[0]);
	    Event event = new Event(getOs().getSimulationTime() + 10000000000L, this::isVehAtIntersectionRegion);
		getOs().getEventManager().addEvent(event);
		
		//Activate Adhoc Communication Module
		AdHocModuleConfiguration configuration = new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(17)
                .distance(300)
                .create();
        getOs().getAdHocModule().enable(configuration);
        getLog().infoSimTime(this, "Activated WLAN Module");
		intersectionLocation = GeoPoint.latLon(53.547745, 9.966246);
		vehicleID = getOs().getId();
		
		//System.out.println("VehicleOnBoardUnit onStartup Executing");
	}
	
	private void isVehAtIntersectionRegion(Event event) {
		//if (srmAck == 0 || srmAck == 3) {
		getLog().info("isVehAtIntersectionRegion: setArrivalTime = {}, srmAck = {}", setArrivalTime, srmAck);
		if ((setArrivalTime) && ((srmAck == 0) ||(srmAck == 3))){	
			SendSRM();
			
		}
		Event sendSRMEvent = new Event(getOs().getSimulationTime()+ SRMINTERVAL, this::isVehAtIntersectionRegion);
		getOs().getEventManager().addEvent(sendSRMEvent);
	}
	
	// Convert degrees to radians
    private static double toRadians(double degrees) {
        return degrees * Math.PI / 180.0;
    }
    
	private float DistanceToIntersection(GeoPoint vehPos) {
		/*
		 * Calculates distance of a vehicle, given its GeoPoint coordinate, from the
		 * intersection whose coordinates Coordinate of intersection: 53.547718, 9.966288
		 */
		double lat1 = intersectionLocation.getLatitude();
		double lon1 = intersectionLocation.getLongitude();
		
		double lat2 = vehPos.getLatitude();
		double lon2 = vehPos.getLongitude();
		
		double dLat = toRadians(lat2 - lat1);
        double dLon = toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (EARTH_RADIUS * c);
        
		//return (float)(Math.sqrt(Math.pow((vehPos.getLatitude() - intersectionLocation.getLatitude()),2) + Math.pow((vehPos.getLongitude() - intersectionLocation.getLongitude()),2)));
		
	}
	
	
	private void SendSRM() {
		/*
		 * Retrieves parameters and sends Signal RequestMessage
		 */
		if (!(getOs().getCellModule().isEnabled())) {
			getLog().info("ENABLING CELL MODULE AGAIN");
			getOs().getCellModule().enable();
		}
		geoCircle = new GeoCircle(intersectionLocation, 1000.0D);
		routing = getOs().getCellModule().createMessageRouting().geoBroadcastBasedOnUnicast(geoCircle);
		
		timeStamp = getOs().getSimulationTime();
		
		//(long time, String jnctnId, String vehId, String appLeg, String deptLeg, long arvltme, int vehType, GeoPoint loc, float spd, int prty)
		getLog().info("ROUTING INFO {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {} DIST:{}", timeStamp, intersectionName,vehicleID, approachID,
											departID, arrivalTime, vehType, vehLocation.getLatitude(), vehLocation.getLongitude(), speed, vehPriority, msgType, dstToIntersection);
		srmContent = new SignalRequestMessageContent(
									timeStamp, 
									intersectionName,
									vehicleID,
									approachID, 
									departID,
									departID,
									arrivalTime,
									vehType, //Vehicle Type: Default = 1
									vehLocation,
									speed,
									vehPriority,
									msgType
									);
		
		srm = new SignalRequestMessage(routing, srmContent, 200);
		getOs().getCellModule().sendV2xMessage(srm);
	    getLog().infoSimTime(this, "Sent SRM");
	    getLog().info("Sending SRM @ time {} msgType {}", timeStamp, msgType);
		
	    //Send Adhoc V2xMessage
	    final MessageRouting adhocRouting = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoBroadCast();
	    SignalRequestMessage adhocSrm = new SignalRequestMessage(adhocRouting,srmContent, 200L);
        getOs().getAdHocModule().sendV2xMessage(adhocSrm);
        getLog().infoSimTime(this, "Sent Adhoc SRM");
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement arg0) {
		// TODO Auto-generated method stub
		//Update srmAck so that messages are not unnecessarily sent
		getLog().info("ACKNOWLEDGEMENT RECEIVED: {} ", arg0.getSentMessage().toString());
		srmAck = 2;
	}

	@Override
	public void onCamBuilding(CamBuilder arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		// TODO Auto-generated method stub
		
		V2xMessage msg = receivedV2xMessage.getMessage();
		if(msg instanceof SRMAcknowledgement) {
			SRMAcknowledgement srmAckn = (SRMAcknowledgement) msg;
			if (srmAckn.getSender() == getOs().getId()) { //Meaning it is a message from the Traffic Recording Road Side Unit Entity.
				srmAck = srmAckn.getStatus();
				if (srmAck == 2) { // Approach Acknowledgement
					getOperatingSystem().requestVehicleParametersUpdate()
		            .changeColor(Color.CYAN)
		            .apply();
				}else if (srmAck == 4) { // Depart Acknowledgement
					gonePastIntersection = true;
					getOperatingSystem().requestVehicleParametersUpdate()
		            .changeColor(Color.YELLOW)
		            .apply();
				}
				getLog().info("VEH OnReceived V2XMessage");
			}
		}else if(msg instanceof SignalRequestMessage) { //Meaning it should be a V2V Message from another vehicle
			//TODO Record other vehicle's information
			SignalRequestMessage srmMsg = (SignalRequestMessage)msg;
			getLog().info("SRM Recieved from {}", srmMsg.getVehicleId());
		}
	}
		

	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}

	private void setMaxIntersectionDistance(String appLeg, String deptLeg) {
		//This function appropriately set the vehicles depart lane based on app and depart legs info
		if (!setMaxDstToInt) {
			setMaxDstToInt = true;
			if ((appLeg.equals("northApproach")) && (deptLeg.equals("westDepart"))) {
				departDistance = 0.015f;
				
			}else if ((appLeg.equals("southApproach")) && (deptLeg.equals("eastDepart"))) {
				departDistance = 0.015f;
			
			}else if ((appLeg.equals("westApproach")) && (deptLeg.equals("eastDepart"))) {
				departDistance = 0.015f;
				
			}else if ((appLeg.equals("eastApproach")) && (deptLeg.equals("northDepart"))) {
				departDistance = 0.015f;
			
			}
		}
		
	}
	@Override
	public void onVehicleUpdated(VehicleData prevUpdate, VehicleData curUpdate) {
		// TODO Auto-generated method stub
		speed = (float) curUpdate.getSpeed();
		approachID = getOs().getVehicleData().getRoadPosition().getConnection().getId();
		
		vehLocation = curUpdate.getPosition();
		final VehicleRoute routeInfo = Objects.requireNonNull(getOs().getNavigationModule().getCurrentRoute());
		/*MULTI-INTERSECTION COORDINATES:
		 * Simcoe-Taunton: 53.546101, 9.965058
		 * Simcoe-Rossland: 53.546152, 9.968502
		 * Rossland-Mary: 53.547693, 9.968449
		 * Taunton-Mary: 53.547767, 9.965096
		 * 
		 * https://github.com/eclipse/mosaic/blob/main/app/tutorials/example-applications/src/main/java/org/eclipse/mosaic/app/tutorial/eventprocessing/SpecificEventProcessingApp.java
		 * 
		 * See Route Edges
		 */
		List<String> routeEdges = routeInfo.getConnectionIds();
		if (approachID.endsWith("Approach")) {
			for (int i=0;i < routeEdges.size();i++)
			{
				//System.out.println("Value of element "+ i + " : " + routeEdges.get(i));
				if (routeEdges.get(i).equals(approachID)) {
					//System.out.println(approachID + " : " + getOs().getVehicleData().getRoadPosition().getConnection().getEndNode().getId());
					//Read next Junction for traffic light SRM: getOs().getVehicleData().getRoadPosition().getConnection().getEndNode().getId()
				}
				
			}
		}
		
		//End of See Route Edges
		 
		//*/
		
		departID = routeInfo.getLastConnectionId();
		
		dstToIntersection = DistanceToIntersection(vehLocation);
		//getLog().info("DISTANCE: {}", dstToIntersection);
		setMaxIntersectionDistance(approachID, departID);
		if ((departID.equals(approachID))&& (gonePastIntersection == false)){ //Execute once
			
			getLog().info("START SENDING DEPART SRM");
			srmAck = 3;
			msgType = 2; //Meaning Depart Message
			getOperatingSystem().requestVehicleParametersUpdate()
            .changeColor(Color.GREEN)
            .apply();
		}
		if ((dstToIntersection <= MAXDISTOINTERSECION) && (setArrivalTime == false)){ //Execute once
			getLog().info("START SENDING SRM");
			setArrivalTime = true;
			msgType = 1; //Meaning approach message
			arrivalTime = getOs().getSimulationTime();
			getOperatingSystem().requestVehicleParametersUpdate()
            .changeColor(Color.BLUE)
            .apply();
		}
	}

}
