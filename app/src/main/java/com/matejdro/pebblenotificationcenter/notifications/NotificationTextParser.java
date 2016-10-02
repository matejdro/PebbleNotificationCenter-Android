package com.matejdro.pebblenotificationcenter.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;

import com.crashlytics.android.Crashlytics;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationTextParser {
	public String title;
	public String text;

	public NotificationTextParser(Context context, PebbleNotification pebbleNotification, Notification notification)
	{
		this.title = null;
		this.text = "";

        String pkg = pebbleNotification.getKey().getPackage();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && !pebbleNotification.getSettingStorage(context).getBoolean(AppSetting.ALWAYS_PARSE_STATUSBAR_NOTIFICATION))
		{
			if (tryParseNatively(context, pkg, notification))
			{
				return;
			}
		}
				
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
			getExtraBigData(notification);
		else
			getExtraData(notification);
	}
	
	@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
	public boolean tryParseNatively(Context context, String pkg, Notification notification)
	{
		Bundle extras = getExtras(notification);
		if (extras == null)
			return false;

        if (parseMessageStyleNotification(context, pkg, notification, extras))
            return true;

        if (extras.get(Notification.EXTRA_TEXT_LINES) != null && extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).length > 0)
        {
            if (parseInboxNotification(context, pkg, extras))
                return true;
        }

		if (extras.get(Notification.EXTRA_TEXT) == null && extras.get(Notification.EXTRA_TEXT_LINES) == null && extras.get(Notification.EXTRA_BIG_TEXT) == null)
		{
			return false;
		}
		
		if (extras.get(Notification.EXTRA_TITLE_BIG) != null)
		{
			CharSequence bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
			if (bigTitle.length() < 40 || extras.get(Notification.EXTRA_TITLE) == null)
				title = bigTitle.toString();
			else
				title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();
		}
		else if (extras.get(Notification.EXTRA_TITLE) != null)
			title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

        if (extras.get(Notification.EXTRA_TEXT_LINES) != null)
        {
            for (CharSequence line : extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES))
            {
                text += formatCharSequence(line) + "\n\n";
            }
            text = text.trim();
        }
        else if (extras.get(Notification.EXTRA_BIG_TEXT) != null)
        {
            text = formatCharSequence(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        }
        else
        {
            text = formatCharSequence(extras.getCharSequence(Notification.EXTRA_TEXT));
        }

        if (extras.get(Notification.EXTRA_SUB_TEXT) != null)
        {
            text = text.trim();
            text= text + "\n\n" + formatCharSequence(extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        }


        return true;
	}

    public boolean parseMessageStyleNotification(Context context, String pkg, Notification notification, Bundle extras)
    {
        NotificationCompat.MessagingStyle messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
        if (messagingStyle == null)
            return false;

        title = formatCharSequence(messagingStyle.getConversationTitle());
        if (TextUtils.isEmpty(title))
            title = formatCharSequence(extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
        if (TextUtils.isEmpty(title))
            title = formatCharSequence(extras.getCharSequence(Notification.EXTRA_TITLE));
        if (title == null)
            title  = "";

        List<NotificationCompat.MessagingStyle.Message> messagesDescending = new ArrayList<>(messagingStyle.getMessages());
        Collections.sort(messagesDescending, new Comparator<NotificationCompat.MessagingStyle.Message>() {
            @Override
            public int compare(NotificationCompat.MessagingStyle.Message m1, NotificationCompat.MessagingStyle.Message m2) {
                return (int) (m2.getTimestamp() - m1.getTimestamp());
            }
        });

        text = "";
        for (NotificationCompat.MessagingStyle.Message message : messagesDescending)
        {
            String sender;
            if (message.getSender() == null)
                sender = formatCharSequence(messagingStyle.getUserDisplayName());
            else
                sender = formatCharSequence(message.getSender());

            text += sender + ": " + message.getText() + "\n";
        }

        return true;
    }

    @TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
    public boolean parseInboxNotification(Context context, String pkg, Bundle extras)
    {
        AppSettingStorage settingStorage = new SharedPreferencesAppStorage(context, pkg, PebbleNotificationCenter.getInMemorySettings().getDefaultSettingsStorage());
        if (!settingStorage.getBoolean(AppSetting.USE_ALTERNATE_INBOX_PARSER))
            return false;

        boolean useSubText = settingStorage.getBoolean(AppSetting.INBOX_USE_SUB_TEXT);

        if (useSubText && extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT) != null)
            title = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString();
        else if (useSubText && extras.getCharSequence(Notification.EXTRA_SUB_TEXT) != null)
            title = extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString();
        else if (extras.getCharSequence(Notification.EXTRA_TITLE) != null)
            title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        else
            return false;

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        boolean stopFirst = settingStorage.getBoolean(AppSetting.DISPLAY_ONLY_NEWEST);
        boolean reverse = settingStorage.getBoolean(AppSetting.INBOX_REVERSE);

        int i = reverse ? lines.length - 1 : 0;
        while (true)
        {
            text += formatCharSequence(lines[i]) + "\n\n";

            if (stopFirst)
                break;

            if (reverse)
            {
                i--;
                if (i <= 0)
                    break;
            }
            else
            {
                i++;
                if (i >= lines.length)
                    break;
            }
        }

        text = text.trim();

        return true;
    }

    private String formatCharSequence(CharSequence sequence)
    {
        if (sequence == null)
            return "";

        if (!(sequence instanceof SpannableString))
        {
            return sequence.toString();
        }

        SpannableString spannableString = (SpannableString) sequence;
        String text = spannableString.toString();

        StyleSpan[] spans = spannableString.getSpans(0, spannableString.length(), StyleSpan.class);


        int amountOfBoldspans = 0;

       for (int i = spans.length - 1; i >= 0; i--)
       {
          StyleSpan span = spans[i];
          if (span.getStyle() == Typeface.BOLD)
          {
              amountOfBoldspans++;
          }
       }

        if (amountOfBoldspans == 1)
        {
            for (int i = spans.length - 1; i >= 0; i--)
            {
                StyleSpan span = spans[i];
                if (span.getStyle() == Typeface.BOLD)
                {
                    text = insertString(text, "\n",  spannableString.getSpanEnd(span));
                    break;
                }
            }
        }

        return text;
    }

    private static String insertString(String text, String insert, int pos)
    {
        return text.substring(0, pos).trim().concat(insert).trim().concat(text.substring(pos)).trim();
    }

	private void getExtraData(Notification notification) {
		RemoteViews views = notification.contentView;
		if (views == null) {
			return;
		}

		parseRemoteView(views);

//		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		try {
//			//ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), new LinearLayout(context), true);
//			//SparseArray<CharSequence> viewText = getRemoteViewData(views);
//
//			//dumpViewGroup(0, localView, viewText);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return;
//		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void getExtraBigData(Notification notification) {
		RemoteViews views = null;
		try {
			views = notification.bigContentView;
		} catch (NoSuchFieldError e) {
			getExtraData(notification);
			return;
		}
		if (views == null) {
			getExtraData(notification);
			return;
		}

		parseRemoteView(views);

//		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		try {
//			//ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
//
//			//dumpViewGroup(0, localView, viewText);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	private void parseRemoteView(RemoteViews views)
	{
		try {
			Class remoteViewsClass = RemoteViews.class;
			Class baseActionClass = Class.forName("android.widget.RemoteViews$Action");


			Field actionsField = remoteViewsClass.getDeclaredField("mActions");

			actionsField.setAccessible(true);

			ArrayList<Object> actions = (ArrayList<Object>) actionsField.get(views);
			for (Object action : actions) {
                if (!action.getClass().getName().contains("$ReflectionAction"))
					continue;

				Field typeField = action.getClass().getDeclaredField("type");
				typeField.setAccessible(true);
				int type = typeField.getInt(action);
                if (type != 9 && type != 10)
					continue;


				int viewId = -1;
				try
				{
					Field idField = baseActionClass.getDeclaredField("viewId");
					idField.setAccessible(true);
					viewId = idField.getInt(action);
				}
				catch (NoSuchFieldException e)
				{
				}

				Field valueField = action.getClass().getDeclaredField("value");
				valueField.setAccessible(true);
				CharSequence value = (CharSequence) valueField.get(action);
				
				if (value == null ||
                    value.equals("...") ||
                    isInteger(value.toString()) ||
                    text.contains(value))
                {
					continue;
				}

				if (viewId == android.R.id.title)
				{
					if (title == null || title.length() < value.length())
						title = value.toString().trim();
				}
				else
					text += formatCharSequence(value) + "\n\n";

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * Get the extras Bundle from a notification using reflection. Extras were present in
     * Jellybean notifications, but the field was private until KitKat.
     */
    public static Bundle getExtras(Notification notif) {
        try {
            Field extrasField = Notification.class.getDeclaredField("extras");
            extrasField.setAccessible(true);

            Bundle extras = (Bundle) extrasField.get(notif);
            if (extras == null) {
                extras = new Bundle();
            }
            return extras;
        }
        catch (NoSuchFieldException e)
        {
            //Error is normal on pre-4.3 Android (just return null). Otherwise report crash.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                Crashlytics.logException(e);
        }
        catch (IllegalAccessException e)
        {
            Crashlytics.logException(e);
        }

        return null;
    }

	public static boolean isInteger(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
