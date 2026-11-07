import java.security.MessageDigest
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "ru.nuclear.stenographer"
 compileSdk = 35
 buildToolsVersion = "35.0.0"
 androidResources { noCompress += listOf("bin", "gguf", "onnx") }
 defaultConfig { applicationId = "ru.nuclear.stenographer"; minSdk = 26; targetSdk = 35; versionCode = 8; versionName = "0.8"; ndk { abiFilters += "arm64-v8a" } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation(project(":nativeAsr")); implementation(project(":nativeLlm")) }
val verifyEmbeddedModels by tasks.registering {
 doLast {
  val expected = mapOf(
   "asr.bin" to "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
   "llm.gguf" to "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db",
   "segmentation.onnx" to "220ad67ca923bef2fa91f2390c786097bf305bceb5e261d4af67b38e938e1079",
   "embedding.onnx" to "ad4a1802485d8b34c722d2a9d04249662f2ece5d28a7a039063ca22f515a789e"
  )
  expected.forEach { (name, sha) ->
   val model = file("src/main/assets/models/$name")
   check(model.isFile) { "Embedded model missing: $name. Run scripts/fetch-models.sh" }
   val digest = MessageDigest.getInstance("SHA-256")
   model.inputStream().use { stream ->
    val buffer = ByteArray(1024 * 1024)
    while (true) { val n = stream.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
   }
   val actual = digest.digest().joinToString("") { "%02x".format(it) }
   check(actual == sha) { "Embedded model checksum mismatch: $name" }
  }
 }
}
tasks.named("preBuild") { dependsOn(verifyEmbeddedModels) }
