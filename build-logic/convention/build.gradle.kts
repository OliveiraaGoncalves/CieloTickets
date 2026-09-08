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
            implementationClass = "br.com.cielotickets.buildlogic.convention.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "cielotickets.android.application"
            implementationClass = "br.com.cielotickets.buildlogic.convention.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "cielotickets.android.compose"
            implementationClass = "br.com.cielotickets.buildlogic.convention.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "cielotickets.android.hilt"
            implementationClass = "br.com.cielotickets.buildlogic.convention.AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "cielotickets.android.feature"
            implementationClass = "br.com.cielotickets.buildlogic.convention.AndroidFeatureConventionPlugin"
        }
        register("androidTestJunit5") {
            id = "cielotickets.android.test.junit5"
            implementationClass = "br.com.cielotickets.buildlogic.convention.AndroidTestJunit5ConventionPlugin"
        }
    }
}
