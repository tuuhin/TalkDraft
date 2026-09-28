cmake_minimum_required(VERSION 3.22.1)
project(native_vad LANGUAGES CXX)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../cmake")
include(OnnxConfig)

add_library(native_vad SHARED
        src/sherpa_vad.cpp
        src/sherpa_vad_manager.cpp
        src/vad_onnx_jni.cpp
)

target_include_directories(native_vad PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

find_library(log-lib log)

target_link_libraries(native_vad PRIVATE
        sherpa-onnx-c-api
        ${log-lib}
        android
)
