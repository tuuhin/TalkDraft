cmake_minimum_required(VERSION 3.22.1)
project(native_whisper)

include(FetchContent)

# Disable OpenMP & Tests
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(WHISPER_OPENMP OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
        whisper
        GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
        GIT_TAG v1.9.4
)
FetchContent_MakeAvailable(whisper)

add_library(
        native_whisper
        SHARED
        src/whisper_engine.cpp
        src/whisper_engine_manager.cpp
        src/whisper_transcription_jni.cpp
)

target_include_directories(
        native_whisper
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_CURRENT_SOURCE_DIR}/src
        ${whisper_SOURCE_DIR}/include
        ${whisper_SOURCE_DIR}/ggml/include
)

find_library(log-lib log)

target_link_libraries(
        native_whisper
        PRIVATE
        whisper
        ${log-lib}
        android
)
