package at.ac.uibk.se.bimclipse.reasoning.engine.steps;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

import java.util.List;

/**
 * The step in the checking pipeline that executes the checks on the model.
 * <p>
 * It is executed after the {@link InferenceStep} and the {@link SpecificationImportStep}. It receives the
 * enriched model and the checking rules as input and produces a new {@link Model} containing the check results.
 * <p>
 * The checks are implemented as Jena Rules ({@link Rule}), where the antecedent of the rule defines the subjects to
 * be tested, and the consequent defines the result of that check on that subject. The check results are regular RDF
 * triples in the form of (?subject, ?check, ?result), where ?subject defines the object that is being tested, ?check
 * identifies the specific check on which the subject is tested, and ?result is the result of that check on that
 * subject.
 * <p>
 * A check implementation could, for example, look like this:
 * <p>
 * {@code [(?s rdf:type myspec:Door), (?s myspec:hasWidth ?w), lessThan(?w, "813"^^xsd:double) -> (?s myspec:check_IBC_2015_Section_1010_b myspec:FAIL)]}
 * <p>
 * Conceptually, this check defines that every door that is less than 813mm wide fails the IBC_2015_Section_1010_b
 * rule.
 * <p>
 * The check results are inferred from the enriched model using the {@link GenericRuleReasoner} from the Jena API.
 */
public class CheckExecutionStep {

    /**
     * Executes this step.
     *
     * @param enrichedModel the model after model enrichment.
     * @param checkingRules the checks to be tested.
     * @return the model after check execution, containing the check results.
     */
    public Model execute(Model enrichedModel, List<Rule> checkingRules) {
        GenericRuleReasoner reasoner = new GenericRuleReasoner(checkingRules);
        return ModelFactory.createInfModel(reasoner, enrichedModel);
    }

}
