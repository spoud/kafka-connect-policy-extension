plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("maven-publish")
    id("jacoco")
}

group = "io.spoud.kafka"
version = System.getenv("VERSION") ?: "0"

java {
    targetCompatibility = JavaVersion.VERSION_11
}


repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/releases/")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation("org.apache.kafka:connect-api:3.6.1") {
        // remove unused libs to reduce jar size
        exclude("org.xerial.snappy", "snappy-java")
        exclude("com.github.luben", "zstd-jni")
    }
    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // json + json schema
    implementation("io.github.optimumcode:json-schema-validator:0.0.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.1")
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("org.glassfish.jersey.core:jersey-server:2.4.1")


    // testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.4")
    testImplementation("org.testcontainers:junit-jupiter:1.19.4")
    testImplementation("org.testcontainers:kafka:1.19.4")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["shadowJar"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/octocat/hello-world")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        csv.required = true
    }
}
kotlin {
    jvmToolchain(11)
}

tasks.register("prepareFolderForConfluentHubArchive") {
    dependsOn("shadowJar")
    doLast {
        createConfluentArchiveFolder()
    }
}

val extensionName = "spoud-connect-extension-policy-checker-$version"
val archiveFolder = "toArchive/$extensionName"

tasks.register<Zip>("createConfluentHubComponentArchive") {
    dependsOn("prepareFolderForConfluentHubArchive")
    from(layout.buildDirectory.dir("toArchive"))
    destinationDirectory = layout.buildDirectory.dir("libs")
    archiveFileName = "$extensionName.zip"
}

fun updateJsonVersion(fileProvider: Provider<RegularFile>, newVersion: String) {
    val file = fileProvider.get().asFile
    val content = file.readText().replace("{{VERSION}}", newVersion)
    file.writeText(content)
}

fun createConfluentArchiveFolder() {
    copy {
        from(layout.projectDirectory.dir("confluentArchiveBase"))
        into(layout.buildDirectory.dir(archiveFolder))
    }
    updateJsonVersion(layout.buildDirectory.file("$archiveFolder/manifest.json"), version.toString())
    copy {
        from(layout.projectDirectory.dir("../examples/")) {
            include("*.json")
        }
        into(layout.buildDirectory.dir("$archiveFolder/etc"))
    }
    copy {
        from(layout.buildDirectory.file("libs/connect-extension-$version-all.jar"))
        into(layout.buildDirectory.dir("$archiveFolder/lib"))
    }
    mkdir(layout.buildDirectory.dir("$archiveFolder/docs"))
    copy {
        from(layout.projectDirectory.file("../README.md"))
        into(layout.buildDirectory.dir("$archiveFolder/docs"))
    }
    copy {
        from(layout.projectDirectory.file("../LICENSE"))
        into(layout.buildDirectory.dir("$archiveFolder/docs"))
    }
}
