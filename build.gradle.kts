plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.lowdragmc"
version = "1.3"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.modules.json")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
            <h3>Version 1.4</h3>
            <ul>
               <li>Added @ConditionalSynced annotation support</li>
               <li>Added @RPCPacket annotation support</li>
            </ul>
            <h3>Version 1.3</h3>
            <ul>
               <li>Added @ConfigSearch annotation support</li>
               <li>Added @SkipPersistedValueMethodInspection annotation support</li>
            </ul>
            <h3>Version 1.2</h3>
            <ul>
              <li> Added @ConfigHeader annotation support</li>
              <li> Added @ConfigColor annotation support</li>
              <li>Improved Lang key inspection and fixes</li>
            </ul>
            <h3>Version 1.1</h3>
            <ul>
              <li>Support for LDLib configuration annotations</li>
              <li>Method reference and navigation for annotation values</li>
              <li>Inspection and quick fixes for annotation usage</li>
            </ul>
        """.trimIndent()
    }
    
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = listOf(providers.gradleProperty("channel").getOrElse("default"))
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
}