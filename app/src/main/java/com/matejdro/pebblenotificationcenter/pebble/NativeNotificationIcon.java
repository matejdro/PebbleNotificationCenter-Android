package com.matejdro.pebblenotificationcenter.pebble;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum NativeNotificationIcon implements Parcelable
{
    NOTIFICATION_GENERIC(1),
    TIMELINE_MISSED_CALL(2),
    NOTIFICATION_REMINDER(3),
    NOTIFICATION_FLAG(4),
    NOTIFICATION_WHATSAPP(5),
    NOTIFICATION_TWITTER(6),
    NOTIFICATION_TELEGRAM(7),
    NOTIFICATION_GOOGLE_HANGOUTS(8),
    NOTIFICATION_GMAIL(9),
    NOTIFICATION_FACEBOOK_MESSENGER(10),
    NOTIFICATION_FACEBOOK(11),
    AUDIO_CASSETTE(12),
    ALARM_CLOCK(13),
    TIMELINE_WEATHER(14),
    TIMELINE_SUN(16),
    TIMELINE_SPORTS(17),
    GENERIC_EMAIL(19),
    AMERICAN_FOOTBALL(20),
    TIMELINE_CALENDAR(21),
    TIMELINE_BASEBALL(22),
    BIRTHDAY_EVENT(23),
    CAR_RENTAL(24),
    CLOUDY_DAY(25),
    CRICKET_GAME(26),
    DINNER_RESERVATION(27),
    GENERIC_WARNING(28),
    GLUCOSE_MONITOR(29),
    HOCKEY_GAME(30),
    HOTEL_RESERVATION(31),
    LIGHT_RAIN(32),
    LIGHT_SNOW(33),
    MOVIE_EVENT(34),
    MUSIC_EVENT(35),
    NEWS_EVENT(36),
    PARTLY_CLOUDY(37),
    PAY_BILL(38),
    RADIO_SHOW(39),
    SCHEDULED_EVENT(40),
    SOCCER_GAME(41),
    STOCKS_EVENT(42),
    RESULT_DELETED(43),
    CHECK_INTERNET_CONNECTION(44),
    GENERIC_SMS(45),
    RESULT_MUTE(46),
    RESULT_SENT(47),
    WATCH_DISCONNECTED(48),
    DURING_PHONE_CALL(49),
    TIDE_IS_HIGH(50),
    RESULT_DISMISSED(51),
    HEAVY_RAIN(52),
    HEAVY_SNOW(53),
    SCHEDULED_FLIGHT(54),
    GENERIC_CONFIRMATION(55),
    DAY_SEPARATOR(56),
    NO_EVENTS(57),
    NOTIFICATION_BLACKBERRY_MESSENGER(58),
    NOTIFICATION_INSTAGRAM(59),
    NOTIFICATION_MAILBOX(60),
    NOTIFICATION_GOOGLE_INBOX(61),
    RESULT_FAILED(62),
    GENERIC_QUESTION(63),
    NOTIFICATION_OUTLOOK(64),
    RAINING_AND_SNOWING(65),
    REACHED_FITNESS_GOAL(66),
    NOTIFICATION_LINE(67),
    NOTIFICATION_SKYPE(68),
    NOTIFICATION_SNAPCHAT(69),
    NOTIFICATION_VIBER(70),
    NOTIFICATION_WECHAT(71),
    NOTIFICATION_YAHOO_MAIL(72),
    TV_SHOW(73),
    BASKETBALL(74);

    private int iconID;

    NativeNotificationIcon(int iconID)
    {
        this.iconID = iconID;
    }

    public int getIconID()
    {
        return iconID;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(this.ordinal());
    }

    public static final Parcelable.Creator<NativeNotificationIcon> CREATOR = new Creator<NativeNotificationIcon>()
    {
        @Override
        public NativeNotificationIcon createFromParcel(Parcel source)
        {
            return values()[source.readInt()];
        }

        @Override
        public NativeNotificationIcon[] newArray(int size)
        {
            return new NativeNotificationIcon[size];
        }
    };

    private static List<Map.Entry<String, NativeNotificationIcon>> iconKeywords = new ArrayList<>();
    static
    {
        iconKeywords.add(new AbstractMap.SimpleEntry<>("facebook.orca", NOTIFICATION_FACEBOOK_MESSENGER));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("whatsapp", NOTIFICATION_WHATSAPP));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("gmail", NOTIFICATION_GMAIL));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("facebook", NOTIFICATION_FACEBOOK));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("telegram", NOTIFICATION_TELEGRAM));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("twitter", NOTIFICATION_TWITTER));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("inbox", NOTIFICATION_GOOGLE_INBOX));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("mailbox", NOTIFICATION_MAILBOX));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("outlook", NOTIFICATION_OUTLOOK));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("instagram", NOTIFICATION_INSTAGRAM));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("bbm", NOTIFICATION_BLACKBERRY_MESSENGER));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("snapchat", NOTIFICATION_SNAPCHAT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("wechat", NOTIFICATION_WECHAT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("viber", NOTIFICATION_VIBER));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("skype", NOTIFICATION_SKYPE));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("calendar", TIMELINE_CALENDAR));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("alarm", ALARM_CLOCK));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("google.android.keep", NOTIFICATION_REMINDER));

        iconKeywords.add(new AbstractMap.SimpleEntry<>("weather", TIMELINE_SUN));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("event", TIMELINE_CALENDAR));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("line", NOTIFICATION_LINE));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("yahoo", NOTIFICATION_YAHOO_MAIL));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("phone", NOTIFICATION_VIBER));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("dialer", NOTIFICATION_VIBER));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("call", NOTIFICATION_VIBER));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("flight", SCHEDULED_FLIGHT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("air", SCHEDULED_FLIGHT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("jet", SCHEDULED_FLIGHT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("plane", SCHEDULED_FLIGHT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("music", AUDIO_CASSETTE));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("audio", AUDIO_CASSETTE));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("video", MOVIE_EVENT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("movie", MOVIE_EVENT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("feed", NEWS_EVENT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("rss", NEWS_EVENT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("news", NEWS_EVENT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("stock", STOCKS_EVENT));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("sms", GENERIC_SMS));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("mail", GENERIC_EMAIL));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("messag", GENERIC_EMAIL));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("mms", GENERIC_SMS));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("text", GENERIC_SMS));
        iconKeywords.add(new AbstractMap.SimpleEntry<>("reminder", NOTIFICATION_REMINDER));
    }

    public static NativeNotificationIcon getIconForApplication(String pkg, String appName)
    {
        pkg = pkg.toLowerCase();
        appName = appName.toLowerCase();

        for (Map.Entry<String, NativeNotificationIcon> potentialIcon : iconKeywords)
        {
            if (pkg.contains(potentialIcon.getKey()) || appName.contains(potentialIcon.getKey()))
                return potentialIcon.getValue();
        }

        return NOTIFICATION_GENERIC;
    }
}
