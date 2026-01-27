pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google {
      url = uri("https://dl.google.com/dl/android/maven2/")
    }
    mavenCentral {
      url = uri("https://repo1.maven.org/maven2/")
    }
    // Add JCenter as fallback (though deprecated, some dependencies might still need it)
    maven {
      url = uri("https://jcenter.bintray.com/")
    }
  }
}

rootProject.name = "timetrack"
include(":app")