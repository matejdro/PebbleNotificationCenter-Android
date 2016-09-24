package com.matejdro.pebblenotificationcenter.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebblenotificationcenter.NotificationHistoryStorage;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.pebble.WatchappHandler;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;
import com.matejdro.pebblenotificationcenter.util.ConfigBackup;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

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
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                //actionBar.setSelectedNavigationItem(position);
            }
        });

        checkServiceRunning();
        checkPermissions();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.action_xposed_settings).setVisible(PebbleNotificationCenter.isXposedModuleRunning());
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
            case R.id.action_xposed_settings:
            Intent intent = new Intent(this, XposedSettingsActivity.class);
            startActivity(intent);
            return true;
            case R.id.action_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_test_notification:
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			NotificationCompat.Builder mBuilder =
              new NotificationCompat.Builder(this).setSmallIcon(R.drawable.notificationicon)
                  .setContentTitle("Test Notification").setContentText("See notifcation on pebble ")
                  .setSubText("Hello World");
          mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
            break;
            case R.id.clearHistory:
                clearHistory();
                break;
            case R.id.clearTemporaryMutes:
                NotificationSendingModule.clearTemporaryMutes(this);
                Toast.makeText(this, R.string.mutes_cleared, Toast.LENGTH_SHORT).show();
                break;
            case R.id.openInPebbleApp:
                WatchappHandler.openPebbleApp(this, PreferenceManager.getDefaultSharedPreferences(this).edit());
                break;
            case R.id.backupConfig:
                backupConfig();
                break;
            case R.id.restoreConfig:
                restoreConfig();
                break;
            case R.id.help:
                openHelpWebpage();
                break;
            default:
                return false;
		}

		return true;
	}

    private void checkServiceRunning()
    {
        if (NotificationHandler.active)
        {
            checkWatchFaceInstalled();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Service not running").setNegativeButton("Cancel", null);

        if (NotificationHandler.isNotificationListenerSupported())
        {
            builder.setMessage("Notification service is not running. You must enable it to get this app to work!");
            builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                }
            });
        }
        else
        {
            builder.setMessage("Accesibility service is not running. You must enable it to get this app to work!");
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

    private void checkWatchFaceInstalled()
    {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (!WatchappHandler.isFirstRun(settings))
        {
            WatchappHandler.displayNotification(this, settings.edit());
        }
    }

    private void clearHistory()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(R.string.deleteHistoryConfirm).setTitle(R.string.clearHistory);

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                dialogInterface.dismiss();
            }
        });

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                new NotificationHistoryStorage(MainActivity.this).clearDatabase();
                dialogInterface.dismiss();
                Toast.makeText(MainActivity.this, R.string.historyCleared, Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private void backupConfig()
    {
        if (!checkAndRequestStoragePermission(this))
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(R.string.backupDialogText).setTitle(R.string.backupDialogTitle);

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                dialogInterface.dismiss();
            }
        });

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                ConfigBackup.backup(MainActivity.this);
                Toast.makeText(MainActivity.this, R.string.backupCompleted, Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private void restoreConfig()
    {
        if (!checkAndRequestStoragePermission(this))
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(R.string.restoreConfigDialogText).setTitle(R.string.restoreConfigDialogTitle);

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                dialogInterface.dismiss();
            }
        });

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                if (ConfigBackup.restore(MainActivity.this))
                    Toast.makeText(MainActivity.this, R.string.configRestoreOK, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, R.string.configRestoreError, Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    public void openHelpWebpage()
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setData(Uri.parse("https://docs.google.com/document/d/1P6OUhs91ESYrHAC-O5Axz81HSTFuNjQei-4URxmcSIA/pub"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try
        {
            startActivity(intent);
        }
        catch (ActivityNotFoundException e)
        {
        }
    }

    private void checkPermissions()
    {
        List<String> wantedPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.RECORD_AUDIO);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.BLUETOOTH);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Light screen after sunset requires location for sunset times
        if (preferences.getString(PebbleNotificationCenter.LIGHT_SCREEN_ON_NOTIFICATIONS, "2").equals("3") &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
        {
            wantedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        //Log writer needs to access external storage
        if (preferences.getBoolean(LogWriter.SETTING_ENABLE_LOG_WRITING, false) &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        {
            wantedPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }


        if (!wantedPermissions.isEmpty())
            ActivityCompat.requestPermissions(this, wantedPermissions.toArray(new String[wantedPermissions.size()]), 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++)
        {
            String permission = permissions[i];

            if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_DENIED)
            {
                //Location permission was denied, lets disable after-sunset-only light
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

                if (preferences.getString(PebbleNotificationCenter.LIGHT_SCREEN_ON_NOTIFICATIONS, "2").equals("3"))
                    preferences.edit().putString(PebbleNotificationCenter.LIGHT_SCREEN_ON_NOTIFICATIONS, "2").apply();
            }
            else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

                if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                {
                    //We got external storage permission, lets restart the log writer
                    if (preferences.getBoolean(LogWriter.SETTING_ENABLE_LOG_WRITING, false))
                        LogWriter.reopen();
                }
                else
                {
                    //Permission was denied, lets disable log writing
                    preferences.edit().putBoolean(LogWriter.SETTING_ENABLE_LOG_WRITING, false).apply();
                }
            }
        }
    }

    public static boolean checkAndRequestStoragePermission(Activity activity)
    {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                Toast.makeText(activity, R.string.accept_and_reselect, Toast.LENGTH_SHORT).show();
            }
            else
            {
                new AlertDialog.Builder(activity).setMessage(R.string.cannot_use_without_storage_permission).setNegativeButton(R.string.ok, null).show();
            }

            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return false;
        }

        return true;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public static final int PAGE_DEFAULT = 0;
        public static final int PAGE_APPS = 1;
        public static final int PAGE_APPS_SYSTEM = 2;
        public static final int PAGE_APPS_PEBBLE = 3;
        public static final int PAGE_TEXT_REPLACEMENT = 4;

        public static final String[] TITLES = {
                "General",
                "User Apps",
                "System Apps",
                "Pebble Apps",
                "Character Replacement"
        };

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

                case PAGE_APPS_PEBBLE:
                    return new PebbleAppListFragment();

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
