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
val kotlinVersion: String by project

dependencies {
  testImplementation(kotlin("test", kotlinVersion))
}
