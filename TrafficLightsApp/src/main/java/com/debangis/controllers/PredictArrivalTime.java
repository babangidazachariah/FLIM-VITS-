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
import com.fuzzylite.term.Bell;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;

public class PredictArrivalTime {
	Engine engine;
    public PredictArrivalTime() {
    	System.out.println("Predict Arrivat Time: FUZZY Controller");
        // Create a new engine
        engine = new Engine();
        engine.setName("PredictArrivalTime");
        engine.setDescription("");

        // Input variable 'thisDensity'
        InputVariable thisDensity = new InputVariable();
        thisDensity.setName("thisDensity");
        thisDensity.setRange(0.0, 100.0);
        thisDensity.addTerm(new Bell("low", 4.441e-16, 20.83, 2.5));
        thisDensity.addTerm(new Bell("high", 50, 20.83, 2.5));
        thisDensity.addTerm(new Bell("veryhigh", 100, 20.83, 2.5));
        engine.addInputVariable(thisDensity);

        // Input variable 'otherDensity'
        InputVariable otherDensity = new InputVariable();
        otherDensity.setName("otherDensity");
        otherDensity.setRange(0.0, 100.0);
        otherDensity.addTerm(new Bell("low", 4.441e-16, 20.83, 2.5));
        otherDensity.addTerm(new Bell("high", 50, 20.83, 2.5));
        otherDensity.addTerm(new Bell("veryhigh", 100, 20.83, 2.5));
        engine.addInputVariable(otherDensity);

        // Output variable 'arrivalTime'
        OutputVariable arrivalTime = new OutputVariable();
        arrivalTime.setName("arrivalTime");
        arrivalTime.setRange(70.0, 200.0);
        arrivalTime.setAggregation(new Maximum());
        arrivalTime.setDefuzzifier(new Centroid(200));
        arrivalTime.setDefaultValue(Double.NaN);
        
        arrivalTime.setLockValueInRange(false);
        arrivalTime.addTerm(new Bell("early",  70,  25, 2.5));
        arrivalTime.addTerm(new Bell("later", 140.0, 25, 2.5));
        arrivalTime.addTerm(new Bell("muchlater", 200.0, 25, 2.5));
        engine.addOutputVariable(arrivalTime);

        // Create rule block
        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setName("");
        ruleBlock.setConjunction(new Minimum());
        ruleBlock.setDisjunction(new Maximum());
        ruleBlock.setImplication(new Minimum());
        ruleBlock.setActivation(new General());

        // Add rules
        ruleBlock.addRule(Rule.parse("if thisDensity is low and otherDensity is low then arrivalTime is early", engine));
        ruleBlock.addRule(Rule.parse("if thisDensity is low and otherDensity is high then arrivalTime is early", engine));
        ruleBlock.addRule(Rule.parse("if thisDensity is low and otherDensity is veryhigh then arrivalTime is later", engine));
        ruleBlock.addRule(Rule.parse("if thisDensity is high and otherDensity is low then arrivalTime is early", engine));
        ruleBlock.addRule(Rule.parse("if thisDensity is high and otherDensity is high then arrivalTime is muchlater", engine));
        ruleBlock.addRule(Rule.parse("if thisDensity is high and otherDensity is veryhigh then arrivalTime is muchlater", engine));
        ruleBlock.addRule(Rule.parse("if thisDensity is veryhigh and otherDensity is low then arrivalTime is muchlater", engine));
        ruleBlock.addRule(Rule.parse("if thisDensity is veryhigh and otherDensity is high then arrivalTime is muchlater", engine));
        ruleBlock.addRule(Rule.parse("if thisDensity is veryhigh and otherDensity is veryhigh then arrivalTime is muchlater", engine));

        // Add rule block to engine
        engine.addRuleBlock(ruleBlock);

        // Validate the engine
       // engine.configure("AlgebraicProduct", "AlgebraicSum", "AlgebraicProduct", "UnboundedSum", "WeightedAverage", "General");
    }
    

    public double[] EvaluateFIS(double oD, double tD, double sP, double dS){
		//oD: waiting Time; tD: Queue Length; sP: speed; dS: Distance
		
		StringBuilder status = new StringBuilder();
        if (! engine.isReady(status))
            throw new RuntimeException("[engine error] engine is not ready:\n" + status);
        if (oD > 50) {
        	oD = 49.9;
        }
        if (tD > 50) {
        	tD = 49.9;
        } 
        
        InputVariable otherDensity = engine.getInputVariable("otherDensity");
  		InputVariable thisDensity = engine.getInputVariable("thisDensity");
  		
		
        OutputVariable arrivalTime = engine.getOutputVariable("arrivalTime");
		
		
		otherDensity.setValue(oD);
		thisDensity.setValue(tD);
		
		
        engine.process();
        double[] outputs = {arrivalTime.getValue()};
        System.out.println("PredictArrivalTime: FUZZY INPUT-OUTPUT is: " + " otherDensity: " + oD + " thisDensity: " + tD + " ArrivalTime: " + outputs[0]);
		return  outputs; 
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

