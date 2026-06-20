pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "hubble"
include(":core")
include(":demo")
include(":app") // Android client (needs the SDK; see local.properties)
include(":desktop") // Compose for Desktop companion (Linux/Mac/Windows)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
