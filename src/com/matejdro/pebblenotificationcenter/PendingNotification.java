package com.matejdro.pebblenotificationcenter;

import java.util.ArrayList;
import java.util.List;

public class PendingNotification {
	public int id;
	public Integer androidID;
	public String pkg;
	public String tag;
	public String title;
	public String subtitle;
	public List<String> textChunks = new ArrayList<String>(13);
	public boolean dismissable;
	public boolean isListNotification;
	
	public String text;
	public long postTime;
}
