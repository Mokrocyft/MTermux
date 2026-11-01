cmake_minimum_required(VERSION 3.22.1)
project(termux-core LANGUAGES C CXX ASM)

include(CheckCXXCompilerFlag)

check_cxx_compiler_flag("-std=c++23" HAS_CXX23)
if(HAS_CXX23)
    set(CMAKE_CXX_STANDARD 23)
else()
    set(CMAKE_CXX_STANDARD 20)
endif()
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(COMMON_CXX_FLAGS
    -Wall -Wextra -Wno-unused-command-line-argument -O3 -ffast-math
    -fomit-frame-pointer -fno-exceptions -fno-rtti -fvisibility=hidden
    -fdata-sections -ffunction-sections
)

add_library(termux-core SHARED
    termux-bootstrap-zip.S
    termux_core.cpp
)

add_library(termuxx-redirect SHARED
    termuxx_redirect.cpp
)

target_compile_options(termux-core PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
    "-Wa,-I${CMAKE_CURRENT_SOURCE_DIR}"
)

target_compile_options(termuxx-redirect PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
)

check_cxx_compiler_flag("-march=armv9.2-a+sve2+crypto+dotprod+fp16+bf16+i8mm" HAS_ARMV9_2_SVE2)
if(HAS_ARMV9_2_SVE2)
    target_compile_options(termux-core PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv9.2-a+sve2+crypto+dotprod+fp16+bf16+i8mm>)
    target_compile_options(termuxx-redirect PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv9.2-a+sve2+crypto+dotprod+fp16+bf16+i8mm>)
else()
    target_compile_options(termux-core PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
    target_compile_options(termuxx-redirect PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-march=armv8.2-a+crypto+dotprod+fp16>)
endif()

find_library(log-lib log)

target_link_libraries(termux-core
    ${log-lib}
)

target_link_libraries(termuxx-redirect
    dl
)

set(COMMON_LINK_FLAGS
    -Wl,-z,max-page-size=16384
    -Wl,-z,common-page-size=16384
    -Wl,--gc-sections
    -Wl,-O3
)

target_link_options(termux-core PRIVATE ${COMMON_LINK_FLAGS})
target_link_options(termuxx-redirect PRIVATE ${COMMON_LINK_FLAGS})
