pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the correct Loom variant (remapping for obfuscated versions, plain for 26.1+)
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Creates `versions/{project}-{loader}` subprojects using `build.{loader}.gradle.kts`.
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("1.21.1", "fabric", "neoforge")
        match("1.21.4", "fabric", "neoforge")
        match("1.21.5", "fabric", "neoforge")
        match("1.21.8", "fabric", "neoforge")
        match("1.21.10", "fabric", "neoforge")
        match("1.21.11", "fabric", "neoforge")
        match("26.1.2", "fabric", "neoforge")
        match("26.2", "fabric", "neoforge")
        match("26.3", "fabric", "neoforge")
        vcsVersion = "1.21.1-fabric"
    }
}

rootProject.name = "Forest Growth"
