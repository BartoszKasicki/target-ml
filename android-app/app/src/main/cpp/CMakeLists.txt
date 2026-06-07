cmake_minimum_required(VERSION 3.18.1)
project(przestrzeliny_ncnn)

# 1. Wskazujemy ścieżki (upewnij się, że struktura folderów jest zgodna)
set(ncnn_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ncnn/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

# 2. Tworzymy bibliotekę
add_library(ncnn_bridge SHARED ncnn_bridge.cpp)

# 3. Dodajemy ścieżki do nagłówków
# Zakładamy, że folder 'include' z NCNN jest obok folderu lib
target_include_directories(ncnn_bridge PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/ncnn/${ANDROID_ABI}/include)

# 4. Biblioteki systemowe
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

# 5. Linkowanie (używamy ncnn zamiast ncnn::ncnn, bo taki target jest definiowany w Twoich plikach cmake)
target_link_libraries(ncnn_bridge
                      ncnn
                      ${log-lib}
                      ${jnigraphics-lib})