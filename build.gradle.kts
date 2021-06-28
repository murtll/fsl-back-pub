import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

group = "com.freshsoundlife"
version = "0.3.7"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://dl.bintray.com/icerockdev/backend") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("org.apache.logging.log4j:log4j-core:2.14.0")

    implementation("org.kodein.di:kodein-di-generic-jvm:5.3.0")
    implementation("com.github.jillesvangurp:es-kotlin-client:1.0.6")
    implementation("com.icerockdev.service:email-service:0.0.3")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.cloudinary:cloudinary-http44:1.29.0")
    implementation("org.json:json:20201115")

    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "com.freshsoundlife.server.ServerKt"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
