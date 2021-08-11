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
val kotlinVersion: String by project

dependencies {
  testImplementation(kotlin("test", kotlinVersion))
}
