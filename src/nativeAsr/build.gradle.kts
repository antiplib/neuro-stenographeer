plugins { id("com.android.library") }
android {
 namespace = "ru.nuclear.nativeasr"
 sourceSets.getByName("main").jniLibs.srcDir("../vendor/sherpa/jniLibs")
 compileSdk = 35
 buildToolsVersion = "35.0.0"
 ndkVersion = "27.2.12479018"
 defaultConfig { minSdk = 26; ndk { abiFilters += "arm64-v8a" }; externalNativeBuild { cmake { arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DANDROID_STL=c++_static"); cppFlags += "-std=c++17" } } }
 externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
}
