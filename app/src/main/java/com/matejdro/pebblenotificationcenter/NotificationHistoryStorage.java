package com.matejdro.pebblenotificationcenter;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;

import timber.log.Timber;

public class NotificationHistoryStorage extends SQLiteOpenHelper {


	private Context context;
	
	public NotificationHistoryStorage(Context context) {
		super(context, "notifications", null, 2);
		this.context = context;
	}


	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS notifications (PostTime INTEGER, Title STRING, Subtitle STRING, Text STRING, Icon BLOB DEFAULT NULL)");
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion == 2)
		{
			db.execSQL("ALTER TABLE notifications ADD COLUMN Icon BLOB DEFAULT NULL");
		}
	}


	public void storeNotification(long time, String title, String subtitle, String text, Bitmap icon)
	{
		ContentValues values = new ContentValues();
		values.put("PostTime", time);
		values.put("Title", title);
		values.put("Subtitle", subtitle);
		values.put("Text", text);

		if (icon == null)
			values.put("Icon", (byte[]) null);
		else
		{
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			icon.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
			values.put("Icon", byteArrayOutputStream.toByteArray());
		}

		try
		{
			getWritableDatabase().insert("notifications", null, values);
		}
		catch (SQLiteCantOpenDatabaseException e)
		{
			Timber.e(e, "Database open exception!");
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void close() {
		getWritableDatabase().close();
		super.close();
	}	

    public void clearDatabase()
    {
        SQLiteDatabase database = getWritableDatabase();

        database.delete("notifications", null, null);


        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong("lastCleanup", System.currentTimeMillis());
        editor.apply();
    }

	public void cleanDatabase()
	{
		SQLiteDatabase database = getWritableDatabase();
		Cursor cursor = database.rawQuery("SELECT MIN(PostTime) FROM (SELECT PostTime FROM notifications ORDER BY PostTime DESC LIMIT 100)", null);
		if (!cursor.moveToNext())
			return;

		long lastDate = cursor.getLong(0);

		database.delete("notifications", "PostTime < ?", new String[] {Long.toString(lastDate)});


		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putLong("lastCleanup", System.currentTimeMillis());
		editor.apply();

		cursor.close();
	}


	public void tryCleanDatabase()
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		long lastCleanup = preferences.getLong("lastCleanup", 0);
		if (System.currentTimeMillis() - lastCleanup > 24 * 3600 * 1000)
			cleanDatabase();
	}
}
