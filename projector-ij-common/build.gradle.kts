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

val intellijPlatformVersion: String by project
val kotlinVersion: String by project

dependencies {
  implementation(project(":projector-util-loading"))

  compileOnly("com.jetbrains.intellij.platform:core-impl:$intellijPlatformVersion")

  testImplementation(kotlin("test", kotlinVersion))
}
