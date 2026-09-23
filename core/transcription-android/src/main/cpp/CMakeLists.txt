cmake_minimum_required(VERSION 3.22.1)
project(talkdraft_native_modules LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Enable Interprocedural Optimization (ThinLTO) portably
include(CheckIPOSupported)
check_ipo_supported(RESULT ipo_supported OUTPUT ipo_error)
if (ipo_supported)
    set(CMAKE_INTERPROCEDURAL_OPTIMIZATION TRUE)
    message(STATUS "IPO / LTO enabled for transcription native modules")
endif ()

# High-performance and size-reduction compile options
set(TALKDRAFT_OPT_FLAGS
        -O3
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -fomit-frame-pointer
        -funroll-loops
)

# ABI-specific architecture tuning
if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    list(APPEND TALKDRAFT_OPT_FLAGS -march=armv8-a+fp16+dotprod)
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    list(APPEND TALKDRAFT_OPT_FLAGS -mfpu=neon-vfpv4 -mfloat-abi=softfp)
endif ()

add_compile_options(${TALKDRAFT_OPT_FLAGS})

# Linker optimizations for binary size and symbol hygiene
add_link_options(
        -Wl,--gc-sections
        -Wl,--icf=safe
        -Wl,--exclude-libs,ALL
        -flto=thin
)

add_subdirectory(native_whisper)
add_subdirectory(native_zipformer)
add_subdirectory(native_vad)
