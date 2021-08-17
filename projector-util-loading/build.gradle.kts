plugins {
  kotlin("jvm")
  `maven-publish`
  jacoco
}

jacoco {
  toolVersion = "0.8.7"
}

tasks.withType<JacocoReport> {
  setupReporting(project, "ProjectorUtilLoading")
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
