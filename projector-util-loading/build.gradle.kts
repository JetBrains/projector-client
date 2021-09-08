plugins {
  kotlin("jvm")
  `maven-publish`
  jacoco
}

setupJacoco()

kotlin {
  explicitApi()
}

publishToSpace("java")

dependencies {
  api(project(":projector-util-logging"))
  testImplementation(kotlin("test"))
}
