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

public class sugeno_tip_calculator{
public static void main(String[] args){
Engine engine = new Engine();
engine.setName("Sugeno-Tip-Calculator");

InputVariable FoodQuality = new InputVariable();
FoodQuality.setEnabled(true);
FoodQuality.setName("FoodQuality");
FoodQuality.setRange(1.000, 10.000);
FoodQuality.setLockValueInRange(false);
FoodQuality.addTerm(new Trapezoid("Bad", 0.000, 1.000, 3.000, 7.000));
FoodQuality.addTerm(new Trapezoid("Good", 3.000, 7.000, 10.000, 11.000));
engine.addInputVariable(FoodQuality);

InputVariable Service = new InputVariable();
Service.setEnabled(true);
Service.setName("Service");
Service.setRange(1.000, 10.000);
Service.setLockValueInRange(false);
Service.addTerm(new Trapezoid("Bad", 0.000, 1.000, 3.000, 7.000));
Service.addTerm(new Trapezoid("Good", 3.000, 7.000, 10.000, 11.000));
engine.addInputVariable(Service);

OutputVariable CheapTip = new OutputVariable();
CheapTip.setEnabled(true);
CheapTip.setName("CheapTip");
CheapTip.setRange(5.000, 25.000);
CheapTip.setLockValueInRange(false);
CheapTip.fuzzyOutput().setAggregation(null);
CheapTip.setDefuzzifier(new WeightedAverage("TakagiSugeno"));
CheapTip.setDefaultValue(Double.NaN);
CheapTip.setLockPreviousValue(false);
CheapTip.addTerm(new Constant("Low", 10.000));
CheapTip.addTerm(new Constant("Medium", 15.000));
CheapTip.addTerm(new Constant("High", 20.000));
engine.addOutputVariable(CheapTip);

OutputVariable AverageTip = new OutputVariable();
AverageTip.setEnabled(true);
AverageTip.setName("AverageTip");
AverageTip.setRange(5.000, 25.000);
AverageTip.setLockValueInRange(false);
AverageTip.fuzzyOutput().setAggregation(null);
AverageTip.setDefuzzifier(new WeightedAverage("TakagiSugeno"));
AverageTip.setDefaultValue(Double.NaN);
AverageTip.setLockPreviousValue(false);
AverageTip.addTerm(new Constant("Low", 10.000));
AverageTip.addTerm(new Constant("Medium", 15.000));
AverageTip.addTerm(new Constant("High", 20.000));
engine.addOutputVariable(AverageTip);

OutputVariable GenerousTip = new OutputVariable();
GenerousTip.setEnabled(true);
GenerousTip.setName("GenerousTip");
GenerousTip.setRange(5.000, 25.000);
GenerousTip.setLockValueInRange(false);
GenerousTip.fuzzyOutput().setAggregation(null);
GenerousTip.setDefuzzifier(new WeightedAverage("TakagiSugeno"));
GenerousTip.setDefaultValue(Double.NaN);
GenerousTip.setLockPreviousValue(false);
GenerousTip.addTerm(new Constant("Low", 10.000));
GenerousTip.addTerm(new Constant("Medium", 15.000));
GenerousTip.addTerm(new Constant("High", 20.000));
engine.addOutputVariable(GenerousTip);

RuleBlock ruleBlock = new RuleBlock();
ruleBlock.setEnabled(true);
ruleBlock.setName("");
ruleBlock.setConjunction(new EinsteinProduct());
ruleBlock.setDisjunction(null);
ruleBlock.setImplication(null);
ruleBlock.setActivation(fl:null);
ruleBlock.addRule(Rule.parse("if FoodQuality is extremely Bad and Service is extremely Bad then CheapTip is extremely Low and AverageTip is very Low and GenerousTip is Low", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is Good and Service is extremely Bad then CheapTip is Low and AverageTip is Low and GenerousTip is Medium", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is very Good and Service is very Bad then CheapTip is Low and AverageTip is Medium and GenerousTip is High", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is Bad and Service is Bad then CheapTip is Low and AverageTip is Low and GenerousTip is Medium", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is Good and Service is Bad then CheapTip is Low and AverageTip is Medium and GenerousTip is High", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is extremely Good and Service is Bad then CheapTip is Low and AverageTip is Medium and GenerousTip is very High", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is Bad and Service is Good then CheapTip is Low and AverageTip is Medium and GenerousTip is High", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is Good and Service is Good then CheapTip is Medium and AverageTip is Medium and GenerousTip is very High", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is very Bad and Service is very Good then CheapTip is Low and AverageTip is Medium and GenerousTip is High", engine));
ruleBlock.addRule(Rule.parse("if FoodQuality is very very Good and Service is very very Good then CheapTip is High and AverageTip is very High and GenerousTip is extremely High", engine));
engine.addRuleBlock(ruleBlock);


}
}
