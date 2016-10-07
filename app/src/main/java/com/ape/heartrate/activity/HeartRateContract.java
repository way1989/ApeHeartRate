package com.ape.heartrate.activity;

import com.ape.heartrate.base.BaseModel;
import com.ape.heartrate.base.BasePresenter;
import com.ape.heartrate.base.BaseView;

/**
 * Created by android on 16-10-7.
 */

public class HeartRateContract {
    public interface Model extends BaseModel {
    }

    public interface View extends BaseView {
        void openCamera();
    }

    public abstract static class Presenter extends BasePresenter<View, Model> {
        abstract void startMeasure();
    }
}
