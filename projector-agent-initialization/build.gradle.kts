plugins {
  kotlin("jvm")
  `maven-publish`
}

kotlin {
  explicitApi()
}

val usernameProp: String by project
val passwordProp: String by project

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "org.jetbrains.projector-client"
      artifactId = "projector-agent-initialization"
      version = "-SNAPSHOT"
      from(components["java"])
    }
  }
  repositories {
    maven {
      url = uri("https://packages.jetbrains.team/maven/p/prj/projector-client")
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
