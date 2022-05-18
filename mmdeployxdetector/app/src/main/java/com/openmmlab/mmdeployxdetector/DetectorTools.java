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
package com.openmmlab.mmdeployxdetector;

public class DetectorTools
{
    public static native PointerWrapper[] pixArrToMat(int width, int height, int [] pixArr);
    public static native PointerWrapper createCppObject(String nativeClassName);
    public static native boolean drawResult(String [] className, int [] colors, PointerWrapper sourceMatPointer, PointerWrapper rgbPointer, PointerWrapper resultsPointer, PointerWrapper resultCountPointer);
    static {
        System.loadLibrary("detector");
    }
}