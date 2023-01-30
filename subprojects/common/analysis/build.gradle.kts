plugins {
    id("java-common")
    id("kotlin-common")
}

dependencies {
    implementation(project(":theta-common"))
    implementation(project(":theta-core"))
    implementation(project(":theta-solver"))
    implementation(project(":theta-graph-solver"))
    testImplementation(project(":theta-solver-z3"))
}
