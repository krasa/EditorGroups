import org.jetbrains.changelog.Changelog

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.14.1"
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("org.jetbrains.changelog") version "2.1.0"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()
val javaVersion = properties("javaVersion").get()
val pluginVersion = properties("pluginVersion").get()

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  path.set("${project.projectDir}/docs/CHANGELOG.md")
  version.set(properties("pluginVersion").get())
  header.set(provider { version.get() })
  itemPrefix.set("-")
  keepUnreleasedSection.set(true)
  unreleasedTerm.set("Changelog")
  groups.set(listOf("Features", "Fixes", "Removals", "Other"))
}


tasks {
  patchPluginXml {
    version.set(properties("pluginVersion").get())
    sinceBuild.set(properties("pluginSinceBuild").get())
    untilBuild.set(properties("pluginUntilBuild").get())

    val changelog = project.changelog // local variable for configuration cache compatibility
    // Get the latest available change notes from the changelog file
    changeNotes.set(provider {
      with(changelog) {
        renderItem(
          (getOrNull(pluginVersion) ?: getUnreleased())
            .withHeader(false)
            .withEmptySections(false),
          Changelog.OutputType.HTML,
        )
      }
    })
  }

  wrapper {
    gradleVersion = properties("gradleVersion").get()
  }

  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    dependsOn("patchChangelog")
    token.set(System.getenv("PUBLISH_TOKEN") ?: file("./publishToken").readText().trim())
    // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    channels.set(properties("pluginVersion")
      .map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) })
  }

  buildSearchableOptions {
    enabled = false
  }

  compileKotlin {
    kotlinOptions.jvmTarget = javaVersion
  }

  compileTestKotlin {
    kotlinOptions.jvmTarget = javaVersion
  }
}

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
  pluginName.set(properties("pluginName").get())
  version.set(properties("platformVersion").get())
  type.set(properties("platformType").get())
  downloadSources.set(true)
  instrumentCode.set(true)
  updateSinceUntilBuild.set(true)

  plugins.set(
    listOf(
      "java",
    )
  )
}


dependencies {
// https://mvnrepository.com/artifact/commons-io/commons-io
  implementation("commons-io:commons-io:2.11.0")

  // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
  implementation("org.apache.commons:commons-lang3:3.12.0")

}

