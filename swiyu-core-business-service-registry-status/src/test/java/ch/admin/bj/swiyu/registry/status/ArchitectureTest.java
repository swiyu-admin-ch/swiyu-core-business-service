/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status;

import static com.tngtech.archunit.lang.conditions.ArchConditions.beAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@AnalyzeClasses(
    packagesOf = { StatusRegistryConfig.class },
    importOptions = { ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class }
)
public class ArchitectureTest {

    @ArchTest
    public static final ArchTests codingRules = ArchTests.in(CodingRules.class);

    @ArchTest
    public static final ArchTests architectureRules = ArchTests.in(ArchitectureRules.class);

    @ArchTest
    public static final ArchTests namingRules = ArchTests.in(NamingRules.class);

    @Getter
    enum Layer {
        COMMON("Common", "..common.."),
        DOMAIN("Domain", "..domain.."),
        SERVICE("Service", "..service.."),
        API("Api", "..api..");

        final String layerName;
        final String[] packageIdentifiers;

        Layer(String layerName, String... packageIdentifiers) {
            this.layerName = layerName;
            this.packageIdentifiers = packageIdentifiers;
        }
    }

    @Getter
    enum Naming {
        DOMAIN("Domain", "..domain.."),
        REPOSITORY("Repository", "..domain.."),
        SERVICE("Service", "..service.."),
        MAPPER("Mapper", "..mapper.."),
        FACTORY("Factory", "..service..", "..infrastructure..");

        final String classNameEnding;
        final String[] packageIdentifiers;

        Naming(String nameEnding, String... packageIdentifiers) {
            this.classNameEnding = nameEnding;
            this.packageIdentifiers = packageIdentifiers;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CodingRules {

        @ArchTest
        static final ArchRule classes_should_not_throw_generic_exceptions =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

        @ArchTest
        static final ArchRule classes_should_not_use_java_util_logging =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

        @ArchTest
        public static final ArchRule classes_should_not_use_field_injection = noFields()
            .should(beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired"))
            .because("field injection is evil, see http://olivergierke.de/2013/11/why-field-injection-is-evil/");
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ArchitectureRules {

        @ArchTest
        static final ArchRule architecture_is_respected = getArchitectureLayers()
            .whereLayer(Layer.API.layerName)
            .mayOnlyBeAccessedByLayers(Layer.SERVICE.layerName)
            .whereLayer(Layer.DOMAIN.layerName)
            .mayOnlyBeAccessedByLayers(Layer.SERVICE.layerName);

        @ArchTest
        static final ArchRule no_cycles_between_slices = SlicesRuleDefinition.slices()
            .matching("..registry.status.(**)..")
            .should()
            .beFreeOfCycles();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class NamingRules {

        @ArchTest
        static final ArchRule entities = classes()
            .that()
            .areAnnotatedWith(Entity.class)
            .should()
            .resideInAnyPackage(Naming.DOMAIN.packageIdentifiers)
            .allowEmptyShould(true);

        @ArchTest
        static final ArchRule repositories = classes()
            .that()
            .areAnnotatedWith(Repository.class)
            .should()
            .haveSimpleNameEndingWith(Naming.REPOSITORY.classNameEnding)
            .andShould()
            .resideInAnyPackage(Naming.REPOSITORY.packageIdentifiers)
            .allowEmptyShould(true);

        @ArchTest
        static final ArchRule services = classes()
            .that()
            .areAnnotatedWith(Service.class)
            .and()
            .haveSimpleNameEndingWith(Naming.SERVICE.classNameEnding)
            .should()
            .resideInAnyPackage(Naming.SERVICE.packageIdentifiers)
            .allowEmptyShould(true);

        @ArchTest
        static final ArchRule mappers = classes()
            .that()
            .areAnnotatedWith(Service.class)
            .and()
            .haveSimpleNameEndingWith(Naming.MAPPER.classNameEnding)
            .should()
            .resideInAnyPackage(Naming.MAPPER.packageIdentifiers)
            .allowEmptyShould(true);

        @ArchTest
        static final ArchRule interfaces_should_not_have_names_ending_with_the_word_interface = noClasses()
            .that()
            .areInterfaces()
            .should()
            .haveNameMatching(".*Interface");

        @ArchTest
        static final ArchRule interfaces_should_not_have_simple_class_names_containing_the_word_interface = noClasses()
            .that()
            .areInterfaces()
            .should()
            .haveSimpleNameContaining("Interface");

        @ArchTest
        static final ArchRule interfaces_must_not_be_placed_in_implementation_packages = noClasses()
            .that()
            .resideInAPackage("..service")
            .should()
            .beInterfaces();
    }

    private static Architectures.LayeredArchitecture getArchitectureLayers() {
        return layeredArchitecture()
            .consideringAllDependencies()
            .layer(Layer.API.layerName)
            .definedBy(Layer.API.packageIdentifiers)
            .layer(Layer.DOMAIN.layerName)
            .definedBy(Layer.DOMAIN.packageIdentifiers)
            .layer(Layer.SERVICE.layerName)
            .definedBy(Layer.SERVICE.packageIdentifiers);
    }
}
