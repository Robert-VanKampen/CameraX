cmake_minimum_required(VERSION 3.10.2)
project("cameraxapp")

# Set path to OpenCV
set(OpenCV_DIR /home/robert/android/opencv/sdk/native/jni)

# Find OpenCV package
find_package(OpenCV REQUIRED)

# Your native source file(s)
add_library(
    native-lib
    SHARED
    native-lib.cpp
)

# Include native headers (optional)
target_include_directories(
    native-lib
    PRIVATE ${OpenCV_INCLUDE_DIRS}
)

# Link OpenCV and Android log
target_link_libraries(
    native-lib
    ${OpenCV_LIBS}
    log
)
