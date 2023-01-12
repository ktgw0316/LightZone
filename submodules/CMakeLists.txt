cmake_minimum_required(VERSION 3.12..16)
project(ProjectName CXX)

add_subdirectory(rawspeed)
add_subdirectory(LibRaw-cmake)
# target_link_libraries(ProjectName PRIVATE libraw::libraw)