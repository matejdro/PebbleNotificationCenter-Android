package com.matejdro.pebblenotificationcenter.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.matejdro.pebblenotificationcenter.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Created by Matej on 23.3.2014.
 */
public class CrashLogger extends BroadcastReceiver
{
    public static void report(Context context, Throwable throwable)
    {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("bugReports", true))
            return;

        Random random = new Random();
        int notificationId = random.nextInt();

        String stackTrace = Log.getStackTraceString(throwable);

        Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
        String uriText = "mailto:" + Uri.encode("matejdro@gmail.com") +
                "?subject=" + Uri.encode("Notification Center Crash report") +
                "&body=" + Uri.encode(stackTrace);
        Uri uri = Uri.parse(uriText);
        mailIntent.setData(uri);
        mailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent notificationIntent = new Intent("com.matejdro.pebblenotificationcenter.BUG_REPORT");
        notificationIntent.putExtra("mailIntent", mailIntent);
        notificationIntent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, random.nextInt(), notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).
                setContentTitle("Notification Center crashed").
                setContentText("Please press here to send error report").
                setSmallIcon(R.drawable.icon).
                setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());

    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent mailIntent = intent.getParcelableExtra("mailIntent");
        int notificationId = intent.getIntExtra("notificationId", -1);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);

        Intent activityIntent = Intent.createChooser(mailIntent, "Send Bug report");
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(activityIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "No mail app found!", Toast.LENGTH_LONG).show();
        }
    }
}
