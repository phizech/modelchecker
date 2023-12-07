package at.ac.uibk.se.bimclipse.reasoning.engine.specification;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.reasoner.rulesys.Rule;

import java.util.List;

/**
 * Record representing a parsed specification used by the
 * {@link at.ac.uibk.se.bimclipse.reasoning.engine.CheckExecutionEngine}.
 */
public record Specification(List<Rule> inferenceRules, List<Query> enrichmentRules, List<Model> additionalOntologies,
                            List<Rule> checks, Query extractionQuery) {

}
