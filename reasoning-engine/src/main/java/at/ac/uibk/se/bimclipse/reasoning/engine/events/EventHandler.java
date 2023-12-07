package at.ac.uibk.se.bimclipse.reasoning.engine.events;

import at.ac.uibk.se.bimclipse.reasoning.engine.CheckExecutionEngine;
import at.ac.uibk.se.bimclipse.reasoning.engine.specification.Specification;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.reasoner.rulesys.Rule;

import java.util.List;

/**
 * Interface defining a set of events that might be published by a {@link CheckExecutionEngine}.
 * <p>
 * Classes that wish to listen for events from the checking engine have to implement this interface and be passed to the
 * {@link CheckExecutionEngine} constructor. This might be useful for debugging and observing state changes.
 */
public interface EventHandler {

    /**
     * Called before any computation is started.
     *
     * @param modelPath         the path to the model.
     * @param specificationPath the path to the specification.
     */
    void beforeReasoningEngineStarted(String modelPath, String specificationPath);

    /**
     * Called before the specification is imported.
     *
     * @param specificationPath the path to the specification.
     */
    void beforeSpecificationImport(String specificationPath);

    /**
     * Called right after the specification is imported.
     *
     * @param specification the imported and parsed specification.
     */
    void afterSpecificationImport(Specification specification);

    /**
     * Called before the model is imported.
     *
     * @param modelPath the path to the model.
     */
    void beforeModelImport(String modelPath);

    /**
     * Called right after the model is imported.
     *
     * @param baseModel the imported model.
     */
    void afterModelImport(Model baseModel);

    /**
     * Called before executing the model enrichment.
     *
     * @param currentModel the model before performing model enrichment.
     * @param ontologies   the ontologies used for enrichment.
     * @param rules        the rules used for enrichment.
     * @param queries      the queries used for enrichment.
     */
    void beforeModelEnrichment(Model currentModel, List<Model> ontologies, List<Rule> rules, List<Query> queries);

    /**
     * Called right after the model enrichment is performed.
     *
     * @param enrichedModel the model after enrichment.
     */
    void afterModelEnrichment(Model enrichedModel);

    /**
     * Called before the checks are executed.
     *
     * @param currentModel the model before check execution.
     * @param checks       the checks to be executed.
     */
    void beforeCheckExecution(Model currentModel, List<Rule> checks);

    /**
     * Called right after check execution.
     *
     * @param finalModel the model after check execution.
     */
    void afterCheckExecution(Model finalModel);

    /**
     * Called before the check results are extracted from the model.
     *
     * @param model the model after check execution.
     */
    void beforeResultExtraction(Model model);

    /**
     * Called right after the check results are extracted.
     *
     * @param solutions the list of check results.
     */
    void afterResultExtraction(List<QuerySolution> solutions);

    /**
     * Called after the reasoning engine is done with computation.
     *
     * @param solutions the final list of check results.
     */
    void afterReasoningEngineDone(List<QuerySolution> solutions);

    /**
     * Called when an exception is thrown during computation.
     *
     * @param causingException the exception that was caught.
     */
    void onFatalException(Exception causingException);

    /**
     * Called when an error is caught during computation.
     *
     * @param causingError the error that was caught.
     */
    void onFatalError(Error causingError);


}
