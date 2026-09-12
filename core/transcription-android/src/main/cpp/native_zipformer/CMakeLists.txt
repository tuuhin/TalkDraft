cmake_minimum_required(VERSION 3.22.1)
project(native_zipformer LANGUAGES CXX)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../cmake")
include(OnnxConfig)

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
