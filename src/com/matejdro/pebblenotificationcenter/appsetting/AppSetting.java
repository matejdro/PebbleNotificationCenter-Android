package com.matejdro.pebblenotificationcenter.appsetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 16.9.2014.
 */
public enum AppSetting
{

    SEND_ONGOING_NOTIFICATIONS("enableOngoing", false),
    SEND_BLANK_NOTIFICATIONS("sendBlank", false),
    SWITCH_TO_MOST_RECENT_NOTIFICATION("autoSwitch", false),
    IGNORE_QUIET_HOURS("ignoreQuietHours", false),
    DISMISS_UPRWADS("syncDismissUp", true),
    SAVE_TO_HISTORY("saveToHistory", true),
    USE_WEAR_GROUP_NOTIFICATIONS("useWearGroupNotifications", true),
    USE_ALTERNATE_INBOX_PARSER("useInboxParser", true),
    INBOX_REVERSE("inboxReverse", false),
    DISPLAY_ONLY_NEWEST("inboxDisplayOnlyNewest", false),
    INBOX_USE_SUB_TEXT("inboxUseSubtext", true),
    SELECT_PRESS_ACTION("selectPressAction", 2),
    SELECT_HOLD_ACTION("selectHoldAction", 0),
    DISMISS_ON_PHONE_OPTION_LOCATION("dismissOnPhoneLocation", 1),
    DISMISS_ON_PEBBLE_OPTION_LOCATION("dismissOnPebbleLocation", 1),
    OPEN_ON_PHONE_OPTION_LOCATION("openOnPhoneLocation", 1),
    LOAD_WEAR_ACTIONS("loadWearActions", true),
    LOAD_PHONE_ACTIONS("loadPhoneActions", true),
    ENABLE_VOICE_REPLY("enableVoiceReply", true),
    CANNED_RESPONSES("cannedResponses", null),
    TASKER_ACTIONS("taskerActions", null),
    VIBRATION_PATTERN("vibrationPattern", "500"),
    PERIODIC_VIBRATION("settingPeriodicVibration", "20"),
    INCLUDED_REGEX("WhitelistRegexes", null),
    EXCLUDED_REGEX("BlacklistRegexes", null);

    private String key;
    private Object def;

    private AppSetting(String key, Object def)
    {
        this.key = key;
        this.def = def;
    }

    public String getKey()
    {
        return key;
    }

    public Object getDefault()
    {
        return def;
    }

    public static final String VIRTUAL_APP_DEFAULT_SETTINGS = "com.matejdro.pebblenotificationcenter.virtual.default";
    public static final String VIRTUAL_APP_THIRD_PARTY = "com.matejdro.pebblenotificationcenter.virtual.thirdparty";
    public static final String VIRTUAL_APP_TASKER_RECEIVER = "com.matejdro.pebblenotificationcenter.virtual.taskerReceiver";

    public static List<Byte> parseVibrationPattern(AppSettingStorage storage)
    {
        String pattern = storage.getString(VIBRATION_PATTERN);
        String split[] = pattern.split(",");

        List<Byte> bytes = new ArrayList<Byte>(40);
        int max = Math.min(20, split.length);
        int total = 0;

        for (int i = 0; i < max; i++)
        {
            try
            {
                int segment = Integer.parseInt(split[i].trim());
                segment = Math.min(segment, 10000 - total);
                total -= segment;

                bytes.add((byte) (segment & 0xFF));
                bytes.add((byte) ((segment >> 8) & 0xFF));

                if (total >= 10000)
                    break;

            } catch (NumberFormatException e)
            {
            }
        }

        if (bytes.size() == 0)
        {
            bytes.add((byte) 0);
            bytes.add((byte) 0);

        }

        return bytes;
    }
}
