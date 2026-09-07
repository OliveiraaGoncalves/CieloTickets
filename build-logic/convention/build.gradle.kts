plugins {
    `kotlin-dsl`
}

group = "br.com.cielotickets.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(blLibs.android.gradlePlugin)
    compileOnly(blLibs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "cielotickets.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "cielotickets.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "cielotickets.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "cielotickets.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "cielotickets.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidTestJunit5") {
            id = "cielotickets.android.test.junit5"
            implementationClass = "AndroidTestJunit5ConventionPlugin"
        }
    }
}
