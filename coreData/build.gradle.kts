plugins {
    alias(libs.plugins.cointester.jvm.library)
}

dependencies {
    implementation(projects.coreDomain)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
}
