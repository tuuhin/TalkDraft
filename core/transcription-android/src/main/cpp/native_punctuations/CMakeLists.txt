cmake_minimum_required(VERSION 3.22.1)
project(native_punctuations LANGUAGES CXX)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../cmake")
include(OnnxConfig)

add_library(native_punctuations SHARED
        src/onnx_punctuations.cpp
        src/native_punctuations_jni.cpp
        src/punctuations_manager.cpp
)

target_include_directories(native_punctuations PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

find_library(log-lib log)

target_link_libraries(native_punctuations PRIVATE
        sherpa-onnx-c-api
        ${log-lib}
        android
)
