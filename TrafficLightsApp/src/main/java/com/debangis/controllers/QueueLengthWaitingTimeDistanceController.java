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

public class QueueLengthWaitingTimeDistanceController {
	Engine engine;
	public QueueLengthWaitingTimeDistanceController() {
		// TODO Auto-generated constructor stub
		// Create an engine
        engine = new Engine();
        engine.setName("QueueLengthWaitingTimeDistanceController");
        engine.setDescription("");

        // Input variables
        InputVariable queueLength = new InputVariable();
        queueLength.setName("queueLength");
        queueLength.setRange(0, 20);
        queueLength.addTerm(new Triangle("short", -8.333, 0, 8.333));
        queueLength.addTerm(new Triangle("long", 1.667, 10, 18.33));
        queueLength.addTerm(new Triangle("verylong", 11.67, 20, 28.33));
        engine.addInputVariable(queueLength);

        InputVariable waitingTime = new InputVariable();
        waitingTime.setName("waitingTime");
        waitingTime.setRange(0, 50);
        waitingTime.addTerm(new Triangle("low", -20.83, 0, 20.83));
        waitingTime.addTerm(new Triangle("high", 4.167, 25, 45.83));
        waitingTime.addTerm(new Triangle("veryhigh", 29.17, 50, 70.83));
        engine.addInputVariable(waitingTime);

        InputVariable distance = new InputVariable();
        distance.setName("distance");
        distance.setRange(0, 100);
        distance.addTerm(new Triangle("near", -41.67, 0, 41.67));
        distance.addTerm(new Triangle("far", 8.333, 50, 91.67));
        distance.addTerm(new Triangle("veryfar", 58.33, 100, 141.7));
        engine.addInputVariable(distance);

        // Output variables
        OutputVariable phaseDuration = new OutputVariable();
        phaseDuration.setName("phaseDuration");
        phaseDuration.setRange(5, 25);
        phaseDuration.addTerm(new Triangle("short", -10.42, 0, 10.42));
        phaseDuration.addTerm(new Triangle("long", 2.083, 12.5, 22.92));
        phaseDuration.addTerm(new Triangle("verylong", 14.58, 25, 35.42));
        phaseDuration.setDefaultValue(Double.NaN);
        phaseDuration.setLockValueInRange(false);
		phaseDuration.setAggregation(new Maximum());
		phaseDuration.setDefuzzifier(new Centroid(200));
        engine.addOutputVariable(phaseDuration);


		RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setName("");
		ruleBlock.setDescription("");
		ruleBlock.setEnabled(true);
		ruleBlock.setConjunction(new Minimum());
		ruleBlock.setDisjunction(new Maximum());
		ruleBlock.setImplication(new Minimum());
		ruleBlock.setActivation(new General());
		
        // Rules
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is low and distance is near then phaseDuration is short", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is low and distance is far then phaseDuration is short", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is low and distance is veryfar then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is high and distance is near then phaseDuration is short", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is high and distance is far then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is high and distance is veryfar then phaseDuration is verylong", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is veryhigh and distance is near then phaseDuration is short", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is veryhigh and distance is far then phaseDuration is short", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is veryhigh and distance is veryfar then phaseDuration is short", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is low and distance is near then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is low and distance is far then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is low and distance is veryfar then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is high and distance is near then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is high and distance is far then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is high and distance is veryfar then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is veryhigh and distance is near then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is veryhigh and distance is far then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is veryhigh and distance is veryfar then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is low and distance is near then phaseDuration is long", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is low and distance is far then phaseDuration is verylong", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is low and distance is veryfar then phaseDuration is verylong", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is high and distance is near then phaseDuration is verylong", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is high and distance is far then phaseDuration is verylong", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is high and distance is veryfar then phaseDuration is verylong", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is veryhigh and distance is near then phaseDuration is verylong", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is veryhigh and distance is far then phaseDuration is verylong", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is veryhigh and distance is veryfar then phaseDuration is verylong", engine));
        
        engine.addRuleBlock(ruleBlock);

		System.out.println("Fuzzy Model Created");

	}
	
	public double[] EvaluateFIS(double wT, double qL, double sP, double dS){
		//wT: waiting Time; qL: Queue Length; sP: speed; dS: Distance
		//This model does not use speed 
		StringBuilder status = new StringBuilder();
        if (! engine.isReady(status))
            throw new RuntimeException("[engine error] engine is not ready:\n" + status);
        if (wT > 50) {
        	wT = 49.9;
        }
        if (qL > 20) {
        	qL = 19.9;
        } 
        if (dS > 100) {
        	dS = 99.9;
        }
        if (sP > 60) {
        	sP = 59.9;
        }
        InputVariable waitingTime = engine.getInputVariable("waitingTime");
  		InputVariable queueLength = engine.getInputVariable("queueLength");
  		InputVariable distance = engine.getInputVariable("distance");
		//InputVariable speed = engine.getInputVariable("speed");
		
		
        OutputVariable phaseDuration = engine.getOutputVariable("phaseDuration");
		//OutputVariable speedOut = engine.getOutputVariable("speedOut");
		
		waitingTime.setValue(wT);
		queueLength.setValue(qL);
		distance.setValue(dS);
		//speed.setValue(sP);
		
        engine.process();
        double[] outputs = {phaseDuration.getValue()}; //, speedOut.getValue()};
        System.out.println("QWD Model: FUZZY INPUT-OUTPUT is: " + " WT: " + wT + " QL: " + qL + " SPD: " + sP + " DST: " + dS + " PD: " + outputs[0]);
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
