package com.matejdro.pebblenotificationcenter.ui.perapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.util.TextUtil;

/**
 * Created by Matej on 16.9.2014.
 */
public class PerAppActivity extends Activity
{
    protected AppSettingStorage settingsStorage;
    protected String appPackage;
    protected String appName;

    private RegexList includingRegexList;
    private RegexList excludingRegexList;
    private CannedResponseList cannedResponseList;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_per_app_settings);

        Intent startIntent = getIntent();
        appName = startIntent.getStringExtra("appName");
        appPackage = startIntent.getStringExtra("appPackage");

        settingsStorage = initAppSettingStorage();

        ((TextView) findViewById(R.id.appName)).setText(appName);

        linkViewToCheckbox(R.id.isAppSelectedView, R.id.isAppSelectedCheck);
        linkViewToCheckbox(R.id.useDefaultView, R.id.useDefaultCheck);
        linkViewToCheckbox(R.id.sendOngoingView, R.id.sendOngoingCheck);
        linkViewToCheckbox(R.id.sendBlankView, R.id.sendBlankCheck);
        linkViewToCheckbox(R.id.switchToRecentView, R.id.switchToRecentCheck);
        linkViewToCheckbox(R.id.overrideQuietView, R.id.overrideQuietCheck);
        linkViewToCheckbox(R.id.saveToHistoryView, R.id.saveToHistoryCheck);
        linkViewToCheckbox(R.id.dismissUpwardsView, R.id.dismissUpwardsCheck);
        linkViewToCheckbox(R.id.alternateInboxParsingView, R.id.alternateInboxParsingCheck);
        linkViewToCheckbox(R.id.reverseInboxView, R.id.reverseInboxCheck);
        linkViewToCheckbox(R.id.displayFirstOnlyView, R.id.displayFirstOnlyCheck);
        linkViewToCheckbox(R.id.inboxUseSubtextView, R.id.inboxUseSubtextCheck);
        linkViewToCheckbox(R.id.showActionMenuView, R.id.showActionMenuCheck);
        linkViewToCheckbox(R.id.loadWearActionsView, R.id.loadWearActionsCheck);

        linkCheckboxToSetting(R.id.sendOngoingCheck, AppSetting.SEND_ONGOING_NOTIFICATIONS);
        linkCheckboxToSetting(R.id.sendBlankCheck, AppSetting.SEND_BLANK_NOTIFICATIONS);
        linkCheckboxToSetting(R.id.switchToRecentCheck, AppSetting.SWITCH_TO_MOST_RECENT_NOTIFICATION);
        linkCheckboxToSetting(R.id.overrideQuietCheck, AppSetting.IGNORE_QUIET_HOURS);
        linkCheckboxToSetting(R.id.dismissUpwardsCheck, AppSetting.DISMISS_UPRWADS);
        linkCheckboxToSetting(R.id.saveToHistoryCheck, AppSetting.SAVE_TO_HISTORY);
        linkCheckboxToSetting(R.id.alternateInboxParsingCheck, AppSetting.USE_ALTERNATE_INBOX_PARSER);
        linkCheckboxToSetting(R.id.reverseInboxCheck, AppSetting.INBOX_REVERSE);
        linkCheckboxToSetting(R.id.displayFirstOnlyCheck, AppSetting.DISPLAY_ONLY_NEWEST);
        linkCheckboxToSetting(R.id.inboxUseSubtextCheck, AppSetting.INBOX_USE_SUB_TEXT);
        linkCheckboxToSetting(R.id.showActionMenuCheck, AppSetting.ACTIONS_SHOW_MENU);
        linkCheckboxToSetting(R.id.loadWearActionsCheck, AppSetting.LOAD_WEAR_ACTIONS);

        //Two special checkboxes that are not linked to AppSetting
        CheckBox checkBox = (CheckBox) findViewById(R.id.isAppSelectedCheck);
        checkBox.setChecked(settingsStorage.isAppChecked());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                settingsStorage.setAppChecked(b);
            }
        });

        final View useDefaultParent = findViewById(R.id.useDefaultView);
        checkBox = (CheckBox) findViewById(R.id.useDefaultCheck);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                settingsStorage.setAppUseDefaultSettings(b);
                useDefaultParent.setBackgroundColor(b ? 0xFFFFAAAA : 0x00000000);
            }
        });
        checkBox.setChecked(settingsStorage.shouldAppUseDefaultSettings());


        ((EditText) findViewById(R.id.vibrationPatternEdit)).setText(settingsStorage.getString(AppSetting.VIBRATION_PATTERN));
        ((EditText) findViewById(R.id.periodicVibrationEdit)).setText(settingsStorage.getString(AppSetting.PERIODIC_VIBRATION));

        cannedResponseList = new CannedResponseList(this, R.id.cannedResponsesList, R.id.cannedResponsesAddButton, R.id.cannedResponsestEmptyText);
        cannedResponseList.addAll(settingsStorage.getStringList(AppSetting.CANNED_RESPONSES));
        includingRegexList = new RegexList(this, R.id.includingRegexList, R.id.includingRegexAddButton, R.id.includingRegexListEmptyText);
        includingRegexList.addAll(settingsStorage.getStringList(AppSetting.INCLUDED_REGEX));
        excludingRegexList = new RegexList(this, R.id.excludingRegexList, R.id.excludingRegexAddButton, R.id.excludingRegexListEmptyText);
        excludingRegexList.addAll(settingsStorage.getStringList(AppSetting.EXCLUDED_REGEX));
    }

    public AppSettingStorage initAppSettingStorage()
    {
        AppSettingStorage settingsStorage = null;

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor defaultEditor = defaultSharedPreferences.edit();
        DefaultAppSettingsStorage defaultAppSettingsStorage = new DefaultAppSettingsStorage(defaultSharedPreferences, defaultEditor);

        if (appPackage.equals(AppSetting.VIRTUAL_APP_DEFAULT_SETTINGS))
        {
            settingsStorage = defaultAppSettingsStorage;
            findViewById(R.id.isAppSelectedView).setVisibility(View.GONE);
            findViewById(R.id.isAppSelectedSeparator).setVisibility(View.GONE);
            findViewById(R.id.useDefaultView).setVisibility(View.GONE);

        } else
        {
            settingsStorage = new SharedPreferencesAppStorage(this, appPackage, defaultAppSettingsStorage, false);
        }

        return settingsStorage;

    }

    @Override
    public void onBackPressed()
    {
        save();

        super.onBackPressed();
    }

    protected void save()
    {
        settingsStorage.setString(AppSetting.PERIODIC_VIBRATION, ((EditText) findViewById(R.id.periodicVibrationEdit)).getText().toString());

        String vibrationPattern = ((EditText) findViewById(R.id.vibrationPatternEdit)).getText().toString();
        if (!validateVibrationPattern(vibrationPattern))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.invalidVibrationPattern);
            builder.setPositiveButton(R.string.ok, null);
            builder.show();

            return;
        }
        settingsStorage.setString(AppSetting.VIBRATION_PATTERN, vibrationPattern);

        settingsStorage.setStringList(AppSetting.CANNED_RESPONSES, cannedResponseList.getInternalStorage());
        settingsStorage.setStringList(AppSetting.INCLUDED_REGEX, includingRegexList.getInternalStorage());
        settingsStorage.setStringList(AppSetting.EXCLUDED_REGEX, excludingRegexList.getInternalStorage());
    }

    private void linkViewToCheckbox(int viewId, int checkBoxId)
    {
        View view = findViewById(viewId);
        final CheckBox checkbox = (CheckBox) findViewById(checkBoxId);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                checkbox.setChecked(!checkbox.isChecked());
            }
        });
    }

    private void linkCheckboxToSetting(int checkboxViewId, final AppSetting setting)
    {
        final CheckBox checkbox = (CheckBox) findViewById(checkboxViewId);

        checkbox.setChecked(settingsStorage.getBoolean(setting));
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                settingsStorage.setBoolean(setting, b);
            }
        });
    }


    private static boolean validateVibrationPattern(String pattern)
    {
        if (pattern.trim().isEmpty())
            return false;

        String split[] = pattern.split(",");

        for (String s : split)
        {
            if (!TextUtil.isInteger(s.trim()))
                return false;
        }

        return true;
    }

}
