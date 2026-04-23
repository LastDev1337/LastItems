plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.by1337.space/repository/maven-releases/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("dev.by1337.core:BLibV2:1.6") // BLibV2
    compileOnly("dev.by1337.item:ConfigurableItems:1.3") // ConfigurableItems (в BLibV2 прописан, но тут на всякий случай)
    compileOnly("dev.by1337.yaml:byaml-bukkit:1.2.1") // BYaml (в BLibV2 прописан, но тут на всякий случай)
    compileOnly("me.clip:placeholderapi:2.12.2") // PlaceholderAPI
    compileOnly("org.bstats:bstats-bukkit:3.1.0") // bStats (для WorldEdit + WorldGuard, а также для анонимной статистики)
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.6") { // WorldEdit
        isTransitive = false
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9") { // WorldGuard
        isTransitive = false
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

tasks.withType<ProcessResources> {
    filteringCharset = "UTF-8"
}