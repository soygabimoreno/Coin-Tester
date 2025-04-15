plugins {
    alias(libs.plugins.cointester.android.library)
}

android {
    namespace = "com.appacoustic.cointester.libprocessingandroid"
}

dependencies {
    implementation(projects.libFramework)
}
