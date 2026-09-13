rootProject.name = "ILife798"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupByRegex("androidx(\\..*)?")
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google(\\..*)?")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

include(":androidApp")
include(":shared")
