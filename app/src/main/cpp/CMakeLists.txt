cmake_minimum_required(VERSION 3.22.1)

project("fptn_native_lib")

add_definitions(-BOOST_PROCESS_V2_HEADER_ONLY)
add_definitions(-DBOOST_ASIO_HAS_CO_AWAIT)
add_definitions(-DBOOST_ASIO_HAS_CO_SPAWN)
add_definitions(-DBOOST_ASIO_HAS_COROUTINES)
set(CMAKE_INCLUDE_CURRENT_DIR ON)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if (CMAKE_CXX_COMPILER_ID STREQUAL "Clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unused-command-line-argument")
endif ()

find_package(fptn REQUIRED)
find_package(nlohmann_json REQUIRED)

message(STATUS "fptn_FOUND: ${fptn_FOUND}")
message(STATUS "fptn_INCLUDE_DIRS: ${fptn_INCLUDE_DIRS}")
message(STATUS "fptn_LIBRARIES: ${fptn_LIBRARIES}")

include_directories(src/)
include_directories(libs/fptn/src/)
include_directories(libs/fptn/src/fptn-protocol-lib)

include(libs/fptn/depends/cmake/CamouflageTLS.cmake)  # FIXME

# disable pcap++
add_compile_definitions(FPTN_IP_ADDRESS_WITHOUT_PCAP)

add_library(
  ${CMAKE_PROJECT_NAME} SHARED
  src/jnienv/jnienv.h
  src/jnienv/jnienv.cpp
  src/wrappers/wrapper_websocket_client/wrapper_websocket_client.h
  src/wrappers/wrapper_websocket_client/wrapper_websocket_client.cpp
  src/wrappers/wrapper_https_client/wrapper_https_client.h
  src/wrappers/wrapper_https_client/wrapper_https_client.cpp
  src/sni_checker.cpp
  src/https_client.cpp
  src/websocket_client.cpp
  src/wrappers/utils/utils.h
  src/wrappers/utils/utils.cpp)

target_link_libraries(
  ${CMAKE_PROJECT_NAME}
  PRIVATE
        android
        log
        fptn::fptn
        nlohmann_json::nlohmann_json
        camouflage-tls  # FIXME
)

target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE FPTN_VERSION=\"0.0.0\")
set_target_properties(
        ${CMAKE_PROJECT_NAME}
        PROPERTIES CXX_STANDARD 20
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF)
