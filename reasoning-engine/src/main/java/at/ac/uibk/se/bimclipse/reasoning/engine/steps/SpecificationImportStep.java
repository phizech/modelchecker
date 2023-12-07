package at.ac.uibk.se.bimclipse.reasoning.engine.steps;

import at.ac.uibk.se.bimclipse.reasoning.engine.specification.Specification;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.rulesys.Rule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The step in the checking pipeline that imports the specification from the specification path.
 * <p>
 * Together with the {@link ModelImportStep}, it forms the first stage in the checking pipeline. It takes the
 * path to a specification as input, imports it and parses it to a {@link Specification} object.
 * <p>
 * The specification is a ZIP-file with following folder structure:
 * <p>
 * <pre>
 * some_specification.zip
 *     checks/
 *         check1.jr
 *         check2.jr
 *         ...
 *     enrichment/
 *         ontologies/
 *             ontology1.ttl
 *             ontology2.ttl
 *             ...
 *         rules/
 *             rules1.jr
 *             rules2.jr
 *             rules3.jr
 *             ...
 *         queries/
 *             query1.sparql
 *             query2.sparql
 *             ...
 *     extraction/
 *         extraction.sparql
 * </pre>
 * <p>
 * The {@code checks/} directory contains the set of check implementations. The files in this directory are Jena rules
 * (.jr). There can be multiple files, and there might be multiple check implementations per file. The file names are
 * not specified. For the layout of the checking rules, see {@link CheckExecutionStep}.
 * <p>
 * The {@code enrichment/ontologies/} directory contains the set of models used for model enrichment. The files in
 * this directory are RDF graphs in TUTRLE syntax (.ttl). There can be multiple files and the file names are not
 * specified. This directory can also be empty.
 * <p>
 * The {@code enrichment/rules/} directory contains the set of rules used for model enrichment. The files in this
 * directory are Jena rules (.jr). There can be multiple files, and there might be multiple enrichment rules per file.
 * The file names are not specified. This directory can also be empty. For the layout of the enrichment rules, see
 * {@link InferenceStep}.
 * <p>
 * The {@code enrichment/queries/} directory contains the set of models used for model enrichment. The files in this
 * directory are SPARQL queries (.sparql). There can be multiple files. The file names are not specified.
 * This directory can also be empty. For the layout of the enrichment queries, see {@link InferenceStep}.
 * <p>
 * The {@code extraction/} directory contains the query used to extract the check results. It has to be a SPARQL query
 * (.sparql), and only one can be present. The file name has to be extraction.sparql.
 */
public class SpecificationImportStep {

    /**
     * Executes this step. Reads the specification from the local file system and parses it to a {@link Specification}.
     *
     * @param path path to the specification
     * @return the parsed specification.
     * @throws IOException when there are errors while reading the specification file.
     */
    public Specification execute(String path) throws IOException {
        return parseSpecification(importSpecification(path));
    }

    private UnparsedSpecification importSpecification(String pathToSpecification) throws IOException {
        Path path = Path.of(pathToSpecification).toAbsolutePath();
        ZipFile specificationZip = new ZipFile(path.toString());

        List<String> inferenceRules = new ArrayList<>();
        List<String> inferenceQueries = new ArrayList<>();
        List<String> additionalOntologies = new ArrayList<>();
        List<String> checks = new ArrayList<>();
        String extractionQuery = null;

        Enumeration<? extends ZipEntry> entries = specificationZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            if (entry.getName().startsWith("checks/")) {
                checks.add(new String(specificationZip.getInputStream(entry).readAllBytes(), Charset.defaultCharset()));
            } else if (entry.getName().startsWith("enrichment/rules/")) {
                inferenceRules.add(new String(specificationZip.getInputStream(entry).readAllBytes(), Charset.defaultCharset()));
            } else if (entry.getName().startsWith("enrichment/ontologies/")) {
                additionalOntologies.add(new String(specificationZip.getInputStream(entry).readAllBytes(), Charset.defaultCharset()));
            } else if (entry.getName().startsWith("enrichment/queries/")) {
                inferenceQueries.add(new String(specificationZip.getInputStream(entry).readAllBytes(), Charset.defaultCharset()));
            } else if (entry.getName().equals("extraction/extraction.sparql")) {
                System.out.println(entry.getName());
                extractionQuery = new String(specificationZip.getInputStream(entry).readAllBytes(), Charset.defaultCharset());
            }
        }

        specificationZip.close();

        return new UnparsedSpecification(
                inferenceRules,
                inferenceQueries,
                additionalOntologies,
                checks,
                extractionQuery
        );
    }

    private Specification parseSpecification(UnparsedSpecification unparsedSpecification) throws IOException {
        List<Rule> inferenceRules = new ArrayList<>();
        List<Query> inferenceQueries = new ArrayList<>();
        List<Model> additionalOntologies = new ArrayList<>();
        List<Rule> checks = new ArrayList<>();
        Query extractionQuery;

        for (String unparsedRule : unparsedSpecification.enrichmentRules()) {
            File temp = File.createTempFile(UUID.randomUUID().toString(), ".jr");
            temp.deleteOnExit();
            try (FileWriter fileWriter = new FileWriter(temp)) {
                fileWriter.write(unparsedRule);
            }
            inferenceRules.addAll(Rule.rulesFromURL(temp.toURI().toURL().toString()));
        }

        for (String unparsedQuery : unparsedSpecification.enrichmentQueries()) {
            inferenceQueries.add(QueryFactory.create(unparsedQuery));
        }

        for (String unparsedCheck : unparsedSpecification.checks()) {
            File temp = File.createTempFile(UUID.randomUUID().toString(), ".jr");
            temp.deleteOnExit();
            try (FileWriter fileWriter = new FileWriter(temp)) {
                fileWriter.write(unparsedCheck);
            }
            checks.addAll(Rule.rulesFromURL(temp.toURI().toURL().toString()));
        }

        for (String unparsedOntology : unparsedSpecification.enrichmentOntologies()) {
            Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            model.read(new ByteArrayInputStream(unparsedOntology.getBytes(Charset.defaultCharset())), null, "TURTLE");
            additionalOntologies.add(model);
        }

        extractionQuery = QueryFactory.create(unparsedSpecification.extractionQuery());

        return new Specification(
                inferenceRules,
                inferenceQueries,
                additionalOntologies,
                checks,
                extractionQuery
        );
    }

    private record UnparsedSpecification(List<String> enrichmentRules, List<String> enrichmentQueries,
                                         List<String> enrichmentOntologies,
                                         List<String> checks, String extractionQuery) {

    }

}
