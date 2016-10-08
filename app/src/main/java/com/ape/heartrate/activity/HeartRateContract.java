package com.ape.heartrate.activity;

import android.support.annotation.NonNull;

import com.ape.heartrate.base.BaseModel;
import com.ape.heartrate.base.BasePresenter;
import com.ape.heartrate.base.BaseView;
import com.ape.heartrate.view.AutoFitTextureView;

/**
 * Created by android on 16-10-7.
 */

public class HeartRateContract {
    public interface Model extends BaseModel {
        void setTextureView(@NonNull AutoFitTextureView textureView);

        void startMeasure(@NonNull HeartRateModel.Callback callback);

        void stopMeasure();
    }

    public interface View extends BaseView {
        AutoFitTextureView getTextureView();

        void keepScreenOn(boolean on);

        void showOpenCameraError();


        void changeUiOnStartMeasure();

        void onTick(long millisUntilFinished, int beatsAvg);

        void onFinished(int avgBeats);

        void gotoHistory();
    }

    public abstract static class Presenter extends BasePresenter<View, Model> {
        abstract void startMeasure();

        abstract void stopMeasure();

        abstract void saveMeasure(long time, int rate);
    }
}
