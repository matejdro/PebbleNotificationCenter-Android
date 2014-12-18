package com.matejdro.pebblenotificationcenter;

import com.matejdro.pebblenotificationcenter.lists.actions.ActionList;
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
