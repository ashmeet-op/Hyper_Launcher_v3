@file:Suppress("AvoidApplyPluginMethod")

import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application") version "9.3.2"
    id("de.undercouch.download") version "5.7.0"
    id("org.jetbrains.kotlin.android") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"
}

apply(from = rootProject.file("gradle/prefab_bypass.gradle"))

val localProperties = Properties()
val localPropertiesFile = project.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

fun getCFApiKey(): String {
    val key = System.getenv("CURSEFORGE_API_KEY")
    if (key != null) return key
    val curseforgeKeyFile = File("./curseforge_key.txt")
    if (curseforgeKeyFile.canRead() && curseforgeKeyFile.isFile) {
        return curseforgeKeyFile.readText().trim()
    }
    return "DUMMY"
}

configurations {
    create("instrumentedClasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}

val hyperVersionNumber = localProperties.getProperty("VERSION_NUMBER")
    ?: (project.findProperty("VERSION_NUMBER")?.toString() ?: "4.1.3")
val hyperVersionSuffix = localProperties.getProperty("VERSION_NAME")
    ?: (project.findProperty("VERSION_NAME")?.toString() ?: "mercury")

configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "net.ashmeet.hyperlauncher"

    compileSdk = 37

    lint {
        abortOnError = false
    }

    signingConfigs {
        create("customDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "net.ashmeet.hyperlauncher"
        minSdk = 24
        targetSdk = 37

        val propVersionCode = localProperties.getProperty("VERSION_CODE")?.toIntOrNull()
            ?: project.findProperty("VERSION_CODE")?.toString()?.toIntOrNull()
            ?: localProperties.getProperty("hyperversioncode")?.toIntOrNull()
            ?: 5
        versionCode = propVersionCode

        versionName = "$hyperVersionNumber-$hyperVersionSuffix"
        multiDexEnabled = false
        resValue("string", "curseforge_api_key", getCFApiKey())
        resValue("string", "group_id", "git.artdeell")

        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a"))
        }

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                arguments("-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384")
            }
        }
    }

    flavorDimensions.add("runtime")

    productFlavors {
        create("full") {
            dimension = "runtime"
        }

        create("noruntime") {
            dimension = "runtime"
            versionNameSuffix = "-noruntime"
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            //noinspection NotShrinkingResources
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("customDebug")
            resValue("string", "application_package", "net.ashmeet.hyperlauncher.debug")
            resValue("string", "storageProviderAuthorities", "net.ashmeet.hyperlauncher.scoped.gamefolder.debug")
        }
        create("proguard") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += listOf("debug")
        }
        create("proguardNoDebug") {
            initWith(getByName("proguard"))
            isDebuggable = false
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "storageProviderAuthorities", "net.ashmeet.hyperlauncher.scoped.gamefolder")
            resValue("string", "application_package", "net.ashmeet.hyperlauncher")
        }

        create("gplay") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }

    ndkVersion = "29.0.14206865"

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    bundle {
        language {
            @Suppress("UnstableApiUsage")
            enableSplit = false
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            pickFirsts.add("**/libbytehook.so")
            pickFirsts.add("META-INF/INDEX.LIST")
            pickFirsts.add("META-INF/DEPENDENCIES")
            pickFirsts.add("META-INF/LICENSE")
            pickFirsts.add("META-INF/NOTICE")
            pickFirsts.add("META-INF/LICENSE.txt")
            pickFirsts.add("META-INF/NOTICE.txt")
            pickFirsts.add("META-INF/ASL2.0")
        }
    }

    buildFeatures {
        buildConfig = true
        prefab = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        )
    }
}

class AssetTaskRegistrar(private val project: Project) {
    private lateinit var preBuildTask: TaskProvider<Task>
    private lateinit var targetAssetsDir: File
    private lateinit var variantName: String
    private val targetDownloadDir: File = File(project.projectDir, "build/intermediates/download_deps")
    private val projectJarDir: File = File(project.projectDir, "build/intermediates/package_jar_deps")

    init {
        createDirectories(targetDownloadDir)
        createDirectories(projectJarDir)
    }

    @Suppress("DEPRECATION")
    fun setVariant(variant: com.android.build.gradle.api.BaseVariant) {
        variantName = variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        preBuildTask = variant.preBuildProvider

        targetAssetsDir = File(project.projectDir, "build/intermediates/remote_asset_set/${variantName.lowercase()}/")
        createDirectories(targetAssetsDir)

        val androidExtension = project.extensions.getByType(com.android.build.api.dsl.ApplicationExtension::class.java)
        androidExtension.sourceSets.getByName(variant.name).assets.srcDirs(targetAssetsDir.absolutePath)
    }

    private fun createDirectories(location: File) {
        if (!location.isDirectory && !location.mkdirs()) {
            throw RuntimeException("Failed to create directory " + location.absolutePath)
        }
    }

    private fun writeVersion(dir: File) {
        File(dir, "version").writeText(System.currentTimeMillis().toString())
    }

    private fun assetDestination(relativePath: String): File {
        return File(targetAssetsDir, relativePath)
    }

    private fun onlineUnzipTask(downloadUrl: String, name: String, targetExtractionDir: File): TaskProvider<Copy> {
        createDirectories(targetExtractionDir)

        val downloadTarget = File(targetDownloadDir, name)
        val dependencySuffix = "$name$variantName"

        val downloadTask = project.tasks.register<Download>("download$dependencySuffix") {
            src(downloadUrl)
            dest(downloadTarget)
            overwrite(false)
        }

        return project.tasks.register<Copy>("unzip$dependencySuffix") {
            from(project.zipTree(downloadTarget))
            into(targetExtractionDir)
            dependsOn(downloadTask)
        }
    }

    fun onlineZipDependency(downloadUrl: String, name: String, relativePath: String) {
        val unzipTask = onlineUnzipTask(downloadUrl, name, assetDestination(relativePath))
        preBuildTask.configure {
            dependsOn(unzipTask)
        }
    }

    fun projectJarDependency(targetProject: Project, relativePath: String) {
        val assetTargetDir = assetDestination(relativePath)
        val jarTargetDir = File(projectJarDir, relativePath)
        createDirectories(assetTargetDir)
        createDirectories(jarTargetDir)

        val copyTaskName = "copyProjectJar${jarTargetDir.name}$variantName"
        val copyTask = project.tasks.register<Copy>(copyTaskName) {
            from(jarTargetDir)
            into(assetTargetDir)
            dependsOn(":${targetProject.name}:jar")
        }

        preBuildTask.configure {
            dependsOn(copyTask)
        }

        targetProject.tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
            destinationDirectory.set(jarTargetDir)
            doLast {
                if (!didWork) return@doLast
                writeVersion(jarTargetDir)
            }
        }
    }

    fun onlineJarDependency(jarUrl: String, jarProject: String, dependency: String, version: String, suffix: String, relativePath: String) {
        val targetDir = assetDestination(relativePath)
        createDirectories(targetDir)
        val dependencySuffix = "$jarProject$dependency$variantName"
        val downloadTask = project.tasks.register<Download>("download$dependencySuffix") {
            src(String.format(jarUrl, jarProject, dependency, version, suffix))
            dest(targetDir)
            overwrite(false)
            doLast {
                if (!didWork) return@doLast
                writeVersion(targetDir)
            }
        }
        preBuildTask.configure {
            dependsOn(downloadTask)
        }
    }

    fun jreRuntimeDependency(version: Int, relPath: String) {
        val jreUrl = "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre%d-pojav.zip"
        onlineZipDependency(String.format(jreUrl, version), "ComponentRuntime$version", relPath)
    }
}

val registrar = AssetTaskRegistrar(project)
val androidExtension = project.extensions.getByType(com.android.build.gradle.AppExtension::class.java)

androidExtension.applicationVariants.configureEach {
    registrar.setVariant(this)

    registrar.projectJarDependency(project(":forge_installer"), "components/forge_installer")
    registrar.projectJarDependency(project(":MioLibPatcher"), "components/MioLibPatcher")

    if (name.lowercase().contains("full")) {
        registrar.jreRuntimeDependency(8, "components/jre")
    }

    val cacioJarUrl = "https://jitpack.io/com/github/MojoLauncher/%1\$s/%2\$s/%3\$s/%2\$s-%3\$s%4\$s.jar"

    registrar.onlineJarDependency(cacioJarUrl, "caciocavallo", "cacio-shared", "-01d2dc1d65-1", "", "components/caciocavallo")
    registrar.onlineJarDependency(cacioJarUrl, "caciocavallo", "cacio-androidnw", "-01d2dc1d65-1", "", "components/caciocavallo")

    // FIX: Replaced "cacio17" with "caciocavallo17" to match Groovy
    registrar.onlineJarDependency(cacioJarUrl, "caciocavallo17", "cacio-tta", "72a9ab6323", "-jar-with-dependencies", "components/caciocavallo17")
}

dependencies {
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("commons-codec:commons-codec:1.22.1")

    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.activity:activity:1.13.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.annotation:annotation:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")

    implementation("com.github.duanhong169:checkerboarddrawable:1.0.2")
    implementation("com.github.PojavLauncherTeam:portrait-sdp:ed33e89cbc")
    implementation("com.github.PojavLauncherTeam:portrait-ssp:6c02fd739b")
    implementation("com.github.Mathias-Boulay:ExtendedView:1.0.0")
    implementation("com.github.Mathias-Boulay:android_gamepad_remapper:2.0.3")
    implementation("com.github.Mathias-Boulay:virtual-joystick-android:1.14")

    implementation("org.tukaani:xz:1.12")
    implementation("net.sourceforge.htmlcleaner:htmlcleaner:2.29")
    implementation("com.bytedance:bytehook:1.1.2")
    implementation("com.bytedance.android:shadowhook:2.0.1")

    implementation("com.github.MojoLauncher:alsoft-android-aar:f369161d5f")

    val ktorVersion = "3.5.2"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation(project(":glfw:android-gradle"))
    implementation(project(":sdl:android-gradle"))
    implementation(project(":mojoexec:jni_bindings"))
    implementation(project(":ColorPicker"))

    val composeBom = platform("androidx.compose:compose-bom-alpha:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation(
        fileTree(
            mapOf(
                "dir" to "libs",
                "include" to listOf("*.jar", "*.aar")
            )
        )
    )

    val lwjglVersion = "3.4.3"
    val imguiVersion = "1.92.7.1"

    compileOnly("org.lwjgl:lwjgl:$lwjglVersion")
    compileOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    compileOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
}

tasks.register("prepareKotlinBuildScriptModel") {}