plugins {
	id("com.android.application")
	id("com.google.gms.google-services")
	id("org.jetbrains.kotlin.plugin.compose")
}

android {
	namespace = "com.comp90018.app"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.comp90018.app"
		minSdk = 26
		targetSdk = 37
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildFeatures {
		compose = true
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro",
			)
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}
}

dependencies {
	val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
	val firebaseBom = platform("com.google.firebase:firebase-bom:34.18.0")
	implementation(composeBom)
	androidTestImplementation(composeBom)
	implementation(firebaseBom)

	implementation("androidx.activity:activity-compose:1.13.0")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.material:material-icons-extended")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("com.google.firebase:firebase-auth")
	implementation("com.google.firebase:firebase-firestore")
	implementation("com.google.firebase:firebase-storage")
	debugImplementation("androidx.compose.ui:ui-tooling")
}
