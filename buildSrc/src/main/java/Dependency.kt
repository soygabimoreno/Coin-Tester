object Version {
    const val KOTLIN = "1.4.10"
    const val FIREBASE_BOM = "25.12.0"

    const val CORE_KTX = "1.3.1"
    const val COROUTINES = "1.3.9"
    const val APP_COMPAT = "1.2.0"
    const val CONSTRAINT_LAYOUT = "2.0.1"
    const val RECYCLER_VIEW = "1.2.0-alpha02"
    const val LIFECYCLE = "2.2.0"
    const val ACTIVITY_API = "1.2.0-beta01"
    const val FRAGMENT_API = "1.3.0-beta01"
    const val FIREBASE_ANALYTICS = "17.5.0"
    const val FIREBASE_CRASHLYTICS = "17.2.1"
    const val MATERIAL = "1.2.1"
    const val GSON = "2.8.5"
    const val KOIN = "2.2.0-rc-1"
    const val ARROW = "0.10.5"
    const val AMPLITUDE = "2.25.2"
    const val OK_HTTP = "4.8.1"
}

object TestVersion {
    const val J_UNIT = "4.13"
    const val MOCKK = "1.10.0"
    const val ARCH_CORE = "2.1.0"
    const val COROUTINES = "1.3.9"

    const val J_UNIT_EXT = "1.1.2"
    const val TEST_RUNNER = "1.3.0"
    const val BARISTA = "3.6.0"
    const val ARCH_CORE_TEST = "2.1.0"
    const val MOCK_WEB_SERVER = "4.6.0"
    const val IDLING_RESOURCE = "1.0.0"
}

object ModulesDependency {
    const val CORE = ":core"
    const val CORE_INFRASTRUCTURE = ":coreInfrastructure"
    const val CORE_DOMAIN = ":coreDomain"
    const val CORE_ANALYTICS = ":coreAnalytics"
    const val CORE_DATA = ":coreData"

    const val CORE_NETWORK = ":coreNetwork"
    const val CORE_DB = ":coreDb"

    const val LIB_FRAMEWORK = ":libFramework"
    const val LIB_BASE = ":libBase"
    const val LIB_PLAYER = ":libPlayer"
    const val LIB_IMAGE_LOADER = ":libImageLoader"
    const val LIB_PROCESSING = ":libProcessing"
    const val LIB_PROCESSING_ANDROID = ":libProcessingAndroid"

    const val APP = ":app"
}

object KotlinDependency {
    const val KOTLIN = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.KOTLIN}"
    const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.COROUTINES}"
}

object AndroidDependency {
    const val CORE_KTX = "androidx.core:core-ktx:${Version.CORE_KTX}"
    const val APP_COMPAT = "androidx.appcompat:appcompat:${Version.APP_COMPAT}"
    const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:${Version.CONSTRAINT_LAYOUT}"
    const val RECYCLER_VIEW = "androidx.recyclerview:recyclerview:${Version.RECYCLER_VIEW}"
    const val LIFECYCLE_VIEW_MODEL = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Version.LIFECYCLE}"
    const val LIFECYCLE_RUNTIME = "androidx.lifecycle:lifecycle-runtime-ktx:${Version.LIFECYCLE}"
    const val ACTIVITY_API = "androidx.activity:activity-ktx:${Version.ACTIVITY_API}"
    const val FRAGMENT_API = "androidx.fragment:fragment-ktx:${Version.FRAGMENT_API}"
}

object FirebaseBomDependency {
    const val FIREBASE_BOM = "com.google.firebase:firebase-bom:${Version.FIREBASE_BOM}"
    const val FIREBASE_ANALYTICS = "com.google.firebase:firebase-analytics-ktx"
    const val FIREBASE_CRASHLYTICS = "com.google.firebase:firebase-crashlytics"
    const val FIREBASE_CONFIG = "com.google.firebase:firebase-config-ktx"
}

object GoogleDependency {
    const val FIREBASE_ANALYTICS = "com.google.firebase:firebase-analytics-ktx:${Version.FIREBASE_ANALYTICS}"
    const val FIREBASE_CRASHLYTICS = "com.google.firebase:firebase-crashlytics:${Version.FIREBASE_CRASHLYTICS}"
    const val MATERIAL = "com.google.android.material:material:${Version.MATERIAL}"
    const val GSON = "com.google.code.gson:gson:${Version.GSON}"
}

object KoinDependency {
    const val KOIN_CORE = "org.koin:koin-core:${Version.KOIN}"
    const val KOIN_SCOPE = "org.koin:koin-androidx-scope:${Version.KOIN}"
    const val KOIN_VIEW_MODEL = "org.koin:koin-androidx-viewmodel:${Version.KOIN}"
}

object ArrowDependency {
    const val ARROW_CORE = "io.arrow-kt:arrow-core:${Version.ARROW}"
    const val ARROW_SYNTAX = "io.arrow-kt:arrow-syntax:${Version.ARROW}"
    const val ARROW_META = "io.arrow-kt:arrow-meta:${Version.ARROW}"
}

object AmplitudeDependency {
    const val AMPLITUDE = "com.amplitude:android-sdk:${Version.AMPLITUDE}"
    const val OK_HTTP = "com.squareup.okhttp3:okhttp:${Version.OK_HTTP}"
}

object TestDependency {
    const val J_UNIT = "junit:junit:${TestVersion.J_UNIT}"
    const val MOCKK = "io.mockk:mockk:${TestVersion.MOCKK}"
    const val ARCH_CORE_TESTING = "androidx.arch.core:core-testing:${TestVersion.ARCH_CORE}"
    const val COROUTINES_TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${TestVersion.COROUTINES}"
}

object AndroidTestDependency {
    const val J_UNIT_EXT = "androidx.test.ext:junit-ktx:${TestVersion.J_UNIT_EXT}"
    const val TEST_RUNNER = "androidx.test:runner:${TestVersion.TEST_RUNNER}"
    const val TEST_RULES = "androidx.test:rules:${TestVersion.TEST_RUNNER}"
    const val KOIN_TEST = "org.koin:koin-test:${Version.KOIN}"
    const val MOCKK_ANDROID = "io.mockk:mockk-android:${TestVersion.MOCKK}"
    const val BARISTA = "com.schibsted.spain:barista:${TestVersion.BARISTA}"
    const val ARCH_CORE_TESTING = "androidx.arch.core:core-testing:${TestVersion.ARCH_CORE_TEST}"
    const val MOCK_WEB_SERVER = "com.squareup.okhttp3:mockwebserver:${TestVersion.MOCK_WEB_SERVER}"
    const val IDLING_RESOURCE = "com.jakewharton.espresso:okhttp3-idling-resource:${TestVersion.IDLING_RESOURCE}"
}
