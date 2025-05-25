package com.debangis;

import static tech.tablesaw.aggregate.AggregateFunctions.max;
import static tech.tablesaw.aggregate.AggregateFunctions.mean;
import static tech.tablesaw.aggregate.AggregateFunctions.min;

import java.io.IOException;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.OperatingSystemAccess;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import com.debangis.messages.SignalRequestMessage;

import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class MultiIntersectionVehDelayRecordingRSU extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {
	/*
	This RSU implementation is only used to record vehicular delay for the purpose of analysis.
		
	}
	*/
	TrafficRecording trfRcdng;
	Table vehDelay, vehicles;
	
	public MultiIntersectionVehDelayRecordingRSU() {
		// TODO Auto-generated constructor stub
	}

	

	@Override
	public void onStartup() {
		// TODO Auto-generated method stub
		 getLog().infoSimTime((OperatingSystemAccess)this, "Initialize Adhoc Communication Application", new Object[0]);
		    ((RoadSideUnitOperatingSystem)getOs()).getAdHocModule().enable();
		    getLog().infoSimTime((OperatingSystemAccess)this, "Activated Adhoc Communication Wifi Module", new Object[0]);
		    
		    Event event = new Event(getOs().getSimulationTime() + 3000000000L, this::initializeParameters);
			getOs().getEventManager().addEvent(event);
		
	}


	 private void initializeParameters(Event event) {
		 
		 vehDelay =  Table.create("VehicleDelay",
				 			StringColumn.create("junctionid"),
				 			StringColumn.create("vehicleid"),
				 			StringColumn.create("turn"),
				 			IntColumn.create("priority"),
				 			LongColumn.create("arrivaltime"),
				 			LongColumn.create("departuretime"),
				 			FloatColumn.create("delay")
				 		);
		 vehicles = Table.create("Vehicles",
				 			StringColumn.create("junctionid"),
				 			StringColumn.create("vehicleid"),
				 			StringColumn.create("approachleg"),
				 			StringColumn.create("departleg"),
				 			IntColumn.create("priority"),
				 			LongColumn.create("arrivaltime"),
				 			LongColumn.create("departuretime")
				 			
				 
				 		);
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

	private void recordVehicle(SignalRequestMessage srm) {
		Row row = vehicles.appendRow();
		
		row.setString("junctionid", srm.getJunctionId());
		row.setString("vehicleid", srm.getVehicleId());
		row.setString("approachleg", srm.getApproachLeg());
		row.setString("departleg", srm.getDepartLeg());
		row.setInt("priority", srm.getPriority());
		row.setLong("arrivaltime", srm.getArrivalTime());
		row.setLong("departuretime", getOs().getSimulationTime());
		vehicles.addRow(row);
		//System.out.println("REC TYPE 1: " + vehicles.printAll());
	}
	private void recordDelay(SignalRequestMessage srm) {
	
		String jncID = srm.getJunctionId();
		String vehID = srm.getVehicleId();
		Table veh = vehDelay.where(vehDelay.stringColumn("vehicleid").isEqualTo(vehID).and(vehDelay.stringColumn("junctionid").isEqualTo(jncID)));
		//System.out.println("Query Details: Junction : " + jncID + " Veh : " + vehID);
		if(veh.rowCount() == 0) {
			
			veh = vehicles.where(vehicles.stringColumn("vehicleid").isEqualTo(vehID).and(vehicles.stringColumn("junctionid").isEqualTo(jncID)));
			//System.out.println(veh.printAll());
			if(veh.rowCount() > 0) {
				String appleg = veh.getString(0, "approachleg");
				String dptleg = veh.getString(0, "departleg");
				String turn = determineVehiculeTurn(appleg, dptleg);
				
				//System.out.println("TURNING: " + turn);
				
				Row row = vehDelay.appendRow();
				
				row.setString("junctionid", jncID);
				row.setString("vehicleid", vehID);
				row.setString("turn", turn);
				row.setInt("priority", srm.getPriority());
				row.setLong("arrivaltime", srm.getArrivalTime());
				row.setLong("departuretime", srm.getTimeStamp());
				row.setFloat("delay", ((getOs().getSimulationTime() - srm.getArrivalTime())/ 1000000000L));
				vehDelay.addRow(row);
				//System.out.println("REC DELAY : Veh " + vehID + " Junction : " + jncID + " APPLeg : " + appleg + " DPTLeg : " + dptleg + " Turn : " + turn);
			}
		}
	
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
	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		// TODO Auto-generated method stub
		V2xMessage msg = receivedV2xMessage.getMessage();
	    if (!(msg instanceof SignalRequestMessage)) {
	    	getLog().infoSimTime((OperatingSystemAccess)this, "Ignoring message of type: {}", new Object[] { msg.getSimpleClassName() });
	    	return;
	    }
	    
	    SignalRequestMessage srm = (SignalRequestMessage) msg;
	    int msgType = srm.getMsgType();
	    //System.out.println("MSG TYPE : " + msgType);
	    if (msgType == 2) { //Its a depart message, thus record delay
			
	    	recordDelay(srm);
	    }else {
	    	recordVehicle(srm);
	    }
	    
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		try {
			vehDelay = vehDelay.dropDuplicateRows();
			vehDelay.write().csv("VehicularDelay.csv");
			getLog().info("VEHICULAR DELAY: {}", vehDelay.printAll());
			
			Table turningSummary = vehDelay.summarize("delay", min, max, mean ).by("turn");
			getLog().info("VEHICULAR DELAY: {}", turningSummary.printAll());
			
			FloatColumn allDelay = vehDelay.floatColumn("delay");
			double overallDelay = allDelay.mean();
			
			StringColumn allVehs = vehicles.stringColumn("vehicleid");  //Get IDs of priority vehicles
			StringColumn uniqueVehs = allVehs.unique();
			
			getLog().info("Overall Vehicular Delay at the Intersection: {} ", overallDelay);
			System.out.println("Overall Vehicular Delay at the Intersection: " + overallDelay);
			System.out.println("Summary of Delay Based on Vehicular Turning at Intersection: "+ turningSummary.printAll());
			System.out.println("Throughput: " + uniqueVehs.countUnique());
			getLog().info("Throughput: {}", vehDelay.rowCount());
			
			
			
			
			System.out.println("PRIORITY VEHICLES DETAILS:");
			
			Table priorityVehs = vehDelay.where(vehDelay.intColumn("priority").isGreaterThanOrEqualTo(3));
			
			FloatColumn allPriorityDelay = priorityVehs.floatColumn("delay");
			double overallPriorityDelay = allPriorityDelay.mean();
			//System.out.println(priorityVehs.printAll());
			Table priorityVehsSummary = priorityVehs.summarize("delay", min, max, mean ).by("turn");
			
			System.out.println("Overall Average Delay for priority vehicles at the Intersections: " + overallPriorityDelay);
			System.out.println("Summary of Priority Vehicles' Delay Based on Vehicular Turning at Intersections: "+ priorityVehsSummary.printAll());
			
			StringColumn prtVehs = priorityVehs.stringColumn("vehicleid");  //Get IDs of priority vehicles
			StringColumn uniquePrtVehs = prtVehs.unique();
			double totalTravelTime = 0.0;
			for (String value : uniquePrtVehs) { //Loop through to calculate travel time
				Table veh = priorityVehs.where(priorityVehs.stringColumn("vehicleid").isEqualTo(value));
				LongColumn arrTime = veh.longColumn("arrivaltime");
				double travelTime = (-1 *(arrTime.min() - arrTime.max()))/1000000000l;
				totalTravelTime += travelTime;
				//System.out.println("Veh: " + value + " Travel Time: " + travelTime);
				if(value.equals("veh_24")) {
					//System.out.println(veh.printAll());
				}
		    }
			System.out.println("Total : "+ totalTravelTime + " Count : " + uniquePrtVehs.countUnique());
			System.out.println("Average Travel Time of Priority Vehicles : "+ (totalTravelTime/uniquePrtVehs.countUnique()));
			
			System.out.println("OTHER VEHICLES DETAILS:");
			Table otherVehs = vehDelay.where(vehDelay.intColumn("priority").isLessThan(3));
			
			FloatColumn allOtherDelay = otherVehs.floatColumn("delay");
			double overallOtherDelay = allOtherDelay.mean();
			//System.out.println(otherVehs.printAll());
			Table otherVehsSummary = otherVehs.summarize("delay", min, max, mean ).by("turn");
			
			System.out.println("Summary of Other Vehicles' Delay Based on Vehicular Turning at Intersections: "+ otherVehsSummary.printAll());
			System.out.println("Overall Average Delay for other vehicles at the Intersections: " + overallOtherDelay);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		    
	}
}
