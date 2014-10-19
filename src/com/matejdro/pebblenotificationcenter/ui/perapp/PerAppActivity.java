package com.matejdro.pebblenotificationcenter.ui.perapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.ActivityResultItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.AppEnabledCheckboxItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.BaseSettingItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.CannedResponsesItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.CheckBoxItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.EditTextItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.RegexItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.SpinnerItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.TaskerTaskListItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.UseDefaultCheckBoxItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.VibrationPatternItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 16.9.2014.
 */
public class PerAppActivity extends Activity
{
    protected AppSettingStorage settingsStorage;
    protected String appPackage;
    protected String appName;
    private boolean defaultSettings;

    public List<SettingsCategory> settings = new ArrayList<SettingsCategory>();

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_per_app_settings);

        Intent startIntent = getIntent();
        appName = startIntent.getStringExtra("appName");
        appPackage = startIntent.getStringExtra("appPackage");

        settingsStorage = initAppSettingStorage();

        ((TextView) findViewById(R.id.appName)).setText(appName);

        loadAppSettings();
        attachSettings();
    }

    protected AppSettingStorage initAppSettingStorage()
    {
        AppSettingStorage settingsStorage = null;

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor defaultEditor = defaultSharedPreferences.edit();
        DefaultAppSettingsStorage defaultAppSettingsStorage = new DefaultAppSettingsStorage(defaultSharedPreferences, defaultEditor);

        if (appPackage.equals(AppSetting.VIRTUAL_APP_DEFAULT_SETTINGS))
        {
            settingsStorage = defaultAppSettingsStorage;
            defaultSettings = true;

        } else
        {
            settingsStorage = new SharedPreferencesAppStorage(this, appPackage, defaultAppSettingsStorage, false);
            defaultSettings = false;
        }

        return settingsStorage;
    }

    private void loadAppSettings()
    {
        if (!defaultSettings)
        {
            List<BaseSettingItem> category = new ArrayList<BaseSettingItem>();
            category.add(new AppEnabledCheckboxItem(settingsStorage, R.string.settingAppSelected, R.string.settingAppSelectedDescription));
            category.add(new UseDefaultCheckBoxItem(settingsStorage, R.string.settingUseDefaultSettings, R.string.settingUseDefaultSettingsDescription));
            settings.add(new SettingsCategory(null, category));
        }

        //General settings
        List<BaseSettingItem> category = new ArrayList<BaseSettingItem>();
        category.add(new CheckBoxItem(settingsStorage, AppSetting.SEND_ONGOING_NOTIFICATIONS, R.string.settingSendOngoing, R.string.settingSendOngoingDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.SEND_BLANK_NOTIFICATIONS, R.string.settingSendBlankNotifications, R.string.settingSendBlankNotificationsDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.RESPECT_ANDROID_INTERRUPT_FILTER, R.string.settingRespectInterruptFilter, R.string.settingRespectInterruptFilterDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.SWITCH_TO_MOST_RECENT_NOTIFICATION, R.string.settingSwitchToRecent, R.string.settingSwitchToRecentDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.IGNORE_QUIET_HOURS, R.string.settingIgnoreQuietHours, R.string.settingIgnoreQuietHoursDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.SAVE_TO_HISTORY, R.string.settingSaveToHistory, R.string.settingSaveToHistoryDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.DISMISS_UPRWADS, R.string.settingDismissUpwards, R.string.settingDismissUpwardsDescripition));
        settings.add(new SettingsCategory(0, category));

        //Actions
        category = new ArrayList<BaseSettingItem>();
        category.add(new SpinnerItem(settingsStorage, AppSetting.SELECT_PRESS_ACTION, R.array.settingSelectButtonAction, R.string.settingSelectPress, R.string.settingSelectHoldDescription));
        category.add(new SpinnerItem(settingsStorage, AppSetting.SELECT_HOLD_ACTION, R.array.settingSelectButtonAction, R.string.settingSelectHold, R.string.settingSelectHoldDescription));
        category.add(new SpinnerItem(settingsStorage, AppSetting.DISMISS_ON_PHONE_OPTION_LOCATION, R.array.settingActionVisibility, R.string.settingDismissOnPhone, 0));
        category.add(new SpinnerItem(settingsStorage, AppSetting.DISMISS_ON_PEBBLE_OPTION_LOCATION, R.array.settingActionVisibility, R.string.settingDismissOnPebble, 0));
        category.add(new SpinnerItem(settingsStorage, AppSetting.OPEN_ON_PHONE_OPTION_LOCATION, R.array.settingActionVisibility, R.string.settingOpenOnPhonePosition, 0));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.LOAD_WEAR_ACTIONS, R.string.settingLoadWearActions, R.string.settingLoadWearActionsDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.LOAD_PHONE_ACTIONS, R.string.settingLoadPhoneActions, R.string.settingLoadPhoneActionsDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.ENABLE_VOICE_REPLY, R.string.settingEnableVoiceReply, R.string.settingEnableVoiceReplyDescription));
        category.add(new CannedResponsesItem(settingsStorage, AppSetting.CANNED_RESPONSES, R.string.settingCannedResponses, R.string.settingCannedResponsesDescription));
        category.add(new TaskerTaskListItem(settingsStorage, AppSetting.TASKER_ACTIONS, R.string.settingTaskerActions, R.string.settingTaskerActionsDescription));
        settings.add(new SettingsCategory(R.string.settingCategoryActions, category));

        //Inbox parsing
        category = new ArrayList<BaseSettingItem>();
        category.add(new CheckBoxItem(settingsStorage, AppSetting.USE_WEAR_GROUP_NOTIFICATIONS, R.string.settingUseWearGroupNotifications, R.string.settingUseWearGroupNotificationsDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.USE_ALTERNATE_INBOX_PARSER, R.string.settingAlternateInboxParser, R.string.settingAlternateInboxParserDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.INBOX_REVERSE, R.string.settingReverseInbox, R.string.settingReverseInboxDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.DISPLAY_ONLY_NEWEST, R.string.settingDisplayFirstOnly, R.string.settingDisplayFirstOnlyDescription));
        category.add(new CheckBoxItem(settingsStorage, AppSetting.INBOX_USE_SUB_TEXT, R.string.settingInboxUseSubtext, R.string.settingInboxUseSubtextDescription));
        settings.add(new SettingsCategory(R.string.settingsCategoryInboxParsing, category));

        //Vibration
        category = new ArrayList<BaseSettingItem>();
        category.add(new VibrationPatternItem(settingsStorage, R.string.settingVibrationPattern, R.string.settingVibrationPatternDescription));
        category.add(new EditTextItem(settingsStorage, AppSetting.PERIODIC_VIBRATION, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL, R.string.settingPeriodicVibration, R.string.settingPeriodicVibrationDescription));
        category.add(new EditTextItem(settingsStorage, AppSetting.MINIMUM_VIBRATION_INTERVAL, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL, R.string.settingMinimumVibrationInterval, R.string.settingMinimumVibrationIntervalDescription));
        settings.add(new SettingsCategory(R.string.settingsCategoryVibration, category));

        //Regex
        category = new ArrayList<BaseSettingItem>();
        category.add(new RegexItem(settingsStorage, AppSetting.EXCLUDED_REGEX, R.string.settingExcludingRegex, R.string.settingExcludingRegexDescription));
        category.add(new RegexItem(settingsStorage, AppSetting.INCLUDED_REGEX, R.string.settingIncludingRegex, R.string.settingExcludingRegexDescription));
        settings.add(new SettingsCategory(R.string.settingsCategoryRegularExpressions, category));
    }

    public void attachSettings()
    {
        LinearLayout root = (LinearLayout) findViewById(R.id.perAppSettingsList);
        for (SettingsCategory category : settings)
        {
            if (category.categoryNameResource != null)
            {
                View header = getLayoutInflater().inflate(R.layout.setting_category_header, null);
                if (category.categoryNameResource != 0)
                    ((TextView) header.findViewById(R.id.categoryHeaderText)).setText(category.categoryNameResource);

                root.addView(header);
            }

            for (int i = 0; i < category.settings.size(); i++)
            {
                root.addView(category.settings.get(i).getView(this));

                if (i < category.settings.size() - 1)
                {
                    getLayoutInflater().inflate(R.layout.setting_separator, root, true);
                }
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        if (!save())
            return;

        super.onBackPressed();
    }

    protected boolean save()
    {
        for (SettingsCategory category : settings)
        {
            for (BaseSettingItem item : category.settings)
            {
                if (!item.onClose())
                    return false;
            }
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        for (SettingsCategory category : settings)
        {
            for (BaseSettingItem item : category.settings)
            {
                if (item instanceof ActivityResultItem)
                    ((ActivityResultItem) item).onActivityResult(requestCode, resultCode, data);
            }
        }
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

    private void linkSpinnerToSetting(int spinnerId, final AppSetting setting, int settingNamesArrayId)
    {
        final Spinner spinner = (Spinner) findViewById(spinnerId);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, settingNamesArrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(settingsStorage.getInt(setting));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                settingsStorage.setInt(setting, i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
    }

    public class SettingsCategory
    {
        public SettingsCategory(Integer categoryNameResource, List<BaseSettingItem> settings)
        {
            this.categoryNameResource = categoryNameResource;
            this.settings = settings;
        }

        public Integer categoryNameResource;
        public List<BaseSettingItem> settings;
    }
}
