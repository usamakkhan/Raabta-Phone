plugins {
    id("com.android.library") version "8.11.1" apply false
    id("com.google.devtools.ksp") version "2.3.0" apply false
    alias(libs.plugins.android).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.detekt).apply(false)
}
