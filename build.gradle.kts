plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.agentoffice"
version = "1.0.0-SNAPSHOT"
description = "Minecraft plugin: Claude agents work in a virtual office driven by beads tasks"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // HTTP client for Claude API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.10.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("okhttp3", "com.agentoffice.libs.okhttp3")
    relocate("okio", "com.agentoffice.libs.okio")
    relocate("com.google.gson", "com.agentoffice.libs.gson")
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
