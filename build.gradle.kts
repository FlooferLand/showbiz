import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val java = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5"))
    JavaVersion.VERSION_21 else JavaVersion.VERSION_17
val kotlinVersion = "2.2.20"
val loader = "fabric"

val minecraft = stonecutter.current.version
val modId = property("mod.id") as String
val modVersion = property("mod.version") as String
group = "com.flooferland"
version = "${modVersion}+$minecraft"
base {
    archivesName.set(modId)
}
val isAlpha = "alpha" in modVersion
val isBeta = "beta" in modVersion

stonecutter {
    constants["fabric"] = (loader == "fabric")
}

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.devtools.ksp") version "2.2.20-2.0.2"
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.11-SNAPSHOT"
}

repositories {
    mavenCentral()
    maven("https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") {
        name = "DevAuth"
    }
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        name = "GeckoLib"
    }
    maven("https://www.jitpack.io") {
        name = "Jitpack"
    }
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }; filter { includeGroup("maven.modrinth") }
    }
}

fun vers(name: String): String = property("vers.${name}") as String
fun dep(name: String): String = property("deps.${name}") as String
dependencies {
    minecraft("com.mojang:minecraft:${minecraft}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Loader-specific
    if (loader == "fabric") {
        @Suppress("UnstableApiUsage")  // Fabric.. why is this needed..
        mappings(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${minecraft}:${dep("parchment")}@zip")
        })

        if (dep("fabric_language_kotlin").split("+")[1] != "kotlin.$kotlinVersion") {
            error("Fabric Language Kotlin and Kotlin version do not match up")
        }
        modImplementation("net.fabricmc:fabric-loader:${dep("fabric_loader")}")
        modImplementation("net.fabricmc.fabric-api:fabric-api:${dep("fabric_api")}")
        modImplementation("net.fabricmc:fabric-language-kotlin:${dep("fabric_language_kotlin")}")
    }

    // Showbiz data library (https://github.com/FlooferLand/bizlib)
    implementation("com.flooferland:bizlib:${dep("bizlib")}")
    include("com.flooferland:bizlib:${dep("bizlib")}")

    // ktoml
    implementation("com.akuleshov7:ktoml-core:${dep("ktoml")}")
    include("com.akuleshov7:ktoml-core:${dep("ktoml")}")

    // GeckoLib
    modImplementation("software.bernie.geckolib:geckolib-${loader}-${minecraft}:${dep("geckolib")}")

    // Useful dev tools
    modRuntimeOnly("me.djtheredstoner:DevAuth-$loader:${dep("dev_auth")}")
    //modRuntimeOnly("maven.modrinth:sodium:mc$minecraft-${dep("dev.sodium")}-$loader")
    //modRuntimeOnly("maven.modrinth:sodium-extra:mc$minecraft-${dep("dev.sodium_extra")}+$loader")
}

// Mappings
loom {
    splitEnvironmentSourceSets()
    mods {
        register(modId) {
            sourceSet("main")
            sourceSet("client")
        }
    }
    runConfigs.all {
        ideConfigGenerated(true) // Run configurations are not created for subprojects by default
        runDir = "../../run" // Shared run folder between versions
    }
}

// License
tasks.jar {
    inputs.property("archivesName", base.archivesName.get())
    from("LICENSE") {
        rename { "${it}_${base.archivesName}" }
    }
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.WARN
    exclude("**/*.lnk", "**/*.exe", "**/*.dll", "**/*.so", "**/*.jar")
    exclude("projects")

    // Inserting strings into what-not
    val fabricLanguageKotlin = "${dep("fabric_language_kotlin")}+kotlin.$kotlinVersion"
    val properties = mapOf(
        "minecraft" to vers("minecraft"),
        "version" to modVersion,
        "versionFull" to version as String,
        "java" to java.toString(),
        "kotlin" to kotlinVersion,
        "fabric_loader" to dep("fabric_loader"),
        "fabric_language_kotlin" to fabricLanguageKotlin,
        "fabric_api" to dep("fabric_api"),
        "geckolib" to vers("geckolib"),
        "archivesName" to modId,
        "archivesBaseName" to modId
    )
    properties.forEach() { (k, v) ->
        inputs.property(k, v)
    }
    filesMatching("fabric.mod.json") { expand(properties) }
    filesMatching("$modId.client.mixins.json") { expand(properties) }
    filesMatching("data/showbiz/showbiz.addon.toml")  { expand(properties) }
}

// Datagen
tasks.register<JavaExec>("runDatagen") {
    group = "flooferland"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.flooferland.showbiz.datagen.DataGenerator")
    systemProperty("$modId.datagen", "true")
}
sourceSets {
    main {
        resources {
            srcDir("src/main/generated/resources")
        }
    }
}

// Java/Kotlin
java {
    withSourcesJar()
    targetCompatibility = java
    sourceCompatibility = java
}
tasks.remapSourcesJar {
    duplicatesStrategy = DuplicatesStrategy.WARN
}
kotlin {
    jvmToolchain(java.ordinal + 1)
}
tasks.withType<KotlinCompile>() {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}
