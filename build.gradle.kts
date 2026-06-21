plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.3.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.by1337.space/repository/maven-releases/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("dev.by1337.core:BLibV2:1.7.3") // BLibV2
    compileOnly("me.clip:placeholderapi:2.12.2") // PlaceholderAPI
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") // Vault
    compileOnly("org.black_ixx:playerpoints:3.3.4") // PlayerPoints
    implementation("org.bstats:bstats-bukkit:3.2.1") // bStats
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    processResources {
        val props = mapOf("version" to project.version, "description" to project.description)

        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    withType<JavaCompile> {
        options.release.set(17)
        options.encoding = "UTF-8"
    }

    withType<ProcessResources> {
        filteringCharset = "UTF-8"
    }

    shadowJar {
        archiveClassifier.set("")

        relocate("org.bstats", "ru.last.lastitems.bstats")
    }
}

tasks.build {
    dependsOn("shadowJar")
}