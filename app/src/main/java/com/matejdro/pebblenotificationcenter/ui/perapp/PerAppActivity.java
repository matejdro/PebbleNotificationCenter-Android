package com.matejdro.pebblenotificationcenter.ui.perapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.danielnilsson9.colorpickerview.dialog.ColorPickerDialogFragment;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AbsAppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.ActivityResultItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.AppEnabledCheckboxItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.BaseSettingItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.CannedResponsesItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.CheckBoxItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.ColorPickerItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.EditTextItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.IconPickerItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.IntentActionsItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.QuietHoursItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.RegexItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.ResetDefaultsButtonItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.SpinnerItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.TaskerTaskListItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.VibrationPatternItem;
import com.matejdro.pebblenotificationcenter.ui.perapp.settingitems.WritingPhrasesItem;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Matej on 16.9.2014.
 */
public class PerAppActivity extends Activity implements ColorPickerDialogFragment.ColorPickerDialogListener
{
    protected AppSettingStorage settingsStorage;
    protected String appPackage;
    protected String appName;
    protected boolean defaultSettings;

    private boolean expertMode;

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

        expertMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PebbleNotificationCenter.EXPERT_MODE, true);

        loadAppSettings();
        attachSettings();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState)
    {

    }

    protected AbsAppSettingStorage initAppSettingStorage()
    {
        AbsAppSettingStorage settingsStorage = null;

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor defaultEditor = defaultSharedPreferences.edit();
        DefaultAppSettingsStorage defaultAppSettingsStorage = new DefaultAppSettingsStorage(defaultSharedPreferences, defaultEditor);

        if (appPackage.equals(AppSetting.VIRTUAL_APP_DEFAULT_SETTINGS))
        {
            settingsStorage = defaultAppSettingsStorage;
            defaultSettings = true;

        } else
        {
            settingsStorage = new SharedPreferencesAppStorage(this, appPackage, defaultAppSettingsStorage);
            defaultSettings = false;
        }

        return settingsStorage;
    }

    protected void loadAppSettings()
    {
        if (!defaultSettings)
        {
            List<BaseSettingItem> category = new ArrayList<BaseSettingItem>();
            addToCategory(category, new AppEnabledCheckboxItem(settingsStorage, R.string.settingAppSelected, R.string.settingAppSelectedDescription));
            addToCategory(category, new ResetDefaultsButtonItem(settingsStorage, R.string.settingResetToDefault, R.string.settingResetToDefaultDescription, R.string.settingResetToDefaultButton));
            settings.add(new SettingsCategory(null, category));
        }

        //General settings
        List<BaseSettingItem> category = new ArrayList<BaseSettingItem>();
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.SEND_ONGOING_NOTIFICATIONS, R.string.settingSendOngoing, R.string.settingSendOngoingDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.SEND_BLANK_NOTIFICATIONS, R.string.settingSendBlankNotifications, R.string.settingSendBlankNotificationsDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.SEND_IDENTICAL_NOTIFICATIONS, R.string.settingSendIdenticalNotifications, R.string.settingSendIdenticalNotificationsDescription));
        if (version(Build.VERSION_CODES.LOLLIPOP)) addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.RESPECT_ANDROID_INTERRUPT_FILTER, R.string.settingRespectInterruptFilter, R.string.settingRespectInterruptFilterDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.DISABLE_LOCAL_ONLY_NOTIFICATIONS, R.string.settingDisableLocalOnlyNotifications, R.string.settingDisableLocalOnlyNotificationsDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.DISABLE_NOTIFY_SCREEN_OIN, R.string.settingNoNotificationsScreenOn, R.string.settingNoNotificationsScreenOnDescription));
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.MINIMUM_NOTIFICATION_PRIORITY, R.array.settingNotificationPriority, R.string.settingMinimumNotificationPriority, R.string.settingMinimumNotificationPriorityDescription, R.array.settingNotificationPriorityValues));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.SWITCH_TO_MOST_RECENT_NOTIFICATION, R.string.settingSwitchToRecent, R.string.settingSwitchToRecentDescription));
        addToCategory(category, new QuietHoursItem(settingsStorage, R.string.settingQuietHours, R.string.settingQuietHoursDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.SAVE_TO_HISTORY, R.string.settingSaveToHistory, R.string.settingSaveToHistoryDescription));
        if (version(Build.VERSION_CODES.JELLY_BEAN_MR2)) addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.DISMISS_UPWARDS, R.string.settingDismissUpwards, R.string.settingDismissUpwardsDescripition));
        addToCategory(category, new EditTextItem(settingsStorage, AppSetting.CUSTOM_TITLE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL, R.string.settingCustomTitle, R.string.settingCustomTitleDescription));
        addToCategory(category, new EditTextItem(settingsStorage, AppSetting.MAXIMUM_TEXT_LENGTH, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL, R.string.settingMaximumLength, R.string.settingMaximumLengthDescription));
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.TITLE_FONT, R.array.pebbleFonts, R.string.settingFontTitle, R.string.settingDescriptionWatchappOnly, R.array.fontValues));
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.SUBTITLE_FONT, R.array.pebbleFonts, R.string.settingFontSubtitle, R.string.settingDescriptionWatchappOnly, R.array.fontValues));
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.BOCY_FONT, R.array.pebbleFonts, R.string.settingFontBody, R.string.settingDescriptionWatchappOnly, R.array.fontValues));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.ALWAYS_PARSE_STATUSBAR_NOTIFICATION, R.string.settingAlwaysParseStatusbarNotification, R.string.settingAlwaysParseStatusbarNotificationDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.HIDE_NOTIFICATION_TEXT, R.string.settingHideNotificationText, R.string.settingHideNotificationTextDescription));
        if (version(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)) addToCategory(category, new ColorPickerItem(settingsStorage, AppSetting.STATUSBAR_COLOR, R.string.settingStatusbarColor, R.string.settingStatusbarColorDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.SHOW_IMAGE, R.string.settingShowImage, R.string.settingShowImageDescription));
        addToCategory(category, new IconPickerItem(settingsStorage, R.string.settingNotificationIcon, R.string.settingNotificationIconDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.WATCHAPP_NOTIFICATION_ICON, R.string.settingWatchappNotificationIcon, R.string.settingWatchappNotificationIconDescription));

        settings.add(new SettingsCategory(0, category));

        //Actions
        category = new ArrayList<BaseSettingItem>();
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.SELECT_PRESS_ACTION, R.array.settingSelectButtonAction, R.string.settingSelectPress, R.string.settingSelectPressDescription, R.array.settingSelectButtonActionValues));
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.SELECT_HOLD_ACTION, R.array.settingSelectButtonAction, R.string.settingSelectHold, R.string.settingSelectHoldDescription, R.array.settingSelectButtonActionValues));
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.SHAKE_ACTION, R.array.settingShakeAction, R.string.settingShakeAction, R.string.settingShakeActionDescription, R.array.settingShakeActionValues));
        if (version(Build.VERSION_CODES.JELLY_BEAN_MR2)) addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.DISMISS_ON_PHONE_OPTION_LOCATION, R.array.settingActionVisibility, R.string.settingDismissOnPhone, 0, null));
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.DISMISS_ON_PEBBLE_OPTION_LOCATION, R.array.settingActionVisibility, R.string.settingDismissOnPebble, 0, null));
        addToCategory(category, new SpinnerItem(settingsStorage, AppSetting.OPEN_ON_PHONE_OPTION_LOCATION, R.array.settingActionVisibility, R.string.settingOpenOnPhonePosition, 0, null));
        if (version(Build.VERSION_CODES.JELLY_BEAN)) addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.LOAD_WEAR_ACTIONS, R.string.settingLoadWearActions, R.string.settingLoadWearActionsDescription));
        if (version(Build.VERSION_CODES.JELLY_BEAN)) addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.LOAD_PHONE_ACTIONS, R.string.settingLoadPhoneActions, R.string.settingLoadPhoneActionsDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.ENABLE_VOICE_REPLY, R.string.settingEnableVoiceReply, R.string.settingEnableVoiceReplyDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.ENABLE_TIME_VOICE_REPLY, R.string.settingEnableTimeVoiceReply, R.string.settingEnableTimeVoiceReplyDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.ENABLE_WRITING_REPLY, R.string.settingEnableWritingReply, R.string.settingEnableWritingReplyDescription));
        addToCategory(category, new CannedResponsesItem(settingsStorage, AppSetting.CANNED_RESPONSES, R.string.settingCannedResponses, R.string.settingCannedResponsesDescription));
        addToCategory(category, new WritingPhrasesItem(settingsStorage, AppSetting.WRITING_PHRASES, R.string.settingWritingPhrases, R.string.settingWritingPhrasesDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.DISMISS_AFTER_REPLY, R.string.settingDismissAfterReply, R.string.settingDismissAfterReplyDescription));
        addToCategory(category, new TaskerTaskListItem(settingsStorage, AppSetting.TASKER_ACTIONS, R.string.settingTaskerActions, R.string.settingTaskerActionsDescription));
        addToCategory(category, new IntentActionsItem(settingsStorage, AppSetting.INTENT_ACTIONS_NAMES, R.string.settingBroadcastIntentActions, R.string.settingBroadcastIntentActionsDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.SHOW_MUTE_APP_ACTION, R.string.settingShowMuteApp, R.string.settingShowMuteAppDescription));
        settings.add(new SettingsCategory(R.string.settingCategoryActions, category));

        //Inbox parsing
        category = new ArrayList<BaseSettingItem>();
        if (version(Build.VERSION_CODES.JELLY_BEAN_MR2))  addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.USE_WEAR_GROUP_NOTIFICATIONS, R.string.settingUseWearGroupNotifications, R.string.settingUseWearGroupNotificationsDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.USE_ALTERNATE_INBOX_PARSER, R.string.settingAlternateInboxParser, R.string.settingAlternateInboxParserDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.INBOX_REVERSE, R.string.settingReverseInbox, R.string.settingReverseInboxDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.DISPLAY_ONLY_NEWEST, R.string.settingDisplayFirstOnly, R.string.settingDisplayFirstOnlyDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.INBOX_USE_SUB_TEXT, R.string.settingInboxUseSubtext, R.string.settingInboxUseSubtextDescription));
        if (version(Build.VERSION_CODES.JELLY_BEAN)) settings.add(new SettingsCategory(R.string.settingsCategoryInboxParsing, category));

        //Vibration
        category = new ArrayList<BaseSettingItem>();
        addToCategory(category, new VibrationPatternItem(settingsStorage, R.string.settingVibrationPattern, R.string.settingVibrationPatternDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.USE_PROVIDED_VIBRATION, R.string.settingUseProvidedVibration, R.string.settingUseProvidedVibrationDescription));
        addToCategory(category, new EditTextItem(settingsStorage, AppSetting.PERIODIC_VIBRATION, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL, R.string.settingPeriodicVibration, R.string.settingPeriodicVibrationDescription));
        addToCategory(category, new EditTextItem(settingsStorage, AppSetting.MINIMUM_VIBRATION_INTERVAL, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL, R.string.settingMinimumVibrationInterval, R.string.settingMinimumVibrationIntervalDescription));
        addToCategory(category, new EditTextItem(settingsStorage, AppSetting.MINIMUM_NOTIFICATION_INTERVAL, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL, R.string.settingMinimumNotificationInterval, R.string.settingMinimumNotificationIntervalDescription));
        addToCategory(category, new CheckBoxItem(settingsStorage, AppSetting.NO_UPDATE_VIBRATION, R.string.settingNoUpdateVibration, R.string.settingNoUpdateVibrationDescription));
        settings.add(new SettingsCategory(R.string.settingsCategoryVibration, category));

        //Regex
        category = new ArrayList<BaseSettingItem>();
        addToCategory(category, new RegexItem(settingsStorage, AppSetting.INCLUDED_REGEX, R.string.settingIncludingRegex, R.string.settingIncludingRegexDescription));
        addToCategory(category, new RegexItem(settingsStorage, AppSetting.EXCLUDED_REGEX, R.string.settingExcludingRegex, R.string.settingExcludingRegexDescription));
        settings.add(new SettingsCategory(R.string.settingsCategoryRegularExpressions, category));
    }

    private void addToCategory(List<BaseSettingItem> category, BaseSettingItem settingItem)
    {
        AppSetting associatedSetting = settingItem.getAssociatedSetting();
        if (associatedSetting != null && (expertMode || !associatedSetting.isAdvanced()))
            category.add(settingItem);
    }

    public void attachSettings()
    {
        LinearLayout root = (LinearLayout) findViewById(R.id.perAppSettingsList);

        for (SettingsCategory category : settings)
        {
            if (category.settings.isEmpty())
                continue;

            ViewGroup categoryView = (ViewGroup) getLayoutInflater().inflate(R.layout.setting_category, root, false);
            ViewGroup categoryBody = (ViewGroup) categoryView.findViewById(R.id.category_body);

            if (category.categoryNameResource != null)
            {
                if (category.categoryNameResource != 0)
                    ((TextView) categoryView.findViewById(R.id.categoryHeaderText)).setText(category.categoryNameResource);
                else
                    categoryView.findViewById(R.id.categoryHeaderText).setVisibility(View.GONE);
            }
            else
            {
                categoryView.findViewById(R.id.categorySeparator).setVisibility(View.GONE);
            }

            for (int i = 0; i < category.settings.size(); i++)
            {
                categoryBody.addView(category.settings.get(i).getView(this));

                if (i < category.settings.size() - 1)
                {
                    getLayoutInflater().inflate(R.layout.setting_separator, categoryBody, true);
                }
            }

            root.addView(categoryView);
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

    @Override
    public void onColorSelected(int i, int i1)
    {
        for (SettingsCategory category : settings)
        {
            for (BaseSettingItem item : category.settings)
            {
                if (item instanceof ColorPickerDialogFragment.ColorPickerDialogListener)
                    ((ColorPickerDialogFragment.ColorPickerDialogListener) item).onColorSelected(i, i1);
            }
        }

    }

    @Override
    public void onDialogDismissed(int i)
    {
        for (SettingsCategory category : settings)
        {
            for (BaseSettingItem item : category.settings)
            {
                if (item instanceof ColorPickerDialogFragment.ColorPickerDialogListener)
                    ((ColorPickerDialogFragment.ColorPickerDialogListener) item).onDialogDismissed(i);
            }
        }
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

    private static boolean version(int v)
    {
        return Build.VERSION.SDK_INT >= v;
    }
}
