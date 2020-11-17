plugins {
    kotlin("js")
}

val electronVersion: String by project
val openVersion: String by project
val kotlinVersion: String by project
val coroutinesVersion: String by project
val kotlinExtensionsVersion: String by project

kotlin {
    js {
        // todo: switch to nodejs or electron (KT-35327)
        //       while webpack is not supported in nodejs target, need to override target in webpack.config.d
        browser()
    }
}

dependencies {
    implementation(npm("electron", electronVersion))
    implementation(npm("open", openVersion))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
    implementation("org.jetbrains:kotlin-extensions:$kotlinExtensionsVersion")
}
