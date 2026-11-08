cmake_minimum_required(VERSION 3.22.1)
project(mtermux-core LANGUAGES C CXX ASM)

# Automatically use ccache when available to cache C++ object compilation across builds
find_program(CCACHE_PROGRAM ccache)
if(CCACHE_PROGRAM)
    set(CMAKE_C_COMPILER_LAUNCHER "${CCACHE_PROGRAM}")
    set(CMAKE_CXX_COMPILER_LAUNCHER "${CCACHE_PROGRAM}")
endif()

include(CheckCXXCompilerFlag)

# Prefer C++26 (-std=c++26 / -std=c++2c) with automatic Clang dialect detection
check_cxx_compiler_flag("-std=c++26" HAS_CXX26)
check_cxx_compiler_flag("-std=c++2c" HAS_CXX2C)
if(HAS_CXX26)
    set(CXX_STD_FLAG "-std=c++26")
elseif(HAS_CXX2C)
    set(CXX_STD_FLAG "-std=c++2c")
else()
    set(CXX_STD_FLAG "-std=c++26")
endif()

set(COMMON_CXX_FLAGS
    ${CXX_STD_FLAG}
    -Wall -Wextra -Werror -Wno-unused-command-line-argument
    -Wno-null-character -Wno-unused-parameter -Wno-unused-variable -Wno-unused-function
    -O3 -fno-math-errno -fomit-frame-pointer -fno-exceptions -fno-rtti
    -fvisibility=hidden -fdata-sections -ffunction-sections
)

set_source_files_properties(termux-bootstrap-zip.S PROPERTIES
    OBJECT_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/bootstrap-aarch64.zip"
)

add_library(mtermux-core SHARED
    termux-bootstrap-zip.S
    termux_core.cpp
)

add_library(mtermux-redirect SHARED
    mtermux_redirect.cpp
)

target_compile_options(mtermux-core PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
    "-Wa,-I${CMAKE_CURRENT_SOURCE_DIR}"
)

target_compile_options(mtermux-redirect PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
)

# Use ARMv8.2-A with NEON/DotProd/FP16/Crypto instead of SVE2 to prevent SIGILL
# on Qualcomm Oryon (Snapdragon 8 Elite / Gen 5) and other ARM64 SoCs without SVE2.
check_cxx_compiler_flag("-march=armv8.2-a+crypto+dotprod+fp16" HAS_ARMV8_2)
if(HAS_ARMV8_2)
    target_compile_options(mtermux-core PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
    target_compile_options(mtermux-redirect PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
endif()

find_library(log-lib log)
find_library(z-lib z)

target_link_libraries(mtermux-core
    ${log-lib}
    ${z-lib}
)

target_link_libraries(mtermux-redirect
    dl
)

set(COMMON_LINK_FLAGS
    -Wl,-z,max-page-size=16384
    -Wl,-z,common-page-size=16384
    -Wl,--gc-sections
    -Wl,-O3
    -Wl,--strip-all
)

target_link_options(mtermux-core PRIVATE ${COMMON_LINK_FLAGS})
target_link_options(mtermux-redirect PRIVATE ${COMMON_LINK_FLAGS})

add_executable(mtermux-open
    mtermux_open.cpp
)

add_executable(mtermux-clip
    mtermux_clip.cpp
)

set_target_properties(mtermux-open PROPERTIES
    OUTPUT_NAME "mtermux-open"
    PREFIX "lib"
    SUFFIX ".so"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}"
)

set_target_properties(mtermux-clip PROPERTIES
    OUTPUT_NAME "mtermux-clip"
    PREFIX "lib"
    SUFFIX ".so"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}"
)

target_compile_options(mtermux-open PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
)

target_compile_options(mtermux-clip PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
)

if(HAS_ARMV8_2)
    target_compile_options(mtermux-open PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
    target_compile_options(mtermux-clip PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
endif()

target_link_options(mtermux-open PRIVATE ${COMMON_LINK_FLAGS} -pie)
target_link_options(mtermux-clip PRIVATE ${COMMON_LINK_FLAGS} -pie)

add_executable(mtermux-exec
    mtermux_exec.cpp
)

set_target_properties(mtermux-exec PROPERTIES
    OUTPUT_NAME "mtermux-exec"
    PREFIX "lib"
    SUFFIX ".so"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}"
)

target_compile_options(mtermux-exec PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
)

if(HAS_ARMV8_2)
    target_compile_options(mtermux-exec PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
endif()

target_link_options(mtermux-exec PRIVATE ${COMMON_LINK_FLAGS} -pie)
