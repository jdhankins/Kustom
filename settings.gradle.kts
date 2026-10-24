
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootDir.resolve("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
val gprUser: String? = localProperties.getProperty("gpr.user")
    ?.takeIf { it.isNotBlank() && !it.startsWith("YOUR_GITHUB") }
    ?: System.getenv("GITHUB_ACTOR")
    ?: System.getenv("USERNAME")

val gprKey: String? = localProperties.getProperty("gpr.key")
    ?.takeIf { it.isNotBlank() && !it.contains("YOUR_GITHUB") }
    ?: System.getenv("GITHUB_TOKEN")
    ?: System.getenv("GH_TOKEN")
    ?: System.getenv("TOKEN")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
            if (!gprUser.isNullOrBlank() && !gprKey.isNullOrBlank()) {
                credentials {
                    username = gprUser
                    password = gprKey
                }
            }
        }
    }
}

rootProject.name = "Kustom"
include(":app")
