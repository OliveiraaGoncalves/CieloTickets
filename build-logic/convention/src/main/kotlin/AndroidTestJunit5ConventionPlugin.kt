import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

class AndroidTestJunit5ConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libraryExtension = extensions.findByType<LibraryExtension>()
                ?: error("cielotickets.android.test.junit5 precisa ser aplicado depois de cielotickets.android.library")
            libraryExtension.testOptions.unitTests.all { it.useJUnitPlatform() }

            dependencies {
                add("testImplementation", libs.findLibrary("junit-jupiter-api").get())
                add("testRuntimeOnly", libs.findLibrary("junit-jupiter-engine").get())
                add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
                add("testImplementation", libs.findLibrary("mockk").get())
                add("testImplementation", libs.findLibrary("turbine").get())
                add("testImplementation", libs.findLibrary("coroutines-test").get())
            }
        }
    }
}
