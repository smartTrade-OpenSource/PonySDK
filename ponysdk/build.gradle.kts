plugins {
    id("jacoco")
    `maven-publish`
}


val resourcesCoreTest = "src/test/resources"
val gwtOutputDirName = "${layout.buildDirectory.get()}/gwt"

group = "com.ponysdk"
version = "2.9.0${if (project.hasProperty("BUILD_RELEASE")) "" else "-SNAPSHOT"}"

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "ponysdk"
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/smartTrade-OpenSource/PonySDK")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        xml.outputLocation = file("${layout.buildDirectory.get()}/reports/jacoco/report.xml")
        html.required = false
        csv.required = false
    }
}

val gwt: Configuration by configurations.creating
val gwtDev: Configuration by configurations.creating

configurations.implementation {
    extendsFrom(gwt)
}

dependencies {
    gwt(libs.gwt.user)
    gwt(libs.gwt.elemental)
    gwt(libs.elemental2)

    gwtDev(libs.gwt.dev)

    implementation(libs.bundles.jetty.server)
    implementation(libs.bundles.jetty.ws)
    implementation(libs.validation.api)
    implementation(libs.javax.ws)
    implementation(libs.jsinterop)
    implementation(libs.jsinterop.annotations)
    implementation(libs.bundles.spring)
    implementation(libs.slf4j.api)
    implementation(libs.bundles.json)
    implementation(libs.junit)
    implementation(libs.mockito.core)
    implementation(libs.tyrus.core)
    implementation(libs.tyrus.client)
    implementation(libs.tyrus.extension.deflate)
    implementation(libs.selenium.api)

    testImplementation(libs.jetty.ws.client.impl)
    testImplementation(libs.jsoup)

    testRuntimeOnly(libs.bundles.slf4j)
    testRuntimeOnly(libs.tyrus.client)
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Javadoc>("javadoc") {
    classpath += gwtDev
    exclude("**/terminal/**")
}

tasks.named<Jar>("jar") {
    into("ponyterminal") {
        from("$gwtOutputDirName/gwt/ponyterminal")
    }
    exclude("*.devmode.js")
    exclude("*compilation-mappings.txt")
}

tasks.register<JavaExec>("gwtc") {
    group = "build"

    dependsOn("classes")

    inputs.dir("src/main/java/com/ponysdk/core/terminal")
    outputs.dir("$gwtOutputDirName/gwt/ponyterminal")
    description = "GWT compile to JavaScript (production mode)"
    mainClass.set("com.google.gwt.dev.Compiler")
    workingDir = file(gwtOutputDirName)
    classpath =
        sourceSets["main"].java.sourceDirectories + configurations.getByName("gwtDev") + configurations.getByName("gwt")
    maxHeapSize = "512M"
    args(
        "-war",
        "gwt",
        "-localWorkers",
        Runtime.getRuntime().availableProcessors().toString(),
        "com.ponysdk.core.PonyTerminal",
        "-generateJsInteropExports"
        // Debug Mode
        // "-style", "DETAILED",
        // "-optimize", "0"
    )
}

tasks.named<Test>("test") {
    dependsOn("gwtc")
    classpath += files(resourcesCoreTest, gwtOutputDirName)
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) println("Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
        }
    })
}


tasks.named("check") {
    dependsOn("jacocoTestReport")
}

tasks.named("jar") {
    dependsOn("gwtc")
}

tasks.named("publish") {
    dependsOn("check")
}