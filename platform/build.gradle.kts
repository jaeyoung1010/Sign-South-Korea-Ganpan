plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.signkorean.platform"
    compileSdk = 37

    defaultConfig {
        minSdk =
            libs.versions.min.sdk
                .get()
                .toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media3.exoplayer)
    testImplementation(libs.junit4)
}
