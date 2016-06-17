package com.matejdro.pebblenotificationcenter.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.TwoStatePreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;

import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.VibrationPattern;
import com.matejdro.pebblenotificationcenter.pebble.WatchappHandler;

import de.psdev.licensesdialog.LicensesDialog;


public class SettingsActivity extends PreferenceActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
	private SharedPreferences settings;
    private SharedPreferences.Editor editor;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        init();

        addPreferencesFromResource(R.xml.settings);

        settings = getPreferenceManager().getSharedPreferences();
        editor = settings.edit();

        setPopupOptionsEnabled(!settings.getBoolean("noNotifications", false), settings.getBoolean("enableQuietTime", false));
        findPreference("noNotifications").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                setPopupOptionsEnabled(!(Boolean) newValue, settings.getBoolean("enableQuietTime", false));
                return true;
            }
        });

        Preference notifierLicenseButton = findPreference("notifierLicense");
        notifierLicenseButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {

            @Override
            public boolean onPreferenceClick(Preference preference)
            {

                new LicensesDialog.Builder(SettingsActivity.this)
                        .setNotices(R.raw.notices)
                        .setIncludeOwnLicense(true)
                        .build()
                        .show();

                return true;
            }
        });

        EditTextPreference timeoutPreference = (EditTextPreference) findPreference("watchappTimeout");
        timeoutPreference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        timeoutPreference = (EditTextPreference) findPreference("periodicVibrationTimeout");
        timeoutPreference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        timeoutPreference = (EditTextPreference) findPreference("lightTimeout");
        timeoutPreference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

        try
        {
            findPreference("version").setSummary( getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        }
        catch (PackageManager.NameNotFoundException e)
        {

        }

        findPreference("donateButton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5HV63YFE6SW44"));
                startActivity(intent);
                return true;
            }
        });

        findPreference(PebbleNotificationCenter.LIGHT_SCREEN_ON_NOTIFICATIONS).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                if (newValue.equals("3") && ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
                {
                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                    return false;
                }
                return true;
            }
        });

        findPreference(LogWriter.SETTING_ENABLE_LOG_WRITING).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                if (((Boolean) newValue) && ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                {
                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    return false;
                }
                return true;
            }
        });

        findPreference(PebbleNotificationCenter.PERIODIC_VIBRATION_PATTERN).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                if (!VibrationPattern.validateVibrationPattern((String) newValue))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                    builder.setMessage(R.string.invalidVibrationPattern);
                    builder.setPositiveButton(R.string.ok, null);
                    builder.show();

                    return false;
                }
                return true;
            }
        });

        findPreference("gesturesDemo").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                WatchappHandler.openWebpage(SettingsActivity.this, "https://www.youtube.com/watch?v=PUpJrldQrfk");
                return true;
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED)
            {
                ListPreference backlightPreference = (ListPreference) findPreference(PebbleNotificationCenter.LIGHT_SCREEN_ON_NOTIFICATIONS);
                backlightPreference.setValue("3");
            }
            else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED)
            {
                TwoStatePreference logWriterPreference = (TwoStatePreference) findPreference(LogWriter.SETTING_ENABLE_LOG_WRITING);
                logWriterPreference.setChecked(true);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void init()
    {
    }

	@SuppressWarnings("deprecation")
	private void setPopupOptionsEnabled(boolean popupEnabled, boolean timeEnabled)
	{		
		findPreference("noNotificationsNoPebble").setEnabled(popupEnabled);
		findPreference("noNotificationsSilent").setEnabled(popupEnabled);
	}
}
