plugins {
    id("com.android.library")
    alias(libs.plugins.kotlinAndroid)
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.rabta.contacts"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "APPLICATION_ID", "\"org.rabta.phone.classic.debug\"")
        buildConfigField("String", "VERSION_NAME", "\"${project.property("VERSION_NAME")}\"")
        buildConfigField("int", "VERSION_CODE", project.property("VERSION_CODE").toString())
    }
    buildFeatures { viewBinding = true; buildConfig = true }
    sourceSets.getByName("main").java.srcDirs("src/main/kotlin")
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        lintConfig = rootProject.file("lint.xml")
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(libs.commons.library)
    implementation(libs.indicator.fast.scroll)
    implementation(libs.autofit.text.view)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation("com.googlecode.ez-vcard:ez-vcard:0.12.2")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    testImplementation("junit:junit:4.13.2")
}
