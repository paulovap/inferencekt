import org.jetbrains.compose.ComposePlugin.DesktopDependencies.currentOs
import java.nio.file.Files

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

val wDir = layout.buildDirectory.dir("clib").get().asFile

tasks.create<Exec>("desktopRunCmake") {
    group = "build"
    workingDir = wDir
    val javaHome = environment["JAVA_HOME"]!!
    doFirst {
        Files.createDirectories(wDir.toPath())
    }
    environment("TARGET", "desktop")
    environment("JAVA_HOME", javaHome)
    commandLine(listOf("cmake", "${projectDir}/src/jniMain/cpp"))
}
tasks.create<Exec>("desktopRunMake") {
    dependsOn("desktopRunCmake")
    val javaHome = environment["JAVA_HOME"]!!
    group = "build"
    workingDir = wDir
    environment("TARGET", "desktop")
    environment("JAVA_HOME", javaHome)
    environment("TARGET", currentOs)
    commandLine(listOf("make"))
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
    jvm("desktop") {
        val processResources = compilations["main"].processResourcesTaskName
        (tasks[processResources] as ProcessResources).apply {
            // A task that build the JNI shared library for all targets and
            // copy each outputs into $buildDir/resources-jni
            dependsOn("desktopRunMake")
            from("${layout.buildDirectory}/clib")
        }
    }

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
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            implementation(project(":inferencekt"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val desktopMain by getting
        val androidMain by getting

        val jniMain by creating
        desktopMain.dependsOn(jniMain)
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
