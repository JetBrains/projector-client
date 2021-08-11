plugins {
  kotlin("jvm")
  `maven-publish`
}

kotlin {
  explicitApi()
}

publishing {
  publishOnSpace(this, "java")
}

dependencies {
  api(project(":projector-util-logging"))
}
