package br.pucpr.ativos.arquitetura

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test

/**
 * Testes de arquitetura (ArchUnit): o build falha se o código quebrar as regras da
 * Clean Architecture (Domain, Application, Infrastructure, API) ou do Vertical Slice.
 */
class ArquiteturaTest {

    private val classes: JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(RAIZ)

    @Test
    fun `camadas so dependem para dentro`() {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer(DOMAIN).definedBy("$RAIZ.domain..")
            .layer(APPLICATION).definedBy("$RAIZ.application..")
            .layer(INFRASTRUCTURE).definedBy("$RAIZ.infrastructure..")
            .layer(API).definedBy("$RAIZ.api..")
            .whereLayer(DOMAIN).mayNotAccessAnyLayer()
            .whereLayer(APPLICATION).mayOnlyAccessLayers(DOMAIN)
            .whereLayer(API).mayOnlyAccessLayers(APPLICATION, DOMAIN)
            .whereLayer(INFRASTRUCTURE).mayOnlyAccessLayers(APPLICATION, DOMAIN)
            .whereLayer(API).mayNotBeAccessedByAnyLayer()
            .whereLayer(INFRASTRUCTURE).mayNotBeAccessedByAnyLayer()
            .check(classes)
    }

    @Test
    fun `domain e application nao dependem de framework nem de banco`() {
        noClasses().that().resideInAnyPackage("$RAIZ.domain..", "$RAIZ.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "com.mongodb..",
                "org.bson..",
                "io.swagger..",
                "jakarta..",
                "tools.jackson..",
            )
            .because("as regras de negócio não podem depender de detalhes de entrega ou de persistência")
            .check(classes)
    }

    @Test
    fun `apenas a infrastructure conhece o MongoDB`() {
        noClasses().that().resideOutsideOfPackage("$RAIZ.infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage("com.mongodb..", "org.bson..")
            .check(classes)
    }

    @Test
    fun `fatias verticais da application nao dependem umas das outras`() {
        slices().matching("$RAIZ.application.(*)..")
            .should().notDependOnEachOther()
            .because("cada funcionalidade (ativos, manutencoes, indicadores) evolui sem tocar nas outras")
            .check(classes)
    }

    @Test
    fun `controllers ficam na API e terminam com Controller`() {
        classes().that().areAnnotatedWith(RestController::class.java)
            .should().resideInAPackage("$RAIZ.api..")
            .andShould().haveSimpleNameEndingWith("Controller")
            .check(classes)
    }

    private companion object {
        const val RAIZ = "br.pucpr.ativos"
        const val DOMAIN = "Domain"
        const val APPLICATION = "Application"
        const val INFRASTRUCTURE = "Infrastructure"
        const val API = "API"
    }
}
