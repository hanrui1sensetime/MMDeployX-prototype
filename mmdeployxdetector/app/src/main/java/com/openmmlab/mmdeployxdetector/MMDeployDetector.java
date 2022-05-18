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

public class MMDeployDetector
{
    static {
        System.loadLibrary("javaapis");
    }
    public native boolean mmdeployDetectorCreateByPath(String modelPath, String deviceName, int deviceID, PointerWrapper handlePointer);
    public native boolean mmdeployDetectorApply(PointerWrapper handle, PointerWrapper matsPointer, int matCount, PointerWrapper resultsPointer, PointerWrapper resultCountPointer);
    public native void mmdeployDetectorReleaseResult(PointerWrapper resultsPointer, PointerWrapper resultCountPointer, int count);
    public native void mmdeployDetectorDestroy(PointerWrapper handle);
}
