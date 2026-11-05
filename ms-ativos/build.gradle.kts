plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "br.pucpr"
version = "1.0.0"
description = "Microsserviço de Ativos (MongoDB): ciclo de vida dos ativos de TI e manutenções"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        // Respeita as anotações de nulidade (@Nullable/@NonNull) das bibliotecas Java.
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Driver oficial do MongoDB, usado só na camada Infrastructure (sem Spring Data).
    implementation("org.mongodb:mongodb-driver-sync")
    // Swagger UI + OpenAPI 3 em /swagger-ui.html e /v3/api-docs.
    implementation(libs.springdoc.webmvc.ui)
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.mockk)
    testImplementation(libs.archunit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Nome fixo do jar executável (usado pelo Dockerfile).
tasks.bootJar {
    archiveFileName.set("app.jar")
}
