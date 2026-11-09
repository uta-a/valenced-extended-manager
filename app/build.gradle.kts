import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

/** Valenced's Reseam patches public key. */
val patchesPublicKey = providers.gradleProperty("patchesPublicKey").orNull.orEmpty().also {
    require(it.matches(Regex("[0-9a-f]{64}"))) {
        "Set patchesPublicKey in gradle.properties to the hex public key Valenced patch bundle is signed with"
    }
}

android {
    namespace = "app.valenced.manager"
    compileSdk = 36

    defaultConfig {
        // valenced-extended: 上流の Manager と共存できるよう別の ID にする
        applicationId = "dev.uta.valencedextended.manager"
        minSdk = 28
        targetSdk = 34
        versionName = version.toString()
        // valenced-extended: `1.1.2-ext3` → 11203（上流の版の算出に ext の番号を足す）。旧形式の `1.1.2-ext.1` も読む
        versionCode = Regex("""^v?([0-9.]+)(?:-ext\.?([0-9]+))?""").find(versionName!!)!!.destructured.let { (base, ext) ->
            base.replace(".", "").toInt() * 100 + (ext.toIntOrNull() ?: 0)
        }

        buildConfigField("String", "MOD_NAME", "\"Valenced Extended\"")
        // valenced-extended: パッチバンドルとモジュールは fork のリリースから、Manager 自身の更新はこのリポジトリから取る
        buildConfigField("String", "REPO", "\"uta-a/valenced-extended\"")
        buildConfigField("String", "MANAGER_REPO", "\"uta-a/valenced-extended-manager\"")
        buildConfigField("String", "BUNDLE_PUBLIC_KEY", "\"$patchesPublicKey\"")
        buildConfigField("String", "ORG_LINK", "\"https://github.com/uta-a/valenced-extended\"")
        buildConfigField("String", "INVITE_LINK", "\"https://discord.com/invite/dtBTQHYSx4\"")
        buildConfigField("String", "MODDED_APP_PACKAGE_NAME", "\"dev.uta.valencedextended.discord\"")
        buildConfigField("int", "MODDED_APP_ICON", "0xFF37EBFF")
        buildConfigField("int", "MODDED_APP_ICON_ALPHA", "0xFFFBB33C")
        buildConfigField("int", "MODDED_APP_ICON_OTHER", "0xFFD3575E")

        buildConfigField("String", "GIT_BRANCH", "\"${getCurrentBranch()}\"")
        buildConfigField("String", "GIT_COMMIT", "\"${getLatestCommit()}\"")
        buildConfigField("boolean", "GIT_LOCAL_COMMITS", "${hasLocalCommits()}")
        buildConfigField("boolean", "GIT_LOCAL_CHANGES", "${hasLocalChanges()}")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isCrunchPngs = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            val keystoreFile = file("keystore.jks")
            signingConfig = if (keystoreFile.exists()) {
                 signingConfigs.create("release") {
                    storeFile = keystoreFile
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEYSTORE_ENTRY_ALIAS")
                    keyPassword = System.getenv("KEYSTORE_ENTRY_PASSWORD")
                }
            } else signingConfigs["debug"]
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidComponents {
        onVariants(selector().withBuildType("release")) {
            it.packaging.resources.excludes.apply {
                // Debug metadata
                add("/**/*.version")
                add("/kotlin-tooling-metadata.json")
                // Kotlin debugging (https://github.com/Kotlin/kotlinx.coroutines/issues/2274)
                add("/DebugProbesKt.bin")
            }
        }
    }

    packaging {
        resources {
            // Reflection symbol list (https://stackoverflow.com/a/41073782/13964629)
            excludes += "/**/*.kotlin_builtins"
        }
    }

}

base {
    archivesName = "${rootProject.name}-$version"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("report")
}

dependencies {
    implementation(platform(libs.compose.bom))

    implementation(libs.bundles.accompanist)
    implementation(libs.bundles.androidx)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.shizuku)
    implementation(libs.bundles.voyager)

    implementation(libs.aboutlibraries.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections)
    implementation(libs.reseam.sdk)
}

fun getCurrentBranch(): String? =
    exec("git", "symbolic-ref", "--short", "HEAD")

fun getLatestCommit(): String? =
    exec("git", "rev-parse", "--short", "HEAD")

fun hasLocalCommits(): Boolean {
    val branch = getCurrentBranch() ?: return false
    return exec("git", "log", "origin/$branch..HEAD")?.isNotBlank() ?: false
}

fun hasLocalChanges(): Boolean =
    exec("git", "status", "-s")?.isNotEmpty() ?: false

fun exec(vararg command: String): String? {
    return try {
        val result = providers.exec {
            commandLine = command.toList()
            isIgnoreExitValue = true
        }
        val errout = result.standardError.asText.get()

        if (errout.isNotEmpty())
            throw Error(errout)

        result.standardOutput.asText.get().trim()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}
