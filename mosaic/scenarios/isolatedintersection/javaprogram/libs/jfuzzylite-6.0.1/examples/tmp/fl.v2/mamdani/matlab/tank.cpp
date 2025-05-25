#include <fl/Headers.h>

int main(int argc, char** argv){
using namespace fl;

Engine* engine = new Engine;
engine->setName("tank");

InputVariable* level = new InputVariable;
level->setEnabled(true);
level->setName("level");
level->setRange(-1.000, 1.000);
level->setLockValueInRange(false);
level->addTerm(new Gaussian("high", -1.000, 0.300));
level->addTerm(new Gaussian("okay", 0.000, 0.300));
level->addTerm(new Gaussian("low", 1.000, 0.300));
engine->addInputVariable(level);

InputVariable* rate = new InputVariable;
rate->setEnabled(true);
rate->setName("rate");
rate->setRange(-0.100, 0.100);
rate->setLockValueInRange(false);
rate->addTerm(new Gaussian("negative", -0.100, 0.030));
rate->addTerm(new Gaussian("none", 0.000, 0.030));
rate->addTerm(new Gaussian("positive", 0.100, 0.030));
engine->addInputVariable(rate);

OutputVariable* valve = new OutputVariable;
valve->setEnabled(true);
valve->setName("valve");
valve->setRange(-1.000, 1.000);
valve->setLockValueInRange(false);
valve->fuzzyOutput()->setAggregation(new Maximum);
valve->setDefuzzifier(new Centroid(200));
valve->setDefaultValue(fl::nan);
valve->setLockPreviousValue(false);
valve->addTerm(new Triangle("close_fast", -1.000, -0.900, -0.800));
valve->addTerm(new Triangle("close_slow", -0.600, -0.500, -0.400));
valve->addTerm(new Triangle("no_change", -0.100, 0.000, 0.100));
valve->addTerm(new Triangle("open_slow", 0.200, 0.300, 0.400));
valve->addTerm(new Triangle("open_fast", 0.800, 0.900, 1.000));
engine->addOutputVariable(valve);

RuleBlock* ruleBlock = new RuleBlock;
ruleBlock->setEnabled(true);
ruleBlock->setName("");
ruleBlock->setConjunction(new AlgebraicProduct);
ruleBlock->setDisjunction(new AlgebraicSum);
ruleBlock->setImplication(new AlgebraicProduct);
ruleBlock->setActivation(fl:null);
ruleBlock->addRule(fl::Rule::parse("if level is okay then valve is no_change", engine));
ruleBlock->addRule(fl::Rule::parse("if level is low then valve is open_fast", engine));
ruleBlock->addRule(fl::Rule::parse("if level is high then valve is close_fast", engine));
ruleBlock->addRule(fl::Rule::parse("if level is okay and rate is positive then valve is close_slow", engine));
ruleBlock->addRule(fl::Rule::parse("if level is okay and rate is negative then valve is open_slow", engine));
engine->addRuleBlock(ruleBlock);


}
