object Version {
    val KOTLIN = "1.4.10"

    val CORE_KTX = "1.3.1"
    val APP_COMPAT = "1.2.0"
    val CONSTRAINT_LAYOUT = "2.0.1"
    val RECYCLER_VIEW = "1.2.0-alpha02"
    val FIREBASE_ANALYTICS = "17.5.0"
    val FIREBASE_CRASHLYTICS = "17.2.1"
    val MATERIAL = "1.2.1"
    val GSON = "2.8.5"
    val KOIN = "2.2.0-rc-1"
    val ARROW = "0.10.5"
}

object TestVersion {
    val J_UNIT = "4.13"
    val MOCKK = "1.10.0"

    val J_UNIT_EXT = "1.1.2"
    val TEST_RUNNER = "1.3.0"
    val BARISTA = "3.6.0"
    val ARCH_CORE_TEST = "2.1.0"
    val MOCK_WEB_SERVER = "4.6.0"
    val IDLING_RESOURCE = "1.0.0"
}

object ModulesDependency {
    val CORE_DOMAIN = ":coreDomain"
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
}

object AndroidDependency {
    val CORE_KTX = "androidx.core:core-ktx:${Version.CORE_KTX}"
    val APP_COMPAT = "androidx.appcompat:appcompat:${Version.APP_COMPAT}"
    val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:${Version.CONSTRAINT_LAYOUT}"
    val RECYCLER_VIEW = "androidx.recyclerview:recyclerview:${Version.RECYCLER_VIEW}"
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

object TestDependency {
    val J_UNIT = "junit:junit:${TestVersion.J_UNIT}"
    val MOCKK = "io.mockk:mockk:${TestVersion.MOCKK}"
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