package com.debangis.controllers;

import java.io.File;
import java.io.IOException;

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

public class FuzzyModel{
	Engine engine;
	public FuzzyModel(){
		
		//Code automatically generated with jfuzzylite 6.0.
		System.out.println("Creating Fuzzy Model");
		engine = new Engine();
		engine.setName("FuzzyLogicTLSController");
		engine.setDescription("Determines phase duration for given flow given the waitingTime, queueLength, maxSpeed and maxiDistance");

		InputVariable waitingTime = new InputVariable();
		waitingTime.setName("waitingTime");
		waitingTime.setDescription("");
		waitingTime.setEnabled(true);
		waitingTime.setRange(0.000, 50.000);
		waitingTime.setLockValueInRange(false);
		waitingTime.addTerm(new Triangle("low", 0.00, 10.0, 20.0));
		waitingTime.addTerm(new Triangle("high", 15.0, 35.0, 40.0));
		waitingTime.addTerm(new Triangle("veryhigh", 38.0, 45.0, 50.0));
		engine.addInputVariable(waitingTime);

		
		InputVariable queueLength = new InputVariable();
		queueLength.setName("queueLength");
		queueLength.setDescription("");
		queueLength.setEnabled(true);
		queueLength.setRange(0.000, 21.000);
		queueLength.setLockValueInRange(false);
		queueLength.addTerm(new Triangle("short", 0.00, 3.0, 6.0));
		queueLength.addTerm(new Triangle("long", 5.0, 7.50, 10.0));
		queueLength.addTerm(new Triangle("verylong", 9.0, 14.50, 20.0));
		engine.addInputVariable(queueLength);


		InputVariable distance = new InputVariable();
		distance.setName("distance");
		distance.setDescription("");
		distance.setEnabled(true);
		distance.setRange(0.000, 100.000);
		distance.setLockValueInRange(false);
		distance.addTerm(new Triangle("near", 0.00, 20.0, 35.0));
		distance.addTerm(new Triangle("far", 25.0, 45.0, 65.0));
		distance.addTerm(new Triangle("veryfar", 55.0, 77.50, 100.0));
		engine.addInputVariable(distance);

		
		InputVariable speed = new InputVariable();
		speed.setName("speed");
		speed.setDescription("");
		speed.setEnabled(true);
		speed.setRange(0.000, 61.000);
		speed.setLockValueInRange(false);
		speed.addTerm(new Triangle("low", 0.00, 20.0, 30.0));
		speed.addTerm(new Triangle("high", 25.0, 40.0, 50.0));
		speed.addTerm(new Triangle("veryhigh", 45.0, 55.0, 60.0));
		engine.addInputVariable(speed);


		OutputVariable phaseDuration = new OutputVariable();
		phaseDuration.setName("phaseDuration");
		phaseDuration.setDescription("");
		phaseDuration.setEnabled(true);
		phaseDuration.setRange(0.000, 26.000);
		phaseDuration.setLockValueInRange(false);
		phaseDuration.setAggregation(new Maximum());
		phaseDuration.setDefuzzifier(new Centroid(200));
		phaseDuration.setDefaultValue(Double.NaN);
		phaseDuration.setLockPreviousValue(false);
		phaseDuration.addTerm(new Triangle("short", 5.00, 7.5, 9.0));
		phaseDuration.addTerm(new Triangle("long", 8.5, 13.0, 16.0));
		phaseDuration.addTerm(new Triangle("verylong", 14.0, 20.0, 25.0));
		engine.addOutputVariable(phaseDuration);


		OutputVariable speedOut = new OutputVariable();
		speedOut.setName("speedOut");
		speedOut.setDescription("");
		speedOut.setEnabled(true);
		speedOut.setRange(0.000, 61.000);
		speedOut.setLockValueInRange(false);
		speedOut.setAggregation(new Maximum());
		speedOut.setDefuzzifier(new Centroid(200));
		speedOut.setDefaultValue(Double.NaN);
		speedOut.setLockPreviousValue(false);
		speedOut.addTerm(new Triangle("low", 0.00, 20.0, 30.0));
		speedOut.addTerm(new Triangle("high", 25.0, 40.0, 50.0));
		speedOut.addTerm(new Triangle("veryhigh", 45.0, 55.0, 60.0));
		engine.addOutputVariable(speedOut);

		RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setName("");
		ruleBlock.setDescription("");
		ruleBlock.setEnabled(true);
		ruleBlock.setConjunction(new Minimum());
		ruleBlock.setDisjunction(new Maximum());
		ruleBlock.setImplication(new Minimum());
		ruleBlock.setActivation(new General());
		
		ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  low and distance  is  near then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  low and distance  is  far then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  low and distance  is  veryfar then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  high and distance  is  near then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  high and distance  is  far then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  high and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  veryhigh and distance  is  near then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  veryhigh and distance  is  far then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  low and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  low and distance  is  near then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  low and distance  is  far then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  low and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  high and distance  is  near then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  high and distance  is  far then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  high and distance  is  veryfar then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  veryhigh and distance  is  near then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  veryhigh and distance  is  far then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  high and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  low and distance  is  near then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  low and distance  is  far then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  low and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  high and distance  is  near then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  high and distance  is  far then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  high and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  near then  phaseDuration  is  short  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  far then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  short and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  veryhigh", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  low and distance  is  near then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  low and distance  is  far then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  low and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  high and distance  is  near then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  high and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  high and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  veryhigh and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  veryhigh and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  low and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  low and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  low and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  low and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  high and distance  is  near then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  high and distance  is  far then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  high and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  veryhigh and distance  is  near then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  veryhigh and distance  is  far then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  high and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  low and distance  is  near then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  low and distance  is  far then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  low and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  high and distance  is  near then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  high and distance  is  far then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  high and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  near then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  far then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  long and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  long  and  speedOut  is  high", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  low and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  low and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  low and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  high and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  high and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  high and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  veryhigh and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  veryhigh and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  low and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  low and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  low and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  low and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  high and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  high and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  high and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  veryhigh and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  veryhigh and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  high and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  low and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  low and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  low and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  high and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  high and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  high and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  near then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  far then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

        ruleBlock.addRule(Rule.parse("if queueLength  is  verylong and waitingTime  is  veryhigh and speed  is  veryhigh and distance  is  veryfar then  phaseDuration  is  verylong  and  speedOut  is  low", engine));

		engine.addRuleBlock(ruleBlock);

		System.out.println("Fuzzy Model Created");

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
		OutputVariable speedOut = engine.getOutputVariable("speedOut");
		
		waitingTime.setValue(wT);
		queueLength.setValue(qL);
		distance.setValue(dS);
		speed.setValue(sP);
		
        engine.process();
        double[] outputs = {phaseDuration.getValue(), speedOut.getValue()};
        System.out.println("FuzzyModel: FUZZY INPUT-OUTPUT is: " + " WT: " + wT + " QL: " + qL + " SPD: " + sP + " DST: " + dS + " PD: " + outputs[0]);
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