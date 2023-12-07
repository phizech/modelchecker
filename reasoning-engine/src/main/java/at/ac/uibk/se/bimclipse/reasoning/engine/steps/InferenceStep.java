package at.ac.uibk.se.bimclipse.reasoning.engine.steps;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

import java.util.List;

/**
 * The step in the checking pipeline that performs the model enrichment, extracting information from the model,
 * inferring knew knowledge and appending it to the base model.
 * <p>It is executed after the {@link ModelImportStep} and {@link SpecificationImportStep}. It receives the base model
 * from the {@link ModelImportStep}, and a list of ontologies, inference rules and inference queries from the
 * {@link SpecificationImportStep}, as input and produces a new enriched model as output.
 * <p>
 * This step is divided into three sub-steps: ontology import, rule-based inference, and query-based
 * inference. These sub-steps are executed sequentially, in the following order: ontology-based -> rule-based ->
 * query-based. As the base model is passed through those steps, it is incrementally enhanced. This means, that later
 * steps can use the inferences from the previous steps to form new inferences on top of them. Thus, the order of
 * execution has to be kept in mind. For now, this is fixed. In the future, it might be possible to set the order
 * dynamically.
 * <p>
 * The ontology import sub-step is straight-forward. It uses the parsed models from the
 * {@link SpecificationImportStep} and appends them to the base model from the {@link ModelImportStep}. No inferences
 * are made in this step. This step is mostly used to append raw knowledge to the model, without doing any inferences
 * directly. For now, only ontologies in TURTLE format are supported.
 * <p>
 * The rule inference sub-step generally performs most of the inferences. It takes the inference rules from the
 * {@link SpecificationImportStep} and uses the {@link GenericRuleReasoner} from the Jena API to infer new information.
 * It is also here where the OWL semantics are implemented, such as subclass inferences. Note: the
 * {@link GenericRuleReasoner} does not include the OWL semantics, they have to be implemented as Jena rules and passed
 * to this step. This step is mainly used to perform simple inferences and to implement recursions.
 * <p>
 * The query inference sub-step is used to perform complex inference that cannot be implemented using the previous
 * sub-steps. This could be existential conditions, counting or complex arithmetic computation and aggregation. Query
 * inference is implemented using SPARQL CONSTRUCT queries. The queries are executed on the model after the
 * rule inference step using the {@link QueryExecutionFactory} and {@link QueryExecution} and the results are added
 * to the model.
 */
public class InferenceStep {

    /**
     * Executes this step.
     *
     * @param baseModel            the base model.
     * @param additionalOntologies the additional ontologies that are appended to the base model.
     * @param inferenceRules       the rules used for rule-based inference.
     * @param inferenceQueries     the queries used for query-based inference.
     * @return the enriched model.
     */
    public Model execute(Model baseModel, List<Model> additionalOntologies, List<Rule> inferenceRules,
                         List<Query> inferenceQueries) {
        Model ontologyEnrichedModel = performAdditionalOntologyImport(baseModel, additionalOntologies);
        Model ruleEnrichmentModel = performRuleInference(ontologyEnrichedModel, inferenceRules);
        return performQueryInference(ruleEnrichmentModel, inferenceQueries);
    }

    private Model performAdditionalOntologyImport(Model model, List<Model> ontologies) {
        for (Model ontology : ontologies) {
            model.add(ontology);
        }
        return model;
    }

    private Model performRuleInference(Model model, List<Rule> rules) {
        GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
        return ModelFactory.createInfModel(reasoner, model);
    }

    private Model performQueryInference(Model model, List<Query> queries) {
        Model queryInferenceResults = ModelFactory.createDefaultModel();
        for (Query query : queries) {
            queryInferenceResults.add(QueryExecutionFactory.create(query, model).execConstruct());
        }
        return model.add(queryInferenceResults);
    }

}
