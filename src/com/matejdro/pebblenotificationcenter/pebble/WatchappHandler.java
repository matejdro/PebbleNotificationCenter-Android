package com.matejdro.pebblenotificationcenter.pebble;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import com.matejdro.pebblenotificationcenter.R;

public class WatchappHandler {
    public static final int SUPPORTED_PROTOCOL = 16;

    public static boolean isFirstRun(SharedPreferences settings)
    {
        return settings.getBoolean("FirstRun", false);
    }

	public static void displayNotification(final Context context, final SharedPreferences.Editor editor)
	{
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.pebbleAppInstallDialog).setNegativeButton(
                R.string.no, null).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openPebbleApp(context, editor);
            }
        }).show();


	}

    public static void openPebbleApp(Context context, SharedPreferences.Editor editor)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setData(Uri.parse("pebble://appstore/531c8f3646b8f200dd00018d"));
        try
        {
            context.startActivity(intent);

            editor.putBoolean("FirstRun", true);
            editor.apply();
        }
        catch (ActivityNotFoundException e)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.openingPebbleAppFailed).setNegativeButton("OK", null).show();
        }
    }
}
