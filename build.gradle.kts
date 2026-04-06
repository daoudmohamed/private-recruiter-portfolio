import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
    id("org.sonarqube") version "6.3.1.5724"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
}

group = "com.knowledgebase"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["springAiVersion"] = "2.0.0-M2"
extra["testcontainersVersion"] = "1.21.3"

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.75.0"))

    // Spring Boot WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    // Spring AI - OpenAI (for chat and embeddings)
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // Spring AI - Vector Store (Qdrant)
    implementation("org.springframework.ai:spring-ai-starter-vector-store-qdrant")

    // Spring AI - Document Readers
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")
    implementation("org.springframework.ai:spring-ai-tika-document-reader")

    // Redis - Reactive
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Security - Basic (for API Key filter)
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.14")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    // TestContainers
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.redis:testcontainers-redis:2.2.0")

    // MockK for Kotlin mocking
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

// Disable plain jar
tasks.named<Jar>("jar") {
    enabled = false
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property=param-property"))
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

sonar {
    properties {
        property("sonar.projectName", "private-recruiter-portfolio")
        property(
            "sonar.projectKey",
            System.getenv("SONAR_PROJECT_KEY") ?: "private-recruiter-portfolio"
        )
        System.getenv("SONAR_ORGANIZATION")?.let {
            property("sonar.organization", it)
        }
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.sources", "src/main,frontend/src")
        property("sonar.tests", "src/test,frontend/src")
        property(
            "sonar.test.inclusions",
            "src/test/**/*,frontend/src/**/*.test.ts,frontend/src/**/*.test.tsx,frontend/src/**/*.spec.ts,frontend/src/**/*.spec.tsx"
        )
        property(
            "sonar.exclusions",
            "frontend/dist/**,frontend/node_modules/**,frontend/coverage/**,frontend/src/**/*.test.ts,frontend/src/**/*.test.tsx,frontend/src/**/*.spec.ts,frontend/src/**/*.spec.tsx,build/**,**/*.min.js,**/generated/**"
        )
        property("sonar.java.binaries", "build/classes")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.javascript.lcov.reportPaths", "frontend/coverage/lcov.info")
        property("sonar.typescript.lcov.reportPaths", "frontend/coverage/lcov.info")
    }
}
