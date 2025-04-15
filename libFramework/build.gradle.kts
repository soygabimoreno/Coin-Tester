plugins {
    alias(libs.plugins.cointester.android.library)
}

android {
    namespace = "com.appacoustic.cointester.libFramework"
}
dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.gson)
}
