cmake_minimum_required(VERSION 3.22.1)

if (POLICY CMP0077)
    cmake_policy(SET CMP0077 NEW)
endif ()

project(android_archiver C CXX)
include(FetchContent)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS ON)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections -fdata-sections -g0")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections -fdata-sections -g0")

foreach (cfg RELEASE MINSIZEREL RELWITHDEBINFO)
    set(CMAKE_C_FLAGS_${cfg} "-Os -DNDEBUG")
    set(CMAKE_CXX_FLAGS_${cfg} "-Os -DNDEBUG")
endforeach ()

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--gc-sections -Wl,--strip-all -Wl,-z,max-page-size=16384")

set(ENABLE_TEST OFF CACHE BOOL "" FORCE)
set(ENABLE_INSTALL OFF CACHE BOOL "" FORCE)
set(ENABLE_WERROR OFF CACHE BOOL "" FORCE)
set(ENABLE_TAR OFF CACHE BOOL "" FORCE)
set(ENABLE_CPIO OFF CACHE BOOL "" FORCE)
set(ENABLE_CAT OFF CACHE BOOL "" FORCE)
set(ENABLE_UNZIP OFF CACHE BOOL "" FORCE)
set(ENABLE_ZLIB ON CACHE BOOL "" FORCE)
set(ENABLE_BZip2 ON CACHE BOOL "" FORCE)
set(ENABLE_LZ4 OFF CACHE BOOL "" FORCE)
set(ENABLE_LZMA OFF CACHE BOOL "" FORCE)
set(ENABLE_LZO OFF CACHE BOOL "" FORCE)
set(ENABLE_ZSTD OFF CACHE BOOL "" FORCE)
set(ENABLE_OPENSSL OFF CACHE BOOL "" FORCE)
set(ENABLE_LIBXML2 OFF CACHE BOOL "" FORCE)
set(ENABLE_EXPAT OFF CACHE BOOL "" FORCE)
set(ENABLE_ICONV OFF CACHE BOOL "" FORCE)
set(ENABLE_ACL OFF CACHE BOOL "" FORCE)
set(ENABLE_XATTR OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
        bzip2
        GIT_REPOSITORY https://github.com/libarchive/bzip2.git
        GIT_TAG master
        GIT_SHALLOW TRUE
)

FetchContent_MakeAvailable(bzip2)

set(BZIP2_INCLUDE_DIR ${bzip2_SOURCE_DIR})
set(BZIP2_LIBRARIES bz2)
set(BZIP2_FOUND TRUE)

FetchContent_Declare(
        libarchive
        GIT_REPOSITORY https://github.com/libarchive/libarchive.git
        GIT_TAG v3.8.9
        GIT_SHALLOW TRUE
)

FetchContent_MakeAvailable(libarchive)

add_library(android_archiver SHARED
        src/compression_engine.cpp src/compression_jni.cpp
        src/tar_archive_engine.cpp src/tar_archive_jni.cpp
        src/cpp_result.cpp)

target_include_directories(android_archiver PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${libarchive_SOURCE_DIR}/libarchive
        ${bzip2_SOURCE_DIR})

target_link_libraries(android_archiver PRIVATE archive bz2)
target_compile_features(android_archiver PRIVATE cxx_std_20)

target_compile_options(android_archiver PRIVATE
        -fvisibility=hidden -fvisibility-inlines-hidden
        -flto=thin)

target_link_options(android_archiver PRIVATE
        -Wl,--exclude-libs,ALL
        -Wl,--gc-sections
        -Wl,--icf=safe
        -Wl,--build-id=none
        -Wl,--strip-all
)
