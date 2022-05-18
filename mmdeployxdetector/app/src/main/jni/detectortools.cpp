// Copyright (c) OpenMMLab. All rights reserved.
// This file is modified from https://github.com/nihui/ncnn-android-nanodet and
// https://github.com/EdVince/Android_learning/tree/main/ncnnnanodetCameraX

// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>
#include <unistd.h>

#include "detector.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static int draw_fps(int w, int h, cv::Mat& rgb)
{
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "%dx%d FPS=%.2f", w, h, avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

extern "C" {

JNIEXPORT jobjectArray JNICALL Java_com_openmmlab_mmdeployxdetector_DetectorTools_pixArrToMat(JNIEnv* env, jclass thizClazz, jint jw, jint jh, jintArray jPixArr)
{
    // this function returns 3 mats, so we use jobjectArray instead of jobject.
    jint *cPixArr = env->GetIntArrayElements(jPixArr, JNI_FALSE);
    if (cPixArr == NULL) {
        return JNI_FALSE;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 用传入的数组构建Mat，然后从RGBA转成RGB
    cv::Mat mat_image_src(jh, jw, CV_8UC4, (unsigned char *) cPixArr);
    cv::Mat rgb;
    cvtColor(mat_image_src, rgb, cv::COLOR_RGBA2RGB, 3);
    mm_mat_t* mat = new mm_mat_t{rgb.data, rgb.rows, rgb.cols, 3, MM_BGR, MM_INT8};

    jclass clazz = env->FindClass("com/openmmlab/mmdeployxdetector/PointerWrapper");
    jfieldID id_address = env->GetFieldID(clazz, "address", "J");
    jobject pMat;
    env->SetLongField(pMat, id_address, (long)mat);
    jobject pMatImageSrc;
    env->SetLongField(pMatImageSrc, id_address, (long)&mat_image_src);
    jobject pRgb;
    env->SetLongField(pRgb, id_address, (long)&rgb);
    jobjectArray imageArray = env->NewObjectArray(3, clazz, 0);
    env->SetObjectArrayElement(imageArray, 0, pMat);
    env->SetObjectArrayElement(imageArray, 1, pMatImageSrc);
    env->SetObjectArrayElement(imageArray, 2, pRgb);
    return imageArray;
}
JNIEXPORT jobject JNICALL Java_com_openmmlab_mmdeployxdetector_DetectorTools_createCppObject(JNIEnv* env, jclass thizclazz, jstring nativeClassName)
{
    const char* className = env->GetStringUTFChars(nativeClassName, 0);
    if (className == "mm_detect_t") {
        jobject pResult;
        mm_detect_t* result = new mm_detect_t;
        jclass clazz = env->FindClass("com/openmmlab/mmdeployxdetector/PointerWrapper");
        jfieldID id_address = env->GetFieldID(clazz, "address", "J");
        env->SetLongField(pResult, id_address, (long)result);
        return pResult;
    }
    else if (className == "int") {
        jobject pCount;
        int* count = new int; //is it int* or int **?
        jclass clazz = env->FindClass("com/openmmlab/mmdeployxdetector/PointerWrapper");
        jfieldID id_address = env->GetFieldID(clazz, "address", "J");
        env->SetLongField(pCount, id_address, (long)count);
        return pCount;
    }
}
JNIEXPORT jboolean JNICALL Java_com_openmmlab_mmdeployxdetector_DetectorTools_drawResult(JNIEnv* env, jclass thizclazz, jobjectArray className, jintArray colors, jobject jSourceMat, jobject jRgb, jobject jResult, jobject jResultCount)
{
    int color_index = 0;
    int* pcolor = env->GetIntArrayElements(colors, 0);
    int color_length = env->GetArrayLength(colors);
    assert(("length of colors should divided by 3!", color_length % 3 == 0));
    int color_count = color_length / 3;
    jclass clazzSourceMat = env->GetObjectClass(jSourceMat);
    jfieldID id_source_mat_address = env->GetFieldID(clazzSourceMat, "address", "J");
    cv::Mat mat_image_src = *(cv::Mat*) env->GetLongField(jSourceMat, id_source_mat_address);
    jclass clazzRgb = env->GetObjectClass(jRgb);
    jfieldID id_rgb_address = env->GetFieldID(clazzRgb, "address", "J");
    cv::Mat rgb = *(cv::Mat*) env->GetLongField(jRgb, id_rgb_address);
    jclass clazzBboxes = env->GetObjectClass(jResult);
    jfieldID id_bboxes_address = env->GetFieldID(clazzBboxes, "address", "J");
    mm_detect_t *bboxes = (mm_detect_t *)env->GetLongField(jResult, id_bboxes_address);
    jclass clazzResultCount = env->GetObjectClass(jResultCount);
    jfieldID id_result_count_address = env->GetFieldID(clazzResultCount, "address", "J");
    int* res_count = (int *)env->GetLongField(jResultCount, id_result_count_address);
    for (int i = 0; i < *res_count; i++)
    {
        const mm_detect_t& det_result = bboxes[i];
        // skip detections with invalid bbox size (bbox height or width < 1)
        if ((det_result.bbox.right - det_result.bbox.left) < 1 || (det_result.bbox.bottom - det_result.bbox.top) < 1) {
            continue;
        }
        // skip detections less than specified score threshold
        if (det_result.score < 0.3) {
            continue;
        }
        const int* color = pcolor + (color_index % color_count) * 3;
        color_index++;

        cv::Scalar cc((const unsigned char)color[0], (const unsigned char)color[1], (const unsigned char)color[2]);
        cv::rectangle(rgb, cv::Point{(int)det_result.bbox.left, (int)det_result.bbox.top},
            cv::Point{(int)det_result.bbox.right, (int)det_result.bbox.bottom}, cc, 2);

        char text[256];
        std::string detected_class_name;
        jstring jclassName = (jstring)env->GetObjectArrayElement(className, det_result.label_id);
        detected_class_name = (std::string)env->GetStringUTFChars(jclassName, 0);
        sprintf(text, "%s %.1f%%", detected_class_name.c_str(), det_result.score * 100);

        int baseLine = 0;
        cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

        int x = (int)det_result.bbox.left;
        int y = (int)det_result.bbox.top - label_size.height - baseLine;
        if (y < 0)
            y = 0;
        if (x + label_size.width > rgb.cols)
            x = rgb.cols - label_size.width;

        cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)), cc, -1);

        cv::Scalar textcc = (color[0] + color[1] + color[2] >= 381) ? cv::Scalar(0, 0, 0) : cv::Scalar(255, 255, 255);

        cv::putText(rgb, text, cv::Point(x, y + label_size.height), cv::FONT_HERSHEY_SIMPLEX, 0.5, textcc, 1);
    }
    draw_fps(rgb.cols, rgb.rows, rgb);
    // refresh java.
    cvtColor(rgb, mat_image_src, cv::COLOR_RGB2RGBA, 4);
    ////////////////////////////////////////////////////////////////////////////////////////////////
    return JNI_TRUE;
}

}
