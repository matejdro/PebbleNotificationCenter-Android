package com.matejdro.pebblenotificationcenter.util;

import java.util.Collection;
import java.util.Iterator;

import android.content.SharedPreferences;

public class ListSerialization {
	
	public static void saveCollection(SharedPreferences.Editor editor, Collection<String> list, String listKey)
	{
		editor.putInt(listKey, list.size());
		int counter = 0;
		for (String item : list)
		{
			editor.putString(listKey + counter, item);
			counter++;
		}
		
		editor.apply();
	}
		
	public static void loadCollection(SharedPreferences preferences, Collection<String> list, String listKey)
	{
		int size = preferences.getInt(listKey, 0);
		
		for (int i = 0; i < size; i++)
		{
			list.add(preferences.getString(listKey + i, null));
		}
	}
	
	public static boolean listContains(SharedPreferences preferences, String listKey, String searchingValue)
	{
		int size = preferences.getInt(listKey, 0);
		
		for (int i = 0; i < size; i++)
		{
			String value = preferences.getString(listKey + i, null);
			if (value.equals(searchingValue))
				return true;
		}
		
		return false;
	}
	
	public static Iterator<String> getDirectIterator(final SharedPreferences preferences, final String listKey)
	{
		return new Iterator<String>() {
			
			private int loc;
			private int size;
			
			{
				loc = -1;
				size = preferences.getInt(listKey, 0);
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public String next() {				
				loc++;
				
				return preferences.getString(listKey + loc, null);
			}
			
			@Override
			public boolean hasNext() {
				return loc + 1 < size;
			}
		};
	}
}
