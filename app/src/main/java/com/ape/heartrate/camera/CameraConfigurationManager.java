/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ape.heartrate.camera;

import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;

import java.util.List;

/**
 * 邮箱: 1076559197@qq.com | tauchen1990@gmail.com
 * <p/>
 * 作者: 陈涛
 * <p/>
 * 日期: 2014年8月20日
 * <p/>
 * 描述: 该类主要负责设置相机的参数信息，获取最佳的预览界面
 */
public final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";

    // 相机分辨率
    private Point cameraResolution;

    public CameraConfigurationManager() {
    }

    public void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();

        cameraResolution = findSmallestPreviewSize(parameters);

        Log.i(TAG, "Camera resolution x=" + cameraResolution.x + ", y=" + cameraResolution.y);
    }

    private Point findSmallestPreviewSize(Camera.Parameters parameters) {
        Camera.Size defaultSize = parameters.getPreviewSize();

        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Log.w(TAG, "Device returned no supported preview sizes; using default");
            return new Point(defaultSize.width, defaultSize.height);
        }
        for (Camera.Size size : rawSupportedSizes) {
            if (size.width <= defaultSize.width && size.height <= defaultSize.height) {
                defaultSize = size;
            }
        }
        return new Point(defaultSize.width, defaultSize.height);
    }


    public void setDesiredCameraParameters(Camera camera, boolean safeMode) {
        Camera.Parameters parameters = camera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
        }

        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
        camera.setParameters(parameters);

        Camera.Parameters afterParameters = camera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (cameraResolution.x != afterSize.width || cameraResolution.y != afterSize.height)) {
            Log.w(TAG, "Camera said it supported preview size " + cameraResolution.x + 'x' + cameraResolution.y + ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
            cameraResolution.x = afterSize.width;
            cameraResolution.y = afterSize.height;
        }

        /** 设置相机预览为竖屏 */
        camera.setDisplayOrientation(90);
    }

    public Point getCameraResolution() {
        return cameraResolution;
    }

}
