package io.showkart.auth.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class HexagonalArchitectureTest {

    private static JavaClasses classesUnderTest;

    @BeforeAll
    static void importClasses() {
        classesUnderTest = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.showkart.auth");
    }

    @Test
    void domain_has_no_framework_imports() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "com.fasterxml..",
                        "org.hibernate..",
                        "io.jsonwebtoken.."
                )
                .check(classesUnderTest);
    }

    @Test
    void application_stays_off_infrastructure_libraries() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.jsonwebtoken..",
                        "org.springframework.security..",
                        "org.hibernate..",
                        "jakarta.persistence.."
                )
                .check(classesUnderTest);
    }

    @Test
    void application_does_not_depend_on_adapters() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .check(classesUnderTest);
    }

    @Test
    void layers_only_flow_inward() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Adapters").definedBy("..adapter..")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters")
                .whereLayer("Adapters").mayNotBeAccessedByAnyLayer()
                .check(classesUnderTest);
    }

    @Test
    void controllers_live_in_the_inbound_rest_adapter() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..adapter.in.rest..")
                .check(classesUnderTest);
    }
}
