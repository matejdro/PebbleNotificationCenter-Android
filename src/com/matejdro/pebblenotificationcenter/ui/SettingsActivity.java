package com.matejdro.pebblenotificationcenter.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.text.format.DateFormat;
import android.widget.TimePicker;
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
        findPreference("enableQuietTime").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPopupOptionsEnabled(!settings.getBoolean("noNotifications", false), (Boolean) newValue);
                return true;
            }
        });

        final Preference quietFrom = findPreference("quietTimeStart");
        int startHour = settings.getInt("quiteTimeStartHour", 0);
        int startMinute = settings.getInt("quiteTimeStartMinute", 0);
        quietFrom.setSummary(formatTime(startHour, startMinute));
        quietFrom.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                int startHour = settings.getInt("quiteTimeStartHour", 0);
                int startMinute = settings.getInt("quiteTimeStartMinute", 0);

                TimePickerDialog dialog = new TimePickerDialog(SettingsActivity.this, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        editor.putInt("quiteTimeStartHour", hourOfDay);
                        editor.putInt("quiteTimeStartMinute", minute);
                        editor.apply();

                        quietFrom.setSummary(formatTime(hourOfDay, minute));
                    }
                }, startHour, startMinute, DateFormat.is24HourFormat(SettingsActivity.this));
                dialog.show();

                return true;
            }
        });

        final Preference quietTo = findPreference("quietTimeEnd");
        int endHour = settings.getInt("quiteTimeEndHour", 23);
        int endMinute = settings.getInt("quiteTimeEndMinute", 59);
        quietTo.setSummary(formatTime(endHour, endMinute));
        quietTo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                int startHour = settings.getInt("quiteTimeEndHour", 0);
                int startMinute = settings.getInt("quiteTimeEndMinute", 0);

                TimePickerDialog dialog = new TimePickerDialog(SettingsActivity.this, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        editor.putInt("quiteTimeEndHour", hourOfDay);
                        editor.putInt("quiteTimeEndMinute", minute);
                        editor.apply();

                        quietTo.setSummary(formatTime(hourOfDay, minute));
                    }
                }, startHour, startMinute, DateFormat.is24HourFormat(SettingsActivity.this));
                dialog.show();

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

    }

    public void init()
    {
    }

	@SuppressWarnings("deprecation")
	private void setPopupOptionsEnabled(boolean popupEnabled, boolean timeEnabled)
	{		
		findPreference("noNotificationsScreenOn").setEnabled(popupEnabled);
		findPreference("enableQuietTime").setEnabled(popupEnabled);
		findPreference("noNotificationsNoPebble").setEnabled(popupEnabled);		
		findPreference("quietTimeStart").setEnabled(popupEnabled && timeEnabled);
		findPreference("quietTimeEnd").setEnabled(popupEnabled && timeEnabled);
		findPreference("noNotificationsSilent").setEnabled(popupEnabled);		
	}

	public String formatTime(int hours, int minutes)
	{
		boolean hour24 = DateFormat.is24HourFormat(this);


		int displayHours;

		if (!hour24)
			displayHours = hours % 12;
		else
			displayHours = hours;

		StringBuilder builder = new StringBuilder(hour24 ? 5 : 8);

		if (displayHours < 10)
			builder.append('0');
		builder.append(displayHours);

		builder.append(":");

		if (minutes < 10)
			builder.append('0');
		builder.append(minutes);

		if (!hour24)
		{
			if (hours > 12)
				builder.append(" PM");
			else
				builder.append(" AM");
		}

		return builder.toString();

	}
}
