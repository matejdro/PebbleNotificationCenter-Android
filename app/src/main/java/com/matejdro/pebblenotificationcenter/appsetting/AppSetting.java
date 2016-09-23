package com.matejdro.pebblenotificationcenter.appsetting;

import com.matejdro.pebblenotificationcenter.pebble.NativeNotificationIcon;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 16.9.2014.
 */
public enum AppSetting
{

    SEND_ONGOING_NOTIFICATIONS("enableOngoing", false, true),
    SEND_BLANK_NOTIFICATIONS("sendBlank", false, true),
    SEND_IDENTICAL_NOTIFICATIONS("sendIdentical", true, true),
    DISABLE_NOTIFY_SCREEN_OIN("noNotificationsScreenOn", false),
    DISABLE_LOCAL_ONLY_NOTIFICATIONS("disableLocalOnly", false, true),
    MINIMUM_NOTIFICATION_PRIORITY("minimumNotificationPriority", -2, true),

    QUIET_TIME_ENABLED("enableQuietTime", false),
    QUIET_TIME_START_HOUR("quietTimeStartHour", 0),
    QUIET_TIME_START_MINUTE("quietTimeStartMinute", 0),
    QUIET_TIME_END_HOUR("quietTimeEndHour", 0),
    QUIET_TIME_END_MINUTE("quietTimeEndMinute", 0),

    SWITCH_TO_MOST_RECENT_NOTIFICATION("autoSwitch", false),
    DISMISS_UPWARDS("syncDismissUp", true, true),
    SAVE_TO_HISTORY("saveToHistory", true),
    CUSTOM_TITLE("customTitle", ""),
    MAXIMUM_TEXT_LENGTH("maximumTextLength", Integer.toString(NotificationSendingModule.DEFAULT_TEXT_LIMIT), true),
    USE_WEAR_GROUP_NOTIFICATIONS("useWearGroupNotifications", true, true),
    ALWAYS_PARSE_STATUSBAR_NOTIFICATION("alwaysParseStatusbarNotification", false, true),
    RESPECT_ANDROID_INTERRUPT_FILTER("respectAndroidInterruptFilter", false),
    TITLE_FONT("fontTitle", 6),
    SUBTITLE_FONT("fontSubtitle", 5),
    BOCY_FONT("fontBody", 4),
    HIDE_NOTIFICATION_TEXT("hideNotiifcationText", false),
    STATUSBAR_COLOR("statusbarColor", 0x00000000),
    SHOW_IMAGE("showImage", true),
    NATIVE_NOTIFICATION_ICON("nativeNotificationIcon", NativeNotificationIcon.AUTOMATIC),
    WATCHAPP_NOTIFICATION_ICON("watchappNotificationIcon", true),

    USE_ALTERNATE_INBOX_PARSER("useInboxParser", true, true),
    INBOX_REVERSE("inboxReverse", false, true),
    DISPLAY_ONLY_NEWEST("inboxDisplayOnlyNewest", false, true),
    INBOX_USE_SUB_TEXT("inboxUseSubtext", true, true),

    SELECT_PRESS_ACTION("selectPressAction", 2),
    SELECT_HOLD_ACTION("selectHoldAction", 0),
    SHAKE_ACTION("shakeActionNew", 0),

    DISMISS_ON_PHONE_OPTION_LOCATION("dismissOnPhoneLocation", 1),
    DISMISS_ON_PEBBLE_OPTION_LOCATION("dismissOnPebbleLocation", 1),
    OPEN_ON_PHONE_OPTION_LOCATION("openOnPhoneLocation", 1),
    LOAD_WEAR_ACTIONS("loadWearActions", true, true),
    LOAD_PHONE_ACTIONS("loadPhoneActions", true, true),
    ENABLE_VOICE_REPLY("enableVoiceReply", true),
    ENABLE_TIME_VOICE_REPLY("enableTimeVoiceReply", true),
    ENABLE_WRITING_REPLY("enableWritingReply", true),
    SHOW_MUTE_APP_ACTION("showMuteApp", true),
    DISMISS_AFTER_REPLY("dismissAfterReply", false, true),
    CANNED_RESPONSES("cannedResponses", null),
    WRITING_PHRASES("writingPhrases", null),
    TASKER_ACTIONS("taskerActions", null),
    INTENT_ACTIONS_NAMES("intentActionsNames", null, true),
    INTENT_ACTIONS_ACTIONS("intentActionsActions", null, true),
    VIBRATION_PATTERN("vibrationPattern", "500"),
    USE_PROVIDED_VIBRATION("useProvidedVibration", true, true),
    PERIODIC_VIBRATION("settingPeriodicVibration", "20"),
    MINIMUM_VIBRATION_INTERVAL("minimumVibrationInterval", "0"),
    MINIMUM_NOTIFICATION_INTERVAL("minimumNotificationInterval", "0"),
    NO_UPDATE_VIBRATION("noUpdateVibration", false),
    INCLUDED_REGEX("WhitelistRegexes", null),
    EXCLUDED_REGEX("BlacklistRegexes", null);

    private String key;
    private Object def;
    private boolean advanced;

    private AppSetting(String key, Object def)
    {
        this.key = key;
        this.def = def;
        this.advanced = false;
    }

    AppSetting(String key, Object def, boolean advanced) {
        this.key = key;
        this.def = def;
        this.advanced = advanced;
    }

    public String getKey()
    {
        return key;
    }

    public Object getDefault()
    {
        return def;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public static final String VIRTUAL_APP_DEFAULT_SETTINGS = "com.matejdro.pebblenotificationcenter.virtual.default";
    public static final String VIRTUAL_APP_THIRD_PARTY = "com.matejdro.pebblenotificationcenter.virtual.thirdparty";
    public static final String VIRTUAL_APP_TASKER_RECEIVER = "com.matejdro.pebblenotificationcenter.virtual.taskerReceiver";

}
