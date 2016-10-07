package com.ape.heartrate;

import android.app.Application;
import android.content.Context;

/**
 * Created by android on 16-10-7.
 */

public class App extends Application {
    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }
}
