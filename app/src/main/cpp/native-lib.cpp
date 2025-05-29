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

extern "C" JNIEXPORT jfloat JNICALL
Java_com_example_cameraxapp_MainActivity_analyzeFrameNative(
        JNIEnv* env, jobject /* this */,
        jbyteArray frameData, jint width, jint height) {

    jbyte* dataPtr = env->GetByteArrayElements(frameData, nullptr);
    cv::Mat gray(height, width, CV_8UC1, reinterpret_cast<unsigned char*>(dataPtr));

    cv::Scalar avg = cv::mean(gray);

    env->ReleaseByteArrayElements(frameData, dataPtr, 0);

    LOGD("Native brightness: %.2f", avg[0]);

    return static_cast<jfloat>(avg[0]);
}
