plugins {
    id("antlr-grammar")
    id("java-common")
    id("kotlin-common")
    id("jacoco-common")
}

dependencies {
    implementation(project(":theta-xcfa"))
    implementation(project(":theta-common"))
    implementation(project(":theta-analysis"))
    implementation(project(":theta-core"))
    implementation(project(":theta-solver"))
    implementation(project(":theta-solver-z3"))
    implementation(project(":theta-graph-solver"))
}
tasks.named("compileKotlin") {
    dependsOn("generateGrammarSource")
}
tasks.named("compileTestKotlin") {
    dependsOn("generateTestGrammarSource")
}

