package com.matejdro.pebblenotificationcenter.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;
import com.crashlytics.android.Crashlytics;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class NotificationParser {
	public String title;
	public String text;

	public NotificationParser(Context context, String pkg, Notification notification)
	{
		this.title = null;
		this.text = "";
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
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
		
		if ((title == null || title.trim().length() == 0) && text.contains("\n"))
		{
			int firstLineBreak = text.indexOf('\n');
			if (firstLineBreak < 40 && firstLineBreak < text.length() * 0.8)
			{
				title = text.substring(0, firstLineBreak).trim();
				text = text.substring(firstLineBreak).trim();
			}
		}
	}
	
	@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
	public boolean tryParseNatively(Context context, String pkg, Notification notification)
	{
		Bundle extras = getExtras(notification);
		if (extras == null)
			return false;

        if (extras.get(Notification.EXTRA_TEXT_LINES) != null && extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).length > 0)
        {
            if (parseInboxNotification(context, pkg, extras))
                return true;
        }

		if ((extras.get(Notification.EXTRA_TEXT) == null && extras.get(Notification.EXTRA_TEXT_LINES) == null))
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

    @TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
    public boolean parseInboxNotification(Context context, String pkg, Bundle extras)
    {
        AppSettingStorage settingStorage = new SharedPreferencesAppStorage(context, pkg, PebbleNotificationCenter.getInMemorySettings().getDefaultSettingsStorage(), true);
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
        if (!(sequence instanceof SpannableString))
        {
            return sequence.toString();
        }

        SpannableString spannableString = (SpannableString) sequence;
        String text = spannableString.toString();

        StyleSpan[] spans = spannableString.getSpans(0, spannableString.length(), StyleSpan.class);


       int addedLength = 0;

       for (int i = spans.length - 1; i >= 0; i--)
       {
          StyleSpan span = spans[i];
          if (span.getStyle() == Typeface.BOLD)
          {
              text = insertString(text, "\n",  spannableString.getSpanEnd(span));
              addedLength++;
          }
       }

        return text;
    }

    private static String insertString(String text, String insert, int pos)
    {
        return text.substring(0, pos).trim().concat(insert).concat(text.substring(pos).trim());
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
			Class secretClass = views.getClass();
			Class baseActionClass = Class.forName("android.widget.RemoteViews$Action");
			
			Field actionsField = secretClass.getDeclaredField("mActions");

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
				
				if (value.equals("...")
						|| isInteger(value.toString())
						|| text.contains(value)) {
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
        } catch (IllegalAccessException e) {
            Crashlytics.logException(e);
        } catch (NoSuchFieldException e) {
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
