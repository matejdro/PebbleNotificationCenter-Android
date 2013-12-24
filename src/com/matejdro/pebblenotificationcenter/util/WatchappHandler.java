package com.matejdro.pebblenotificationcenter.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

public class WatchappHandler {
	public static final int INCLUDED_WATCHFACE = 6;

	public static boolean isLatest(SharedPreferences settings)
	{
		return settings.getInt("InstalledWatchface", -1) >= INCLUDED_WATCHFACE;
	}

	public static void install(Context context, Editor editor)
	{
		File publicDir = context.getExternalCacheDir();

		File watchappFile = new File(publicDir, "notificationcenter.pbw");

		//Copy file from assets
		try
		{
			InputStream myInput = context.getAssets().open("notificationcenter.pbw");
			OutputStream myOutput = new FileOutputStream(watchappFile);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = myInput.read(buffer))>0){
				myOutput.write(buffer, 0, length);
			}

			myOutput.close();
			myInput.close();

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
		
		intent.setDataAndType(Uri.fromFile(watchappFile), "application/octet-stream");
		try
		{
			context.startActivity(intent);
			
			editor.putInt("InstalledWatchface", INCLUDED_WATCHFACE);
			editor.apply();
		}
		catch (ActivityNotFoundException e)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage("Watchapp installation has failed. Do you have Pebble app installed?").setNegativeButton("OK", null).show();
		}
	}
}
