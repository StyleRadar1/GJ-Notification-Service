package edu.dosw.styleradar.notificaciones.unit.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Verifica de forma automatica que ms-Notifications respeta la arquitectura hexagonal:
 * el dominio no depende de nada externo, la aplicacion solo depende del dominio y los
 * adaptadores de infraestructura hablan entre si unicamente a traves de puertos.
 */
@AnalyzeClasses(
        packages = ArquitecturaHexagonalTest.RAIZ,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaHexagonalTest {

    static final String RAIZ = "edu.dosw.styleradar.notificaciones";
    private static final String DOMINIO = RAIZ + ".domain..";
    private static final String APLICACION = RAIZ + ".application..";
    private static final String INFRAESTRUCTURA = RAIZ + ".infrastructure..";
    private static final String ADAPTADORES_ENTRADA = RAIZ + ".infrastructure.adapter.in..";
    private static final String ADAPTADORES_SALIDA = RAIZ + ".infrastructure.adapter.out..";

    @ArchTest
    static final ArchRule capas_hexagonales = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Dominio").definedBy(DOMINIO)
            .layer("Aplicacion").definedBy(APLICACION)
            .layer("Infraestructura").definedBy(INFRAESTRUCTURA)
            .whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer()
            .whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Infraestructura")
            .because("las dependencias apuntan siempre hacia el dominio");

    @ArchTest
    static final ArchRule dominio_no_depende_de_aplicacion_ni_infraestructura = noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAnyPackage(APLICACION, INFRAESTRUCTURA)
            .because("el dominio es el centro del hexagono");

    @ArchTest
    static final ArchRule dominio_libre_de_frameworks = noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.servlet..",
                    "org.hibernate..", "com.rabbitmq..", "org.springframework.amqp..")
            .because("el dominio no conoce Spring, JPA ni RabbitMQ");

    @ArchTest
    static final ArchRule aplicacion_no_depende_de_infraestructura = noClasses()
            .that().resideInAPackage(APLICACION)
            .should().dependOnClassesThat().resideInAPackage(INFRAESTRUCTURA)
            .because("la aplicacion usa solo puertos del dominio");

    @ArchTest
    static final ArchRule aplicacion_no_usa_persistencia_ni_http = noClasses()
            .that().resideInAPackage(APLICACION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.springframework.web..", "org.springframework.amqp..")
            .because("los casos de uso no conocen la tecnologia de los adaptadores");

    @ArchTest
    static final ArchRule puertos_son_interfaces = classes()
            .that().resideInAnyPackage(RAIZ + ".domain.port.in..", RAIZ + ".domain.port.out..")
            .should().beInterfaces()
            .because("un puerto es un contrato, no una implementacion");

    @ArchTest
    static final ArchRule casos_de_uso_implementan_puertos_de_entrada = classes()
            .that().resideInAPackage(RAIZ + ".application.usecase..")
            .and().haveSimpleNameEndingWith("UseCaseImpl")
            .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                    "a port in domain.port.in",
                    interfaz -> interfaz.getPackageName().startsWith(RAIZ + ".domain.port.in")));

    @ArchTest
    static final ArchRule adaptadores_de_entrada_no_usan_adaptadores_de_salida = noClasses()
            .that().resideInAPackage(ADAPTADORES_ENTRADA)
            .should().dependOnClassesThat().resideInAPackage(ADAPTADORES_SALIDA)
            .because("REST, AMQP y el planificador llaman casos de uso, no adaptadores de salida");

    @ArchTest
    static final ArchRule adaptadores_de_salida_no_usan_adaptadores_de_entrada = noClasses()
            .that().resideInAPackage(ADAPTADORES_SALIDA)
            .should().dependOnClassesThat().resideInAPackage(ADAPTADORES_ENTRADA);

    @ArchTest
    static final ArchRule entidades_jpa_solo_en_persistencia = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage(RAIZ + ".infrastructure.adapter.out.persistence..")
            .because("el modelo de dominio no lleva anotaciones de JPA");

    @ArchTest
    static final ArchRule controladores_solo_en_adaptador_rest = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().resideInAPackage(RAIZ + ".infrastructure.adapter.in.rest..");

    @ArchTest
    static final ArchRule listeners_amqp_solo_en_adaptador_amqp = classes()
            .that().areAnnotatedWith("org.springframework.amqp.rabbit.annotation.RabbitListener")
            .or().haveSimpleNameEndingWith("EventsListener")
            .should().resideInAPackage(RAIZ + ".infrastructure.adapter.in.amqp..");
}
