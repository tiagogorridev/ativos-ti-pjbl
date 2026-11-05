rootProject.name = "ms-ativos"

// Build independente: este microsserviço tem o próprio wrapper, catálogo de versões
// e Dockerfile, para poder ir para um repositório separado sem depender do monorepo.
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
