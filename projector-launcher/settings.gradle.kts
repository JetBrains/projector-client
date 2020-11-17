pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("js") version kotlinVersion apply false
    }
}

rootProject.name = "projector-launcher"
