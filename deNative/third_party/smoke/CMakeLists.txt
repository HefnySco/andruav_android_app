cmake_minimum_required(VERSION 3.22.1)
project(de_deps_smoke CXX)

# DE_DEPS_PREFIX: deNative/third_party/out/<abi> produced by build_deps.sh
if(NOT DE_DEPS_PREFIX)
    message(FATAL_ERROR "DE_DEPS_PREFIX not set")
endif()

add_library(de_deps_smoke SHARED smoke.cpp)

target_include_directories(de_deps_smoke PRIVATE ${DE_DEPS_PREFIX}/include)
target_compile_features(de_deps_smoke PRIVATE cxx_std_17)

target_link_libraries(de_deps_smoke
    ${DE_DEPS_PREFIX}/lib/libssl.a
    ${DE_DEPS_PREFIX}/lib/libcrypto.a
    ${DE_DEPS_PREFIX}/lib/libcurl.a
)
