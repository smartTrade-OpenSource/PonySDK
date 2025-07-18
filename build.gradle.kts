plugins {
    java
    id("maven-publish")
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        toolchain.languageVersion = JavaLanguageVersion.of(17)
    }

    repositories {
        mavenCentral()
    }

//    configurations.all {
//        resolutionStrategy {
//            failOnVersionConflict()
//        }
//    }
}