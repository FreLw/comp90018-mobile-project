// Builds the Android/Compose client and selects its UI, sensor, Firebase, and test dependencies.
import java.util.Properties

plugins {
	id("com.android.application")
	id("com.google.gms.google-services")
	id("org.jetbrains.kotlin.plugin.compose")
}

// Load local map configuration for the manifest without embedding credentials in UI source.
val localProperties = Properties().apply {
	val localPropertiesFile = rootProject.file("local.properties")
	if (localPropertiesFile.exists()) {
		localPropertiesFile.inputStream().use(::load)
	}
}

val mapsApiKey = providers.gradleProperty("MAPS_API_KEY")
	.orElse(localProperties.getProperty("MAPS_API_KEY", ""))

android {
	namespace = "com.comp90018.app"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.comp90018.app"
		minSdk = 26
		targetSdk = 37
		versionCode = 1
		versionName = "1.0"
		manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey.get()

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	// Enable Compose rendering and BuildConfig.DEBUG guards used by development controls.
	buildFeatures {
		compose = true
		buildConfig = true
	}

	// Android selects src/debug or src/release alongside src/main for each build variant.
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
	testImplementation("junit:junit:4.13.2")
	// BOMs align versions within the Compose and Firebase dependency families.
	val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
	val firebaseBom = platform("com.google.firebase:firebase-bom:34.18.0")
	val cameraxVersion = "1.4.1"
	implementation(composeBom)
	androidTestImplementation(composeBom)
	implementation(firebaseBom)

	// Activity hosting and camera preview/capture used by the task interfaces.
	implementation("androidx.activity:activity-compose:1.13.0")
	implementation("androidx.camera:camera-core:$cameraxVersion")
	implementation("androidx.camera:camera-camera2:$cameraxVersion")
	implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
	implementation("androidx.camera:camera-view:$cameraxVersion")
	// Camera analysis, device location, and the embedded Google Maps view.
	implementation("com.google.mlkit:barcode-scanning:17.3.0")
	implementation("com.google.android.gms:play-services-location:21.4.0")
	implementation("com.google.android.gms:play-services-maps:20.0.0")
	// Lifecycle-aware state collection, ViewModels, Material components, and Compose rendering.
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.material:material-icons-extended")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	// Client-side adapters use these SDKs to load/store the data observed by screen ViewModels.
	implementation("com.google.firebase:firebase-auth")
	implementation("com.google.firebase:firebase-firestore")
	implementation("com.google.firebase:firebase-storage")
	// Device/Compose test support; tooling dependencies are available only in debug builds.
	androidTestImplementation("androidx.test.ext:junit:1.3.0")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")
}
