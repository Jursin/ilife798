import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.spotless) apply false
}

// 统一 Kotlin 代码格式；规则见 .editorconfig 与下方 ktlint 配置。格式化为 ./gradlew spotlessApply，检查为 spotlessCheck
val ktlintVersion = "1.8.0"

subprojects {
    pluginManager.withPlugin("com.diffplug.spotless") {
        extensions.configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**")
                ktlint(ktlintVersion).editorConfigOverride(
                    mapOf(
                        "ktlint_code_style" to "ktlint_official",
                        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                        "ktlint_standard_filename" to "disabled",
                    ),
                )
            }
        }
    }
}
