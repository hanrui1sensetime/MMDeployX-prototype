project(detector)

cmake_minimum_required(VERSION 3.14)

include_directories(${CMAKE_SOURCE_DIR}/include/c)
include_directories(${CMAKE_SOURCE_DIR}/include/cpp/core)
include_directories(${CMAKE_SOURCE_DIR}/include/cpp/experimental)
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/OpenCV-android-sdk/sdk/native/jni)
find_package(OpenCV REQUIRED core imgproc)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)
set(spdlog_DIR ${CMAKE_SOURCE_DIR}/spdlog/lib/cmake/spdlog)
find_package(spdlog REQUIRED)
set(MMDeploy_DIR ${CMAKE_SOURCE_DIR}/lib/cmake/MMDeploy)
find_package(MMDeploy REQUIRED)
link_directories(${CMAKE_SOURCE_DIR}/spdlog/lib)

add_library(detectortools SHARED detectortools.cpp)
mmdeploy_load_static(detectortools MMDeployStaticModules)
target_link_libraries(detectortools PRIVATE MMDeployLibs PUBLIC ncnn ${OpenCV_LIBS} spdlog)
