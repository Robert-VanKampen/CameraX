#include <jni.h>
#include <string>
#include <opencv2/core.hpp>

#include <android/log.h>
#include <opencv2/core.hpp>

#define LOG_TAG "NativeCV"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#include <jni.h>
#include <string>
#include <opencv2/core.hpp>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_cameraxapp_MainActivity_stringFromJNI(JNIEnv* env, jobject) {
    std::string version = cv::getVersionString();  // Get OpenCV version
    std::string message = "Hello from C++ !\nWe're using OpenCV version: " + version;
    return env->NewStringUTF(message.c_str());
}

#include <opencv2/imgproc.hpp>
#include <sstream>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_cameraxapp_MainActivity_analyzeFrameNative(
        JNIEnv* env, jobject /* this */,
        jbyteArray frameData, jint width, jint height) {

    jbyte* dataPtr = env->GetByteArrayElements(frameData, nullptr);
    cv::Mat gray(height, width, CV_8UC1, reinterpret_cast<unsigned char*>(dataPtr));
    cv::Mat edges;

    // Canny edge detection
    cv::Canny(gray, edges, 100, 200);

    // Hough line detection
    std::vector<cv::Vec2f> lines;
    cv::HoughLines(edges, lines, 1, CV_PI / 180, 150);

    // Build return string
    std::ostringstream oss;
    for (const auto& line : lines) {
        float rho = line[0];
        float theta = line[1];
        oss << rho << "," << theta << ";";
    }
    env->ReleaseByteArrayElements(frameData, dataPtr, 0);
    return env->NewStringUTF(oss.str().c_str());
}

/*
extern "C" JNIEXPORT jfloat JNICALL
Java_com_example_cameraxapp_MainActivity_analyzeFrameNative(
        JNIEnv* env, jobject
 jbyteArray frameData, jint width, jint height) {

    jbyte* dataPtr = env->GetByteArrayElements(frameData, nullptr);
    cv::Mat gray(height, width, CV_8UC1, reinterpret_cast<unsigned char*>(dataPtr));

    cv::Scalar avg = cv::mean(gray);

    env->ReleaseByteArrayElements(frameData, dataPtr, 0);

    LOGD("Native brightness: %.2f", avg[0]);

    return static_cast<jfloat>(avg[0]);
}
*/

