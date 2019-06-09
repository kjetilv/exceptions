dependencies {
    compile(project(":unearth-core"))
    compile(project(":unearth-dto"))

    compile("io.micrometer:micrometer-core:1.1.4")

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.8")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
}
