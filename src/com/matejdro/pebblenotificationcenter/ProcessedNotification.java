package com.matejdro.pebblenotificationcenter;

import java.util.ArrayList;
import java.util.List;

public class ProcessedNotification
{
	public int id;
	public List<String> textChunks = new ArrayList<String>(13);
    public boolean vibrated = false;
    public int nextChunkToSend = -1;
    public boolean nativeNotification;

    public PebbleNotification source;
}
