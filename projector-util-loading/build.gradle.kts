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

dependencies {
  api(project(":projector-util-logging"))
}
