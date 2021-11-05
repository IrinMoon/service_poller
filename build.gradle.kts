import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import com.moowork.gradle.node.npm.NpmTask

plugins {
  java
  application
  id("com.github.johnrengelman.shadow") version "7.0.0"
  id("com.moowork.node") version "1.3.1"
}

group = "service_poller"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.2.0"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "service_poller.servicePoller.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-pg-client")
  implementation("com.ongres.scram:client:1.0.0-beta.2")
  implementation("io.vertx:vertx-shell")
  implementation("io.vertx:vertx-auth-jdbc")
  implementation("io.vertx:vertx-web-client")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.8.9")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}
