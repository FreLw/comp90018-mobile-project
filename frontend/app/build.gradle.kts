plugins {
	id("com.android.application")
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
		// For the emulator, adb reverse maps its localhost:8080 to this computer.
		buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:8080/\"")
	}

	buildFeatures {
		buildConfig = true
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
	implementation(composeBom)
	androidTestImplementation(composeBom)

	implementation("androidx.activity:activity-compose:1.13.0")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
	debugImplementation("androidx.compose.ui:ui-tooling")
}
