import org.gradle.api.tasks.Copy

plugins {
    id("java-library")
}

repositories {
    mavenCentral()

    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.3.0")
    annotationProcessor("com.velocitypowered:velocity-api:4.3.0")
    implementation("com.google.code.gson:gson:2.13.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

val buildVersion = project.version.toString()
val generatedVersionDirectory = layout.buildDirectory.dir(
    "generated/sources/version/java"
)

val generateBuildConstants by tasks.registering(Copy::class) {
    from("src/main/templates") {
        expand("version" to buildVersion)
    }

    into(generatedVersionDirectory)
}

sourceSets {
    named("main") {
        java.srcDir(generatedVersionDirectory)
    }
}

tasks.named("compileJava") {
    dependsOn(generateBuildConstants)
}

tasks.named<Jar>("jar") {
    archiveFileName.set("GBManagement-velocity-${project.version}.jar")
}