plugins {
  kotlin("jvm")
  `maven-publish`
}

kotlin {
  explicitApi()
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}

repositories {
  mavenCentral()
}

val kotlinVersion: String by project

dependencies {
  testImplementation(kotlin("test", kotlinVersion))
}
