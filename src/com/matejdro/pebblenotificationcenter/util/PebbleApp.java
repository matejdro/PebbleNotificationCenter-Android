package com.matejdro.pebblenotificationcenter.util;

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
