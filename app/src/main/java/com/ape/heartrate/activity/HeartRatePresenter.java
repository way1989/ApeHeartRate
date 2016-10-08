package com.ape.heartrate.activity;

import android.util.Log;

import com.ape.heartrate.control.MeasureControl;


/**
 * Created by android on 16-10-7.
 */

public class HeartRatePresenter extends HeartRateContract.Presenter
        implements HeartRateModel.Callback {
    private static final String TAG = "HeartRatePresenter";
    private MeasureControl mControl;

    @Override
    public void onStart() {
        super.onStart();
        mModel.setTextureView(mView.getTextureView());
        mControl = new MeasureControl();
    }

    @Override
    void startMeasure() {
        mModel.startMeasure(this);
        mView.keepScreenOn(true);
        mView.changeUiOnStartMeasure();
    }

    @Override
    void stopMeasure() {
        mModel.stopMeasure();
        mView.keepScreenOn(false);
    }

    @Override
    void saveMeasure(long time, int rate) {
        mControl.saveMeasure(time, rate);
        mView.gotoHistory();
    }

    @Override
    public void showOpenCameraError() {
        mView.showOpenCameraError();
    }

    @Override
    public void onTick(long millisUntilFinished, int beatsAvg) {
        Log.i(TAG, "beat = " + beatsAvg);
        mView.onTick(millisUntilFinished, beatsAvg);
    }

    @Override
    public void onFinished(int avgBeats) {
        mView.onFinished(avgBeats);
    }
}
