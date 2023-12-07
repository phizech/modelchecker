package at.ac.uibk.se.bimclipse.reasoning.engine.events;

import at.ac.uibk.se.bimclipse.reasoning.engine.CheckExecutionEngine;
import at.ac.uibk.se.bimclipse.reasoning.engine.specification.Specification;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.reasoner.rulesys.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A {@link EventHandler} implementation that simply logs events published by the {@link CheckExecutionEngine}.
 */
public class LoggingEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(LoggingEventHandler.class);

    @Override
    public void beforeReasoningEngineStarted(String modelPath, String specificationPath) {
        logger.debug(String.format("beforeReasoningEngineStarted(modelPath=%s, specificationPath=%s)", modelPath,
                specificationPath));
    }

    @Override
    public void beforeSpecificationImport(String specificationPath) {
        logger.debug(String.format("beforeSpecificationImport(specificationPath=%s)", specificationPath));
    }

    @Override
    public void afterSpecificationImport(Specification specification) {
        logger.debug(String.format("afterSpecificationImport(specification=%s)", specification));
    }

    @Override
    public void beforeModelImport(String modelPath) {
        logger.debug(String.format("beforeModelImport(modelPath=%s)", modelPath));
    }

    @Override
    public void afterModelImport(Model baseModel) {
        logger.debug(String.format("afterModelImport(baseModel.size()=%s)", baseModel.size()));
    }

    @Override
    public void beforeModelEnrichment(Model currentModel, List<Model> ontologies, List<Rule> rules, List<Query> queries) {
        logger.debug(String.format("beforeModelEnrichment(currentModel.size()=%s, ontologies.size()=%s, rules.size()=%s, queries.size()=%s)",
                currentModel.size(), ontologies.size(), rules.size(), queries.size()));
    }

    @Override
    public void afterModelEnrichment(Model enrichedModel) {
        logger.debug(String.format("afterModelEnrichment(enrichedModel.size()=%s)", enrichedModel.size()));
    }

    @Override
    public void beforeCheckExecution(Model currentModel, List<Rule> checks) {
        logger.debug(String.format("beforeCheckExecution(currentModel.size()=%s, checks.size()=%s)", currentModel.size(),
                checks.size()));
    }

    @Override
    public void afterCheckExecution(Model finalModel) {
        logger.debug(String.format("afterCheckExecution(finalModel.size()=%s)", finalModel.size()));
    }

    @Override
    public void beforeResultExtraction(Model model) {
        logger.debug(String.format("beforeResultExtraction(model.size()=%s)", model.size()));
    }

    @Override
    public void afterResultExtraction(List<QuerySolution> solutions) {
        logger.debug(String.format("afterResultExtraction(solutions.size()=%s)", solutions.size()));
    }

    @Override
    public void afterReasoningEngineDone(List<QuerySolution> solutions) {
        logger.debug(String.format("afterReasoningEngineDone(solutions=%s)", solutions));
    }

    @Override
    public void onFatalException(Exception causingException) {
        logger.error("onFatalException()", causingException);
    }

    @Override
    public void onFatalError(Error causingError) {
        logger.error("onFatalError()", causingError);
    }

}
