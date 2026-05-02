plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

val externalBuildRoot = providers.gradleProperty("bilitvBuildRoot")
  .orElse("${System.getProperty("user.home")}/.gradle/bilitv-native-build")
  .get()

layout.buildDirectory.set(file("$externalBuildRoot/root"))

subprojects {
  val projectBuildName = path
    .removePrefix(":")
    .replace(':', '-')
    .ifBlank { "root" }
  layout.buildDirectory.set(file("$externalBuildRoot/$projectBuildName"))
}
