plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.timphongtro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.timphongtro"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures{
        viewBinding = true;
    }
}

dependencies {
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0-rc01")
    implementation ("androidx.credentials:credentials:1.5.0-rc01")
    implementation ("androidx.credentials:credentials-play-services-api:1.5.0-rc01")
    implementation("org.jetbrains:annotations:23.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.firebase:firebase-auth:23.2.0")
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-analytics:22.2.0")
    implementation("com.google.firebase:firebase-firestore:25.1.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.activity:activity:1.10.0")
    implementation ("com.github.denzcoskun:ImageSlideshow:0.1.2") // Auto Image Slider
    implementation ("androidx.cardview:cardview:1.0.0") // Cardview
    implementation ("com.github.bumptech.glide:glide:4.16.0") //Load ảnh từ firebase về
    implementation ("com.google.firebase:firebase-storage:21.0.1")
    implementation ("com.google.code.gson:gson:2.10.1") // ép String về JSON
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
}