plugins {
  kotlin("jvm")
  `maven-publish`
}

kotlin {
  explicitApi()
}

val usernameProp: String by project
val passwordProp: String by project
val versionProp: String by project

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "org.jetbrains.projector"
      artifactId = "projector-agent-initialization"
      version = versionProp
      from(components["java"])
    }
  }
  repositories {
    maven {
      url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
      credentials {
        username = usernameProp
        password = passwordProp
      }
    }
  }
}

val kotlinVersion: String by project

dependencies {
  testImplementation(kotlin("test", kotlinVersion))
}
