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

public class tank2{
public static void main(String[] args){
Engine engine = new Engine();
engine.setName("tank");

InputVariable level = new InputVariable();
level.setEnabled(true);
level.setName("level");
level.setRange(-1.000, 1.000);
level.setLockValueInRange(false);
level.addTerm(new Trapezoid("high", -2.000, -1.000, -0.800, -0.001));
level.addTerm(new Triangle("good", -0.150, 0.000, 0.500));
level.addTerm(new Trapezoid("low", 0.001, 0.800, 1.000, 1.500));
engine.addInputVariable(level);

InputVariable change = new InputVariable();
change.setEnabled(true);
change.setName("change");
change.setRange(-0.100, 0.100);
change.setLockValueInRange(false);
change.addTerm(new Trapezoid("falling", -0.140, -0.100, -0.060, 0.000));
change.addTerm(new Trapezoid("rising", -0.001, 0.060, 0.100, 0.140));
engine.addInputVariable(change);

OutputVariable valve = new OutputVariable();
valve.setEnabled(true);
valve.setName("valve");
valve.setRange(-1.000, 1.000);
valve.setLockValueInRange(false);
valve.fuzzyOutput().setAggregation(new Maximum());
valve.setDefuzzifier(new Centroid(200));
valve.setDefaultValue(Double.NaN);
valve.setLockPreviousValue(false);
valve.addTerm(new Triangle("close_fast", -1.000, -0.900, -0.800));
valve.addTerm(new Triangle("close_slow", -0.600, -0.500, -0.400));
valve.addTerm(new Triangle("no_change", -0.100, 0.000, 0.100));
valve.addTerm(new Triangle("open_slow", 0.400, 0.500, 0.600));
valve.addTerm(new Triangle("open_fast", 0.800, 0.900, 1.000));
engine.addOutputVariable(valve);

RuleBlock ruleBlock = new RuleBlock();
ruleBlock.setEnabled(true);
ruleBlock.setName("");
ruleBlock.setConjunction(new AlgebraicProduct());
ruleBlock.setDisjunction(new AlgebraicSum());
ruleBlock.setImplication(new AlgebraicProduct());
ruleBlock.setActivation(fl:null);
ruleBlock.addRule(Rule.parse("if level is low then valve is open_fast", engine));
ruleBlock.addRule(Rule.parse("if level is high then valve is close_fast", engine));
ruleBlock.addRule(Rule.parse("if level is good and change is rising then valve is close_slow", engine));
ruleBlock.addRule(Rule.parse("if level is good and change is falling then valve is open_slow", engine));
engine.addRuleBlock(ruleBlock);


}
}
