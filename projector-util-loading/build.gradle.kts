plugins {
  kotlin("jvm")
  `maven-publish`
  jacoco
}

val jacocoVersion: String by project

jacoco {
  toolVersion = jacocoVersion
}

tasks.withType<JacocoReport> {
  setupReporting(project)
}

kotlin {
  explicitApi()
}

publishing {
  publishOnSpace(project, "java")
}

dependencies {
  api(project(":projector-util-logging"))
  testImplementation(kotlin("test"))
}
