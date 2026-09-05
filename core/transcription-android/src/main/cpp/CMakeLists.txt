cmake_minimum_required(VERSION 3.22.1)

project(native_transcriptions)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# ==============================================================================
# 1. Fetch & Configure whisper.cpp
# ==============================================================================
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
        whisper
        GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
        GIT_TAG v1.9.3
)
FetchContent_MakeAvailable(whisper)

# ==============================================================================
# 2. Fetch & Configure ONNX Runtime (Prebuilt Android AAR)
# ==============================================================================
FetchContent_Declare(
        onnxruntime_android
        URL "https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.29.0/onnxruntime-android-1.29.0.aar"
)
FetchContent_MakeAvailable(onnxruntime_android)

# Set path targets to the extracted headers and shared libraries
set(ORT_EXTRACTED_DIR ${onnxruntime_android_SOURCE_DIR})

add_library(onnxruntime SHARED IMPORTED)
set_target_properties(onnxruntime PROPERTIES
        IMPORTED_LOCATION ${ORT_EXTRACTED_DIR}/jni/${ANDROID_ABI}/libonnxruntime.so
        INTERFACE_INCLUDE_DIRECTORIES ${ORT_EXTRACTED_DIR}/headers
)

# ==============================================================================
# 3. Native Application Library Target
# ==============================================================================
add_library(
        native_transcriptions
        SHARED
        src/transcription_jni.cpp
        src/vad_onnx.cpp
        src/vad_onnx_jni.cpp
)

# Fix: Include local header paths + whisper & ggml include directories
target_include_directories(
        native_transcriptions
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include   # Where your vad_onnx.h or other headers live
        ${CMAKE_CURRENT_SOURCE_DIR}/src       # In case vad_onnx.h is in src/
        ${whisper_SOURCE_DIR}/include        # whisper.h
        ${whisper_SOURCE_DIR}/ggml/include   # ggml core headers required by whisper
)

# Find system libraries
find_library(log-lib log)

# Link dependencies
target_link_libraries(
        native_transcriptions
        PRIVATE
        whisper
        onnxruntime
        ${log-lib}
        android
)
