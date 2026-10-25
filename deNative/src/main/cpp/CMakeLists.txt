cmake_minimum_required(VERSION 3.22.1)
project(de_host)

add_library(de_host SHARED de_host.cpp)

find_library(log-lib log)
target_link_libraries(de_host ${log-lib})
