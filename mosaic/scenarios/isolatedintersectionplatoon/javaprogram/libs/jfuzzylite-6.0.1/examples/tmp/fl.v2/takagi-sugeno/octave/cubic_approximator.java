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

public class cubic_approximator{
public static void main(String[] args){
Engine engine = new Engine();
engine.setName("Cubic-Approximator");

InputVariable X = new InputVariable();
X.setEnabled(true);
X.setName("X");
X.setRange(-5.000, 5.000);
X.setLockValueInRange(false);
X.addTerm(new Triangle("AboutNegFive", -6.000, -5.000, -4.000));
X.addTerm(new Triangle("AboutNegFour", -5.000, -4.000, -3.000));
X.addTerm(new Triangle("AboutNegThree", -4.000, -3.000, -2.000));
X.addTerm(new Triangle("AboutNegTwo", -3.000, -2.000, -1.000));
X.addTerm(new Triangle("AboutNegOne", -2.000, -1.000, 0.000));
X.addTerm(new Triangle("AboutZero", -1.000, 0.000, 1.000));
X.addTerm(new Triangle("AboutOne", 0.000, 1.000, 2.000));
X.addTerm(new Triangle("AboutTwo", 1.000, 2.000, 3.000));
X.addTerm(new Triangle("AboutThree", 2.000, 3.000, 4.000));
X.addTerm(new Triangle("AboutFour", 3.000, 4.000, 5.000));
X.addTerm(new Triangle("AboutFive", 4.000, 5.000, 6.000));
engine.addInputVariable(X);

OutputVariable ApproxXCubed = new OutputVariable();
ApproxXCubed.setEnabled(true);
ApproxXCubed.setName("ApproxXCubed");
ApproxXCubed.setRange(-5.000, 5.000);
ApproxXCubed.setLockValueInRange(false);
ApproxXCubed.fuzzyOutput().setAggregation(null);
ApproxXCubed.setDefuzzifier(new WeightedAverage("TakagiSugeno"));
ApproxXCubed.setDefaultValue(Double.NaN);
ApproxXCubed.setLockPreviousValue(false);
ApproxXCubed.addTerm(Linear.create("TangentatNegFive", engine, 75.000, 250.000));
ApproxXCubed.addTerm(Linear.create("TangentatNegFour", engine, 48.000, 128.000));
ApproxXCubed.addTerm(Linear.create("TangentatNegThree", engine, 27.000, 54.000));
ApproxXCubed.addTerm(Linear.create("TangentatNegTwo", engine, 12.000, 16.000));
ApproxXCubed.addTerm(Linear.create("TangentatNegOne", engine, 3.000, 2.000));
ApproxXCubed.addTerm(Linear.create("TangentatZero", engine, 0.000, 0.000));
ApproxXCubed.addTerm(Linear.create("TangentatOne", engine, 3.000, -2.000));
ApproxXCubed.addTerm(Linear.create("TangentatTwo", engine, 12.000, -16.000));
ApproxXCubed.addTerm(Linear.create("TangentatThree", engine, 27.000, -54.000));
ApproxXCubed.addTerm(Linear.create("TangentatFour", engine, 48.000, -128.000));
ApproxXCubed.addTerm(Linear.create("TangentatFive", engine, 75.000, -250.000));
engine.addOutputVariable(ApproxXCubed);

RuleBlock ruleBlock = new RuleBlock();
ruleBlock.setEnabled(true);
ruleBlock.setName("");
ruleBlock.setConjunction(null);
ruleBlock.setDisjunction(null);
ruleBlock.setImplication(null);
ruleBlock.setActivation(fl:null);
ruleBlock.addRule(Rule.parse("if X is AboutNegFive then ApproxXCubed is TangentatNegFive", engine));
ruleBlock.addRule(Rule.parse("if X is AboutNegFour then ApproxXCubed is TangentatNegFour", engine));
ruleBlock.addRule(Rule.parse("if X is AboutNegThree then ApproxXCubed is TangentatNegThree", engine));
ruleBlock.addRule(Rule.parse("if X is AboutNegTwo then ApproxXCubed is TangentatNegTwo", engine));
ruleBlock.addRule(Rule.parse("if X is AboutNegOne then ApproxXCubed is TangentatNegOne", engine));
ruleBlock.addRule(Rule.parse("if X is AboutZero then ApproxXCubed is TangentatZero", engine));
ruleBlock.addRule(Rule.parse("if X is AboutOne then ApproxXCubed is TangentatOne", engine));
ruleBlock.addRule(Rule.parse("if X is AboutTwo then ApproxXCubed is TangentatTwo", engine));
ruleBlock.addRule(Rule.parse("if X is AboutThree then ApproxXCubed is TangentatThree", engine));
ruleBlock.addRule(Rule.parse("if X is AboutFour then ApproxXCubed is TangentatFour", engine));
ruleBlock.addRule(Rule.parse("if X is AboutFive then ApproxXCubed is TangentatFive", engine));
engine.addRuleBlock(ruleBlock);


}
}
