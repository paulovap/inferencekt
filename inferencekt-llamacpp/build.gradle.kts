import org.jetbrains.compose.ComposePlugin.DesktopDependencies.currentOs
import java.nio.file.Files

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

val wDir = layout.buildDirectory.dir("clib").get().asFile

tasks.register("desktopRunCMake") {
    group = "build"
    println(wDir)
    val javaHome = System.getenv("JAVA_HOME")
        ?: throw GradleException("JAVA_HOME must be set on your environment!")
    val maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).toString()
    doFirst {
        Files.createDirectories(wDir.toPath())
    }
    doLast {
        exec {
            workingDir = wDir
            environment("JAVA_HOME", javaHome)
            commandLine(listOf("cmake", "${projectDir}/src/jniMain/cpp"))
        }
        exec {
            workingDir = wDir
            environment("JAVA_HOME", javaHome)
            commandLine(listOf("make", "-j", maxParallelForks))
        }
    }
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
    jvm {
        val processResources = compilations["main"].processResourcesTaskName
        (tasks[processResources] as ProcessResources).apply {
            // A task that build the JNI shared library for all targets and
            // copy each outputs into $buildDir/resources-jni
            dependsOn("desktopRunCMake")
            from("${layout.buildDirectory}/clib")
        }
    }

    linuxX64()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "inferencekt-llamacpp"
            isStatic = true
        }
    }

    // Apply the default hierarchy again. It'll create, for example, the iosMain source set:
    applyDefaultHierarchyTemplate()

    sourceSets {
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
}

android {
    namespace = "org.pinelang.inferencekt"
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
