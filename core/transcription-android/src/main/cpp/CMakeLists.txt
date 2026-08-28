cmake_minimum_required(VERSION 3.22.1)

project(native_transcriptions)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# whisper.cpp options
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)

include(FetchContent)

FetchContent_Declare(
        whisper
        GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
        GIT_TAG v1.9.3
)

FetchContent_MakeAvailable(whisper)

# Native Android library
add_library(
        native_transcriptions
        SHARED
        src/transcription_jni.cpp
        src/whisper_ctx.cpp
)

# Your headers + whisper headers
target_include_directories(
        native_transcriptions
        PRIVATE
        include
        ${whisper_SOURCE_DIR}/include
)

# Android libraries
find_library(log-lib log)

target_link_libraries(
        native_transcriptions
        PRIVATE
        whisper
        ${log-lib}
        android
)
