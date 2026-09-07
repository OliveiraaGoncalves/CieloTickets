# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Hilt, Room e o compilador do Compose já publicam suas próprias
# consumer-rules.pro dentro dos respectivos artefatos — não precisam de
# regra manual aqui.

# kotlinx.serialization: os modelos do checkout/callback da Cielo Smart
# (core-payment-cielo/deeplink/CieloDeeplinkModels.kt) são serializados via
# reflection no serializer gerado (`$serializer`/`Companion.serializer()`).
# Sem isso, o R8 pode remover/renomear esses membros silenciosamente — o
# app compila e instala normalmente, mas o payload que a Cielo Smart espera
# vem com nomes de campo errados, e o pagamento real quebra sem nenhum erro
# de compilação avisando. Regras recomendadas pelo próprio kotlinx.serialization:
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class br.com.cielotickets.**$$serializer { *; }
-keepclassmembers class br.com.cielotickets.** {
    *** Companion;
}
-keepclasseswithmembers class br.com.cielotickets.** {
    kotlinx.serialization.KSerializer serializer(...);
}
