cmake_minimum_required(VERSION 3.22.1)
project(talkdraft_native_modules)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_compile_options(-O3 -march=armv8-a+fp16+dotprod)
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    add_compile_options(-O3 -mfpu=neon-vfpv4 -mfloat-abi=softfp)
endif ()

add_subdirectory(native_whisper)
add_subdirectory(native_zipformer)
add_subdirectory(native_vad)