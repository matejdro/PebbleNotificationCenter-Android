package com.matejdro.pebblenotificationcenter;

import java.util.ArrayList;
import java.util.List;

public class ProcessedNotification
{
	public int id;
	public List<String> textChunks = new ArrayList<String>(13);
    public PebbleNotification source;
}
