import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.konan.target.linker
import java.nio.file.Files

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    // TODO: Fix a multiplaform bug with Android Studio
    // https://stackoverflow.com/questions/36465824/android-studio-task-testclasses-not-found-in-project
    task("testClasses")
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    listOf(
        jvm() {
            tasks.getByName("compileKotlinJvm") {
                println("${this@getByName}: DEPENDS on ${targetName}.runCMake")
                this@getByName.dependsOn("${targetName}.runCMake")
            }
        },
        linuxX64() {
            binaries {
                executable()
            }
        }
    ).forEach {
        // This tricky bit will be used to compile the llamacpp.so through CMAKE
        // so it can be linked against the klib (or executable) at the end.
        // It is only needed for native targets and Desktop targets as
        // android uses a different approach, handled elsewhere.
        val targetName = it.targetName
        val platformType = it.platformType
        val taskName = "${targetName}.runCMake"

        println("Registering cmake job: $taskName")

        tasks.register(taskName) {
            group = "build"
            val wDir = layout.buildDirectory.dir("clib/$targetName").get().asFile
            val maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).toString()
            var javaHome: String? = null
            if (platformType == KotlinPlatformType.jvm) {
                javaHome = System.getenv("JAVA_HOME")
                    ?: throw GradleException("JAVA_HOME must be set on your environment!")
            }
            doFirst {
                println("Running CMAKE JOB $targetName $platformType")
                Files.createDirectories(wDir.toPath())
            }
            doLast {
                exec {
                    workingDir = wDir
                    if(platformType == KotlinPlatformType.jvm)
                        environment("JAVA_HOME", javaHome)
                    commandLine(listOf("cmake", "-DLLAMACPP_PLATFORM_TYPE=$platformType", "${projectDir}/src/jniMain/cpp"))
                }
                exec {
                    workingDir = wDir
                    commandLine(listOf("make", "-j", maxParallelForks))
                }
            }
        }
    }

//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach {
//        it.binaries.framework {
//            baseName = "inferencekt-llamacpp"
//            isStatic = true
//        }
//    }

    // Apply the default hierarchy again. It'll create, for example, the iosMain source set:
    applyDefaultHierarchyTemplate()

    sourceSets {

        all {
            languageSettings.apply {
                // Required for CPointer etc. since Kotlin 1.9.
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }

        val commonMain by getting
        val jvmMain by getting
        val androidMain by getting

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            implementation(project(":inferencekt"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        val jniMain by creating
        jniMain.dependsOn(commonMain)
        jvmMain.dependsOn(jniMain)
        androidMain.dependsOn(jniMain)
//
//        val appleMain by getting
//        val nativeMain by creating {
//            //dependsOn(iosX64)
//            dependsOn(appleMain)
//        }
    }

    targets.withType<KotlinNativeTarget> {
        val main by compilations.getting
        main.cinterops {
            create("llamacpp") {
                defFile(file("src/nativeInterop/cinterop/llamakt.def"))
                headers("src/jniMain/cpp/llamakt.h", "build/clib/linuxX64/_deps/llama-src/llama.h", "build/clib/linuxX64/_deps/llama-src/ggml.h")
                includeDirs("src/jniMain/cpp/llamakt.h", "build/clib/linuxX64/_deps/llama-src/")
                packageName("inferencekt_llamacpp")
            }
        }
        tasks.forEach {
            if (it.name.contains("cinterop")) {
                println("GENERATING DEPENDENCY FOR TASK ${it.name} $targetName")
                it.dependsOn("${targetName}.runCMake")
                return@forEach
            }
        }
//        binaries.withType<Framework> {
//            linkerOpts += "-Wl -v  -L/home/paulo/proj/pineai/inferencekt-llamacpp/build/clib/linuxX64 -lllamakt"
//
//        }
        binaries.withType<NativeBinary> {
            //TODO: sets the path to llamakt.so file. Maybe there is a better way to run
            // the binaries without setting the path here.
            linkerOpts += "-rpath=/home/paulo/proj/pineai/inferencekt-llamacpp/build/clib/linuxX64"
            println("LINKING binary: $name")
        }
        targets.withType<KotlinNativeTargetWithTests<*>> {
            binaries {
                // Configure a separate test where code is compiled in release mode.
                test(setOf(NativeBuildType.RELEASE))

            }
            testRuns {
                create("release") {
                    setExecutionSourceFrom(binaries.getByName("releaseTest") as TestExecutable)
                }
            }
        }
    }

}

android {
    namespace = "org.pinelang.inferencekt.llamacpp"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                arguments("-DBUILD_SHARED_LIBS=ON")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            path("src/jniMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "8.5"
    distributionType = Wrapper.DistributionType.BIN
}
