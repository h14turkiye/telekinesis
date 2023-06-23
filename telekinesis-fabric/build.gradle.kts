@file:Suppress("SpellCheckingInspection")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import kotlin.io.path.readText

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    id("com.modrinth.minotaur")
    id("com.github.breadmoirai.github-release")

    id("fabric-loom")
    id("io.github.juuxel.loom-quiltflower")

    `maven-publish`
    signing
}

group = "dev.nyon"
val majorVersion = "2.2.0"
val mcVersion = "1.20"
version = "$majorVersion-$mcVersion"
description = "Adds a telekinesis enchantment to minecraft"
val projectAuthors = listOf("btwonion")
val githubRepo = "btwonion/telekinesis"

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = URI("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.terraformersmc.com/releases")
}

dependencies {
    implementation(include(project(":telekinesis-common"))!!)
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.14.21")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.9.4+kotlin.1.8.21")
    modImplementation("dev.isxander.yacl:yet-another-config-lib-fabric:3.0.2+1.20")
    modImplementation("com.terraformersmc:modmenu:7.1.0")

    include("com.akuleshov7:ktoml-core-jvm:0.4.1")

    // Integration
    modImplementation("maven.modrinth:abooMhox:c2klaSgQ") // tree-harvester by ricksouth
}

tasks {
    processResources {
        val modId = "telekinesis"
        val modName = "Telekinesis"
        val modDescription = "Adds an telekinesis enchantment"

        inputs.property("id", modId)
        inputs.property("group", project.group)
        inputs.property("name", modName)
        inputs.property("description", modDescription)
        inputs.property("version", project.version)
        inputs.property("github", githubRepo)

        filesMatching(listOf("fabric.mod.json")) {
            expand(
                "id" to modId,
                "group" to project.group,
                "name" to modName,
                "description" to modDescription,
                "version" to project.version,
                "github" to githubRepo,
            )
        }
    }

    register("releaseMod") {
        group = "publishing"

        dependsOn("modrinth")
        dependsOn("modrinthSyncBody")
        dependsOn("githubRelease")
        dependsOn("publish")
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<JavaCompile> {
        options.release.set(17)
    }
}

val changelogText = rootDir.toPath().resolve("changelogs/fabric-$version.md").readText()

modrinth {
    token.set(findProperty("modrinth.token")?.toString())
    projectId.set("LLfA8jAD")
    versionNumber.set("${project.version}")
    versionType.set("release")
    uploadFile.set(tasks["remapJar"])
    gameVersions.set(listOf(mcVersion))
    loaders.set(listOf("fabric", "quilt"))
    dependencies {
        required.project("fabric-language-kotlin")
    }
    changelog.set(changelogText)
    syncBodyFrom.set(file("../README.md").readText())
}

githubRelease {
    token(findProperty("github.token")?.toString())

    val split = githubRepo.split("/")
    owner(split[0])
    repo(split[1])
    tagName("${project.version}")
    body(changelogText)
    targetCommitish("master")
}

publishing {
    repositories {
        maven {
            name = "nyon"
            url = uri("https://repo.nyon.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.nyon"
            artifactId = "telekinesis-fabric"
            version = "${project.version}"
            from(components["java"])
        }
    }
}

signing {
    sign(publishing.publications)
}

java {
    withSourcesJar()
}