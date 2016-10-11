package com.ape.heartrate.activity;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.ape.heartrate.App;
import com.ape.heartrate.camera.CameraManager;
import com.ape.heartrate.util.ImageProcessing;
import com.ape.heartrate.view.AutoFitTextureView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by android on 16-10-7.
 */

public class HeartRateModel implements HeartRateContract.Model {
    public static final int MEASURE_DURATION = 60;
    private static final String TAG = "HeartRateModel";
    private static final int MSG_HEART_RATE_PROGRESSING = 0;
    private Handler mWorkHandler;
    private CountDownTimer mCountDownTimer;
    private int mBeatsAvg;
    private int mFinalBeatsAvg;
    private Callback mCallback;
    private CameraManager mCameraManager;
    private AutoFitTextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    public void setTextureView(AutoFitTextureView textureView) {
        mTextureView = textureView;
    }

    @Override
    public void startMeasure(Callback callback) {
        mBeatsAvg = 0;
        mFinalBeatsAvg = 0;
        mCallback = callback;
        mCameraManager = new CameraManager(App.getContext());
        HeartRateThread handlerThread = new HeartRateThread(TAG);
        handlerThread.start();
        mWorkHandler = new Handler(handlerThread.getLooper(), handlerThread);
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        mCountDownTimer = new CountDownTimer(MEASURE_DURATION * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (mFinalBeatsAvg == 0) {
                    mCallback.onTick(millisUntilFinished, mBeatsAvg);
                }else {
                    mCallback.onFinished(mFinalBeatsAvg);
                }
            }

            @Override
            public void onFinish() {
                mCallback.onFinished(0);
            }
        };
        mCountDownTimer.start();
    }

    @Override
    public void stopMeasure() {
        if (mCountDownTimer != null)
            mCountDownTimer.cancel();
        if (mWorkHandler != null) {
            mWorkHandler.removeCallbacksAndMessages(null);
            mWorkHandler.getLooper().quitSafely();
        }
        if (mTextureView.isAvailable()) {
            mTextureView.setSurfaceTextureListener(null);
        }
        if (mCameraManager != null) {
            mCameraManager.stopPreview();
            mCameraManager.closeDriver();
        }
    }


    private void openCamera(int width, int height) {
        if (mCameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            mCameraManager.openDriver(mTextureView.getSurfaceTexture());
            mCameraManager.startPreview();
            mCameraManager.openLight();
            mCameraManager.requestPreviewFrame(mWorkHandler, MSG_HEART_RATE_PROGRESSING);
            configureTransform(width, height);
        } catch (IOException | RuntimeException e) {
            mCallback.showOpenCameraError();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {

        int rotation = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mCameraManager.getCameraResolution().y,
                mCameraManager.getCameraResolution().x);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mCameraManager.getCameraResolution().y,
                    (float) viewWidth / mCameraManager.getCameraResolution().x);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * 类型枚举
     */
    public enum TYPE {
        GREEN, RED
    }

    interface Callback {
        void showOpenCameraError();

        void onTick(long millisUntilFinished, int beatsAvg);

        void onFinished(int avgBeats);
    }

    private class HeartRateThread extends HandlerThread implements Handler.Callback {
        private static final int AVERAGE_ARRAY_SIZE = 4;
        //心跳数组的大小
        private static final int BEATS_ARRAY_SIZE = 3;
        private static final int AVERAGE_RATE_SIZE = 5;
        private final AtomicBoolean mIsProcessing = new AtomicBoolean(false);
        private final int[] mAverageArray = new int[AVERAGE_ARRAY_SIZE];
        //心跳数组
        private final int[] mBeatsArray = new int[BEATS_ARRAY_SIZE];
        private final int[] mAverageRate = new int[AVERAGE_RATE_SIZE];
        private int mAverageIndex = 0;
        //设置默认类型
        private TYPE mCurrentType = TYPE.GREEN;
        //心跳下标值
        private int mBeatsIndex = 0;
        //心跳脉冲
        private double mBeats = 0;
        //开始时间
        private long mStartTime = 0;
        private int mLastHeartRate;
        private int mRateIndex = 0;

        public HeartRateThread(String name) {
            super(name);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HEART_RATE_PROGRESSING:
                    //Log.i(TAG, "msg = " + msg);
                    if (measureHeartRate(msg)) return true;
                    break;
                default:
                    break;
            }
            return false;
        }

        private boolean measureHeartRate(Message msg) {
            byte[] data = (byte[]) msg.obj;
            if (data == null) {
                throw new NullPointerException();
            }
            if (!mIsProcessing.compareAndSet(false, true)) {
                return true;
            }
            int width = msg.arg1;
            int height = msg.arg2;
            //图像处理
            int imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), height, width);
            Log.i(TAG, "imgAvg = " + imgAvg);
            if(imgAvg < 200){
                HeartRateModel.this.mBeatsAvg = 0;
            }
            if (imgAvg == 0 || imgAvg == 255) {
                mIsProcessing.set(false);
                return true;
            }
            //计算平均值
            int averageArrayAvg = 0;
            int averageArrayCnt = 0;
            for (int i = 0; i < mAverageArray.length; i++) {
                if (mAverageArray[i] > 0) {
                    averageArrayAvg += mAverageArray[i];
                    averageArrayCnt++;
                }
            }

            //计算平均值
            int rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt) : 0;
            TYPE newType = mCurrentType;
            if (imgAvg < rollingAverage) {
                newType = TYPE.RED;
                if (newType != mCurrentType) mBeats++;
            } else if (imgAvg > rollingAverage) {
                newType = TYPE.GREEN;
            }

            if (mAverageIndex == AVERAGE_ARRAY_SIZE) {
                mAverageIndex = 0;
            }
            mAverageArray[mAverageIndex] = imgAvg;
            mAverageIndex++;

            if (newType != mCurrentType) {
                mCurrentType = newType;
            }
            if (calculateAverageBeat(imgAvg)) return true;

            mIsProcessing.set(false);
            return false;
        }

        private boolean calculateAverageBeat(int imgAvg) {
            //获取系统结束时间（ms）
            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - mStartTime) / 1000d;
            if (totalTimeInSecs >= 2) {
                double bps = (mBeats / totalTimeInSecs);
                int dpm = (int) (bps * 60d);
                if (dpm < 30 || dpm > 180 || imgAvg < 200) {
                    //获取系统开始时间（ms）
                    mStartTime = System.currentTimeMillis();
                    //beats心跳总数
                    mBeats = 0;
                    mIsProcessing.set(false);
                    return true;
                }

                if (mBeatsIndex == BEATS_ARRAY_SIZE) {
                    mBeatsIndex = 0;
                }
                mBeatsArray[mBeatsIndex] = dpm;
                mBeatsIndex++;

                int beatsArrayAvg = 0;
                int beatsArrayCnt = 0;
                for (int i = 0; i < mBeatsArray.length; i++) {
                    if (mBeatsArray[i] > 0) {
                        beatsArrayAvg += mBeatsArray[i];
                        beatsArrayCnt++;
                    }
                }
                int beatsAvg = (beatsArrayAvg / beatsArrayCnt);
                Log.i(TAG, "beatsAvg = " + beatsAvg);
                //获取系统时间（ms）
                mStartTime = System.currentTimeMillis();
                mBeats = 0;

                HeartRateModel.this.mBeatsAvg = beatsAvg;

                //计算最终结果,连续5次差距不超过10,就作为最终测试结果
                int distance = Math.abs(beatsAvg - mLastHeartRate);
                mLastHeartRate = beatsAvg;
                if (distance > 10) {
                    mRateIndex = 0;
                } else {
                    mAverageRate[mRateIndex] = beatsAvg;
                    mRateIndex++;
                }
                int rateSum = 0;
                if (mRateIndex == AVERAGE_RATE_SIZE) {
                    mRateIndex = 0;
                    for (int rate : mAverageRate) {
                        rateSum += rate;
                    }
                    final int avgRate = rateSum / AVERAGE_RATE_SIZE;
                    Log.i(TAG, "final avgRate = " + avgRate);
                    mFinalBeatsAvg = avgRate;
                }
            }
            return false;
        }
    }
}
