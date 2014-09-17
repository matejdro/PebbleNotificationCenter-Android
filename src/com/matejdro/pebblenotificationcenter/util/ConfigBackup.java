package com.matejdro.pebblenotificationcenter.util;

import android.content.Context;
import android.os.Environment;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;

/**
 * Created by Matej on 17.9.2014.
 */
public class ConfigBackup
{
    public static void backup(Context context)
    {
        File targetFolder = Environment.getExternalStoragePublicDirectory("NotificationCenter");
        if (!targetFolder.exists())
            targetFolder.mkdir();

        targetFolder = new File(targetFolder, "settings");
        if (!targetFolder.exists())
            targetFolder.mkdir();

        File[] existingBackup = targetFolder.listFiles();
        for (File f : existingBackup)
            f.delete();

        File sourceFolder = context.getFilesDir();

        sourceFolder = new File(sourceFolder, "../shared_prefs");

        for (File file : sourceFolder.listFiles())
        {
            if (file.getName().startsWith("com.crash"))
                continue;

            File target = new File(targetFolder, file.getName());
            try
            {
                Files.copy(file, target);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static boolean restore(Context context)
    {
        File sourceFolder = Environment.getExternalStoragePublicDirectory("NotificationCenter");
        if (!sourceFolder.exists())
            return false;

        sourceFolder = new File(sourceFolder, "settings");
        if (!sourceFolder.exists())
            return false;


        File targetFolder = context.getFilesDir();
        targetFolder = new File(targetFolder, "../shared_prefs");

        File[] existingConfig = targetFolder.listFiles();
        for (File f : existingConfig)
        {
            if (f.getName().startsWith("com.crash"))
                continue;

            f.delete();
        }

        for (File file : sourceFolder.listFiles())
        {
            if (!file.getName().endsWith(".xml"))
                continue;

            File target = new File(targetFolder, file.getName());

            try
            {
                Files.copy(file, target);
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            context.getSharedPreferences(file.getName().substring(0, file.getName().length() - 4), Context.MODE_MULTI_PROCESS);
        }

        return true;
    }
}
