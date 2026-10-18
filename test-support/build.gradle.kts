plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":domain"))
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
