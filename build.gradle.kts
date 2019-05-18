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
}

