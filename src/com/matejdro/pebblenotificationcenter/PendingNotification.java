package com.matejdro.pebblenotificationcenter;

import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
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
	public AppSettingStorage settingStorage;

	public String text;
	public long postTime;
}
