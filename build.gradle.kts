plugins {
    kotlin("jvm") version "1.3.31"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    maven
    `maven-publish`
}

allprojects {

    apply(plugin = "java")
    apply(plugin = "maven")
    apply(plugin = "maven-publish")

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    group = "link.stuf.exceptions"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        testCompile("junit:junit:4.12")
    }
}

