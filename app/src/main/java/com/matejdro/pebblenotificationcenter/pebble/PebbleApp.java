package com.matejdro.pebblenotificationcenter.pebble;

import android.content.Context;
import com.matejdro.pebblenotificationcenter.R;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Matej on 11.9.2014.
 */
public class PebbleApp
{
    private String name;
    protected UUID uuid;
    private int notificationMode;

    public String getName()
    {
        return name;
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public PebbleApp(String name, UUID uuid)
    {
        this.name = name;
        this.uuid = uuid;
        notificationMode = 0;
    }

    public int getNotificationMode()
    {
        return notificationMode;
    }

    public void setNotificationMode(int notificationMode)
    {
        this.notificationMode = notificationMode;
    }

    public static List<PebbleApp> getFromByteBuffer(ByteBuffer buffer)
    {
        buffer.getInt(); //Number of app slots on pebble (not needed)
        int numberOfInstalledApps = buffer.getInt();

        List<PebbleApp> apps = new ArrayList<PebbleApp>(numberOfInstalledApps + 2);

        for (int i = 0; i < numberOfInstalledApps; i++)
        {
            buffer.getInt(); //index of the app (not needed)
            buffer.getInt(); //ID of the app slot (not needed)
            String name = PebbleDeveloperConnection.getPebbleStringFromByteBuffer(buffer, 32);
            PebbleDeveloperConnection.getPebbleStringFromByteBuffer(buffer, 32); //Company name (not needed)
            buffer.getInt(); //Flags (not needed)
            buffer.getShort(); //Version (not needed)

            PebbleApp app = new PebbleApp(name, null);
            apps.add(app);
        }

        return apps;
    }

    public static List<PebbleApp> getSystemApps(Context context)
    {
        List<PebbleApp> apps = new ArrayList<PebbleApp>(9);

        if (context == null)
            return apps;

        apps.add(new PebbleApp(context.getString(R.string.PebbleAppMainMenu), UUID.fromString("dec0424c-0625-4878-b1f2-147e57e83688")));
        apps.add(new PebbleApp(context.getString(R.string.PebbleAppSettings), UUID.fromString("07e0d9cb-8957-4bf7-9d42-35bf47caadfe")));
        apps.add(new PebbleApp(context.getString(R.string.PebbleAppMusic), UUID.fromString("1f03293d-47af-4f28-b960-f2b02a6dd757")));
        apps.add(new PebbleApp(context.getString(R.string.PebbleAppNotificationHistory), UUID.fromString("b2cae818-10f8-46df-ad2b-98ad2254a3c1")));
        apps.add(new PebbleApp(context.getString(R.string.PebbleAppAlarms), UUID.fromString("67a32d95-ef69-46d4-a0b9-854cc62f97f9")));
        apps.add(new PebbleApp(context.getString(R.string.PebbleAppWatchfaceList), UUID.fromString("18e443ce-38fd-47c8-84d5-6d0c775fbe55")));
        apps.add(new PebbleApp(context.getString(R.string.PebbleWatchfaceSimplicity), UUID.fromString("6bf6215b-c97f-409e-8c31-4f55657222b4")));
        apps.add(new PebbleApp(context.getString(R.string.PebbleWatchfaceAnalog), UUID.fromString("55cb7c75-8a35-4487-90a4-913f1fa67601")));
        apps.add(new PebbleApp(context.getString(R.string.PebbleWatchfaceText), UUID.fromString("7c652eb9-26d6-442c-9868-a436797de205")));

        return apps;
    }

    public static List<UUID> getUUIDListFromByteBuffer(ByteBuffer buffer)
    {
        int numberOfInstalledApps = buffer.getInt();

        List<UUID> UUIDs = new ArrayList<UUID>(numberOfInstalledApps);

        for (int i = 0; i < numberOfInstalledApps; i++)
        {
            UUID uuid = new UUID(buffer.getLong(), buffer.getLong());

            UUIDs.add(uuid);
        }

        return UUIDs;
    }
}
