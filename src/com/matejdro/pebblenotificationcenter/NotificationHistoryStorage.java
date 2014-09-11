package com.matejdro.pebblenotificationcenter;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class NotificationHistoryStorage extends SQLiteOpenHelper {


	private Context context;
	
	public NotificationHistoryStorage(Context context) {
		super(context, "notifications", null, 1);
		this.context = context;
	}


	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS notifications (PostTime INTEGER, Title STRING, Subtitle STRING, Text STRING)");
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {		
	}


	public void storeNotification(long time, String title, String subtitle, String text)
	{
		ContentValues values = new ContentValues();
		values.put("PostTime", time);
		values.put("Title", title);
		values.put("Subtitle", subtitle);
		values.put("Text", text);

		getWritableDatabase().insert("notifications", null, values);
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
	}
}
