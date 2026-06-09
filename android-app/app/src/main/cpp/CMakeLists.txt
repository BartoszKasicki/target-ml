cmake_minimum_required(VERSION 3.18.1)
project(przestrzeliny_ncnn)

# Wskazujemy ścieżki (upewnij się, że struktura folderów jest zgodna)
set(ncnn_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ncnn/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

# Tworzymy bibliotekę
add_library(ncnn_bridge SHARED ncnn_bridge.cpp)

#  Dodajemy ścieżki do nagłówków
target_include_directories(ncnn_bridge PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/ncnn/${ANDROID_ABI}/include)

# Biblioteki systemowe
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

# Linkowanie (używamy ncnn zamiast ncnn::ncnn)
target_link_libraries(ncnn_bridge
                      ncnn
                      ${log-lib}
                      ${jnigraphics-lib})