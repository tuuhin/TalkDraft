# cmake/OnnxConfig.cmake
include(FetchContent)

# Avoid the timestamp warning on CMake >= 3.24 (guarded so 3.22 doesn't choke)
if (POLICY CMP0135)
    cmake_policy(SET CMP0135 NEW)
endif ()

# Package variant: "shared" (separate libonnxruntime.so) or "static" (ORT linked into sherpa)
set(SHERPA_VARIANT "shared" CACHE STRING "sherpa-onnx package variant: static or shared")
set(SHERPA_VERSION "1.13.8")

if (SHERPA_VARIANT STREQUAL "static")
    set(SHERPA_PKG "sherpa-onnx-v${SHERPA_VERSION}-android-static-link-onnxruntime")
else ()
    set(SHERPA_PKG "sherpa-onnx-v${SHERPA_VERSION}-android")
endif ()

FetchContent_Declare(
        sherpa_onnx_android
        URL "https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}/${SHERPA_PKG}.tar.bz2"
)
FetchContent_MakeAvailable(sherpa_onnx_android)
set(SHERPA_DIR "${sherpa_onnx_android_SOURCE_DIR}")

# Not using find_file: the NDK toolchain forces find_file/find_path into
# ONLY-mode re-rooting, which ignores absolute paths outside the sysroot.
set(SHERPA_LIB_DIR "${SHERPA_DIR}/${ANDROID_ABI}")
set(SHERPA_CAPI_LIB "${SHERPA_LIB_DIR}/libsherpa-onnx-c-api.so")
set(SHERPA_ORT_LIB "${SHERPA_LIB_DIR}/libonnxruntime.so")

if (NOT EXISTS "${SHERPA_CAPI_LIB}")
    file(GLOB _found "${SHERPA_LIB_DIR}/*")
    message(FATAL_ERROR "sherpa c-api lib not found: ${SHERPA_CAPI_LIB} | dir contents: ${_found}")
endif ()

if (NOT EXISTS "${SHERPA_ORT_LIB}")
    set(SHERPA_ORT_LIB "")   # static variant: no separate ORT lib
endif ()

# (For offline/CI builds, commit these files into your repo and point
# SHERPA_INCLUDE_DIR at that folder instead.)
set(SHERPA_INCLUDE_DIR "${CMAKE_BINARY_DIR}/sherpa_headers")
set(_hdr_dir "${SHERPA_INCLUDE_DIR}/sherpa-onnx/c-api")
file(MAKE_DIRECTORY "${_hdr_dir}")

foreach (_hdr c-api.h cxx-api.h)
    if (NOT EXISTS "${_hdr_dir}/${_hdr}")
        file(DOWNLOAD
                "https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v${SHERPA_VERSION}/sherpa-onnx/c-api/${_hdr}"
                "${_hdr_dir}/${_hdr}"
                STATUS _dl_status
                TLS_VERIFY ON)
        list(GET _dl_status 0 _dl_code)
        if (NOT _dl_code EQUAL 0)
            file(REMOVE "${_hdr_dir}/${_hdr}")
            message(FATAL_ERROR "Failed to download ${_hdr}: ${_dl_status}")
        endif ()
    endif ()
endforeach ()

# libonnxruntime.so exists as a separate file only in the "shared" variant
if (SHERPA_ORT_LIB AND NOT TARGET onnxruntime)
    add_library(onnxruntime SHARED IMPORTED GLOBAL)
    set_target_properties(onnxruntime PROPERTIES
            IMPORTED_LOCATION "${SHERPA_ORT_LIB}")
endif ()

if (NOT TARGET sherpa-onnx-c-api)
    add_library(sherpa-onnx-c-api SHARED IMPORTED GLOBAL)
    set_target_properties(sherpa-onnx-c-api PROPERTIES
            IMPORTED_LOCATION "${SHERPA_CAPI_LIB}"
            INTERFACE_INCLUDE_DIRECTORIES "${SHERPA_INCLUDE_DIR}")
    if (TARGET onnxruntime)
        set_property(TARGET sherpa-onnx-c-api PROPERTY INTERFACE_LINK_LIBRARIES onnxruntime)
    endif ()
endif ()
