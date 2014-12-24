package com.matejdro.pebblenotificationcenter.util;

import android.content.Context;

/**
 * Created by Matej on 21.10.2014.
 */
public class ViewUtil
{
    public static float getDensity(Context context)
    {
        return context.getResources().getDisplayMetrics().density;
    }
}
