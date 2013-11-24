package com.matejdro.pebblenotificationcenter.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.util.WatchappHandler;

public class MainActivity extends ActionBarActivity implements
		ActionBar.OnNavigationListener {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	private AppsFragment appsFragment;
	private BlacklistRegexesFragment blacklistRegexesFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(actionBar.getThemedContext(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								"App picker",
								"Excluded notifications"}), this);		
	
		checkServiceRunning();
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getSupportActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getSupportActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_installapp:
			WatchappHandler.install(MainActivity.this, PreferenceManager.getDefaultSharedPreferences(this).edit());
			return true;
		}

		return false;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		Fragment fragment = null;
		switch (position)
		{
		case 0:
			fragment = appsFragment;
			if (fragment == null)
			{
				appsFragment = new AppsFragment();
				fragment = appsFragment;
			}
			break;
		case 1:
			fragment = blacklistRegexesFragment;
			if (fragment == null)
			{
				blacklistRegexesFragment = new BlacklistRegexesFragment();
				fragment = blacklistRegexesFragment;
			}
			break;
		}
		
		getSupportFragmentManager().beginTransaction()
		.replace(R.id.container, fragment).commit();
		
		return true;
	}
	
	private void checkServiceRunning()
	{
		if (NotificationHandler.active)
		{
			checkWatchFaceInstalled();
			return;
		}
		
		AlertDialog.Builder builder = new Builder(this);
		
		builder.setTitle("Service not running").setNegativeButton("Cancel", null);
		
		if (NotificationHandler.isNotificationListenerSupported())
		{
			builder.setMessage("Notification service is not running. You must enable it to get this app to work!");
			builder.setPositiveButton("Open Settings", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
				}
			});
		}
		else
		{
			builder.setMessage("Accesibility service is not running. You must enable it to get this app to work!");
			builder.setPositiveButton("Open Settings", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
				}
			});
		}
		
		AlertDialog dialog = builder.create();
		
		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				checkWatchFaceInstalled();	
			}
		});
		
		dialog.show();
	}
	
	private void checkWatchFaceInstalled()
	{
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if (!WatchappHandler.isLatest(settings))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder.setMessage("Do you want to install the latest watchapp?").setNegativeButton(
					"No", null).setPositiveButton("Yes", new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							WatchappHandler.install(MainActivity.this, settings.edit());
						}
					}).show();
		}
	}

}
