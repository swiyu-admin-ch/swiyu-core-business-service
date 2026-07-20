package ch.admin.bj.swiyu.core.business;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.lang.conditions.ArchConditions.beAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
    packagesOf = { Application.class },
    importOptions = { ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class }
)
public class ArchitectureTest {

    @ArchTest
    public static final ArchTests codingRules = ArchTests.in(CodingRules.class);

    @ArchTest
    public static final ArchTests architectureRules = ArchTests.in(ArchitectureRules.class);

    @ArchTest
    public static final ArchTests namingRules = ArchTests.in(NamingRules.class);

    private static final String ROOT_PACKAGE = "ch.admin.bj.swiyu.core.business..";
    private static final String COMMON_PACKAGE = "ch.admin.bj.swiyu.core.business.common..";

    private static Architectures.LayeredArchitecture getArchitectureLayers() {
        return layeredArchitecture()
            .consideringAllDependencies()
            .optionalLayer(Layer.COMMON.layerName)
            .definedBy(Layer.COMMON.packageIdentifiers)
            .layer(Layer.API.layerName)
            .definedBy(Layer.API.packageIdentifiers)
            .layer(Layer.DOMAIN.layerName)
            .definedBy(Layer.DOMAIN.packageIdentifiers)
            .layer(Layer.SERVICE.layerName)
            .definedBy(Layer.SERVICE.packageIdentifiers)
            .optionalLayer(Layer.INFRASTRUCTURE.layerName)
            .definedBy(Layer.INFRASTRUCTURE.packageIdentifiers)
            .optionalLayer(Layer.WEB.layerName)
            .definedBy(Layer.WEB.packageIdentifiers)
            .whereLayer(Layer.WEB.layerName)
            .mayNotBeAccessedByAnyLayer()
            .ignoreDependency(JavaClass.Predicates.simpleName("DemoDataImportService"), alwaysTrue());
    }

    private static @NonNull ArchCondition<JavaMethod> noReturnTypeFromDomainPackageCondition() {
        return new ArchCondition<JavaMethod>("not have a return type from the domain layer") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method
                    .getReturnType()
                    .getAllInvolvedRawTypes()
                    .stream()
                    .filter(type ->
                        type
                            .getPackageName()
                            .matches(Pattern.quote(ROOT_PACKAGE.replace("..", "")) + ".*\\.domain(\\..+)?")
                    )
                    .forEach(type ->
                        events.add(
                            SimpleConditionEvent.violated(
                                method,
                                method.getFullName() + " returns domain type: " + type.getName()
                            )
                        )
                    );
            }
        };
    }

    @Getter
    enum Layer {
        DOMAIN("Domain", "..domain.."),
        SERVICE("Service", "..service.."),
        API("Api", "..api.."),
        INFRASTRUCTURE("Infrastructure", "..infrastructure.."),
        WEB("Web", "..infrastructure.web.."),
        COMMON("Common", "..common..");

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
        SERVICE("Service", "..service..", "..domain.."),
        MAPPER("Mapper", "..mapper.."),
        FACTORY("Factory", "..service..", "..infrastructure.."),
        CONTROLLER("Controller", "..web..");

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
        public static final ArchRule classes_should_not_use_field_injection = noFields()
            .should(beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired"))
            .because("field injection is evil, see http://olivergierke.de/2013/11/why-field-injection-is-evil/");

        @ArchTest
        static final ArchRule classes_should_not_throw_generic_exceptions =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

        @ArchTest
        static final ArchRule classes_should_not_use_java_util_logging =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ArchitectureRules {

        @ArchTest
        static final ArchRule architecture_is_respected = getArchitectureLayers()
            .whereLayer(Layer.API.layerName)
            .mayOnlyBeAccessedByLayers(Layer.SERVICE.layerName, Layer.INFRASTRUCTURE.layerName)
            .whereLayer(Layer.DOMAIN.layerName)
            .mayOnlyBeAccessedByLayers(Layer.SERVICE.layerName)
            .whereLayer(Layer.INFRASTRUCTURE.layerName)
            .mayNotBeAccessedByAnyLayer()
            .allowEmptyShould(true);

        @ArchTest
        static final ArchRule services_should_not_return_domain_objects = methods()
            .that()
            .arePublic()
            .and()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .and()
            .areDeclaredInClassesThat()
            .resideInAnyPackage(Layer.SERVICE.packageIdentifiers)
            .should(noReturnTypeFromDomainPackageCondition())
            .allowEmptyShould(true);

        @ArchTest
        static final ArchRule no_cycles_between_slices = SlicesRuleDefinition.slices()
            .matching("..core.business.(**)..")
            .should()
            .beFreeOfCycles();

        @ArchTest
        static final ArchRule no_cycles_between_modules = SlicesRuleDefinition.slices()
            .matching("..core.business.modules.(**)..")
            .should()
            .beFreeOfCycles();

        @ArchTest
        static final ArchRule commonModuleDependencies = classes()
            .that()
            .resideInAnyPackage(COMMON_PACKAGE)
            .should()
            .onlyDependOnClassesThat(resideInAnyPackage(COMMON_PACKAGE).or(resideOutsideOfPackage(ROOT_PACKAGE)));

        @ArchTest
        static final ArchRule noDependenciesBetweenModules = slices()
            .matching("ch.admin.bj.swiyu.core.business.modules.(*)..")
            .should()
            .notDependOnEachOther()
            // management -> identifier
            .ignoreDependency(
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.management.service.."),
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.identifier.service..")
            )
            // trust -> management
            .ignoreDependency(
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.trust.service.."),
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.management.[api|service]..")
            )
            // status -> identifier
            .ignoreDependency(
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.status.service.."),
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.identifier.service..")
            )
            // status -> management
            .ignoreDependency(
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.status.service.."),
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.management.service..")
            )
            // trust -> documents
            .ignoreDependency(
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.trust.[service|infrastructure].."),
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.documents.[api|service]..")
            )
            // trust -> identifier
            .ignoreDependency(
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.trust.service.."),
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.identifier.service..")
            )
            // jobs -> any
            .ignoreDependency(
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.jobs.service.."),
                resideInAPackage("ch.admin.bj.swiyu.core.business.modules.(**).[api|service]..")
            )
            // demodata -> allow all
            .ignoreDependency(resideInAPackage("..core.business.modules.dataimport.."), alwaysTrue());
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class NamingRules {

        @ArchTest
        static final ArchRule controllers = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .haveSimpleNameEndingWith(Naming.CONTROLLER.classNameEnding)
            .andShould()
            .resideInAnyPackage(Naming.CONTROLLER.packageIdentifiers)
            .allowEmptyShould(true);

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
            .beInterfaces()
            .allowEmptyShould(true);
    }
}
