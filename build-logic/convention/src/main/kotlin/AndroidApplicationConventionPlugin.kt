import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 34
            }

            // Esqueleto padrão de app Android — só existe um módulo `application`
            // (:app), então isso fica aqui em vez de num plugin à parte.
            dependencies {
                add("implementation", libs.findLibrary("core-ktx").get())
                add("implementation", libs.findLibrary("lifecycle-runtime-ktx").get())
                add("implementation", libs.findLibrary("activity-compose").get())
                add("implementation", libs.findLibrary("navigation-compose").get())
            }
        }
    }
}
