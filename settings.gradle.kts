rootProject.name = "RaabtaPhone"
pluginManagement {
    // Kotlin 2.3 needs newer R8 metadata support. Override only the optimizer
    // to avoid downloading a whole new AGP/Gradle toolchain on this PC.
    buildscript {
        repositories { google(); mavenCentral() }
        dependencies { classpath("com.android.tools:r8:8.13.19") }
    }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://www.jitpack.io") }
        mavenLocal()
    }
}
include(":app")
include(":contacts")
