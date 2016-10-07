package com.ape.heartrate.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.ape.heartrate.R;
import com.ape.heartrate.base.BaseActivity;
import com.ape.heartrate.camera.CameraManager;
import com.ape.heartrate.util.Util;
import com.ape.heartrate.view.AutoFitTextureView;
import com.ape.heartrate.view.ProgressWheel;
import com.ape.heartrate.view.ProgressWheelFitButton;
import com.ape.heartrate.view.WaveView;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class HeartRateActivity extends BaseActivity<HeartRatePresenter, HeartRateModel>
        implements HeartRateContract.View, TextureView.SurfaceTextureListener {
    private static final String TAG = "HeartRateActivity";
    private static final int MEASURE_STATE_START = 0;
    private static final int MEASURE_STATE_SAVE = 1;
    private static final int MEASURE_STATE_FAIL = 2;
    private static final int MEASURE_STATE_ING = 3;

    @BindView(R.id.tv_data_measure)
    TextView mTvDataMeasure;
    @BindView(R.id.tv_error_measure)
    TextView mTvErrorMeasure;
    @BindView(R.id.iv_heart_measure)
    ImageView mIvHeartMeasure;
    @BindView(R.id.tv_done_time_measure)
    TextView mTvDoneTimeMeasure;
    @BindView(R.id.wv_start)
    WaveView mWvStart;
    @BindView(R.id.iv_line_start)
    ImageView mIvLineStart;
    @BindView(R.id.btn_toggle)
    ProgressWheelFitButton mBtnToggle;
    @BindView(R.id.pw_heartrate)
    ProgressWheel mPwHeartrate;
    @BindView(R.id.texture_view)
    AutoFitTextureView mTextureView;
    private CameraManager mCameraManager;
    private int mMeasureState = MEASURE_STATE_START;

    @Override
    protected void onResume() {
        super.onResume();
        changeUiToInitial();
    }

    @Override
    public void onBackPressed() {
        if (mMeasureState == MEASURE_STATE_SAVE || mMeasureState == MEASURE_STATE_FAIL) {
            changeUiToInitial();
        } else {
            super.onBackPressed();
        }
    }

    private void changeUiOnLabel(boolean success, String text) {
        mTvDataMeasure.setText(text);
    }

    private void changeUiToInitial() {
        changeUiOnLabel(true, getString(R.string.measure_rate_default));
        mPwHeartrate.resetCount(false);
        mBtnToggle.setVisibility(View.VISIBLE);
        mIvLineStart.setVisibility(View.VISIBLE);
        mWvStart.setVisibility(View.INVISIBLE);
        mWvStart.clearAnimation();
        mBtnToggle.setText(R.string.measure_btn_start);
        mTvDoneTimeMeasure.setVisibility(View.GONE);

        mMeasureState = MEASURE_STATE_START;
    }

    private void changeUiOnStartMeasure() {
        changeUiOnLabel(true, getString(R.string.measure_rate_default));
        mPwHeartrate.resetCount(false);
        mBtnToggle.setVisibility(View.INVISIBLE);
        mIvLineStart.setVisibility(View.INVISIBLE);
        mWvStart.setVisibility(View.VISIBLE);
        mTvDoneTimeMeasure.setVisibility(View.GONE);

        mMeasureState = MEASURE_STATE_ING;
    }

    private void changeUiOnSuccessFinishMeasure() {
        mPwHeartrate.resetCount(false);
        mBtnToggle.setVisibility(View.VISIBLE);
        mIvLineStart.setVisibility(View.VISIBLE);
        mWvStart.setVisibility(View.INVISIBLE);
        mBtnToggle.setText(R.string.measure_btn_save);
        mTvDoneTimeMeasure.setVisibility(View.VISIBLE);
        mTvDoneTimeMeasure.setText(Util.getReadableDateTime(System.currentTimeMillis()));

        mMeasureState = MEASURE_STATE_SAVE;
    }

    private void changeUiOnFailFinishMeasure() {
        mPwHeartrate.resetCount(false);
        mBtnToggle.setVisibility(View.VISIBLE);
        mIvLineStart.setVisibility(View.VISIBLE);
        mWvStart.setVisibility(View.INVISIBLE);
        mBtnToggle.setText(R.string.measure_btn_fail);
        mTvDoneTimeMeasure.setVisibility(View.GONE);

        mMeasureState = MEASURE_STATE_FAIL;
    }

    private void openCamera(int width, int height) {
        if (mCameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            mCameraManager.openDriver(mTextureView.getSurfaceTexture());
            mCameraManager.startPreview();
            mCameraManager.requestPreviewFrame(new Handler(), 0);
            configureTransform(width, height);
        } catch (IOException | RuntimeException e) {
            displayFrameworkBugMessageAndExit();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mCameraManager.getCameraResolution().y, mCameraManager.getCameraResolution().x);
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

    private void displayFrameworkBugMessageAndExit() {
        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setCancelable(false);
        builder.setMessage("相机打开出错，请稍后重试");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }

        });
        builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mCameraManager != null)
            mCameraManager.closeDriver();
        if (mTextureView.isAvailable()) {
            mTextureView.setSurfaceTextureListener(null);
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_heart_rate;
    }

    @Override
    public void initPresenter() {
        mPresenter.setVM(this, mModel);
    }

    @Override
    public void initView() {

        mPwHeartrate.setOnSizeChangedListener(new ProgressWheel.SizeChangedListener() {
            @Override
            public void onSizeChanged(ProgressWheel wheel) {
                mBtnToggle.clip(wheel.getWidth(), wheel.getHeight(), wheel.getRimWidth());
            }
        });
    }

    @Override
    public void openCamera() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCameraManager = new CameraManager(getApplication());
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    @OnClick(R.id.btn_toggle)
    public void onClick() {
        HeartRateActivityPermissionsDispatcher.startMeasureWithCheck(this);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void startMeasure() {
        mPresenter.startMeasure();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        HeartRateActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void onShowRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.request_permission)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void onPermissionDenied() {
        Snackbar.make(mWvStart, R.string.request_permission_denied, Snackbar.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    void onNeverAskAgain() {
        Snackbar.make(mWvStart, R.string.request_permission_never_ask_again, Snackbar.LENGTH_LONG)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Util.startSettingsPermission();
                    }
                }).show();
    }


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
}
