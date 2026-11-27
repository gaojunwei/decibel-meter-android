plugins { id("com.android.application") }
android {
    namespace = "com.example.decibelmeter.lite"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.decibelmeter.lite"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}
