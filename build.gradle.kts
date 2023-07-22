plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.14.1"
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
}

group = "EditorGroups"
version = "2.0"

tasks {
  patchPluginXml {
    sinceBuild.set("231")
    untilBuild.set("241.*")
    changeNotes.set(
      buildString {
        append("- Icons fix").append("<br>")
      }
    )
  }

  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }


  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }

  buildSearchableOptions {
    enabled = false
  }

  compileKotlin {
    kotlinOptions.jvmTarget = "17"
  }

  compileTestKotlin {
    kotlinOptions.jvmTarget = "17"
  }
}

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
  version.set("LATEST-EAP-SNAPSHOT")
  type.set("IU") // Target IDE Platform

  plugins.set(listOf("java"))
}


dependencies {
// https://mvnrepository.com/artifact/commons-io/commons-io
  implementation("commons-io:commons-io:2.11.0")

  // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
  implementation("org.apache.commons:commons-lang3:3.12.0")

}

