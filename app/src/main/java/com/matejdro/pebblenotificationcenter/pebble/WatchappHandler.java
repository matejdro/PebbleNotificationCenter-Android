package com.matejdro.pebblenotificationcenter.pebble;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.matejdro.pebblenotificationcenter.R;

public class WatchappHandler extends BroadcastReceiver
{
    public static final int SUPPORTED_PROTOCOL = 32;
    public static final String INTENT_UPDATE_WATCHAPP = "com.matejdro.pebblenotificationcenter.UPDATE_WATCHAPP";

    public static final String WATCHAPP_URL = "https://dl.dropboxusercontent.com/u/6999250/dialer/Center/notificationcenter.pbw";
    //public static final String WATCHAPP_URL = "https://dl.dropboxusercontent.com/u/6999250/dialer/Center/beta/notificationcenter.pbw";


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

        editor.putBoolean("FirstRun", true);
        editor.apply();
	}

    public static void openPebbleApp(Context context, SharedPreferences.Editor editor)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setData(Uri.parse("pebble://appstore/531c8f3646b8f200dd00018d"));
        try
        {
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.openingPebbleAppFailed).setNegativeButton("OK", null).show();
        }
    }

    public static void showUpdateNotification(Context context)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context).setSmallIcon(R.drawable.notificationicon).setLocalOnly(true)
                        .setContentTitle("Notification Center watchapp update").setContentText("Click on this notiifcation to update Notification Center watchapp on Pebble")
                        .setContentIntent(PendingIntent.getBroadcast(context, 1, new Intent(INTENT_UPDATE_WATCHAPP), PendingIntent.FLAG_CANCEL_CURRENT));
        NotificationManagerCompat.from(context).notify(1, mBuilder.build());
    }

    public static void openUpdateWebpage(Context context)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setData(Uri.parse(WATCHAPP_URL));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try
        {
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e)
        {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (INTENT_UPDATE_WATCHAPP.equals(intent.getAction()))
        {
            openUpdateWebpage(context);
        }
    }
}
