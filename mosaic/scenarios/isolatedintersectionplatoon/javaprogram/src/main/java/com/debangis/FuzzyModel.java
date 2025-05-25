package com.debangis;

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
import com.fuzzylite.Engine.*;
/*
import com.fuzzylite.term.*;
import com.fuzzylite.variable.*;

import com.fuzzylite.factory.FactoryManager;
import com.fuzzylite.imex.FllExporter;
*/
public class FuzzyModel{
	Engine engine;
	public FuzzyModel(){
		
		engine = new Engine();
        engine.setName("ObstacleAvoidance");
        engine.setDescription("");

        InputVariable obstacle = new InputVariable();
        obstacle.setName("obstacle");
        obstacle.setDescription("");
        obstacle.setEnabled(true);
        obstacle.setRange(0.000, 1.000);
        obstacle.setLockValueInRange(false);
        obstacle.addTerm(new Ramp("left", 1.000, 0.000));
        obstacle.addTerm(new Ramp("right", 0.000, 1.000));
        engine.addInputVariable(obstacle);

        OutputVariable mSteer = new OutputVariable();
        mSteer.setName("mSteer");
        mSteer.setDescription("");
        mSteer.setEnabled(true);
        mSteer.setRange(0.000, 1.000);
        mSteer.setLockValueInRange(false);
        mSteer.setAggregation(new Maximum());
        mSteer.setDefuzzifier(new Centroid(100));
        mSteer.setDefaultValue(Double.NaN);
        mSteer.setLockPreviousValue(false);
        mSteer.addTerm(new Ramp("left", 1.000, 0.000));
        mSteer.addTerm(new Ramp("right", 0.000, 1.000));
        engine.addOutputVariable(mSteer);

        RuleBlock mamdani = new RuleBlock();
        mamdani.setName("mamdani");
        mamdani.setDescription("");
        mamdani.setEnabled(true);
        mamdani.setConjunction(null);
        mamdani.setDisjunction(null);
        mamdani.setImplication(new AlgebraicProduct());
        mamdani.setActivation(new General());
        mamdani.addRule(Rule.parse("if obstacle is left then mSteer is right", engine));
        mamdani.addRule(Rule.parse("if obstacle is right then mSteer is left", engine));
        engine.addRuleBlock(mamdani);

	}
	
	public String EvaluateFIS(double obst){
		
		StringBuilder status = new StringBuilder();
        if (! engine.isReady(status))
            throw new RuntimeException("[engine error] engine is not ready:\n" + status);

        InputVariable obstacle = engine.getInputVariable("obstacle");
        OutputVariable steer = engine.getOutputVariable("mSteer");
		obstacle.setValue(obst);
        engine.process();
		return Op.str(steer.getValue());
	}
}