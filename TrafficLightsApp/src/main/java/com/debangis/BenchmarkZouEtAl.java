package com.debangis;

import com.debangis.messages.*;
import java.io.File;
import java.io.IOException;

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

import com.fuzzylite.*;
import com.fuzzylite.activation.*;
import com.fuzzylite.defuzzifier.*;
import com.fuzzylite.factory.*;
import com.fuzzylite.hedge.*;
import com.fuzzylite.imex.*;
import com.fuzzylite.norm.*;
import com.fuzzylite.norm.s.*;
import com.fuzzylite.norm.t.*;
import com.fuzzylite.rule.*;
import com.fuzzylite.term.*;
import com.fuzzylite.variable.*;

import tech.tablesaw.api.Table;

public class BenchmarkZouEtAl extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
	/*
	 * 
	 *	Hindawi Journal of Advanced Transportation
		Volume 2023, Article ID 9744737, 14 pages
		https://doi.org/10.1155/2023/9744737
	 */
	private ObservationModule obsm;
	private DecisionModule dcsm;
	//Traffic Recording Variables
	TrafficRecording trfRcdng;
	Table adhocTrafficRecords;
	
	int minPhaseDuration = 7;
	int maxPhaseDuration = 25;
	int phaseDuration = minPhaseDuration; //Temp value (15) for testing, set to 1
	int nextPhaseDuration = 0;
	String nextPhase;
	String currentPhase;
	int phasePointer = 1; 
	int prevPhasePointer = 0;
	
	int previousPhaseDuration = phaseDuration; //Used to calculate traffic arrival rate
  
	String[] greenPhaseSequence = {"northsouthleft", "northsouthstraightright", "eastwestleft", "eastweststraightright"};
	String[] yellowPhaseSequence = {"northsouthleftyellow", "northsouthstraightrightyellow", "eastwestleftyellow", "eastweststraightrightyellow"};
	String allRed = "allred";
	
	//Radius of the Earth in kilometers
	private static final double EARTH_RADIUS = 6371; // in kilometers
	private GeoPoint intersectionLocation;

	public BenchmarkZouEtAl() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public void onStartup() {
		// TODO Auto-generated method stub
		// This for Adhoc network Communication. 
	    getLog().infoSimTime((OperatingSystemAccess)this, "Initialize application", new Object[0]);
	    ((TrafficLightOperatingSystem)getOs()).getAdHocModule().enable();
	    getLog().infoSimTime((OperatingSystemAccess)this, "Activated Wifi Module", new Object[0]);
	    //*/
		  
	    Event setGreenEvent = new Event(getOs().getSimulationTime() + (1 * 1000000000L), this::roundRobinControl);
	    getOs().getEventManager().addEvent(setGreenEvent);
	    Event event = new Event(getOs().getSimulationTime() + 300000000L, this::initializeParameters);
		getOs().getEventManager().addEvent(event);
	}
	
	 private void initializeParameters(Event event) {
		 trfRcdng = new TrafficRecording();
		 adhocTrafficRecords = trfRcdng.CreateTrafficRecording("adhocTrafficRecords");
		 intersectionLocation = GeoPoint.latLon(53.547745, 9.966246);
		 obsm = new ObservationModule();
		 dcsm = new DecisionModule();
		 
		 System.out.println("BenchmarkZouEtAl: InitializeParameters Done");
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
		  System.out.println("Setting Traffic Lights to RED: " + allRed );
		  
	  }
	  

	  private void setYellowPhase(Event event) {
		//This function traffic lights 
		  //String phase = yellowPhaseSequence[phasePointer];
		  String phase = yellowPhaseSequence[prevPhasePointer];
		  ((TrafficLightOperatingSystem)getOs()).switchToProgram(phase);
		  getLog().infoSimTime(this, "Traffic Lights Assigned to " + phase );
		  System.out.println("Setting Traffic Lights to YELLOW: " + phase + " Phase Pointer: " + prevPhasePointer);
	  }
	  
	  private String[] getPhaseFlows(String phase) {
		  
		  String[] flows = new String[4];
		  //, "", "", ""
		  if(phase.equals("northsouthleft")) {
			  flows[0] = "northApproach";
			  flows[1] = "southAproach";
			  flows[2] = "eastDepart";
			  flows[3] = "westDepart";
		  }else if(phase.equals("northsouthstraightright")) {
			  flows = new String[8];
			  flows[0] = "northApproach";
			  flows[1] = "southAproach";
			  flows[2] = "northAproach";
			  flows[3] = "southApproach";
			  
			  flows[4] = "southDepart";
			  flows[5] = "northDepart";
			  flows[6] = "westDepart";
			  flows[7] = "eastDepart";
		  }else if(phase.equals("eastwestleft")) {
			  flows[0] = "eastApproach";
			  flows[1] = "westAproach";
			  flows[2] = "southDepart";
			  flows[3] = "northDepart";
		  }else if(phase.equals("eastweststraightright")) {
			  flows = new String[8];
			  flows[0] = "eastApproach";
			  flows[1] = "westAproach";
			  flows[2] = "eastAproach";
			  flows[3] = "westApproach";
			  
			  flows[4] = "northDepart";
			  flows[5] = "southDepart";
			  flows[6] = "westDepart";
			  flows[7] = "eastDepart";
			  	  
		  } 
		  return flows;
	  }
	  public void extendPhaseDuration(Event event) {
		  getLog().info("EXTENDED DUR Executing");
		  if (phasePointer == 0) {
			  prevPhasePointer =  3;
		  }else {
			  prevPhasePointer = phasePointer-1;
		  }
		  
		  if (adhocTrafficRecords.rowCount() > 0) {
			  System.out.println("adhocTrafficRecords GREATER THAN 0: " + adhocTrafficRecords.rowCount()  );
			  int pPoint = phasePointer;
			  int curQueue = 0;
			  int nextQueue = 0;
			  int extPhaseDuration = 0;
			  double curArrivalRate = 0, nextArrivalRate = 0, curPhaseIntensity = 0, nextPhaseIntensity = 0;
			  
			  Table temp;
			  
			  String[] curPhaseFlows = getPhaseFlows(greenPhaseSequence[pPoint]);
			  if (pPoint == 3) {
				  pPoint = 0;
			  }else {
				  pPoint += 1;
			  }
			  String[] nextPhaseFlows = getPhaseFlows(greenPhaseSequence[pPoint]);
			  
			  //Get queue lengths
			  //Current Phase
			  for (int i = 0; i < (curPhaseFlows.length / 2); i++) {
				  temp = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(curPhaseFlows[i]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(curPhaseFlows[((curPhaseFlows.length / 2) + i)])));
				 /*
				  int c = temp.rowCount();
				  if (curQueue < c) {
					  curQueue += c;
				  }
				  */
				  curQueue += temp.rowCount();
			  }
			  //curQueue *= 4;
			  System.out.println("curQueue: " + curQueue);
			  //Compute current phase queue average
			  //curQueue = curQueue/curPhaseFlows.length;
			  //System.out.println("Average curQueue: " + curQueue);
			  
			//Next Phase
			  for (int i = 0; i < (nextPhaseFlows.length / 2); i++) {
				  temp = adhocTrafficRecords.where(adhocTrafficRecords.stringColumn("approachleg").isEqualTo(nextPhaseFlows[i]).and(adhocTrafficRecords.stringColumn("departleg").isEqualTo(nextPhaseFlows[((nextPhaseFlows.length / 2) + i)])));
				  nextQueue += temp.rowCount();
			  }
			  //nextQueue *= 4;
			  System.out.println("nextQueue: " + nextQueue);
			  //Compute next phase queue average
			  //nextQueue = nextQueue/nextPhaseFlows.length;
			  //System.out.println("Average nextQueue: " + nextQueue);
			  
			  //Compute the Arrival rate of Current  and Next Phases
			  curArrivalRate = (double) curQueue / previousPhaseDuration; 
			  nextArrivalRate = (double) nextQueue/previousPhaseDuration;
			  
			  System.out.println("previousPhaseDuration: "+ previousPhaseDuration + " curArrivalRate: " + curArrivalRate + " nextArrivalRate: " + nextArrivalRate);
			  curPhaseIntensity = obsm.evaluateFIS(curQueue, curArrivalRate);
			  nextPhaseIntensity = obsm.evaluateFIS(nextQueue, nextArrivalRate);
			  
			  System.out.println("curPhaseIntensity : " + curPhaseIntensity + " nextPhaseIntensity: " +  nextPhaseIntensity);
			  
			  extPhaseDuration = (int) Math.ceil(dcsm.evaluateFIS(curPhaseIntensity, nextPhaseIntensity));
			  System.out.println("Extending Phase Duration by: " + extPhaseDuration );
			  
			  if ((extPhaseDuration > 8) && ((phaseDuration + extPhaseDuration) < maxPhaseDuration)) {
				  phaseDuration = phaseDuration + extPhaseDuration;
				  System.out.println("Extending Phase Duration by: " + extPhaseDuration );
				  
				  Event extGTime = new Event(getOs().getSimulationTime() + ((phaseDuration - 6)* 1000000000L), this::extendPhaseDuration);
				  getOs().getEventManager().addEvent(extGTime);
				  
				  phaseDuration = minPhaseDuration;
				  
			  }else if ((extPhaseDuration > 8) && ((phaseDuration + extPhaseDuration) >= maxPhaseDuration)) {
				  phaseDuration = phaseDuration + (maxPhaseDuration - phaseDuration);
				  System.out.println("Extending Phase Duration for ((phaseDuration + extPhaseDuration) >= maxPhaseDuration) by: " + (maxPhaseDuration - phaseDuration));
				  
				  Event setGreenEvent = new Event(getOs().getSimulationTime() + (phaseDuration * 1000000000L), this::roundRobinControl);
				  getOs().getEventManager().addEvent(setGreenEvent);
				  Event setYellowEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 5)* 1000000000L, this::setYellowPhase);
				  getOs().getEventManager().addEvent(setYellowEvent);
				  
				  Event setRedEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 2)* 1000000000L, this::setAllRedPhase);
				  getOs().getEventManager().addEvent(setRedEvent);
				  
			  }else if (extPhaseDuration < 8) {
				  Event setGreenEvent = new Event(getOs().getSimulationTime() + (phaseDuration * 1000000000L), this::roundRobinControl);
				  getOs().getEventManager().addEvent(setGreenEvent);
				  Event setYellowEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 5)* 1000000000L, this::setYellowPhase);
				  getOs().getEventManager().addEvent(setYellowEvent);
				  
				  Event setRedEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 2)* 1000000000L, this::setAllRedPhase);
				  getOs().getEventManager().addEvent(setRedEvent);
				  
			  }
		  }else {
			  phaseDuration = minPhaseDuration;
			  
			  //System.out.println("adhocTrafficRecords is empty ");
			  
			  Event setGreenEvent = new Event(getOs().getSimulationTime() + (phaseDuration * 1000000000L), this::roundRobinControl);
			  getOs().getEventManager().addEvent(setGreenEvent);
			  Event setYellowEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 5)* 1000000000L, this::setYellowPhase);
			  getOs().getEventManager().addEvent(setYellowEvent);
			  
			  Event setRedEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 2)* 1000000000L, this::setAllRedPhase);
			  getOs().getEventManager().addEvent(setRedEvent);
		  }
		  
		  previousPhaseDuration = phaseDuration;
		  System.out.println("previousPhaseDuration is set to " + previousPhaseDuration);
	  }
	  
	  public void roundRobinControl(Event event){
		  
		  String trafficLight = "";
		  
		  //while(true){ //Loop forever 
			  if(phasePointer == 0){
				  trafficLight = "northsouthleft";
				  phasePointer += 1;
			  }else if(phasePointer == 1){
				  trafficLight = "northsouthstraightright";
				  phasePointer += 1;
			  }else if(phasePointer == 2){
				  trafficLight = "eastwestleft";
				  phasePointer += 1;
			  }else if(phasePointer == 3){
				  trafficLight = "eastweststraightright";
				  phasePointer = 0;
			  }
			  ((TrafficLightOperatingSystem)getOs()).switchToProgram(trafficLight);
			  long dur = ((TrafficLightOperatingSystem) getOs()).getSimulationTime() + 1000000L;
			  //while (((TrafficLightOperatingSystem) getOs()).getSimulationTime() < dur){

			  getLog().infoSimTime(this, "Traffic Lights Assigned to " + trafficLight );
			  System.out.println("Next schedule is for: "+ trafficLight);
			  
			  
			  Event setNextScheduleEvent = new Event(getOs().getSimulationTime() + (phaseDuration - 6)* 1000000000L, this::extendPhaseDuration);
			  getOs().getEventManager().addEvent(setNextScheduleEvent);
			  
			    
		  	  
	  }
	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
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


final class ObservationModule {
    private Engine fuzzyEngine;

    public ObservationModule() {
        fuzzyEngine = new Engine();
        fuzzyEngine = createFIS();
    }

    private Engine createFIS() {
        Engine fuzzyEngine = new Engine();
        fuzzyEngine.setName("TrafficIntensitySystem");

        // Define input variables
        InputVariable queueCount = new InputVariable();
        queueCount.setName("queueCount");
        queueCount.setRange(0.0, 20.0);
        queueCount.setEnabled(true);
        queueCount.addTerm(new Triangle("NS", 0.0, 0.0, 7.0));
        queueCount.addTerm(new Triangle("S", 0.0, 3.0, 10.0));
        queueCount.addTerm(new Triangle("M", 2.7, 10.0, 17.0));
        queueCount.addTerm(new Triangle("L", 10.0, 16.5, 20.0));
        queueCount.addTerm(new Triangle("PL", 13.0, 20.0, 20.0));
        fuzzyEngine.addInputVariable(queueCount);

        InputVariable arrivalRate = new InputVariable();
        arrivalRate.setName("arrivalRate");
        arrivalRate.setRange(0.0, 1.0);
        arrivalRate.setEnabled(true);
        arrivalRate.addTerm(new Triangle("NS", 0.0, 0.0, 0.37));
        arrivalRate.addTerm(new Triangle("S", 0.0, 0.2, 0.5));
        arrivalRate.addTerm(new Triangle("Z", 0.18, 0.5, 0.83));
        arrivalRate.addTerm(new Triangle("M", 0.5, 0.8, 1.0));
        arrivalRate.addTerm(new Triangle("PM", 0.63, 1.0, 1.0));
        fuzzyEngine.addInputVariable(arrivalRate);

        // Define output variable
        OutputVariable trafficIntensity = new OutputVariable();
        trafficIntensity.setName("trafficIntensity");
        trafficIntensity.setEnabled(true);
        trafficIntensity.setRange(0.0, 6.0);
        trafficIntensity.setLockValueInRange(false);
        trafficIntensity.setAggregation(new AlgebraicSum());
        trafficIntensity.setDefuzzifier(new Centroid(200));
        trafficIntensity.setDefaultValue(Double.NaN);
        trafficIntensity.setLockPreviousValue(false);
        trafficIntensity.addTerm(new Triangle("VD", 0.0, 0.0, 2.1));
        trafficIntensity.addTerm(new Triangle("D", 0.0, 1.0, 3.0));
        trafficIntensity.addTerm(new Triangle("M", 0.9, 3.0, 5.1));
        trafficIntensity.addTerm(new Triangle("U", 3.0, 5.0, 6.0));
        trafficIntensity.addTerm(new Triangle("VU", 3.9, 6.0, 6.0));
        fuzzyEngine.addOutputVariable(trafficIntensity);


        // Define rules
        RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setName("");
		ruleBlock.setDescription("");
		ruleBlock.setEnabled(true);
		ruleBlock.setConjunction(new Minimum());
		ruleBlock.setDisjunction(new Maximum());
		ruleBlock.setImplication(new Minimum());
		ruleBlock.setActivation(new General());
		
        ruleBlock.addRule(Rule.parse("if queueCount is NS and arrivalRate is NS then trafficIntensity is VD", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is NS and arrivalRate is S then trafficIntensity is D", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is NS and arrivalRate is Z then trafficIntensity is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is NS and arrivalRate is M then trafficIntensity is U", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is NS and arrivalRate is PM then trafficIntensity is U", fuzzyEngine));
        
        ruleBlock.addRule(Rule.parse("if queueCount is S and arrivalRate is NS then trafficIntensity is VD", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is S and arrivalRate is S then trafficIntensity is D", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is S and arrivalRate is Z then trafficIntensity is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is S and arrivalRate is M then trafficIntensity is U", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is S and arrivalRate is PM then trafficIntensity is VU", fuzzyEngine));
        
        ruleBlock.addRule(Rule.parse("if queueCount is M and arrivalRate is NS then trafficIntensity is D", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is M and arrivalRate is S then trafficIntensity is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is M and arrivalRate is Z then trafficIntensity is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is M and arrivalRate is M then trafficIntensity is U", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is M and arrivalRate is PM then trafficIntensity is VU", fuzzyEngine));
        
        ruleBlock.addRule(Rule.parse("if queueCount is L and arrivalRate is NS then trafficIntensity is D", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is L and arrivalRate is S then trafficIntensity is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is L and arrivalRate is Z then trafficIntensity is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is L and arrivalRate is M then trafficIntensity is VU", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is L and arrivalRate is PM then trafficIntensity is VU", fuzzyEngine));
        
        ruleBlock.addRule(Rule.parse("if queueCount is PL and arrivalRate is NS then trafficIntensity is D", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is PL and arrivalRate is S then trafficIntensity is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is PL and arrivalRate is Z then trafficIntensity is U", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is PL and arrivalRate is M then trafficIntensity is VU", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if queueCount is PL and arrivalRate is PM then trafficIntensity is VU", fuzzyEngine));
        
        fuzzyEngine.addRuleBlock(ruleBlock);

        return fuzzyEngine;
    }

    public double evaluateFIS(double qCount, double aRate) {
    	if (qCount >= 20) {
    		qCount = 19.9;
    	}else if(qCount <= 0) {
    		qCount = 0.1;
    	}
    	
    	if (aRate >= 1) {
    		aRate = 0.9;
    	}else if (aRate <= 0) {
    		aRate = 0.1;
    	}
    	InputVariable queueCount = fuzzyEngine.getInputVariable("queueCount");
  		InputVariable arrivalRate = fuzzyEngine.getInputVariable("arrivalRate");
  		OutputVariable trafficIntensity = fuzzyEngine.getOutputVariable("trafficIntensity");
		
  		queueCount.setValue(qCount);
  		arrivalRate.setValue(aRate);
        fuzzyEngine.process();
        return trafficIntensity.getValue();
    }
}


final class DecisionModule {
    private Engine fuzzyEngine;

    public DecisionModule() {
    	fuzzyEngine = new Engine();
        fuzzyEngine = createFIS();
    }

    private Engine createFIS() {
        //Engine engine = new Engine();
    	fuzzyEngine.setName("PhaseExtensionSystem");

        // Define input variables
        InputVariable curPhase = new InputVariable();
        curPhase.setName("curPhase");
        curPhase.setRange(0.0, 7.0);
        curPhase.setEnabled(true);
        curPhase.addTerm(new Triangle("VD", 0.0, 0.0, 2.1));
        curPhase.addTerm(new Triangle("D", 0.0, 1.0, 3.0));
        curPhase.addTerm(new Triangle("M", 0.9, 3.0, 5.1));
        curPhase.addTerm(new Triangle("U", 3.0, 5.0, 6.0));
        curPhase.addTerm(new Triangle("VU", 3.9, 6.0, 6.0));
        fuzzyEngine.addInputVariable(curPhase);

        InputVariable nextPhase = new InputVariable();
        nextPhase.setName("nextPhase");
        nextPhase.setRange(0.0, 7.0);
        nextPhase.addTerm(new Triangle("VD", 0.0, 0.0, 2.1));
        nextPhase.addTerm(new Triangle("D", 0.0, 1.0, 3.0));
        nextPhase.addTerm(new Triangle("M", 0.9, 3.0, 5.1));
        nextPhase.addTerm(new Triangle("U", 3.0, 5.0, 6.0));
        nextPhase.addTerm(new Triangle("VU", 3.9, 6.0, 6.0));
        nextPhase.setEnabled(true);
        fuzzyEngine.addInputVariable(nextPhase);

        // Define output variable
        OutputVariable durExtend = new OutputVariable();
        durExtend.setName("durExtend");
        durExtend.setRange(0.0, 30.0);
        durExtend.setEnabled(true);
        durExtend.setRange(0.0, 6.0);
        durExtend.setLockValueInRange(false);
        durExtend.setAggregation(new AlgebraicSum());
        durExtend.setDefuzzifier(new Centroid());
        durExtend.setDefaultValue(Double.NaN);
        durExtend.setLockPreviousValue(false);
        durExtend.addTerm(new Triangle("NS", 0.0, 0.0, 10.9));
        durExtend.addTerm(new Triangle("S", 0.0, 5.0, 15.0));
        durExtend.addTerm(new Triangle("M", 4.9, 15.0, 25.9));
        durExtend.addTerm(new Triangle("L", 15.0, 25.0, 30.0));
        durExtend.addTerm(new Triangle("PL", 19.9, 30.0, 30.0));
        fuzzyEngine.addOutputVariable(durExtend);

        
        // Define rules
        RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setName("");
		ruleBlock.setDescription("");
		ruleBlock.setEnabled(true);
		ruleBlock.setConjunction(new AlgebraicProduct());
		ruleBlock.setDisjunction(new AlgebraicSum());
		ruleBlock.setImplication(new AlgebraicProduct());
		ruleBlock.setActivation(new General());
		


		ruleBlock.addRule(Rule.parse("if curPhase is VD and nextPhase is VD then durExtend is NS", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is VD and nextPhase is D then durExtend is S", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is VD and nextPhase is M then durExtend is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is VD and nextPhase is U then durExtend is PL", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is VD and nextPhase is VU then durExtend is PL", fuzzyEngine));
        
        ruleBlock.addRule(Rule.parse("if curPhase is D and nextPhase is VD then durExtend is NS", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is D and nextPhase is D then durExtend is S", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is D and nextPhase is M then durExtend is L", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is D and nextPhase is U then durExtend is L", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is D and nextPhase is VU then durExtend is PL", fuzzyEngine));
        
        ruleBlock.addRule(Rule.parse("if curPhase is M and nextPhase is VD then durExtend is NS", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is M and nextPhase is D then durExtend is NS", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is M and nextPhase is M then durExtend is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is M and nextPhase is U then durExtend is L", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is M and nextPhase is VU then durExtend is PL", fuzzyEngine));
        
        ruleBlock.addRule(Rule.parse("if curPhase is U and nextPhase is VD then durExtend is NS", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is U and nextPhase is D then durExtend is NS", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is U and nextPhase is M then durExtend is S", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is U and nextPhase is U then durExtend is M", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is U and nextPhase is VU then durExtend is L", fuzzyEngine));
        
        ruleBlock.addRule(Rule.parse("if curPhase is VU and nextPhase is VD then durExtend is NS", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is VU and nextPhase is D then durExtend is NS", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is VU and nextPhase is M then durExtend is S", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is VU and nextPhase is U then durExtend is S", fuzzyEngine));
        ruleBlock.addRule(Rule.parse("if curPhase is VU and nextPhase is VU then durExtend is M", fuzzyEngine));
        fuzzyEngine.addRuleBlock(ruleBlock);
        
        return fuzzyEngine;
    }

    public double evaluateFIS(double curPhaseIntensity, double nextPhaseIntensity) {
    	if (curPhaseIntensity >= 6) {
    		curPhaseIntensity = 5.9;
    	}else if (curPhaseIntensity <= 0) {
    		curPhaseIntensity = 0.1;
    	}
    	
    	if (nextPhaseIntensity >= 6) {
    		nextPhaseIntensity = 5.9;
    	}else if (nextPhaseIntensity <= 0) {
    		nextPhaseIntensity = 0.1;
    	}
        fuzzyEngine.getInputVariable("curPhase").setValue(curPhaseIntensity);
        fuzzyEngine.getInputVariable("nextPhase").setValue(nextPhaseIntensity);
        fuzzyEngine.process();
        return fuzzyEngine.getOutputVariable("durExtend").getValue() * 3.5;
    }
    
    public void GenerateMatLabModel(String name) {
		// Create an instance of FisExporter
        FisExporter exporter = new FisExporter();
        if (!(name.endsWith(".fis"))) {
        	name = name + ".fis";
        }
        // Export the fuzzy inference system to a FLL file
        try {
			exporter.toFile(new File(name), fuzzyEngine);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}