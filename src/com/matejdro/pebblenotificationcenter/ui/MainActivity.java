package com.matejdro.pebblenotificationcenter.ui;


import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.util.WatchappHandler;

public class MainActivity extends ActionBarActivity /* implements ActionBar.TabListener */{

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
   * three primary sections of the app.
   */
  AppSectionsPagerAdapter mAppSectionsPagerAdapter;

  /**
   * The {@link ViewPager} that will display the primary sections of the app
   */
  ViewPager mViewPager;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Create the adapter that will return a fragment for each of the three primary sections
    // of the app.
    mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

    // Set up the action bar.
    final ActionBar actionBar = getSupportActionBar();

    // Specify that the Home/Up button should not be enabled, since there is no hierarchical
    // parent.
    actionBar.setHomeButtonEnabled(false);

    // Specify that we will be displaying tabs in the action bar.
    // actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // Set up the ViewPager, attaching the adapter and setting up a listener for when the
    // user swipes between sections.
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(mAppSectionsPagerAdapter);
    mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        // actionBar.setSelectedNavigationItem(position);
      }
    });

    checkServiceRunning();
    Environment.getExternalStoragePublicDirectory("NotificationCenter");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      case R.id.action_installapp:
        WatchappHandler.install(MainActivity.this,
            PreferenceManager.getDefaultSharedPreferences(this).edit());
        return true;
      case R.id.action_test_notification:
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification =
            new Notification(R.drawable.icon, "Hello World", System.currentTimeMillis());
        notification.defaults = Notification.DEFAULT_ALL;
        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_action_pebble)
                .setContentTitle("Test Notification").setContentText("See notifcation on pebble")
                .setSubText("Hello World")
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
        return true;
    }


    return false;
  }

  private void checkServiceRunning() {
    if (NotificationHandler.active) {
      checkWatchFaceInstalled();
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    builder.setTitle("Service not running").setNegativeButton("Cancel", null);

    if (NotificationHandler.isNotificationListenerSupported()) {
      builder
          .setMessage("Notification service is not running. You must enable it to get this app to work!");
      builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }
      });
    } else {
      builder
          .setMessage("Accesibility service is not running. You must enable it to get this app to work!");
      builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
      });
    }

    AlertDialog dialog = builder.create();

    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
      @Override
      public void onDismiss(DialogInterface dialog) {
        checkWatchFaceInstalled();
      }
    });

    dialog.show();
  }

  private void checkWatchFaceInstalled() {
    final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    if (!WatchappHandler.isLatest(settings)) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);

      builder.setMessage("Do you want to install the latest watchapp?")
          .setNegativeButton("No", null)
          .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              WatchappHandler.install(MainActivity.this, settings.edit());
            }
          }).show();
    }
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
   * sections of the app.
   */
  public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

    public static final int PAGE_DEFAULT = 0;
    public static final int PAGE_APPS = 1;
    public static final int PAGE_APPS_SYSTEM = 2;
    public static final int PAGE_TEXT_REPLACEMENT = 3;

    public static final String[] TITLES = {"General", "User Apps", "System Apps",
        "Character Replacement"};

    public AppSectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      switch (i) {
      // Default
        case PAGE_DEFAULT:
          return new OptionsFragment();

        case PAGE_APPS:
          return AppListFragment.newInstance(false);

        case PAGE_APPS_SYSTEM:
          return AppListFragment.newInstance(true);

        case PAGE_TEXT_REPLACEMENT:
          return new ReplacerFragment();

        default:
          return new Fragment();
      }
    }

    @Override
    public int getCount() {
      return TITLES.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return TITLES[position];
    }
  }


}
