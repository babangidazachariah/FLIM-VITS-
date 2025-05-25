package com.debangis.controllers;

import java.io.File;
import java.io.IOException;

import com.fuzzylite.*;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.Centroid;
import com.fuzzylite.imex.FisExporter;
import com.fuzzylite.norm.s.*;
import com.fuzzylite.norm.t.*;
import com.fuzzylite.rule.*;
import com.fuzzylite.term.*;
import com.fuzzylite.variable.*;

public class UrgencyController {
	Engine engine;
    public UrgencyController() {
        // Create a new engine
        engine = new Engine();
        engine.setName("UrgencyController");
        engine.setDescription("");

        // Input variable 'queueLength'
        InputVariable queueLength = new InputVariable();
        queueLength.setName("queueLength");
        queueLength.setRange(0.0, 20.0);
        queueLength.addTerm(new Triangle("short", -8.335, 0.0, 8.335));
        queueLength.addTerm(new Triangle("long", 1.667, 10.0, 18.34));
        queueLength.addTerm(new Triangle("verylong", 11.66, 20.0, 28.34));
        engine.addInputVariable(queueLength);

        // Input variable 'waitingTime'
        InputVariable waitingTime = new InputVariable();
        waitingTime.setName("waitingTime");
        waitingTime.setRange(0.0, 50.0);
        waitingTime.addTerm(new Triangle("small", -20.83, 0.0, 20.83));
        waitingTime.addTerm(new Triangle("large", 4.167, 25.0, 45.83));
        waitingTime.addTerm(new Triangle("verylarge", 29.17, 50.0, 70.83));
        engine.addInputVariable(waitingTime);

        // Output variable 'urgency'
        OutputVariable urgency = new OutputVariable();
        urgency.setName("urgency");
        urgency.setRange(0.0, 1.0);
        urgency.setAggregation(new Maximum());
        urgency.setDefuzzifier(new Centroid(100));
        urgency.setDefaultValue(Double.NaN);
       
        urgency.addTerm(new Triangle("low", -0.416666666666667, 0.0, 0.416666666666667));
        urgency.addTerm(new Triangle("high", 0.130949047619048, 0.547619047619048, 0.964319047619048));
        urgency.addTerm(new Triangle("veryhigh", 0.5833, 1.0, 1.417));
        engine.addOutputVariable(urgency);

        // Create rule block
        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setName("");
        ruleBlock.setConjunction(new Minimum());
        ruleBlock.setDisjunction(new Maximum());
        ruleBlock.setImplication(new Minimum());
        ruleBlock.setActivation(new General());

        // Add rules
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is small then urgency is low", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is large then urgency is high", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is short and waitingTime is verylarge then urgency is high", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is small then urgency is high", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is large then urgency is high", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is long and waitingTime is verylarge then urgency is veryhigh", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is small then urgency is veryhigh", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is large then urgency is veryhigh", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is verylong and waitingTime is verylarge then urgency is veryhigh", engine));

        // Add rule block to engine
        engine.addRuleBlock(ruleBlock);

        
    }
    
    
    public double EvaluateFIS(double wT, double qL){
		//wT: waiting Time; qL: Queue Length
		
		StringBuilder status = new StringBuilder();
        if (! engine.isReady(status))
            throw new RuntimeException("[engine error] engine is not ready:\n" + status);
        if (wT > 50) {
        	wT = 49.9;
        }
        if (qL > 20) {
        	qL = 19.9;
        } 
        
        InputVariable waitingTime = engine.getInputVariable("waitingTime");
  		InputVariable queueLength = engine.getInputVariable("queueLength");
  		//InputVariable distance = engine.getInputVariable("distance");
		//InputVariable speed = engine.getInputVariable("speed");
		
		
        OutputVariable urgency = engine.getOutputVariable("urgency");
		//OutputVariable speedOut = engine.getOutputVariable("speedOut");
		
		waitingTime.setValue(wT);
		queueLength.setValue(qL);
		//distance.setValue(dS);
		//speed.setValue(sP);
		
        engine.process();
        double outputs = urgency.getValue();
        //System.out.println("UrgencyController: FUZZY INPUT-OUTPUT is: " + " WT: " + wT + " QL: " + qL + " Urgency: " + outputs);
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
