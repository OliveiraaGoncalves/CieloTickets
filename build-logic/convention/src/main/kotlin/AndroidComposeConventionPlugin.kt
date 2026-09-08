package br.com.cielotickets.buildlogic.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

/**
 * Habilita Compose num módulo Android já configurado por
 * `cielotickets.android.library` ou `cielotickets.android.application`
 * (precisa ser aplicado depois de um dos dois no `plugins {}` do módulo).
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val commonExtension = extensions.findByType<ApplicationExtension>()
                ?: extensions.findByType<LibraryExtension>()
                ?: error(
                    "cielotickets.android.compose precisa ser aplicado depois de " +
                        "cielotickets.android.library ou cielotickets.android.application"
                )
            configureAndroidCompose(commonExtension)
        }
    }
}

private fun Project.configureAndroidCompose(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.buildFeatures.compose = true

    dependencies {
        add("implementation", platform(libs.findLibrary("compose-bom").get()))
        add("implementation", libs.findLibrary("compose-ui").get())
        add("implementation", libs.findLibrary("compose-ui-graphics").get())
        add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
        add("implementation", libs.findLibrary("compose-material3").get())
        add("implementation", libs.findLibrary("compose-material-icons-extended").get())
        add("implementation", libs.findLibrary("lifecycle-runtime-compose").get())
        add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
    }
}
