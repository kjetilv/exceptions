dependencies {
    compile(project(":unearth-core"))
    compile(project(":unearth-dto"))

    compile("io.micrometer:micrometer-core:1.1.4")

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.9")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.9")
}
