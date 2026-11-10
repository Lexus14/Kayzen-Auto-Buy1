plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "id.kayzen.autobuy"
    compileSdk = 35
    defaultConfig {
        applicationId = "id.kayzen.autobuy"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.2"
    }
}
