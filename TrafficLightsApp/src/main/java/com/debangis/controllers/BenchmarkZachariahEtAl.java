package com.debangis.controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fuzzylite.Engine;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.WeightedAverage;
import com.fuzzylite.imex.FisExporter;
import com.fuzzylite.norm.s.AlgebraicSum;
import com.fuzzylite.norm.s.Maximum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Gaussian;
import com.fuzzylite.term.Linear;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;

public class BenchmarkZachariahEtAl {

	Engine engine;
	public BenchmarkZachariahEtAl() {
		// TODO Auto-generated constructor stub
		engine = new Engine();
        engine.setName("SugenoANFISModel");

        // Input variables
        InputVariable queueLength = new InputVariable();
        queueLength.setName("queueLength");
        queueLength.setRange(0.0, 90.0);
        queueLength.setEnabled(true);
        queueLength.setLockValueInRange(false);
        //queueLength.addTerm(new Gaussian("in1cluster" + i, 9.1289336596165, 65.0492069190488));

        queueLength.addTerm(new Gaussian("in1cluster1", 65.0492069190488, 9.1289336596165));
        queueLength.addTerm(new Gaussian("in1cluster2", 52.9150926589399, 9.79899020164522));
        queueLength.addTerm(new Gaussian("in1cluster3", 73.9358317648074, 11.3109813005632));
        queueLength.addTerm(new Gaussian("in1cluster4", 40.7807174361163, 8.76650073078863));
        queueLength.addTerm(new Gaussian("in1cluster5", 14.2616776868806, 14.492028764579));
        queueLength.addTerm(new Gaussian("in1cluster6", 22.1617854833444, 11.4734677620776));
        queueLength.addTerm(new Gaussian("in1cluster7", 52.8562351933017, 6.57092003171146));
        queueLength.addTerm(new Gaussian("in1cluster8", 75.1361293765419, 15.9824535419625));
        queueLength.addTerm(new Gaussian("in1cluster9", 32.701230017755, 7.69380367990321));
        queueLength.addTerm(new Gaussian("in1cluster10", 25.9003332289869, 15.1419646759065));
        
        engine.addInputVariable(queueLength);

        InputVariable waitingTime = new InputVariable();
        waitingTime.setName("waitingTime");
        waitingTime.setRange(0.0, 90.0);
        waitingTime.setEnabled(true);
        waitingTime.setLockValueInRange(false);
        //for (int i = 1; i <= 10; i++) {
            //waitingTime.addTerm(new Gaussian("in2cluster" + i, 7.16777393516067, 77.3279025510082));
            
        waitingTime.addTerm(new Gaussian("in2cluster1", 77.3279025510082, 7.16777393516067));
        waitingTime.addTerm(new Gaussian("in2cluster2", 36.5418261410069, 10.8315302295641));
        waitingTime.addTerm(new Gaussian("in2cluster3", 13.124164033569, 11.6414347367915));
        waitingTime.addTerm(new Gaussian("in2cluster4", 11.9602162482646, 15.2193105463358));
        waitingTime.addTerm(new Gaussian("in2cluster5", 37.3424901070943, 7.63150208336307));
        waitingTime.addTerm(new Gaussian("in2cluster6", -3.64259332569715, 11.0714212049267));
        waitingTime.addTerm(new Gaussian("in2cluster7", 74.5234112546107, 6.43949094129638));
        waitingTime.addTerm(new Gaussian("in2cluster8", 51.6899602915834, 8.54203982030102));
        waitingTime.addTerm(new Gaussian("in2cluster9", 54.4049858876924, 11.5768217126296));
        waitingTime.addTerm(new Gaussian("in2cluster10", 71.8776436231643, 8.23710675277065));
        //}
        engine.addInputVariable(waitingTime);

        // Output variable
        OutputVariable phaseDuration = new OutputVariable();
        phaseDuration.setName("phaseDuration");
        phaseDuration.setRange(13.67256334, 46.33397116);
        phaseDuration.setLockValueInRange(false);
		phaseDuration.setAggregation(new Maximum());
		phaseDuration.setDefuzzifier(new WeightedAverage("TakagiSugeno"));
		phaseDuration.setDefaultValue(Double.NaN);
		phaseDuration.setLockPreviousValue(false);
		phaseDuration.addTerm(Linear.create("out1cluster1", engine, 0.278491652436372, 0.202926778780306, 4.06808556073205));
        
		phaseDuration.addTerm(Linear.create("out1cluster2",engine, -0.489290050617491, -0.505725140168499, 75.2089976044829));
		phaseDuration.addTerm(Linear.create("out1cluster3", engine, -0.0386896612985777, -0.0170233924958309, 33.2712584528143));
		phaseDuration.addTerm(Linear.create("out1cluster4", engine, 0.0611493450396454, -0.0700873001451106, 16.3034767236954));
		phaseDuration.addTerm(Linear.create("out1cluster5", engine, -0.0520598526410543, 0.0227995669187938, 19.6537521777371));
		phaseDuration.addTerm(Linear.create("out1cluster6", engine, 0.284844114464051, 0.220478525120484, 12.6008611765317));
		phaseDuration.addTerm(Linear.create("out1cluster7", engine, 0.528640940220564, -0.0202098222440992, 18.214563325084));
		phaseDuration.addTerm(Linear.create("out1cluster8", engine, -0.0609362958092933, 0.00584499677959474, 44.9625327355151));
		phaseDuration.addTerm(Linear.create("out1cluster9", engine, -0.18371969338847, -0.266748555261797, 51.1245724592398));
		phaseDuration.addTerm(Linear.create("out1cluster10", engine, 0.0224041441459823, -0.0174893755281416, 31.2118378961783));
             
        engine.addOutputVariable(phaseDuration);

     // Rules
        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setName("ruleBlock");
        ruleBlock.setEnabled(true);
        ruleBlock.setConjunction(new AlgebraicProduct());
        ruleBlock.setDisjunction(new AlgebraicSum());
        ruleBlock.setActivation(new General());
        ruleBlock.setImplication(new AlgebraicProduct());
        
        // Rules
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster1 and waitingTime is in2cluster1 then phaseDuration is out1cluster1", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster2 and waitingTime is in2cluster2 then phaseDuration is out1cluster2", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster3 and waitingTime is in2cluster3 then phaseDuration is out1cluster3", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster4 and waitingTime is in2cluster4 then phaseDuration is out1cluster4", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster5 and waitingTime is in2cluster5 then phaseDuration is out1cluster5", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster6 and waitingTime is in2cluster6 then phaseDuration is out1cluster6", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster7 and waitingTime is in2cluster7 then phaseDuration is out1cluster7", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster8 and waitingTime is in2cluster8 then phaseDuration is out1cluster8", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster9 and waitingTime is in2cluster9 then phaseDuration is out1cluster9", engine));
        ruleBlock.addRule(Rule.parse("if queueLength is in1cluster10 and waitingTime is in2cluster10 then phaseDuration is out1cluster10", engine));

        engine.addRuleBlock(ruleBlock);
        // Evaluate
        //engine.configure(new AlgebraicProduct(), new AlgebraicSum(), new AlgebraicProduct(), new AlgebraicSum(), null, null);
        //engine.configure("Minimum", "Maximum", "Minimum", "Maximum", "WeightedAverage", "General");
        engine.configure("AlgebraicProduct", "AlgebraicSum", "AlgebraicProduct", "UnboundedSum", "WeightedAverage", "General");
	
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
        System.out.println("BenchmarkZachariah: FUZZY INPUT-OUTPUT is: " + " WT: " + wT + " QL: " + qL + " SPD: " + sP + " DST: " + dS + " PD: " + outputs[0]);
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
