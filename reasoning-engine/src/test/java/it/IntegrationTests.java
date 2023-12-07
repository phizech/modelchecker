package it;

import at.ac.uibk.se.bimclipse.reasoning.engine.CheckExecutionEngine;
import org.junit.jupiter.api.Test;

/**
 * Integration tests of the {@link CheckExecutionEngine} on concrete models and specifications.
 * <p>
 * Note: In Intellij, running the tests from the class level causes the IFC tests to fail. This is caused by a bug in the
 * {@code IfcToOwl} tool. Execute the tests at method level instead.
 */
public class IntegrationTests {

    @Test
    public void testIfcSpecification2_3() {
        System.out.println(new CheckExecutionEngine().execute(
                "src/test/resources/models/Architecturalmodel.ifc",
                "src/test/resources/specifications/myspec/myspec.zip"
        ));
    }

    @Test
    public void testIfcSpecification4() {
        System.out.println(new CheckExecutionEngine().execute(
                "src/test/resources/models/simple.ifc",
                "src/test/resources/specifications/myspec/myspec.zip"
        ));
    }

}
