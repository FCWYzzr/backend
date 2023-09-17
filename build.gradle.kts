val ktorVersion: String by project
val kotlinVersion: String by project
val ktormVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "1.9.0"
    id("io.ktor.plugin") version "2.3.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

group = "com.yunchi"
version = "0.0.1"

application {
    mainClass.set("com.yunchi.ApplicationKt")

    val isDevelopment = true
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-websockets-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-sessions-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-openapi:${ktorVersion}")

    implementation("javax.mail:mail:1.5.0-b01")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.ktorm:ktorm-core:${ktormVersion}")

    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}