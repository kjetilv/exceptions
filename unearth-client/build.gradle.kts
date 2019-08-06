plugins {
    id("com.github.johnrengelman.shadow")
}

val clientJavaVer = JavaVersion.VERSION_1_8

configure<JavaPluginConvention> {
    sourceCompatibility = clientJavaVer
    targetCompatibility = clientJavaVer
}

dependencies {
    compile("com.squareup.retrofit2:retrofit")
    compile("com.squareup.retrofit2:converter-jackson")

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
