package com.debangis;

import com.debangis.messages.*;
import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class TrafficRecording {
	
	Table table;
	int status;
	public TrafficRecording() {
		// TODO Auto-generated constructor stub
		table = null;
	}
	public Table CreateTrafficRecording(String tableName) {
		 table = Table.create(tableName,
				LongColumn.create("timeStamp"),
				StringColumn.create("junctionid"),
				StringColumn.create("vehicleid"),
				StringColumn.create("approachleg"),
				StringColumn.create("departleg"),
				FloatColumn.create("latitude"),
				FloatColumn.create("longitude"),
				LongColumn.create("arrivaltime"),
				IntColumn.create("vehicletype"),
				FloatColumn.create("speed"),
				IntColumn.create("priority"),
				IntColumn.create("msgtype")
				
			);
		return table;
	}
	public Table ProcessSRM(Table table, SignalRequestMessage srm) {
		int mstpe = srm.getMsgType();
		String vehID = srm.getVehicleId();
		if (mstpe == 1) { //It's an approach message. Thus, record it.
			
			
			float lat = (float) srm.getLocation().getLatitude();
			float lon = (float) srm.getLocation().getLongitude();
			
			// Create a Row object with specific values
			
			Row row = table.appendRow();
			
			row.setLong("timestamp",srm.getTimeStamp());
			row.setString("junctionid",srm.getJunctionId());
			row.setString("vehicleid", vehID);
			row.setString("approachleg",srm.getApproachLeg());
			row.setString("departleg",srm.getDepartLeg());
			row.setFloat("latitude", lat);
			row.setFloat("longitude", lon);
			row.setLong("arrivaltime",srm.getArrivalTime());
			row.setInt("vehicletype",srm.getVehicleType());
			row.setFloat("speed",srm.getSpeed());
			row.setInt("priority",srm.getPriority());
			row.setInt("msgType", mstpe);
										
			table.addRow(row);
			status = 2; //Successfully recorded vehicle record
		}else { //if mstpe (msgType) is 2, it's a depart message. Thus, vehicle record is deleted
			table = DropVehicleRecord(table, vehID);
			status = 4; //Successfully deleted vehicle record
		}
		return table.dropDuplicateRows();//table;
	}

	public Table RecordVehicle(Table table, SignalRequestMessage srm) {
		//This function is for vehicles to be able to record instance of other vehicles
		//Thus, given SRM of a vehicle, the previous instance is deleted before recording the new instance

		int mstpe = srm.getMsgType();
		String vehID = srm.getVehicleId();
		
		//Delete Previous information of the vehicle
		table = DropVehicleRecord(table, vehID);
		
		//Record new information of the 
		float lat = (float) srm.getLocation().getLatitude();
		float lon = (float) srm.getLocation().getLongitude();
		
		// Create a Row object with specific values
		
		Row row = table.appendRow();
		
		row.setLong("timestamp",srm.getTimeStamp());
		row.setString("junctionid",srm.getJunctionId());
		row.setString("vehicleid", vehID);
		row.setString("approachleg",srm.getApproachLeg());
		row.setString("departleg",srm.getDepartLeg());
		row.setFloat("latitude", lat);
		row.setFloat("longitude", lon);
		row.setLong("arrivaltime",srm.getArrivalTime());
		row.setInt("vehicletype",srm.getVehicleType());
		row.setFloat("speed",srm.getSpeed());
		row.setInt("priority",srm.getPriority());
		row.setInt("msgType", mstpe);
									
		table.addRow(row);
		
		return table;
	}
	
	private Table DropVehicleRecord(Table t, String vehId) {
		//Drop all vehicles whose arrival time is earlier than this vehicle and their approach and depart is the same with this vehicle
		
		// Filter the table to exclude rows where 'vehicleid' equals the specified value
        t = t.where(t.stringColumn("vehicleid").isNotEqualTo(vehId));

		return t;
	}
	public Table Append(Table t1, Table t2) {
		
		return (Table)t1.append((Table)t2);
	}
	public void setTable(Table t) {
		table = t;
		
	}
	
	
	public int getStatus() {
		return this.status;
	}
}
