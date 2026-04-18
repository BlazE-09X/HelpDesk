import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "firstapp.helpdesk"
    compileSdk = 36

    defaultConfig {
        applicationId = "firstapp.helpdesk"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Чтение секретов из local.properties
        val properties = Properties()
        val propertiesFile = project.rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${properties.getProperty("CLOUDINARY_CLOUD_NAME") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${properties.getProperty("CLOUDINARY_API_KEY") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${properties.getProperty("CLOUDINARY_API_SECRET") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.cloudinary.android)
    implementation(libs.glide)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.firebaseui:firebase-ui-database:8.0.2")
    implementation("androidx.annotation:annotation:1.7.1")
}
