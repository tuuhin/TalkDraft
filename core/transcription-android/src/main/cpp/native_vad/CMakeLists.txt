cmake_minimum_required(VERSION 3.22.1)
project(native_vad)

include(FetchContent)

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

add_library(
        native_vad
        SHARED
        src/vad_onnx.cpp
        src/vad_onnx_jni.cpp
)

target_include_directories(
        native_vad
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

find_library(log-lib log)

target_link_libraries(
        native_vad
        PRIVATE
        onnxruntime
        ${log-lib}
        android
)
