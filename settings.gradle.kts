// Repositorios donde Gradle resuelve plugins Android/Kotlin y librerías del proyecto.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Centraliza repositorios y evita que cada módulo añada repositorios de forma independiente.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ParkSmart"
include(":app")
