package com.ape.heartrate.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.ape.heartrate.R;
import com.ape.heartrate.base.BaseActivity;
import com.ape.heartrate.util.Util;
import com.ape.heartrate.view.AutoFitTextureView;
import com.ape.heartrate.view.ProgressWheel;
import com.ape.heartrate.view.ProgressWheelFitButton;
import com.ape.heartrate.view.TwinkleDrawable;
import com.ape.heartrate.view.WaveView;

import butterknife.BindView;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static com.ape.heartrate.activity.HeartRateModel.MEASURE_DURATION;

@RuntimePermissions
public class HeartRateActivity extends BaseActivity<HeartRatePresenter, HeartRateModel>
        implements HeartRateContract.View {
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
    private int mMeasureState = MEASURE_STATE_START;
    private TwinkleDrawable mHeartDrawable;
    private int mFinalHeartRate;
    private long mMeasureTime;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_history:
                gotoHistory();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

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

    @Override
    public void changeUiToInitial() {
        mTextureView.setVisibility(View.GONE);
        changeUiOnLabel(true, getString(R.string.measure_rate_default));
        mPwHeartrate.resetCount(false);
        mBtnToggle.setVisibility(View.VISIBLE);
        mIvLineStart.setVisibility(View.VISIBLE);
        mWvStart.setVisibility(View.INVISIBLE);
        mBtnToggle.setText(R.string.measure_btn_start);
        mTvDoneTimeMeasure.setVisibility(View.GONE);

        mMeasureState = MEASURE_STATE_START;
    }

    @Override
    public void onTick(long millisUntilFinished, int beatsAvg) {
        changeUiOnLabel(true, String.format("%03d", beatsAvg));
        mWvStart.startWave(beatsAvg);
        mPwHeartrate.incrementProgress();
    }

    @Override
    public void onFinished(int avgBeats) {
        mPresenter.stopMeasure();
        mFinalHeartRate = avgBeats;
        if (avgBeats > 0) {
            changeUiOnSuccessFinishMeasure();
        } else {
            changeUiOnFailFinishMeasure();
        }
    }

    @Override
    public void gotoHistory() {
        startActivity(new Intent(this, HistoryActivity.class));
    }

    @Override
    public void changeUiOnStartMeasure() {
        changeUiOnLabel(true, getString(R.string.measure_rate_default));
        mTextureView.setVisibility(View.VISIBLE);

        mHeartDrawable.startTwinkle();
        mPwHeartrate.resetCount(false);
        mBtnToggle.setVisibility(View.INVISIBLE);
        mIvLineStart.setVisibility(View.INVISIBLE);
        mWvStart.setVisibility(View.VISIBLE);
        mTvDoneTimeMeasure.setVisibility(View.GONE);

        mMeasureState = MEASURE_STATE_ING;
    }

    @Override
    public void changeUiOnSuccessFinishMeasure() {
        mTextureView.setVisibility(View.GONE);
        mHeartDrawable.stopTwinkle();
        mPwHeartrate.resetCount(false);
        mBtnToggle.setVisibility(View.VISIBLE);
        mIvLineStart.setVisibility(View.VISIBLE);
        mWvStart.setVisibility(View.INVISIBLE);
        mBtnToggle.setText(R.string.measure_btn_save);
        mTvDoneTimeMeasure.setVisibility(View.VISIBLE);
        mTvDoneTimeMeasure.setText(Util.getReadableDateTime(mMeasureTime));

        mMeasureState = MEASURE_STATE_SAVE;
    }

    @Override
    public void changeUiOnFailFinishMeasure() {
        mTextureView.setVisibility(View.GONE);
        mHeartDrawable.stopTwinkle();
        mPwHeartrate.resetCount(false);
        mBtnToggle.setVisibility(View.VISIBLE);
        mIvLineStart.setVisibility(View.VISIBLE);
        mWvStart.setVisibility(View.INVISIBLE);
        mBtnToggle.setText(R.string.measure_btn_fail);
        mTvDoneTimeMeasure.setVisibility(View.GONE);

        mMeasureState = MEASURE_STATE_FAIL;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPresenter.stopMeasure();
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
        mPwHeartrate.setMax(MEASURE_DURATION);
        mPwHeartrate.setOnSizeChangedListener(new ProgressWheel.SizeChangedListener() {
            @Override
            public void onSizeChanged(ProgressWheel wheel) {
                mBtnToggle.clip(wheel.getWidth(), wheel.getHeight(), wheel.getRimWidth());
            }
        });
        mHeartDrawable = new TwinkleDrawable(mIvHeartMeasure);
        mHeartDrawable.addDrawable(getResources().getDrawable(R.drawable.ic_heart_big), true);
        mHeartDrawable.addDrawable(getResources().getDrawable(R.drawable.ic_heart_small), false);
    }

    @Override
    public AutoFitTextureView getTextureView() {
        return mTextureView;
    }

    @Override
    public void keepScreenOn(boolean on) {
        if (on) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void showOpenCameraError() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.open_camera_error)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @OnClick(R.id.btn_toggle)
    public void onClick() {
        switch (mMeasureState) {
            case MEASURE_STATE_START:
            case MEASURE_STATE_FAIL:
                HeartRateActivityPermissionsDispatcher.startMeasureWithCheck(this);
                break;
            case MEASURE_STATE_SAVE:
                mPresenter.saveMeasure(mMeasureTime, mFinalHeartRate);
                break;
            case MEASURE_STATE_ING:
                break;
        }
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void startMeasure() {
        mMeasureTime = System.currentTimeMillis();
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
                .setCancelable(false)
                .setTitle(R.string.app_name)
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
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.app_name)
                .setMessage(R.string.request_permission_never_ask_again)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Util.startSettingsPermission();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

}
