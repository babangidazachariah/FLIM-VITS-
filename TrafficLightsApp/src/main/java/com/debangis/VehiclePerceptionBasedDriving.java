package com.debangis;

import com.debangis.messages.*;
import java.awt.Color;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.SimplePerceptionConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.errormodels.DistanceFilter;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.errormodels.PositionModifier;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.errormodels.SimpleOcclusion;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.index.objects.VehicleObject;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.interactions.vehicle.VehicleParametersChange;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleParameter;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import com.google.common.collect.Lists;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VehiclePerceptionBasedDriving  extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication{
	
	// Radius of the Earth in kilometers
    private static final double EARTH_RADIUS = 6371; // in kilometers
    
	private GeoPoint intersectionLocation;
	
	private float MAXDISTOINTERSECION = 0.1f; //Distance from which vehicle should make SRM request
	private float departDistance = 0.009f;;
	private boolean setMaxDstToInt = false;
	
	
	private int srmAck = 0;
	private final long SRMINTERVAL = 2 * TIME.SECOND; //Intervals between SRM Requests until acknowledgement is received 
	
	//Communication parameters
	private GeoCircle geoCircle;
	private  MessageRouting routing;
	private SignalRequestMessageContent srmContent;
	private SignalRequestMessage srm;
	private IntersectionCollisionAvoidanceMessage ica;
	private IntersectionCollisionAvoidanceMessageContent icaContent;


	private boolean gonePastIntersection = false;
	private boolean setArrivalTime = false;
	private boolean sendICA = false; //Send intersection Collision Message?
	
	//PERCEPTION PARAMETERS
	private final static double VIEWING_ANGLE = 60d; // [degree]
	private final static double VIEWING_RANGE = 5d; // [meter]

	private List<VehicleObject> previouslyPerceivedVehicles = new ArrayList<>();
	
	
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
		private float dstToIntersection = 0.025f;
	
		

		private String path;
		private float lat;
		private float lon;
		private float heading;
		private float acceleration;
		private float steeringAngle = 0f;
		
		private SPaT spat;
		boolean ihaveway = true;
		
	public VehiclePerceptionBasedDriving() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStartup() {
		// TODO Auto-generated method stub
		getLog().debugSimTime(this, "Started {} on {}.", this.getClass().getSimpleName(), getOs().getId());

	    enablePerceptionModule();

	    getOs().requestVehicleParametersUpdate()
	    			.changeColor(Color.BLUE)
	                .apply();
	    
	  
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
	    Event event = new Event(getOs().getSimulationTime() + 10000000000L, this::isVehAtIntersectionRegion);
		getOs().getEventManager().addEvent(event);
	}

	private void enablePerceptionModule() {
        // filter to emulate occlusion
        SimpleOcclusion simpleOcclusion = new SimpleOcclusion(3, 5);
        // filter to reduce perception probability based on distance to ego vehicle
        DistanceFilter distanceFilter = new DistanceFilter(getRandom(), 0.0);
        // filter adding noise to longitudinal and lateral
        PositionModifier positionModifier = new PositionModifier(getRandom());

        SimplePerceptionConfiguration perceptionModuleConfiguration =
                new SimplePerceptionConfiguration.Builder(VIEWING_ANGLE, VIEWING_RANGE)
                        .addModifiers(simpleOcclusion, distanceFilter, positionModifier)
                        .build();
        getOs().getPerceptionModule().enable(perceptionModuleConfiguration);
    }

	 	    /**
	     * Perceives vehicles in viewing range and adjusts their colors in the SUMO-GUI.
	     */
	    private boolean perceiveVehicles() {
	    	boolean vehicleInFront = false;
	        List<VehicleObject> perceivedVehicles = getOs().getPerceptionModule().getPerceivedVehicles();
	        getLog().infoSimTime(this, "Perceived vehicles: {}",
	                perceivedVehicles.stream().map(VehicleObject::getId).collect(Collectors.toList()));
	        if (perceivedVehicles.size() > 0) {
	        	vehicleInFront = true;
	        }
	        colorVehicles(perceivedVehicles, previouslyPerceivedVehicles, Color.GREEN); // set color to perceived
	        colorVehicles(previouslyPerceivedVehicles, perceivedVehicles, Color.YELLOW); // reset color of no longer perceived
	        previouslyPerceivedVehicles = perceivedVehicles;
	        return vehicleInFront;
	    }

	    private void colorVehicles(List<VehicleObject> vehiclesToColor, List<VehicleObject> previouslyColoredVehicles, Color color) {
	        for (VehicleObject currentVehicle : vehiclesToColor) {
	            if (!previouslyColoredVehicles.contains(currentVehicle)) {
	                VehicleParameter vehicleParameter = new VehicleParameter(VehicleParameter.VehicleParameterType.COLOR, color);
	                VehicleParametersChange vehicleParametersChange = new VehicleParametersChange(getOs().getSimulationTime(),
	                        currentVehicle.getId(), Lists.newArrayList(vehicleParameter));
	                getOs().sendInteractionToRti(vehicleParametersChange);
	            }
	        }
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
		private float getPointsDistance(float lat1, float lon1, float lat2, float lon2) {
			/*
			 * This function calculate the distance between the locations of two objects(vehicles)
			 */
			double dLat = toRadians(lat2 - lat1);
	        double dLon = toRadians(lon2 - lon1);
	        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
	        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	        return (float) (EARTH_RADIUS * c);
		}
		

		private static double[] getCollisionPoint(double theta1,  double x1, double y1, double theta2, double x2,  double y2) {
			/*
			 * Given the headings (theta1 & theta2), the points ((x1, y1) & (x2, y2)) of vehicles,
			 * this function calculate their future point of intersection. That is the point along 
			 * their path that they may possibly occupy the same point 
			 */
			double tanTheta1 = Math.tan(Math.toRadians(theta1));
			double tanTheta2 = Math.tan(Math.toRadians(theta2));
			double cotTheta1 = 1.0 / Math.tan(Math.toRadians(theta1));
			double cotTheta2 = 1.0 / Math.tan(Math.toRadians(theta2));
			
			double xPlus = ((y2 - y1) - (x2 * tanTheta2 - x1 * tanTheta1)) /
			(tanTheta1 - tanTheta2);
			
			double yPlus = (((x2 - x1) - (y2 * cotTheta2 - y1 * cotTheta1)) /
			(cotTheta1 - cotTheta2));
			
			return new double[]{xPlus, yPlus};
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
										departID, //for isolated intersections, the departId is same as the destination
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
	       // getLog().infoSimTime(this, "Sent Adhoc SRM");
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

	private void SendICA() {
		/*
		 * This function sends Basic Safety Message (ICA) to other vehicles 
		 * using the cellular network.
		 */
		if (sendICA == true && ihaveway == true) {
			
			//MessageRouting msgrouting = getOs().getCellModule().createMessageRouting().geoBroadcastBasedOnUnicast(geoCircle);
			
			timeStamp = getOs().getSimulationTime();
			icaContent = new IntersectionCollisionAvoidanceMessageContent(
									vehicleID,
									path,
									timeStamp,
									lat,
									lon,
									heading,
									speed,
									acceleration,
									steeringAngle,
									vehType
								);
			
			 final MessageRouting msgrouting = getOperatingSystem()
		                .getAdHocModule()
		                .createMessageRouting()
		                .topoBroadCast();
			ica = new IntersectionCollisionAvoidanceMessage(msgrouting, icaContent, 200);
			
			
			 getOs().getAdHocModule().sendV2xMessage(ica);
				
		    getLog().infoSimTime(this, "Sent ICA");
		    getLog().info("Sending ICA @ Time {}, Heading {}, Acceleration {}, Steering Angle {}", timeStamp, heading, acceleration, steeringAngle);
		}
	}
	
	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		// TODO Auto-generated method stub
		
		V2xMessage msg = receivedV2xMessage.getMessage();
		if (msg instanceof SPaT) {
			spat = (SPaT) msg;
			getLog().info("SPaT Received");
			String scheduledVehs[] = spat.getScheduledVehicles().split(",");
			getLog().info("Vehs: {}", spat.getScheduledVehicles());
			if (dstToIntersection > 0.025f) {
				speed = 50 / 3.6f;
				
			}else {
				ihaveway = false; 
				for(int i = 0; i < scheduledVehs.length; i++) {
					if (scheduledVehs[i].equals(getOs().getId())) {
						ihaveway = true;
						break;
					}
				}
				
				if (ihaveway == true || (((approachID.equals("southApproach") || approachID.equals("northApproach")) && dstToIntersection < 0.015)
						 || ((approachID.equals("eastApproach") || approachID.equals("westApproach")) && dstToIntersection < 0.015))
						  || approachID.equals(departID)) {
					
					speed = 50 / 3.6f;
					getLog().info("SPaT Received Right-of-Way Status {}", ihaveway);
				}else {
					getLog().info("I am Stopping at Distance {} on {}", dstToIntersection, approachID);
					speed = 0 / 3.6f;
				}
			}
			getOs().changeSpeedWithInterval(speed, 1 * TIME.SECOND);
			
		}else if(msg instanceof SRMAcknowledgement) {
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
			//System.out.println("V2I SRM Received"); //Vehicles can send and receive SRM. However, it is not being used by the vehicle in this current implementation
			
		}else if(msg instanceof IntersectionCollisionAvoidanceMessage) { //Meaning it is a V2V Basic Safety Message
			//System.out.println("V2V ICA Received");
			getLog().info("V2V ICA Received");
			float colThresh = 2;
			
			//Calculate the time to collision
			//TODO: Remember to check that the instance of SPaT is not conflicting with this
			IntersectionCollisionAvoidanceMessage icamessage = (IntersectionCollisionAvoidanceMessage) msg;
			String othID = icamessage.getVehicleID();
			String othPath = icamessage.getPath();
			//Check if this vehicle is within the critical region of the intersection
			if (sendICA == true) {
			
				//Check to ensure the ICA is not from self
				if (!(othID.equals(getOs().getId()))) {
					float othHeading = icamessage.getHeading();
					float othLat = icamessage.getLatitude();
					float othLon = icamessage.getLongitude();
					float othSpeed = icamessage.getSpeed();
					//Calculate the vehicles'point of intersection
					double[] colPoint = getCollisionPoint(othHeading, othLat, othLon, heading, lat, lon);
					if((Double.isInfinite(colPoint[0])) || (othPath.equals(path))) {
						//No collision point move vehicle
						getLog().info("SPEED {}", speed);
						speed = 50 / 3.6f;
						
						
					}else {
						//There is possible collision
						//calculate distance to collision point
						float dtoColPoint = getPointsDistance((float)colPoint[0], (float)colPoint[1], lat, lon);
						float othDtoColPoint = getPointsDistance((float)colPoint[0], (float)colPoint[1], othLat, othLon);
						float ttc = (dtoColPoint *1000) / speed;
						getLog().info("SPEED {} DISTtoCOLPOINT {}", speed, dtoColPoint);
						float ottc = (othDtoColPoint * 1000) / othSpeed;
						if (Math.abs(ttc - ottc) <= colThresh) { // not greater than 2s implies a collision may happen
							if ((ttc < ottc)&& (ihaveway == true)) { //I will arrive the intersection point first
								speed = (speed + (speed * 1.5f)) > 16.6667f ? 16.667f:(speed + (speed * 1.5f));//dtoColPoint / (ttc - colThresh); //60 / 3.6f;
								getLog().info("TTC: {} DistTOCol: {} New SPEED: {}", ttc, dtoColPoint, speed);
							}else {
								speed = (speed - (speed * 1.5f)) < 5.5556f ? 5.5556f:(speed - (speed * 1.5f));//dtoColPoint / (ttc + colThresh);
							}
						}
					}
				}
				getOs().changeSpeedWithInterval(speed, 1 * TIME.SECOND);
			}	
			
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
		// TODO Auto-generated method stub
		speed = (float) curUpdate.getSpeed();
		approachID = getOs().getVehicleData().getRoadPosition().getConnection().getId();
		vehLocation = curUpdate.getPosition();
		

		lat = (float) vehLocation.getLatitude();
		lon = (float) vehLocation.getLongitude();
		heading =  curUpdate.getHeading().floatValue();
		acceleration = curUpdate.getLongitudinalAcceleration().floatValue();
		
		final VehicleRoute routeInfo = Objects.requireNonNull(getOs().getNavigationModule().getCurrentRoute());
		departID = routeInfo.getLastConnectionId();
		path = approachID + ", " + departID;
		dstToIntersection = DistanceToIntersection(vehLocation);
		getLog().info("DISTANCE: {}", dstToIntersection);
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
		if (dstToIntersection <= (departDistance * 1.5)) {
			//once in the intersection critical point, send ICA
			SendICA();
			sendICA = true;
		}else {
			sendICA = false;
		}
		/*
		if (perceiveVehicles()) {
			speed = speed - (speed*0.40f);
			getOs().changeSpeedWithInterval(speed, 5 * TIME.SECOND);
		}else {
			speed = 50 / 3.6f;
		}
		
		if (dstToIntersection > 0.025f) {
			speed = 50 / 3.6f;
			getOs().changeSpeedWithInterval(speed, 1 * TIME.SECOND);
		}else {
			speed = 0 / 3.6f;
			getOs().changeSpeedWithInterval(speed, 1 * TIME.SECOND);
		}
		*/
	}

	

	

}
