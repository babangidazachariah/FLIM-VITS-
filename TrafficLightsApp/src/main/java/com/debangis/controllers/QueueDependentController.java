package com.debangis.controllers;

import java.io.File;
import java.io.IOException;

import com.fuzzylite.*;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.Centroid;
import com.fuzzylite.defuzzifier.WeightedAverage;
import com.fuzzylite.hedge.Any;
import com.fuzzylite.imex.FisExporter;
import com.fuzzylite.norm.s.*;
import com.fuzzylite.norm.t.*;
import com.fuzzylite.rule.*;
import com.fuzzylite.term.*;
import com.fuzzylite.variable.*;

public class QueueDependentController {
	Engine engine;
    public QueueDependentController() {
    	System.out.println("QueueDependentController: FUZZY Controller");
        // Create a new engine
        engine = new Engine();
        engine.setName("QueueDependentController");
        engine.setDescription("");

        // Input variable 'queueLength'
        InputVariable queueLength = new InputVariable();
        queueLength.setName("queueLength");
        queueLength.setRange(0.0, 20.0);
        queueLength.addTerm(new Bell("short", -0.05291, 4.166, 2.5));
        queueLength.addTerm(new Bell("long", 10.0, 4.166, 2.5));
        queueLength.addTerm(new Bell("verylong", 20.0, 4.166, 2.5));
        engine.addInputVariable(queueLength);

        // Input variable 'waitingTime'
        InputVariable waitingTime = new InputVariable();
        waitingTime.setName("waitingTime");
        waitingTime.setRange(0.0, 50.0);
        waitingTime.addTerm(new Bell("small", -6.94e-16, 10.42, 2.5));
        waitingTime.addTerm(new Bell("large", 25.0, 10.42, 2.5));
        waitingTime.addTerm(new Bell("verylarge", 50.0, 10.42, 2.5));
        engine.addInputVariable(waitingTime);

        // Output variable 'phaseDuration'
        OutputVariable phaseDuration = new OutputVariable();
        phaseDuration.setName("phaseDuration");
        phaseDuration.setRange(5.0, 30.0);
        phaseDuration.setAggregation(new Maximum());
        phaseDuration.setDefuzzifier(new Centroid(200));
        phaseDuration.setDefaultValue(Double.NaN);
        
        phaseDuration.setLockValueInRange(false);
        phaseDuration.addTerm(new Bell("small",  5.0,  4.0, 2.5));
        phaseDuration.addTerm(new Bell("large", 20.0, 4.999, 2.5));
        phaseDuration.addTerm(new Bell("verylarge", 30.0, 4.999, 2.5));
        engine.addOutputVariable(phaseDuration);

        // Create rule block
        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setName("");
        ruleBlock.setConjunction(new Minimum());
        ruleBlock.setDisjunction(new Maximum());
        ruleBlock.setImplication(new Minimum());
        ruleBlock.setActivation(new General());

        // Add rules
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is small then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is large then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is verylarge then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is small then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is large then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is verylarge then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is small then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is large then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is verylarge then phaseDuration is verylarge", engine));

        // Add rule block to engine
        engine.addRuleBlock(ruleBlock);

        // Validate the engine
       // engine.configure("AlgebraicProduct", "AlgebraicSum", "AlgebraicProduct", "UnboundedSum", "WeightedAverage", "General");
    }
    

    public double[] EvaluateFIS(double wT, double qL, double sP, double dS){
		//wT: waiting Time; qL: Queue Length; sP: speed; dS: Distance
		
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
  		//InputVariable distance = engine.getInputVariable("distance");
		//InputVariable speed = engine.getInputVariable("speed");
		
		
        OutputVariable phaseDuration = engine.getOutputVariable("phaseDuration");
		//OutputVariable speedOut = engine.getOutputVariable("speedOut");
		
		waitingTime.setValue(wT);
		queueLength.setValue(qL);
		//distance.setValue(dS);
		//speed.setValue(sP);
		
        engine.process();
        double[] outputs = {phaseDuration.getValue()};
        //System.out.println("QueueDependentController: FUZZY INPUT-OUTPUT is: " + " WT: " + wT + " QL: " + qL + " SPD: " + sP + " DST: " + dS + " PD: " + outputs[0]);
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
