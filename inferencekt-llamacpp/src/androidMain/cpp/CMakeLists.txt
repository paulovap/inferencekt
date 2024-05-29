# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("llamakt")

if ("${CMAKE_SYSTEM_NAME}" STREQUAL "Android")
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        add_compile_options(
                -march=armv8.4-a+fp16+dotprod
                -O3
                -fPIC
        )
    endif()
# For Desktop JVM link with JDK
elseif (CMAKE_SYSTEM_NAME MATCHES "Linux")
    set(JAVA_AWT_LIBRARY NotNeeded)
    set(JAVA_AWT_INCLUDE_PATH NotNeeded)
    find_package(JNI REQUIRED)
    include_directories(${JNI_INCLUDE_DIRS})

    FIND_PACKAGE(CUDAToolkit)
    IF (${CUDAToolkit_FOUND})
        set(LLAMA_CUDA ON)
        MESSAGE(STATUS "CUDA FOUND setting is .${LLAMA_CUDA}")
    ELSE (${CUDAToolkit_FOUND})
        MESSAGE(STATUS "Could not locate CUDA.")
    ENDIF (${CUDAToolkit_FOUND})
endif()

# Download llama.cpp inference backend
include(FetchContent)
FetchContent_Declare(
        llama
        GIT_REPOSITORY https://github.com/ggerganov/llama.cpp
        GIT_TAG 1ea2a00
        GIT_SHALLOW 1
)
FetchContent_MakeAvailable(llama)

set_property(TARGET ggml PROPERTY POSITION_INDEPENDENT_CODE ON)
set_property(TARGET common PROPERTY POSITION_INDEPENDENT_CODE ON)
set_property(TARGET llama PROPERTY POSITION_INDEPENDENT_CODE ON)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        llamakt.cpp)

if ("${CMAKE_SYSTEM_NAME}" STREQUAL "Android")
    # Specifies libraries CMake should link to your target library. You
    # can link libraries from various origins, such as libraries defined in this
    # build script, prebuilt third-party libraries, or Android system libraries.
    target_link_libraries(${CMAKE_PROJECT_NAME}
            # List libraries link to the target library
            android
            llama
            common
            log)
else()
    #Jvm Desktop for now
    target_link_libraries(${CMAKE_PROJECT_NAME}
            # List libraries link to the target library
            llama
            common)
endif()

