plugins {
  kotlin("jvm")
  `maven-publish`
  jacoco
}

kotlin {
  explicitApi()
}

publishToSpace("java")

setupJacoco()

val kotlinVersion: String by project

dependencies {
  testImplementation(kotlin("test", kotlinVersion))
}
