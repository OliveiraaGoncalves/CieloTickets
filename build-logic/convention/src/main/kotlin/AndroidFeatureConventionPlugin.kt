import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Agrega o que toda `feature:*` com ViewModel via Hilt/Compose precisa:
 * library + compose + hilt + navegação de ViewModel + os dois módulos
 * `core` que todas elas compartilham.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("cielotickets.android.library")
                apply("cielotickets.android.compose")
                apply("cielotickets.android.hilt")
            }

            dependencies {
                add("implementation", project(":core:core-common"))
                add("implementation", project(":core:core-designsystem"))
                add("implementation", libs.findLibrary("lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())
                add("ksp", libs.findLibrary("hilt-compiler-androidx").get())
                // `SavedStateHandle.toRoute<T>()` — toda feature com rota
                // tipada (ver `navigation/*Route.kt` de cada uma) precisa
                // disso pra reconstruir seus argumentos após process death.
                add("implementation", libs.findLibrary("navigation-compose").get())
            }
        }
    }
}
