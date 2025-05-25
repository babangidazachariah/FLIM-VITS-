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

public class UrgencyBasedPhaseDurationController {
	Engine engine;
	
    public UrgencyBasedPhaseDurationController() {
        // Create a new engine
        engine = new Engine();
        engine.setName("UrgencyBasedPhaseDurationController");
        engine.setDescription("");

        // Input variable 'otherUrgency'
        InputVariable otherUrgency = new InputVariable();
        otherUrgency.setName("otherUrgency");
        otherUrgency.setRange(0.0, 1.0);
        otherUrgency.addTerm(new Triangle("low", -0.416666666666667, 0.0, 0.416666666666667));
        otherUrgency.addTerm(new Triangle("high", 0.0833333333333333, 0.5, 0.916666666666667));
        otherUrgency.addTerm(new Triangle("veryhigh", 0.583333333333333, 1.0, 1.41666666666667));
        engine.addInputVariable(otherUrgency);
      
        // Input variable 'chosenUrgency'
        InputVariable chosenUrgency = new InputVariable();
        chosenUrgency.setName("chosenUrgency");
        chosenUrgency.setRange(0.0, 1.0);
        chosenUrgency.addTerm(new Triangle("low", -0.416666666666667, 0.0, 0.416666666666667));
        chosenUrgency.addTerm(new Triangle("high", 0.0833333333333333, 0.5, 0.916666666666667));
        chosenUrgency.addTerm(new Triangle("veryhigh", 0.583333333333333, 1.0, 1.41666666666667));
        engine.addInputVariable(chosenUrgency);

        
        // Input variable 'chosenQueue'
        InputVariable chosenQueue = new InputVariable();
        chosenQueue.setName("chosenQueue");
        chosenQueue.setRange(0.0, 40.0);
        chosenQueue.addTerm(new Triangle("short", -8.335, 0.0, 8.335));
        chosenQueue.addTerm(new Triangle("long", 1.667, 10.0, 18.34));
        chosenQueue.addTerm(new Triangle("verylong", 11.66, 20.0, 28.34));
        engine.addInputVariable(chosenQueue);

        
        // Output variable 'phaseDuration'
        OutputVariable phaseDuration = new OutputVariable();
        phaseDuration.setName("phaseDuration");
        phaseDuration.setRange(5.0, 30.0);
        phaseDuration.setAggregation(new Maximum());
        phaseDuration.setDefuzzifier(new Centroid(100));
        phaseDuration.setDefaultValue(Double.NaN);
        phaseDuration.addTerm(new Triangle("verysmall", -3.325, 5.0, 10.92));
        phaseDuration.addTerm(new Triangle("small", 7.447, 12.3, 17.47));
        phaseDuration.addTerm(new Triangle("large", 14.43, 20.91, 27.51));
        phaseDuration.addTerm(new Triangle("verylarge", 23.95, 29.9, 38.0));
        engine.addOutputVariable(phaseDuration);

        // Create rule block
        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setName("");
        ruleBlock.setConjunction(new Minimum());
        ruleBlock.setDisjunction(new Maximum());
        ruleBlock.setImplication(new Minimum());
        ruleBlock.setActivation(new General());

        /*
        // Add rules
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is low and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is low and chosenQueue is long then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is low and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is high and chosenQueue is short then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is high and chosenQueue is long then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is high and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is veryhigh and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is veryhigh and chosenQueue is long then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is veryhigh and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is low and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is low and chosenQueue is long then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is low and chosenQueue is verylong then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is high and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is high and chosenQueue is long then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is high and chosenQueue is verylong then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is veryhigh and chosenQueue is short then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is veryhigh and chosenQueue is long then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is veryhigh and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is low and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is low and chosenQueue is long then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is low and chosenQueue is verylong then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is high and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is high and chosenQueue is long then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is high and chosenQueue is verylong then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is veryhigh and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is veryhigh and chosenQueue is long then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is veryhigh and chosenQueue is verylong then phaseDuration is large", engine));

        //*/
        //Another Set of Rules
        /*
         * Four-Ways
         * 200Veh/hr  34.639/660
         * 400Veh/hr  38.077/1310
         * 800Veh/hr  78.552/2583
         */
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is low and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is low and chosenQueue is long then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is low and chosenQueue is verylong then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is high and chosenQueue is short then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is high and chosenQueue is long then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is high and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is veryhigh and chosenQueue is short then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is veryhigh and chosenQueue is long then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is low and chosenUrgency is veryhigh and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is low and chosenQueue is short then phaseDuration is verysmall", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is low and chosenQueue is long then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is low and chosenQueue is verylong then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is high and chosenQueue is short then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is high and chosenQueue is long then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is high and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is veryhigh and chosenQueue is short then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is veryhigh and chosenQueue is long then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is high and chosenUrgency is veryhigh and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is low and chosenQueue is short then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is low and chosenQueue is long then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is low and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is high and chosenQueue is short then phaseDuration is small", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is high and chosenQueue is long then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is high and chosenQueue is verylong then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is veryhigh and chosenQueue is short then phaseDuration is large", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is veryhigh and chosenQueue is long then phaseDuration is verylarge", engine));
        ruleBlock.addRule(Rule.parse("if otherUrgency is veryhigh and chosenUrgency is veryhigh and chosenQueue is verylong then phaseDuration is verylarge", engine));
        
        // Add rule block to engine
        engine.addRuleBlock(ruleBlock);

        
    }
    
    
    public double EvaluateFIS(double oU, double cU, double cQ){
		
		
		StringBuilder status = new StringBuilder();
        if (! engine.isReady(status))
            throw new RuntimeException("[engine error] engine is not ready:\n" + status);
        if (oU > 1) {
        	oU = 0.99;
        }
        if (cU > 1) {
        	cU = 0.99;
        } 
        if (cQ > 20) {
        	cQ = 19.9;
        }
        
        InputVariable otherUrgency = engine.getInputVariable("otherUrgency");
  		InputVariable chosenUrgency = engine.getInputVariable("chosenUrgency");
  		InputVariable chosenQueue = engine.getInputVariable("chosenQueue");
		//InputVariable speed = engine.getInputVariable("speed");
		
  		
        OutputVariable phaseDuration = engine.getOutputVariable("phaseDuration");
		//OutputVariable speedOut = engine.getOutputVariable("speedOut");
		
		otherUrgency.setValue(oU);
		chosenUrgency.setValue(cU);
		chosenQueue.setValue(cQ);
		//speed.setValue(sP);
		
        engine.process();
        double outputs = phaseDuration.getValue();
        System.out.println("UrgencyBasedController: FUZZY INPUT-OUTPUT is: " + " oU: " + oU + " cU: " + cU + " cQ: " + cQ +  " PD: " + outputs);
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
