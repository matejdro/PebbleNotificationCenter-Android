package com.matejdro.pebblenotificationcenter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import com.matejdro.pebblecommons.pebble.PebbleApp;
import com.matejdro.pebblenotificationcenter.appsetting.PebbleAppNotificationMode;
import com.matejdro.pebblenotificationcenter.pebble.modules.SystemModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GeneralNCDatabase extends SQLiteOpenHelper {

	private static GeneralNCDatabase instance;
	public static GeneralNCDatabase getInstance()
	{
		if (instance == null)
		{
			instance = new GeneralNCDatabase(PebbleNotificationCenter.getInstance());
		}

		return instance;
	}

	private Context context;

	private GeneralNCDatabase(Context context) {
		super(context, "data", null, 1);
		this.context = context;
	}


	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS PebbleApps (Name TEXT, UUID Text PRIMARY KEY, NotificationMode INTEGER)");

		ContentValues dummyOtherAppsEntryValues = new ContentValues();
		dummyOtherAppsEntryValues.put("Name", context.getString(R.string.pebble_apps_other));
		dummyOtherAppsEntryValues.put("UUID", SystemModule.UNKNOWN_UUID.toString());
		dummyOtherAppsEntryValues.put("NotificationMode", PebbleAppNotificationMode.OPEN_IN_NOTIFICATION_CENTER);
		db.insert("PebbleApps", null, dummyOtherAppsEntryValues);
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {		
	}


	public void addPebbleApp(PebbleApp pebbleApp)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put("Name", pebbleApp.getName());
		contentValues.put("UUID", pebbleApp.getUuid().toString());
		contentValues.put("NotificationMode", pebbleApp.getNotificationMode());

		getWritableDatabase().insert("PebbleApps", null, contentValues);
	}

	public void addPebbleApps(Collection<PebbleApp> apps)
	{
		SQLiteDatabase database = getWritableDatabase();
		database.beginTransaction();

		for (PebbleApp app : apps)
		{
			if (app.getUuid().equals(SystemModule.UNKNOWN_UUID))
				continue;

			ContentValues contentValues = new ContentValues();
			contentValues.put("Name", app.getName());
			contentValues.put("UUID", app.getUuid().toString());
			contentValues.put("NotificationMode", app.getNotificationMode());

			getWritableDatabase().replace("PebbleApps", null, contentValues);
		}

		database.setTransactionSuccessful();
		database.endTransaction();

	}


	public List<PebbleApp> getPebbleApps()
	{
		List<PebbleApp> list = new ArrayList<>();

		Cursor cursor = getReadableDatabase().rawQuery("SELECT Name, UUID, NotificationMode FROM PebbleApps ORDER BY UUID = \"" + SystemModule.UNKNOWN_UUID.toString() + "\" ASC, Name ASC", null);
		while (cursor.moveToNext())
		{
			String name = cursor.getString(0);
			UUID uuid = UUID.fromString(cursor.getString(1));
			int notificationMode = cursor.getInt(2);

			PebbleApp pebbleApp = new PebbleApp(name, uuid);
			pebbleApp.setNotificationMode(notificationMode);

			list.add(pebbleApp);
		}
		cursor.close();

		return list;
	}

	public void setPebbleAppNotificationMode(UUID uuid, int notificationMode)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put("NotificationMode", notificationMode);

		getWritableDatabase().update("PebbleApps", contentValues, "UUID = ?", new String[]{uuid.toString()});
	}

	public void setAllPebbleAppNotificationMode(int notificationMode)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put("NotificationMode", notificationMode);

		getWritableDatabase().update("PebbleApps", contentValues, null, null);
	}

	public int getPebbleAppNotificationMode(@Nullable UUID uuid)
	{
		if (uuid == null)
			return getPebbleAppNotificationMode(SystemModule.UNKNOWN_UUID);

		Cursor cursor = getReadableDatabase().rawQuery("SELECT NotificationMode FROM PebbleApps WHERE UUID = ?", new String[]{ uuid.toString() });
		if (!cursor.moveToNext())
		{
			cursor.close();
			return getPebbleAppNotificationMode(SystemModule.UNKNOWN_UUID);
		}

		int notificationMode = cursor.getInt(0);
		cursor.close();

		return notificationMode;
	}

	public void deletePebbleApp(UUID uuid)
	{
		if (uuid.equals(SystemModule.UNKNOWN_UUID))
			return;

		getWritableDatabase().delete("PebbleApps", "UUID = ?", new String[]{uuid.toString()});
	}

	public void deleteAllPebbleApps()
	{
		getWritableDatabase().delete("PebbleApps", "UUID <> ?", new String[]{ SystemModule.UNKNOWN_UUID.toString() });
	}

	public PebbleApp getPebbleApp(UUID uuid)
	{
		PebbleApp pebbleApp = null;

		Cursor cursor = getReadableDatabase().rawQuery("SELECT Name, NotificationMode FROM PebbleApps WHERE UUID = ?", new String[] { uuid.toString() });
		while (cursor.moveToNext())
		{
			String name = cursor.getString(0);
			int notificationMode = cursor.getInt(1);

			pebbleApp = new PebbleApp(name, uuid);
			pebbleApp.setNotificationMode(notificationMode);
		}
		cursor.close();

		return pebbleApp;
	}

}
