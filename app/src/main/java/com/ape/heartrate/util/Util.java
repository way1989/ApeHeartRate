package com.ape.heartrate.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import com.ape.heartrate.App;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by way on 2016/10/7.
 */

public class Util {
    private static final String PACKAGE_URI_PREFIX = "package:";
    private static final String DATE_TIME_FORMAT = "yy-M-d a H:m";

    public static void startSettingsPermission() {
        final Context context = App.getContext();
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse(PACKAGE_URI_PREFIX + context.getPackageName()));
        if (!(context instanceof Activity))
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static String getReadableDateTime(long time) {
        //todo if it take bad performance or take much more memory,then the android.text.format.DateFormat will be thought.
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
        return sdf.format(new Date(time));
    }
}
