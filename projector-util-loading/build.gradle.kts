plugins {
  kotlin("jvm")
  `maven-publish`
}

kotlin {
  explicitApi()
}

publishing {
  publishOnSpace(project, "java")
}

dependencies {
  api(project(":projector-util-logging"))
}
