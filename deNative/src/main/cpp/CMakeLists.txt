cmake_minimum_required(VERSION 3.22.1)
project(de_host)

add_library(de_host SHARED de_host.cpp)

find_library(log-lib log)
target_link_libraries(de_host ${log-lib})

# Upstream DroneEngage modules, synced into deNative/external/ by
# deNative/sync_sources.sh (see deNative/sources.lock).
set(DE_EXTERNAL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../external)
if(NOT EXISTS ${DE_EXTERNAL_DIR}/.synced)
    message(FATAL_ERROR "deNative/external/ is not synced — run deNative/sync_sources.sh first")
endif()

# Third-party deps built by deNative/third_party/build_deps.sh for this ABI.
set(DE_THIRDPARTY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/out/${ANDROID_ABI}
    CACHE PATH "deNative third_party out/<abi> directory" FORCE)
set(DE_ANDROID ON CACHE BOOL "Build DE modules as Android shared libraries" FORCE)

# Seed the dependency paths in the cache: both upstream CMakeLists set these as
# normal variables, but with CMP0125 OLD (cmake_minimum_required 3.10 upstream)
# the second find_package would reuse the empty cache entries that
# mark_as_advanced() leaves behind from the first find.
set(OPENSSL_ROOT_DIR ${DE_THIRDPARTY_DIR} CACHE PATH "" FORCE)
set(OPENSSL_INCLUDE_DIR ${DE_THIRDPARTY_DIR}/include CACHE PATH "" FORCE)
set(OPENSSL_CRYPTO_LIBRARY ${DE_THIRDPARTY_DIR}/lib/libcrypto.a CACHE FILEPATH "" FORCE)
set(OPENSSL_SSL_LIBRARY ${DE_THIRDPARTY_DIR}/lib/libssl.a CACHE FILEPATH "" FORCE)
set(CURL_INCLUDE_DIR ${DE_THIRDPARTY_DIR}/include CACHE PATH "" FORCE)
set(CURL_LIBRARY ${DE_THIRDPARTY_DIR}/lib/libcurl.a CACHE FILEPATH "" FORCE)
set(Boost_INCLUDE_DIR ${DE_THIRDPARTY_DIR}/include CACHE PATH "" FORCE)
set(Boost_NO_SYSTEM_PATHS ON CACHE BOOL "" FORCE)

add_subdirectory(${DE_EXTERNAL_DIR}/droneengage_communication_pro build_de_comm)
add_subdirectory(${DE_EXTERNAL_DIR}/droneengage_mavlink build_de_mavlink)
