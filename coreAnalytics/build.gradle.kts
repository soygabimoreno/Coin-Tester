plugins {
    alias(libs.plugins.cointester.android.library)
}

android {
    namespace = "com.appacoustic.cointester.coreanalytics"
}

dependencies {
    implementation(projects.coreDomain)
    implementation(projects.libFramework)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.arrow.core)
    implementation(libs.arrow.optics)
    ksp(libs.arrow.optics.ksp.plugin)

    implementation(libs.amplitude)
    implementation(libs.okhttp)
}
