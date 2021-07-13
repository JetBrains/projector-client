plugins {
  kotlin("jvm")
}

kotlin {
  explicitApi()
}

repositories {
  mavenCentral()
}

val kotlinVersion: String by project

dependencies {
  testImplementation(kotlin("test", kotlinVersion))
}
