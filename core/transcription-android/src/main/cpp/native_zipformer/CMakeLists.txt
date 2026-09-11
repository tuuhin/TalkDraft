cmake_minimum_required(VERSION 3.22.1)
project(native_zipformer)
include(FetchContent)

FetchContent_Declare(
        sherpa_onnx_android
        URL "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-v1.13.8-android.tar.bz2"
)

FetchContent_MakeAvailable(sherpa_onnx_android)

set(SHERPA_DIR ${sherpa_onnx_android_SOURCE_DIR})
add_library(onnxruntime SHARED IMPORTED)

set_target_properties(onnxruntime PROPERTIES
        IMPORTED_LOCATION
        "${SHERPA_DIR}/${ANDROID_ABI}/libonnxruntime.so"
)

add_library(sherpa-onnx-c-api SHARED IMPORTED)

set_target_properties(sherpa-onnx-c-api PROPERTIES
        IMPORTED_LOCATION
        "${SHERPA_DIR}/${ANDROID_ABI}/libsherpa-onnx-c-api.so"
)

add_library(native_zipformer SHARED
        src/onnx_zipformer.cpp
        src/onnx_zipformer_jni.cpp
)

target_include_directories(native_zipformer PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

find_library(log-lib log)

target_link_libraries(native_zipformer PRIVATE
        sherpa-onnx-c-api
        onnxruntime
        ${log-lib}
        android
)
