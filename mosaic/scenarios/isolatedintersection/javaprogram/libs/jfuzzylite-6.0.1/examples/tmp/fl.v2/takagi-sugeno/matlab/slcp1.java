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

public class slcp1{
public static void main(String[] args){
Engine engine = new Engine();
engine.setName("slcp1");

InputVariable in1 = new InputVariable();
in1.setEnabled(true);
in1.setName("in1");
in1.setRange(-0.300, 0.300);
in1.setLockValueInRange(false);
engine.addInputVariable(in1);

InputVariable in2 = new InputVariable();
in2.setEnabled(true);
in2.setName("in2");
in2.setRange(-1.000, 1.000);
in2.setLockValueInRange(false);
engine.addInputVariable(in2);

InputVariable in3 = new InputVariable();
in3.setEnabled(true);
in3.setName("in3");
in3.setRange(-3.000, 3.000);
in3.setLockValueInRange(false);
engine.addInputVariable(in3);

InputVariable in4 = new InputVariable();
in4.setEnabled(true);
in4.setName("in4");
in4.setRange(-3.000, 3.000);
in4.setLockValueInRange(false);
engine.addInputVariable(in4);

InputVariable in5 = new InputVariable();
in5.setEnabled(true);
in5.setName("in5");
in5.setRange(0.500, 1.500);
in5.setLockValueInRange(false);
in5.addTerm(new Gaussian("small", 0.500, 0.200));
in5.addTerm(new Gaussian("medium", 1.000, 0.200));
in5.addTerm(new Gaussian("large", 1.500, 0.200));
engine.addInputVariable(in5);

OutputVariable out = new OutputVariable();
out.setEnabled(true);
out.setName("out");
out.setRange(-10.000, 10.000);
out.setLockValueInRange(false);
out.fuzzyOutput().setAggregation(null);
out.setDefuzzifier(new WeightedAverage("TakagiSugeno"));
out.setDefaultValue(Double.NaN);
out.setLockPreviousValue(false);
out.addTerm(Linear.create("outmf1", engine, 32.166, 5.835, 3.162, 3.757, 0.000, 0.000));
out.addTerm(Linear.create("outmf2", engine, 39.012, 9.947, 3.162, 4.269, 0.000, 0.000));
out.addTerm(Linear.create("outmf3", engine, 45.009, 13.985, 3.162, 4.666, 0.000, 0.000));
engine.addOutputVariable(out);

RuleBlock ruleBlock = new RuleBlock();
ruleBlock.setEnabled(true);
ruleBlock.setName("");
ruleBlock.setConjunction(null);
ruleBlock.setDisjunction(null);
ruleBlock.setImplication(null);
ruleBlock.setActivation(fl:null);
ruleBlock.addRule(Rule.parse("if in5 is small then out is outmf1", engine));
ruleBlock.addRule(Rule.parse("if in5 is medium then out is outmf2", engine));
ruleBlock.addRule(Rule.parse("if in5 is large then out is outmf3", engine));
engine.addRuleBlock(ruleBlock);


}
}
