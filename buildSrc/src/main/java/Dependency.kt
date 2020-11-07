object Version {
    val KOTLIN = "1.4.10"
    val FIREBASE_BOM = "25.12.0"

    val CORE_KTX = "1.3.1"
    val COROUTINES = "1.3.9"
    val APP_COMPAT = "1.2.0"
    val CONSTRAINT_LAYOUT = "2.0.1"
    val LIFECYCLE = "2.2.0"
    val RECYCLER_VIEW = "1.2.0-alpha02"
    val FIREBASE_ANALYTICS = "17.5.0"
    val FIREBASE_CRASHLYTICS = "17.2.1"
    val MATERIAL = "1.2.1"
    val GSON = "2.8.5"
    val KOIN = "2.2.0-rc-1"
    val ARROW = "0.10.5"
    val AMPLITUDE = "2.25.2"
    val OK_HTTP = "4.8.1"
}

object TestVersion {
    val J_UNIT = "4.13"
    val MOCKK = "1.10.0"
    val ARCH_CORE = "2.1.0"
    val COROUTINES = "1.3.9"

    val J_UNIT_EXT = "1.1.2"
    val TEST_RUNNER = "1.3.0"
    val BARISTA = "3.6.0"
    val ARCH_CORE_TEST = "2.1.0"
    val MOCK_WEB_SERVER = "4.6.0"
    val IDLING_RESOURCE = "1.0.0"
}

object ModulesDependency {
    val CORE_DOMAIN = ":coreDomain"
    val CORE_ANALYTICS = ":coreAnalytics"
    val CORE_INFRASTRUCTURE = ":coreInfrastructure"
    val CORE_DATA = ":coreData"

    val CORE_NETWORK = ":coreNetwork"
    val CORE_DB = ":coreDb"

    val LIB_FRAMEWORK = ":libFramework"
    val LIB_BASE = ":libBase"
    val LIB_PLAYER = ":libPlayer"
    val LIB_IMAGE_LOADER = ":libImageLoader"

    val APP = ":app"
}

object KotlinDependency {
    val KOTLIN = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.KOTLIN}"
    val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.COROUTINES}"
}

object AndroidDependency {
    val CORE_KTX = "androidx.core:core-ktx:${Version.CORE_KTX}"
    val APP_COMPAT = "androidx.appcompat:appcompat:${Version.APP_COMPAT}"
    val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:${Version.CONSTRAINT_LAYOUT}"
    val LIFECYCLE_VIEW_MODEL = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Version.LIFECYCLE}"
    val LIFECYCLE_RUNTIME = "androidx.lifecycle:lifecycle-runtime-ktx:${Version.LIFECYCLE}"
    val RECYCLER_VIEW = "androidx.recyclerview:recyclerview:${Version.RECYCLER_VIEW}"
}

object FirebaseBomDependency {
    val FIREBASE_BOM = "com.google.firebase:firebase-bom:${Version.FIREBASE_BOM}"
    val FIREBASE_ANALYTICS = "com.google.firebase:firebase-analytics-ktx"
    val FIREBASE_CRASHLYTICS = "com.google.firebase:firebase-crashlytics"
    val FIREBASE_CONFIG = "com.google.firebase:firebase-config-ktx"
}

object GoogleDependency {
    val FIREBASE_ANALYTICS = "com.google.firebase:firebase-analytics-ktx:${Version.FIREBASE_ANALYTICS}"
    val FIREBASE_CRASHLYTICS = "com.google.firebase:firebase-crashlytics:${Version.FIREBASE_CRASHLYTICS}"
    val MATERIAL = "com.google.android.material:material:${Version.MATERIAL}"
    val GSON = "com.google.code.gson:gson:${Version.GSON}"
}

object KoinDependency {
    val KOIN_CORE = "org.koin:koin-core:${Version.KOIN}"
    val KOIN_SCOPE = "org.koin:koin-androidx-scope:${Version.KOIN}"
    val KOIN_VIEW_MODEL = "org.koin:koin-androidx-viewmodel:${Version.KOIN}"
}

object ArrowDependency {
    val ARROW_CORE = "io.arrow-kt:arrow-core:${Version.ARROW}"
    val ARROW_SYNTAX = "io.arrow-kt:arrow-syntax:${Version.ARROW}"
    val ARROW_META = "io.arrow-kt:arrow-meta:${Version.ARROW}"
}

object AmplitudeDependency {
    val AMPLITUDE = "com.amplitude:android-sdk:${Version.AMPLITUDE}"
    val OK_HTTP = "com.squareup.okhttp3:okhttp:${Version.OK_HTTP}"
}

object TestDependency {
    val J_UNIT = "junit:junit:${TestVersion.J_UNIT}"
    val MOCKK = "io.mockk:mockk:${TestVersion.MOCKK}"
    val ARCH_CORE_TESTING = "androidx.arch.core:core-testing:${TestVersion.ARCH_CORE}"
    val COROUTINES_TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${TestVersion.COROUTINES}"
}

object AndroidTestDependency {
    val J_UNIT_EXT = "androidx.test.ext:junit-ktx:${TestVersion.J_UNIT_EXT}"
    val TEST_RUNNER = "androidx.test:runner:${TestVersion.TEST_RUNNER}"
    val TEST_RULES = "androidx.test:rules:${TestVersion.TEST_RUNNER}"
    val KOIN_TEST = "org.koin:koin-test:${Version.KOIN}"
    val MOCKK_ANDROID = "io.mockk:mockk-android:${TestVersion.MOCKK}"
    val BARISTA = "com.schibsted.spain:barista:${TestVersion.BARISTA}"
    val ARCH_CORE_TESTING = "androidx.arch.core:core-testing:${TestVersion.ARCH_CORE_TEST}"
    val MOCK_WEB_SERVER = "com.squareup.okhttp3:mockwebserver:${TestVersion.MOCK_WEB_SERVER}"
    val IDLING_RESOURCE = "com.jakewharton.espresso:okhttp3-idling-resource:${TestVersion.IDLING_RESOURCE}"
}
