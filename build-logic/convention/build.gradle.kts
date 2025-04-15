plugins {
    `kotlin-dsl`
}
group = "com.appacoustic.cointester.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}
gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "cointester.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "cointester.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
