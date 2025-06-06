import com.fuzzylite.*;
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

public class tippersg{
public static void main(String[] args){
Engine engine = new Engine();
engine.setName("sugeno tipper");

InputVariable service = new InputVariable();
service.setEnabled(true);
service.setName("service");
service.setRange(0.000, 10.000);
service.setLockValueInRange(false);
service.addTerm(new Gaussian("poor", 0.000, 1.500));
service.addTerm(new Gaussian("average", 5.000, 1.500));
service.addTerm(new Gaussian("good", 10.000, 1.500));
engine.addInputVariable(service);

InputVariable food = new InputVariable();
food.setEnabled(true);
food.setName("food");
food.setRange(0.000, 10.000);
food.setLockValueInRange(false);
food.addTerm(new Trapezoid("rancid", -5.000, 0.000, 1.000, 3.000));
food.addTerm(new Trapezoid("delicious", 7.000, 9.000, 10.000, 15.000));
engine.addInputVariable(food);

OutputVariable tip = new OutputVariable();
tip.setEnabled(true);
tip.setName("tip");
tip.setRange(-30.000, 30.000);
tip.setLockValueInRange(false);
tip.fuzzyOutput().setAggregation(null);
tip.setDefuzzifier(new WeightedAverage("TakagiSugeno"));
tip.setDefaultValue(Double.NaN);
tip.setLockPreviousValue(false);
tip.addTerm(Linear.create("cheap", engine, 0.000, 0.000, 5.000));
tip.addTerm(Linear.create("average", engine, 0.000, 0.000, 15.000));
tip.addTerm(Linear.create("generous", engine, 0.000, 0.000, 25.000));
engine.addOutputVariable(tip);

RuleBlock ruleBlock = new RuleBlock();
ruleBlock.setEnabled(true);
ruleBlock.setName("");
ruleBlock.setConjunction(null);
ruleBlock.setDisjunction(new Maximum());
ruleBlock.setImplication(null);
ruleBlock.setActivation(fl:null);
ruleBlock.addRule(Rule.parse("if service is poor or food is rancid then tip is cheap", engine));
ruleBlock.addRule(Rule.parse("if service is average then tip is average", engine));
ruleBlock.addRule(Rule.parse("if service is good or food is delicious then tip is generous", engine));
engine.addRuleBlock(ruleBlock);


}
}
