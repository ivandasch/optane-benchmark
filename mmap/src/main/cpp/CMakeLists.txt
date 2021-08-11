cmake_minimum_required(VERSION 2.8.9)
project(mmap)
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -shared -D_FORTIFY_SOURCE=2 -z noexecstack -z,relro -z,now -Wformat -Wformat-security -Werror=format-security")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -pie")

find_package(Java REQUIRED)
find_package(JNI REQUIRED)
find_package(Threads REQUIRED)
include_directories(${CMAKE_THREAD_LIBS_INIT})

find_library(LIBPMEM_LIBRARIES NAMES libpmem.a libpmem.so)
if (NOT LIBPMEM_LIBRARIES)
    message(FATAL_ERROR "not found pmem library")
endif (NOT LIBPMEM_LIBRARIES)

include_directories(${JAVA_INCLUDE_PATH})
include_directories(${JAVA_INCLUDE_PATH2})

file(GLOB SOURCES "*.cpp")
add_library(mmap SHARED ${SOURCES})
target_link_libraries(mmap ${LIBPMEM_LIBRARIES})
