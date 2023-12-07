package at.ac.uibk.se.bimclipse.reasoning.engine.steps;

import be.ugent.IfcSpfReader;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * The step in the checking pipeline that imports the base model.
 * <p>
 * Together with the {@link SpecificationImportStep}, it forms the first stage in the reasoning pipeline. It takes the
 * path to a model as input and imports it as Jena {@link Model} (the Jena implementation of an RDF Graph). The path can
 * either point to an ontology file (any syntax, e.g. TURTLE, XML, ...) or an IFC file of a building model (.ifc). If
 * the path points to an IFC file, the IfcToOwl tool is used to create a {@link  Model} from it.
 */
public class ModelImportStep {

    private static final Map<String, String> ifcImportCorrections = Map.of(
            "https://standards.buildingsmart.org/IFC/DEV/IFC2x3/TC1/OWL#", "https://standards.buildingsmart.org/IFC/DEV/IFC2x3/TC1/OWL/IFC2X3_TC1.ttl"
    );

    /**
     * Executes this step.
     *
     * @param path path to the model (ontology file or IFC file).
     * @return the imported model.
     */
    public Model execute(String path) {
        return importModel(path);
    }

    private Model importModel(String modelPath) {
        if (modelPath.endsWith(".ifc")) {
            return importModelFromIfc(modelPath);
        } else {
            return importModelFromOwl(modelPath);
        }
    }

    private Model importModelFromIfc(String modelPath) {
        try {
            // set up the .ifc to .ttl converter
            IfcSpfReader reader = new IfcSpfReader();
            reader.setup(modelPath);

            // create a temporary file
            File tmp = File.createTempFile("bimclipse-reasoning-tmp", ".ttl");
            tmp.deleteOnExit();

            // read the model, convert it to .ttl and store it in the temporary file
            reader.convert(modelPath, tmp.getPath(), "http://uibk.ac.at/se/bimclipse/");

            // adjust some links in the temporary file
            String content = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
            String result = content.replace("http://standards.buildingsmart.org/IFC/", "https://standards.buildingsmart.org/IFC/");
            Files.writeString(tmp.toPath(), result);

            // use the temporary file for regular import
            OntModel model = importModelFromOwl(tmp.getPath());

            // perform import correction
            for (Map.Entry<String, String> importCorrection : ModelImportStep.ifcImportCorrections.entrySet()) {
                if (model.listImportedOntologyURIs().contains(importCorrection.getKey())) {
                    model.remove(
                            model.createStatement(
                                    model.createResource(model.getNsPrefixURI("inst")),
                                    model.getProfile().IMPORTS(),
                                    model.createResource(importCorrection.getKey())
                            )
                    );
                    model.add(
                            model.createStatement(
                                    model.createResource(model.getNsPrefixURI("inst")),
                                    model.getProfile().IMPORTS(),
                                    model.createResource(importCorrection.getValue())
                            )
                    );
                }
            }

            // reload the imports
            model.loadImports();

            return model;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OntModel importModelFromOwl(String modelPath) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.read(modelPath);
        return model;
    }

}
