// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}
buildscript {

    dependencies {

        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.8") //  Укажи ту же версию, что и navigation-fragment-ktx
    }
}