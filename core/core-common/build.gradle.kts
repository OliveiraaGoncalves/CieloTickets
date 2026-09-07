plugins {
    alias(libs.plugins.cielotickets.android.library)
}

android {
    namespace = "br.com.cielotickets.core.common"
}

// `libs.xxx.yyy` tipado não fica disponível num módulo que também aplica
// um plugin de convenção vindo do `build-logic` (includeBuild) — o método
// genérico `findLibrary("nome-no-toml")` do mesmo `libs` continua ok.
dependencies {
    implementation(libs.findLibrary("kotlinx-coroutines-android").get())
    implementation(libs.findLibrary("androidx-annotation").get())
}
