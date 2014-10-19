package com.matejdro.pebblenotificationcenter.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.InputType;
import com.matejdro.pebblenotificationcenter.R;


public class SettingsActivity extends PreferenceActivity {
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
        findPreference("noNotifications").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPopupOptionsEnabled(!(Boolean) newValue, settings.getBoolean("enableQuietTime", false));
                return true;
            }
        });

        Preference notifierLicenseButton = findPreference("notifierLicense");
        notifierLicenseButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(SettingsActivity.this, LicenseActivity.class);
                startActivity(intent);
                return true;
            }
        });

        EditTextPreference timeoutPreference = (EditTextPreference) findPreference("watchappTimeout");
        timeoutPreference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        timeoutPreference = (EditTextPreference) findPreference("periodicVibrationTimeout");
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

    }

    public void init()
    {
    }

	@SuppressWarnings("deprecation")
	private void setPopupOptionsEnabled(boolean popupEnabled, boolean timeEnabled)
	{		
		findPreference("noNotificationsScreenOn").setEnabled(popupEnabled);
		findPreference("noNotificationsNoPebble").setEnabled(popupEnabled);
		findPreference("noNotificationsSilent").setEnabled(popupEnabled);
	}
}
