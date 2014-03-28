package com.matejdro.pebblenotificationcenter.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class WatchappHandler {
	public static final int SUPPORTED_PROTOCOL = 10;

	public static boolean isFirstRun(SharedPreferences settings)
	{
		return settings.getBoolean("FirstRun", false);
	}

	public static void displayNotification(Context context, Editor editor)
	{
		editor.putBoolean("FirstRun", true);
		editor.apply();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Don't forget to also install Notification Center from Pebble appstore!").setNegativeButton("OK", null).show();
	}
}
