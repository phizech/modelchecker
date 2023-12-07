package at.ac.uibk.se.bimclipse.reasoning.engine.steps;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * The step in the checking pipeline that extracts the check results from the model.
 * <p>
 * It is executed after the {@link CheckExecutionStep} and is the last step in the checking pipeline. It receives the
 * model containing the check results from the {@link CheckExecutionStep} and the extraction query from the
 * {@link SpecificationImportStep} as input and produces a list check results in the form {@link QuerySolution}s.
 * <p>
 * The extraction query is implemented as SPARQL query ({@link Query} in Jena).
 * <p>
 * The check results are extracted from the model by executing the extraction query on the final model, using the
 * {@link QueryExecutionFactory} and {@link QueryExecution} from the Jena API.
 */
public class CheckResultExtractionStep {

    /**
     * Executes this step.
     *
     * @param finalModel      the model after check execution.
     * @param extractionQuery the query used to extract the check results.
     * @return the check results.
     */
    public List<QuerySolution> execute(Model finalModel, Query extractionQuery) {
        List<QuerySolution> solutions = new ArrayList<>();
        try (QueryExecution queryExecution = QueryExecutionFactory.create(extractionQuery, finalModel)) {
            ResultSet resultSet = queryExecution.execSelect();
            while (resultSet.hasNext()) {
                solutions.add(resultSet.next());
            }
        }
        return solutions;
    }

}
