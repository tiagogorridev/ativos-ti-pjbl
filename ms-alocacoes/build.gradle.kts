plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "br.pucpr"
version = "1.0.0"
description = "Microsserviço de Alocações (Azure SQL): colaboradores e alocações de ativos"

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
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Swagger UI + OpenAPI 3 em /swagger-ui.html e /v3/api-docs.
    implementation(libs.springdoc.webmvc.ui)
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Driver do SQL Server / Azure SQL e suporte do Flyway a esse banco.
    runtimeOnly("com.microsoft.sqlserver:mssql-jdbc")
    runtimeOnly("org.flywaydb:flyway-sqlserver")

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
