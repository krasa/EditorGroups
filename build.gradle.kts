plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.13.3"
}

group = "EditorGroups"
version = "0.46"

tasks {
    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("")
        changeNotes.set(
            buildString {
                append("- Compatibility with IJ 2023").append("<br>")
            }
        )
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
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
}

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2022.3")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("java"))
}


dependencies {
// https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.11.0")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.12.0")

}

