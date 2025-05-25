package com.debangis.controllers;

import java.io.File;
import java.io.IOException;

import com.fuzzylite.Engine;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.Centroid;
import com.fuzzylite.imex.FisExporter;
import com.fuzzylite.norm.s.Maximum;
import com.fuzzylite.norm.t.Minimum;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Triangle;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;

public class QueueLengthDistanceSpeedWaitingTimeController {

	Engine engine;
	public QueueLengthDistanceSpeedWaitingTimeController() {
		// TODO Auto-generated constructor stub
		System.out.println("Creating Fuzzy Model");
		engine = new Engine();
		engine.setName("QueueLengthDistanceSpeedWaitingTimeController");
		engine.setDescription("Determines phase duration for given flow given the waitingTime, queueLength, maxSpeed and maxiDistance");

		InputVariable waitingTime = new InputVariable();
		waitingTime.setName("waitingTime");
		waitingTime.setDescription("");
		waitingTime.setEnabled(true);
		waitingTime.setRange(0.000, 50.000);
		waitingTime.setLockValueInRange(false);
		waitingTime.addTerm(new Triangle("short", -20.83, 0, 20.83));
		waitingTime.addTerm(new Triangle("long", 4.167, 25, 45.83));
		waitingTime.addTerm(new Triangle("verylong", 29.17, 50, 70.83));
		engine.addInputVariable(waitingTime);

		
		InputVariable queueLength = new InputVariable();
		queueLength.setName("queueLength");
		queueLength.setDescription("");
		queueLength.setEnabled(true);
		queueLength.setRange(0.000, 21.000);
		queueLength.setLockValueInRange(false);
		queueLength.addTerm(new Triangle("short", -8.333, 0.00, 8.333));
		queueLength.addTerm(new Triangle("long", 1.667, 10, 18.33));
		queueLength.addTerm(new Triangle("verylong", 11.67, 20, 28.33));
		engine.addInputVariable(queueLength);


		InputVariable distance = new InputVariable();
		distance.setName("distance");
		distance.setDescription("");
		distance.setEnabled(true);
		distance.setRange(0.000, 100.000);
		distance.setLockValueInRange(false);
		distance.addTerm(new Triangle("near", -41.66, 0, 41.66));
		distance.addTerm(new Triangle("far", 8.334, 50, 91.66));
		distance.addTerm(new Triangle("veryfar", 58.34, 100, 141.7));
		engine.addInputVariable(distance);

		
		InputVariable speed = new InputVariable();
		speed.setName("speed");
		speed.setDescription("");
		speed.setEnabled(true);
		speed.setRange(0.000, 61.000);
		speed.setLockValueInRange(false);
		speed.addTerm(new Triangle("slow", -25, 0, 25));
		speed.addTerm(new Triangle("fast", 5, 30, 55));
		speed.addTerm(new Triangle("veryfast", 35, 60, 85.02));
		engine.addInputVariable(speed);


		OutputVariable phaseDuration = new OutputVariable();
		phaseDuration.setName("phaseDuration");
		phaseDuration.setDescription("");
		phaseDuration.setEnabled(true);
		phaseDuration.setRange(5.000, 25.000);
		phaseDuration.setLockValueInRange(false);
		phaseDuration.setAggregation(new Maximum());
		phaseDuration.setDefuzzifier(new Centroid(200));
		phaseDuration.setDefaultValue(Double.NaN);
		phaseDuration.setLockPreviousValue(false);
		phaseDuration.addTerm(new Triangle("short", -3.336, 5, 13.33));
		phaseDuration.addTerm(new Triangle("long", 6.667, 15, 23.33));
		phaseDuration.addTerm(new Triangle("verylong", 16.67, 25, 33.33));
		engine.addOutputVariable(phaseDuration);



		RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setName("");
		ruleBlock.setDescription("");
		ruleBlock.setEnabled(true);
		ruleBlock.setConjunction(new Minimum());
		ruleBlock.setDisjunction(new Maximum());
		ruleBlock.setImplication(new Minimum());
		ruleBlock.setActivation(new General());
		//queueLengthDistanceSpeedWaitingTime Controller
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is slow) and (waitingTime is short) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is slow) and (waitingTime is long) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is slow) and (waitingTime is verylong) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is fast) and (waitingTime is short) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is fast) and (waitingTime is long) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is fast) and (waitingTime is verylong) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is veryfast) and (waitingTime is short) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is veryfast) and (waitingTime is long) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is near) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is slow) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is slow) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is slow) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is fast) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is fast) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is fast) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is veryfast) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is veryfast) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is far) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is slow) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is slow) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is slow) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is fast) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is fast) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is fast) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is veryfast) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is veryfast) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is short) and (distance is veryfar) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is slow) and (waitingTime is short) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is slow) and (waitingTime is long) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is slow) and (waitingTime is verylong) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is fast) and (waitingTime is short) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is fast) and (waitingTime is long) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is fast) and (waitingTime is verylong) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is veryfast) and (waitingTime is short) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is veryfast) and (waitingTime is long) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is near) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is short", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is slow) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is slow) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is slow) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is fast) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is fast) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is fast) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is veryfast) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is veryfast) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is far) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is slow) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is slow) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is slow) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is fast) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is fast) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is fast) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is veryfast) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is veryfast) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is long) and (distance is veryfar) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is slow) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is slow) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is slow) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is fast) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is fast) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is fast) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is veryfast) and (waitingTime is short) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is veryfast) and (waitingTime is long) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is near) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is long", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is slow) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is slow) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is slow) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is fast) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is fast) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is fast) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is veryfast) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is veryfast) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is far) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is slow) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is slow) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is slow) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is fast) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is fast) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is fast) and (waitingTime is verylong) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is veryfast) and (waitingTime is short) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is veryfast) and (waitingTime is long) then phaseDuration is verylong", engine));
		ruleBlock.addRule(Rule.parse("if (queueLength is verylong) and (distance is veryfar) and (speed is veryfast) and (waitingTime is verylong) then phaseDuration is verylong", engine));

		engine.addRuleBlock(ruleBlock);

		
		System.out.println("QueueLengthDistanceSpeedWaitingTimeController Fuzzy Model Created");
	}

	public double[] EvaluateFIS(double wT, double qL, double sP, double dS){
		//wT: waiting Time; qL: Queue Length; sP: speed; dS: Distance
		
		StringBuilder status = new StringBuilder();
        if (! engine.isReady(status))
            throw new RuntimeException("[engine error] engine is not ready:\n" + status);
        if (wT > 50) {
        	wT = 49;
        }
        if (qL > 30) {
        	qL = 29;
        } 
        if (dS > 100) {
        	dS = 99;
        }
        
        if (sP > 60) {
        	sP = 59;
        }
        InputVariable waitingTime = engine.getInputVariable("waitingTime");
  		InputVariable queueLength = engine.getInputVariable("queueLength");
  		InputVariable distance = engine.getInputVariable("distance");
		InputVariable speed = engine.getInputVariable("speed");
		
		
        OutputVariable phaseDuration = engine.getOutputVariable("phaseDuration");
		
		waitingTime.setValue(wT);
		queueLength.setValue(qL);
		distance.setValue(dS);
		speed.setValue(sP);
		
        engine.process();
        double[] outputs = {phaseDuration.getValue()};
        System.out.println("QDSW Model: FUZZY INPUT-OUTPUT is: " + " WT: " + wT + " QL: " + qL + " SPD: " + sP + " DST: " + dS + " PD: " + outputs[0]);
		return  outputs; //Op.str(dtgnt.getValue()) + Op.str(cle.getValue());
	}
	
	
	public void GenerateMatLabModel(String name) {
		// Create an instance of FisExporter
        FisExporter exporter = new FisExporter();
        if (!(name.endsWith(".fis"))) {
        	name = name + ".fis";
        }
        // Export the fuzzy inference system to a FLL file
        try {
			exporter.toFile(new File(name), engine);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
