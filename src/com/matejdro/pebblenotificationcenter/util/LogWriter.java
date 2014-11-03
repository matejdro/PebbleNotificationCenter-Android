package com.matejdro.pebblenotificationcenter.util;

import android.content.SharedPreferences;
import android.os.Environment;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by Matej on 27.10.2014.
 */
public class LogWriter
{
    private static final String ENABLE_LOG_WRITING = "enableLogWriter";
    private static SharedPreferences.OnSharedPreferenceChangeListener listener;
    private static FileWriter writer = null;

    public static void init()
    {
        final SharedPreferences preferences = PebbleNotificationCenter.getInMemorySettings().getSharedPreferences();
        if (preferences.getBoolean(ENABLE_LOG_WRITING, false))
            open();

        listener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                if (ENABLE_LOG_WRITING.equals(key))
                {
                    if (preferences.getBoolean(ENABLE_LOG_WRITING, false))
                        open();
                    else
                        close();
                }
            }
        };

        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private static void open()
    {
        File targetFolder = Environment.getExternalStoragePublicDirectory("NotificationCenter");
        if (!targetFolder.exists())
            targetFolder.mkdir();

        File file = new File(targetFolder, "log.txt");

        try
        {
            writer = new FileWriter(file, true);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private static void close()
    {
        if (writer == null)
            return;

        try
        {
            writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        writer = null;
    }

    public static void write(String text)
    {
        if (writer != null)
        {
            try
            {
                Calendar calendar = Calendar.getInstance();
                writer.write(calendar.get(Calendar.HOUR) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + ":" + calendar.get(Calendar.MILLISECOND) + " " + text + "\n");
                writer.flush();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }
}
