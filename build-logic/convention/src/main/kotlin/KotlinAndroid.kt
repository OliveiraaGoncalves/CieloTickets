package br.com.cielotickets.buildlogic.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Config comum a todo módulo Android do projeto (aplicada por library/application). */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    // Kover precisa do plugin em CADA módulo Android (não só na raiz) pra
    // expor a variante certa (debug/release) pro relatório agregado —
    // aplicar aqui evita repetir em todo build.gradle.kts. A agregação em
    // si (`kover(project(...))`) mora só no build.gradle.kts raiz.
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    commonExtension.apply {
        namespace = deriveNamespace()
        compileSdk = 34

        defaultConfig {
            minSdk = 26
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

/**
 * Deriva o namespace do path do módulo Gradle, sem precisar repetir
 * `namespace = "br.com.cielotickets.x.y"` em cada `build.gradle.kts`:
 * `:core:core-local-storage` -> `br.com.cielotickets.core.localstorage`,
 * `:feature:feature-ticket-selection` -> `br.com.cielotickets.feature.ticketselection`,
 * `:app` -> `br.com.cielotickets.app`. Um módulo com nome fora desse padrão
 * ainda pode sobrescrever definindo `namespace = "..."` no próprio
 * `build.gradle.kts` — a atribuição do script roda depois da do plugin.
 */
private fun Project.deriveNamespace(): String {
    val segments = path.removePrefix(":").split(":")
    val cleaned = segments.mapIndexed { index, segment ->
        val parent = segments.getOrNull(index - 1)
        val stripped = if (parent != null && segment.startsWith("$parent-")) {
            segment.removePrefix("$parent-")
        } else {
            segment
        }
        stripped.replace("-", "")
    }
    return "br.com.cielotickets." + cleaned.joinToString(".")
}
