package at.ac.uibk.se.bimclipse.reasoning.engine;

import at.ac.uibk.se.bimclipse.reasoning.engine.events.EventHandler;
import at.ac.uibk.se.bimclipse.reasoning.engine.events.LoggingEventHandler;
import at.ac.uibk.se.bimclipse.reasoning.engine.specification.Specification;
import at.ac.uibk.se.bimclipse.reasoning.engine.steps.*;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;

import java.util.List;

/**
 * The engine that is used to check models on specifications. Creating an instance of this class and calling its
 * {@code execute} function is the indented way of using this API.
 * <p>
 * The engine is executed on a model and a specification. Specification (.zip, see {@link SpecificationImportStep})
 * and model (.ifc, see {@link ModelImportStep}) are imported from the local file system and passed through a
 * predefined sequence of steps. The result is a list of check results (see {@link CheckResultExtractionStep}).
 * <p>
 * Conceptually, the pipeline is composed of following steps:
 * <ol>
 *     <li><b>import</b> - here, the model and specification are read from the file system and parsed. See {@link SpecificationImportStep},
 *     {@link ModelImportStep}.</li>
 *     <li><b>enrichment</b> - here, the base model from the import is enriched using additional ontology import, rule
 *     inferences and query inferences from the specification. The idea behind this is to supply the check execution
 *     step with additional knowledge.</li>
 *     <li><b>check execution</b> - here, the checks from the specification are executed on the enriched model. Check
 *     results are appended to the enriched model. See {@link CheckExecutionStep}.</li>
 *     <li><b>result extraction</b> - here, the check results are extracted from the final model.
 *     See {@link CheckResultExtractionStep}.</li>
 * </ol>
 * <p>
 * The checking process is exclusively driven by its inputs (model path and specification path). The logic and inferences are
 * defined in the specification itself. The engine only invokes the logic in a sensible order.
 */
public class CheckExecutionEngine {

    private EventHandler eventHandler = new LoggingEventHandler();

    /**
     * Creates an instance of a check execution engine.
     */
    public CheckExecutionEngine() {

    }

    /**
     * Creates a check execution engine and attaches an {@link EventHandler}.
     *
     * @param handler the event handler that handles events published by the reasoning engine.
     */
    public CheckExecutionEngine(EventHandler handler) {
        this.eventHandler = handler;
    }

    /**
     * Checks the specified model on the specified specification.
     *
     * @param modelPath         path on the local file system from where to load the model.
     * @param specificationPath path on the local file system from where to load the specification.
     * @return the list of check results
     */
    public List<QuerySolution> execute(String modelPath, String specificationPath) {
        try {
            eventHandler.beforeReasoningEngineStarted(modelPath, specificationPath);

            // import and parse the specification
            eventHandler.beforeSpecificationImport(specificationPath);
            Specification specification = new SpecificationImportStep()
                    .execute(specificationPath);
            eventHandler.afterSpecificationImport(specification);

            // import the base model
            eventHandler.beforeModelImport(modelPath);
            Model baseModel = new ModelImportStep()
                    .execute(modelPath);
            eventHandler.afterModelImport(baseModel);

            // perform model enrichment
            eventHandler.beforeModelEnrichment(baseModel, specification.additionalOntologies(),
                    specification.inferenceRules(), specification.enrichmentRules());
            Model enrichedModel = new InferenceStep()
                    .execute(baseModel, specification.additionalOntologies(), specification.inferenceRules(),
                            specification.enrichmentRules());
            eventHandler.afterModelEnrichment(enrichedModel);

            // execute the checks
            eventHandler.beforeCheckExecution(enrichedModel, specification.checks());
            Model enrichedModelWithCheckResults = new CheckExecutionStep()
                    .execute(enrichedModel, specification.checks());
            eventHandler.afterCheckExecution(enrichedModelWithCheckResults);

            // extract the check results
            eventHandler.beforeResultExtraction(enrichedModelWithCheckResults);
            List<QuerySolution> querySolutions = new CheckResultExtractionStep().execute(enrichedModelWithCheckResults,
                    specification.extractionQuery());
            eventHandler.afterResultExtraction(querySolutions);

            eventHandler.afterReasoningEngineDone(querySolutions);

            return querySolutions;
        } catch (Exception e) {
            eventHandler.onFatalException(e);
            return null;
        } catch (Error e) {
            eventHandler.onFatalError(e);
            return null;
        }
    }

}
