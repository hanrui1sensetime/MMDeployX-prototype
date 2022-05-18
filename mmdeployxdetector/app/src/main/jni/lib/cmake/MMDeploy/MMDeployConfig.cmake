
####### Expanded from @PACKAGE_INIT@ by configure_package_config_file() #######
####### Any changes to this file will be overwritten by the next CMake run ####
####### The input file was MMDeployConfig.cmake.in                            ########

get_filename_component(PACKAGE_PREFIX_DIR "${CMAKE_CURRENT_LIST_DIR}/../../" ABSOLUTE)

####################################################################################

cmake_minimum_required(VERSION 3.14)

include("${CMAKE_CURRENT_LIST_DIR}/MMDeployTargets.cmake")

set(MMDEPLOY_CODEBASES all)
set(MMDEPLOY_TARGET_DEVICES cpu)
set(MMDEPLOY_TARGET_BACKENDS ncnn)
set(MMDEPLOY_BUILD_TYPE RelWithDebInfo)
set(MMDEPLOY_BUILD_SHARED OFF)

if (NOT MMDEPLOY_BUILD_SHARED)
    if ("cuda" IN_LIST MMDEPLOY_TARGET_DEVICES)
        find_package(CUDA REQUIRED)
        if(MSVC)
            set(CMAKE_CUDA_COMPILER ${CUDA_TOOLKIT_ROOT_DIR}/bin/nvcc.exe)
        else()
            set(CMAKE_CUDA_COMPILER ${CUDA_TOOLKIT_ROOT_DIR}/bin/nvcc)
        endif()
        set(CMAKE_CUDA_RUNTIME_LIBRARY Shared)
        enable_language(CUDA)
        find_package(pplcv REQUIRED)
    endif ()
endif ()

set(MMDEPLOY_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/modules")
list(APPEND CMAKE_MODULE_PATH ${MMDEPLOY_MODULE_PATH})



find_package(ncnn REQUIRED)


list(REMOVE_ITEM CMAKE_MODULE_PATH ${MMDEPLOY_MODULE_PATH})

find_package(spdlog REQUIRED)
find_package(OpenCV REQUIRED)

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

include("${CMAKE_CURRENT_LIST_DIR}/MMDeploy.cmake")
