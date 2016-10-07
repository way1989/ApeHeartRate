package com.ape.heartrate.activity;

/**
 * Created by android on 16-10-7.
 */

public class HeartRatePresenter extends HeartRateContract.Presenter{
    @Override
    void startMeasure() {
        mView.openCamera();
    }
}
