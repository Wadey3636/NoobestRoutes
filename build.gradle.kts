import dev.architectury.pack200.java.Pack200Adapter
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
/*
    Requirements: Gradle java version is 20
    Project Java version 1.8
 */

plugins {
    idea
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("gg.essential.loom") version "0.10.0.+"
    id("net.kyori.blossom") version "1.3.1"
    kotlin("jvm") version "2.0.0-Beta1"
}


version = project.findProperty("version") as String
val modId = project.findProperty("noobestroutes") as String
val modName = project.findProperty("modname") as String
val prodValue = project.findProperty("prod") as? String ?: "false"
val isDevBuild = (prodValue == false.toString())

blossom {
    replaceToken("@MOD_NAME@", modName)
    replaceToken("@VER@", version)
    replaceToken("@MOD_ID@", modId)
    replaceToken("@DEV_MODE@", isDevBuild.toString())
    replaceToken("@PROD@", prodValue)
}

group = modId


val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}


repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.essential.gg/repository/maven-public/")
}

apply(plugin = "dev.architectury.architectury-pack200")
apply(plugin = "com.github.johnrengelman.shadow")
apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "gg.essential.loom")
apply(plugin = "net.kyori.blossom")
apply(plugin = "java")

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation(kotlin("stdlib-jdk8"))


    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    implementation("org.spongepowered:mixin:0.7.11-SNAPSHOT") { isTransitive = false }

    implementation("gg.essential:loader-launchwrapper:1.1.3")
    implementation("gg.essential:essential-1.8.9-forge:12132+g6e2bf4dc5")
    implementation("com.mojang:brigadier:1.2.9")
}

loom {
    log4jConfigs.from(file("log4j2.xml"))
    forge.pack200Provider.set(Pack200Adapter())

    forge {
        mixinConfig("mixins.$modId.json")
    }

    @Suppress("UnstableApiUsage")
    mixin.defaultRefmapName.set("mixins.$modId.refmap.json")

    runConfigs {
        getByName("client") {
            programArgs("--tweakClass", "gg.essential.loader.stage0.EssentialSetupTweaker")
            programArgs("--mixin", "mixins.$modId.json")
            isIdeConfigGenerated = true
        }
        remove(getByName("server"))
    }
}

sourceSets.main {
    java.srcDir(file("$projectDir/src/main/kotlin"))
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))
kotlin.jvmToolchain(8)


tasks {
    processResources {
        inputs.property("version", version)
        filesMatching("mcmod.info") {
            expand(inputs.properties)
        }
        dependsOn(compileJava)
    }

    jar {
        manifest.attributes(
            "FMLCorePluginContainsFMLMod" to true,
            "ForceLoadAsMod" to true,
            "MixinConfigs" to "mixins.$modId.json",
            "ModSide" to "CLIENT",
            "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker",
            "TweakOrder" to "0"
        )
        dependsOn(shadowJar)
        enabled = false
    }

    remapJar {
        input.set(shadowJar.get().archiveFile)
        if (isDevBuild) archiveClassifier.set("dev") else archiveClassifier.set("")
    }

    shadowJar {
        destinationDirectory.set(layout.buildDirectory.dir("archiveJars"))
        archiveBaseName.set(modId)
        archiveClassifier.set("gay")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        configurations = listOf(shadowImpl)
        exclude("META-INF/versions/**")
        mergeServiceFiles()
    }


}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xlambdas=class"
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    //mustRunAfter(":processResources")
}
