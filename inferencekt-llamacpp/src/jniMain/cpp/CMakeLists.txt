# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("llamacpp")

option(LLAMACPP_PLATFORM_TYPE "LLAMACPP_PLATFORM_TYPE" "")

if (("${CMAKE_SYSTEM_NAME}" STREQUAL "Android") AND (ANDROID_ABI STREQUAL "arm64-v8a"))
    add_compile_options(
                -march=armv8.4-a+fp16+dotprod
                -O3
                -fPIC
        )
    add_compile_definitions("JNI=1")
else()
# CUDA IS NOT WORKING ON MY MACHINE, DISABLING FOR NOW
#    FIND_PACKAGE(CUDAToolkit)
#    IF (${CUDAToolkit_FOUND})
#        set(LLAMA_CUDA ON)
#        MESSAGE(STATUS "CUDA FOUND setting is .${LLAMA_CUDA}")
#    ELSE (${CUDAToolkit_FOUND})
#        MESSAGE(STATUS "Could not locate CUDA.")
#    ENDIF (${CUDAToolkit_FOUND})
endif()

# We only need the wrapper .so (llamakt.so) if we are
# on a jvm target
if(LLAMACPP_PLATFORM_TYPE STREQUAL "jvm")
    #TODO: We don't need JNI if target is native.
    set(JAVA_AWT_LIBRARY NotNeeded)
    set(JAVA_AWT_INCLUDE_PATH NotNeeded)
    find_package(JNI REQUIRED)
    include_directories(${JNI_INCLUDE_DIRS})
    add_compile_definitions("JNI=1")
endif ()

# Download llama.cpp inference backend
include(FetchContent)
FetchContent_Declare(
        llama
        GIT_REPOSITORY https://github.com/ggerganov/llama.cpp
        GIT_TAG b3452
        GIT_SHALLOW 1
)
FetchContent_MakeAvailable(llama)

set_property(TARGET ggml PROPERTY POSITION_INDEPENDENT_CODE ON)
set_property(TARGET common PROPERTY POSITION_INDEPENDENT_CODE ON)
set_property(TARGET llama PROPERTY POSITION_INDEPENDENT_CODE ON)



add_library(${CMAKE_PROJECT_NAME} SHARED
            # List C/C++ source files with relative paths to this CMakeLists.txt.
        llamakt.cpp
        llamakt.h
)

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
            PRIVATE
            llama
            common)
endif()
