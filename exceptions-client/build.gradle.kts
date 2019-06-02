dependencies {
    compile(project(":exceptions-core"))
    compile(project(":exceptions-dto"))
    compile("io.micrometer:micrometer-core:1.1.4")

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.8")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")

    testCompile("junit:junit:4.12")
}
