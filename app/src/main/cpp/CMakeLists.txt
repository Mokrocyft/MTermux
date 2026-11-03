cmake_minimum_required(VERSION 3.22.1)
project(termuxx-core LANGUAGES C CXX ASM)

# Automatically use ccache when available to cache C++23 object compilation across builds
find_program(CCACHE_PROGRAM ccache)
if(CCACHE_PROGRAM)
    set(CMAKE_C_COMPILER_LAUNCHER "${CCACHE_PROGRAM}")
    set(CMAKE_CXX_COMPILER_LAUNCHER "${CCACHE_PROGRAM}")
endif()

include(CheckCXXCompilerFlag)

check_cxx_compiler_flag("-std=c++26" HAS_CXX26)
check_cxx_compiler_flag("-std=c++23" HAS_CXX23)
if(HAS_CXX26)
    set(CXX_STD_FLAG "-std=c++26")
elseif(HAS_CXX23)
    set(CXX_STD_FLAG "-std=c++23")
else()
    set(CXX_STD_FLAG "-std=c++2b")
endif()

set(CMAKE_CXX_EXTENSIONS OFF)

set(COMMON_CXX_FLAGS
    ${CXX_STD_FLAG}
    -Wall -Wextra -Wno-unused-command-line-argument -O3
    -fno-math-errno -fomit-frame-pointer -fno-exceptions -fno-rtti
    -fvisibility=hidden -fdata-sections -ffunction-sections
)

set_source_files_properties(termux-bootstrap-zip.S PROPERTIES
    OBJECT_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/bootstrap-aarch64.zip"
)

add_library(termuxx-core SHARED
    termux-bootstrap-zip.S
    termux_core.cpp
)

add_library(termuxx-redirect SHARED
    termuxx_redirect.cpp
)

target_compile_options(termuxx-core PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
    "-Wa,-I${CMAKE_CURRENT_SOURCE_DIR}"
)

target_compile_options(termuxx-redirect PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
)

# Use ARMv8.2-A with NEON/DotProd/FP16/Crypto instead of SVE2 to prevent SIGILL
# on Qualcomm Oryon (Snapdragon 8 Elite / Gen 5) and other ARM64 SoCs without SVE2.
check_cxx_compiler_flag("-march=armv8.2-a+crypto+dotprod+fp16" HAS_ARMV8_2)
if(HAS_ARMV8_2)
    target_compile_options(termuxx-core PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
    target_compile_options(termuxx-redirect PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
endif()

find_library(log-lib log)
find_library(z-lib z)

target_link_libraries(termuxx-core
    ${log-lib}
    ${z-lib}
)

target_link_libraries(termuxx-redirect
    dl
)

set(COMMON_LINK_FLAGS
    -Wl,-z,max-page-size=16384
    -Wl,-z,common-page-size=16384
    -Wl,--gc-sections
    -Wl,-O3
    -Wl,--strip-all
)

target_link_options(termuxx-core PRIVATE ${COMMON_LINK_FLAGS})
target_link_options(termuxx-redirect PRIVATE ${COMMON_LINK_FLAGS})
