cmake_minimum_required(VERSION 3.22.1)
project(native_zipformer)

include(FetchContent)
FetchContent_Declare(
        onnxruntime_android
        URL "https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.29.0/onnxruntime-android-1.29.0.aar"
)
FetchContent_MakeAvailable(onnxruntime_android)

set(ORT_EXTRACTED_DIR ${onnxruntime_android_SOURCE_DIR})

add_library(onnxruntime SHARED IMPORTED)
set_target_properties(onnxruntime PROPERTIES
        IMPORTED_LOCATION ${ORT_EXTRACTED_DIR}/jni/${ANDROID_ABI}/libonnxruntime.so
        INTERFACE_INCLUDE_DIRECTORIES ${ORT_EXTRACTED_DIR}/headers
)

add_library(
        native_zipformer
        SHARED
        src/onnx_zipformer.cpp
        src/onnx_zipformer_jni.cpp
)

target_include_directories(
        native_zipformer
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

find_library(log-lib log)

target_link_libraries(
        native_zipformer
        PRIVATE
        onnxruntime
        ${log-lib}
        android
)
