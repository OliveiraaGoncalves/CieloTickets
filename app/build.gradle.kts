import java.util.Properties

plugins {
    alias(libs.plugins.cielotickets.android.application)
    alias(libs.plugins.cielotickets.android.compose)
    alias(libs.plugins.cielotickets.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    defaultConfig {
        applicationId = "br.com.cielotickets.app"
        versionCode = 1
        versionName = "1.0"
        // HiltTestRunner troca a Application por HiltTestApplication — sem
        // isso, os módulos @TestInstallIn (payment gateway/Room fake) nunca
        // entram no grafo e o teste tentaria abrir a Cielo Smart de verdade.
        testInstrumentationRunner = "br.com.cielotickets.app.HiltTestRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "keystore.properties não encontrado — release assinado com a chave de " +
                        "debug só pra compilar localmente. NÃO publique esse artefato; " +
                        "configure um keystore de verdade antes (docs/ARCHITECTURE.md#deploy)."
                )
                signingConfigs.getByName("debug")
            }
        }
    }

    lint {
        // `checkDependencies` faz o lint do :app analisar todos os módulos
        // core/feature juntos (senão cada módulo roda lint isolado e perde
        // problema entre módulos, tipo recurso duplicado/não usado).
        checkDependencies = true
        htmlReport = true
        xmlReport = true
        checkAllWarnings = true
        // "Typos" usa dicionário em inglês; nosso conteúdo é 100% pt-BR
        // (ex: "momento" acusado como erro de digitação de "memento"). Pra
        // um app sem i18n, esse check só gera falso positivo — desliga.
        disable += "Typos"
        // GradleDependency/NewerVersionAvailable batem o Maven Central pra
        // avisar de versão mais nova — mas agp/kotlin/ksp/hilt/composeBom/
        // room formam um grupo deliberadamente preso numa versão mais antiga
        // (upgrade é esforço à parte, com bateria de testes própria — ver
        // comentário no libs.versions.toml e docs/ARCHITECTURE.md). Manter
        // esses achados só no baseline não é estável: a checagem de rede é
        // best-effort (timeout vira "não achei nada"), então cada rodada em
        // ambiente com conectividade instável resolve um subconjunto
        // diferente de dependências, gerando achado "novo" fora do baseline
        // toda hora sem nenhuma mudança de versão real ter acontecido.
        disable += "GradleDependency"
        disable += "NewerVersionAvailable"
        // Achados aceitos conscientemente ficam no baseline. Qualquer achado
        // NOVO fora do baseline quebra o build de verdade.
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = true
    }
}

dependencies {
    implementation(projects.core.coreCommon)
    implementation(projects.core.coreLocalStorage)
    implementation(projects.core.corePaymentCielo)
    implementation(projects.core.coreDesignsystem)

    implementation(projects.feature.featureHome)
    implementation(projects.feature.featureTicketSelection)
    implementation(projects.feature.featurePayment)
    implementation(projects.feature.featureReceipt)
    implementation(projects.feature.featureHistory)

    // Rotas type-safe do NavHost (`navigation/CieloRoutes.kt`) usam
    // `@Serializable` — é assim que a Navigation-Compose 2.8 codifica os
    // argumentos de rota no SavedStateHandle.
    implementation(libs.kotlinx.serialization.json)

    // androidTest não herda `implementation` do main source set — precisa
    // dos módulos de novo pra compilar os fakes de DI e os asserts de UI.
    androidTestImplementation(projects.core.coreCommon)
    androidTestImplementation(projects.core.coreLocalStorage)
    androidTestImplementation(projects.core.corePaymentCielo)
    androidTestImplementation(projects.core.coreDesignsystem)
    androidTestImplementation(projects.feature.featureHome)
    androidTestImplementation(projects.feature.featurePayment)
    androidTestImplementation(projects.feature.featureTicketSelection)
    androidTestImplementation(projects.feature.featureReceipt)
    androidTestImplementation(projects.feature.featureHistory)
    androidTestImplementation(projects.core.coreNetwork)

    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    // `implementation` no core-local-storage não propaga pro classpath de
    // compilação do androidTest — precisa declarar nome a nome de novo.
    androidTestImplementation(libs.room.runtime)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
}
