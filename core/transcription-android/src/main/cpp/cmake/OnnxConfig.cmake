include(FetchContent)

# 1. Fetch sherpa-onnx binaries
FetchContent_Declare(
        sherpa_onnx_android
        URL "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-v1.13.8-android.tar.bz2"
)
FetchContent_MakeAvailable(sherpa_onnx_android)
set(SHERPA_DIR ${sherpa_onnx_android_SOURCE_DIR})

# 2. Fetch onnxruntime AAR archive (Do not auto-extract during download)
FetchContent_Declare(
        onnxruntime_android
        URL "https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.28.0/onnxruntime-android-1.28.0.aar"
        DOWNLOAD_NO_EXTRACT TRUE
)
FetchContent_MakeAvailable(onnxruntime_android)

# Extract headers from AAR
set(ORT_EXTRACTED_DIR "${onnxruntime_android_BINARY_DIR}/extracted")
if (NOT EXISTS "${ORT_EXTRACTED_DIR}/headers")
    file(ARCHIVE_EXTRACT
            INPUT "${onnxruntime_android_SOURCE_DIR}/onnxruntime-android-1.28.0.aar"
            DESTINATION "${ORT_EXTRACTED_DIR}"
    )
endif ()

# 3. Define central onnxruntime target safely
if (NOT TARGET onnxruntime)
    add_library(onnxruntime SHARED IMPORTED GLOBAL)
    set_target_properties(onnxruntime PROPERTIES
            IMPORTED_LOCATION "${SHERPA_DIR}/${ANDROID_ABI}/libonnxruntime.so"
            INTERFACE_INCLUDE_DIRECTORIES "${ORT_EXTRACTED_DIR}/headers"
    )
endif ()

# 4. Define central sherpa-onnx-c-api target safely
if (NOT TARGET sherpa-onnx-c-api)
    add_library(sherpa-onnx-c-api SHARED IMPORTED GLOBAL)
    set_target_properties(sherpa-onnx-c-api PROPERTIES
            IMPORTED_LOCATION "${SHERPA_DIR}/${ANDROID_ABI}/libsherpa-onnx-c-api.so"
    )
endif ()
