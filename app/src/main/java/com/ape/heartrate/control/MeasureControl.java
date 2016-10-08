package com.ape.heartrate.control;


import android.util.Log;

import com.ape.heartrate.App;
import com.ape.heartrate.control.util.DbUtil;
import com.ape.heartrate.greendao.HistoryEntity;


public class MeasureControl {
    private static final String TAG = "MeasureControl";
    private int mHeartRate;


    public MeasureControl() {
    }

    private void log(String msg) {
        Log.i(TAG, msg);
    }

    public void saveMeasure(long time, int rate) {
        HistoryEntity historyEntity = new HistoryEntity();
        historyEntity.setRate(rate);
        historyEntity.setCalculateTime(time);
        DbUtil.save(App.getContext(), historyEntity);
    }

    public int getHeartRate() {
        int tmp = mHeartRate;
        mHeartRate = 0;
        return tmp;
    }

    public void clearHeartRate() {
        mHeartRate = 0;
    }


    public void onDestroy() {
    }
}
