#include <fl/Headers.h>

int main(int argc, char** argv){
using namespace fl;

Engine* engine = new Engine;
engine->setName("simple-dimmer");

InputVariable* Ambient = new InputVariable;
Ambient->setEnabled(true);
Ambient->setName("Ambient");
Ambient->setRange(0.000, 1.000);
Ambient->setLockValueInRange(false);
Ambient->addTerm(new Triangle("DARK", 0.000, 0.250, 0.500));
Ambient->addTerm(new Triangle("MEDIUM", 0.250, 0.500, 0.750));
Ambient->addTerm(new Triangle("BRIGHT", 0.500, 0.750, 1.000));
engine->addInputVariable(Ambient);

OutputVariable* Power = new OutputVariable;
Power->setEnabled(true);
Power->setName("Power");
Power->setRange(0.000, 1.000);
Power->setLockValueInRange(false);
Power->fuzzyOutput()->setAggregation(fl::null);
Power->setDefuzzifier(new WeightedAverage("TakagiSugeno"));
Power->setDefaultValue(fl::nan);
Power->setLockPreviousValue(false);
Power->addTerm(new Constant("LOW", 0.250));
Power->addTerm(new Constant("MEDIUM", 0.500));
Power->addTerm(new Constant("HIGH", 0.750));
engine->addOutputVariable(Power);

RuleBlock* ruleBlock = new RuleBlock;
ruleBlock->setEnabled(true);
ruleBlock->setName("");
ruleBlock->setConjunction(fl::null);
ruleBlock->setDisjunction(fl::null);
ruleBlock->setImplication(fl::null);
ruleBlock->setActivation(fl:null);
ruleBlock->addRule(fl::Rule::parse("if Ambient is DARK then Power is HIGH", engine));
ruleBlock->addRule(fl::Rule::parse("if Ambient is MEDIUM then Power is MEDIUM", engine));
ruleBlock->addRule(fl::Rule::parse("if Ambient is BRIGHT then Power is LOW", engine));
engine->addRuleBlock(ruleBlock);


}
