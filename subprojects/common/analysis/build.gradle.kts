plugins {
    id("java-common")
}

dependencies {
    implementation(project(":theta-common"))
    implementation(project(":theta-core"))
    implementation(project(":theta-solver"))
    implementation(project(mapOf("path" to ":theta-xsts-analysis")))
    implementation(project(mapOf("path" to ":theta-xsts-analysis")))
    implementation(project(mapOf("path" to ":theta-xsts-analysis")))
    testImplementation(project(":theta-solver-z3"))
}
