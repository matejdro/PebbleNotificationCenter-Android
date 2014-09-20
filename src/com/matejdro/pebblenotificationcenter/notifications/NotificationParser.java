package com.matejdro.pebblenotificationcenter.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.CharacterStyle;
import android.widget.RemoteViews;
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
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
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
	
	@TargetApi(value = Build.VERSION_CODES.KITKAT)
	public boolean tryParseNatively(Context context, String pkg, Notification notification)
	{
		Bundle extras = notification.extras;
		if (extras == null)
			return false;

        if (extras.get(Notification.EXTRA_TEXT_LINES) != null && extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).length > 0)
        {
            if (parseInboxNotification(context, pkg, extras))
                return true;
        }
		if ((extras.get(Notification.EXTRA_TITLE) == null && extras.get(Notification.EXTRA_TITLE_BIG) == null) ||
			(extras.get(Notification.EXTRA_TEXT) == null && extras.get(Notification.EXTRA_TEXT_LINES) == null))
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
		else
			title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

        if (extras.get(Notification.EXTRA_TEXT_LINES) != null)
        {
            for (CharSequence line : extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES))
            {
                text += line + "\n\n";
            }
            text = text.trim();
        }
        else
        {
            text = extras.getCharSequence(Notification.EXTRA_TEXT).toString();
        }

        if (extras.get(Notification.EXTRA_SUB_TEXT) != null)
        {
            text = text.trim();
            text= text + "\n\n" + extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString();
        }


        return true;
	}

    @TargetApi(value = Build.VERSION_CODES.KITKAT)
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
            text += formatSpannable(lines[i]) + "\n\n";

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

    private String formatSpannable(CharSequence sequence)
    {
        if (!(sequence instanceof SpannableString))
        {
            return sequence.toString();
        }

        SpannableString spannableString = (SpannableString) sequence;

        CharacterStyle[] spans = spannableString.getSpans(0, spannableString.length(), CharacterStyle.class);

        if (spans.length == 0)
            return sequence.toString();

        CharacterStyle firstSpan = spans[0];
        if (spannableString.getSpanStart(firstSpan) != 0)
            return sequence.toString();

        int formatEnd = spannableString.getSpanEnd(firstSpan);

        String firstLine = spannableString.subSequence(0, formatEnd).toString();
        String secondLine = spannableString.subSequence(formatEnd, spannableString.length()).toString();

        return firstLine + "\n" + secondLine;
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
				String value = ((CharSequence) valueField.get(action)).toString();
				
				if (value.equals("...")
						|| isInteger(value)
						|| text.contains(value)) {
					continue;
				}

				value = value.trim();

				if (viewId == android.R.id.title)
				{
					if (title == null || title.length() < value.length())
						title = value;
				}
				else
					text += value + "\n\n";

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//	private void dumpViewGroup(int depth, ViewGroup vg, SparseArray<CharSequence> texts) {
	//		
	//		System.out.println("vg size " + vg.getChildCount());
	//		for (int i = 0; i < vg.getChildCount(); ++i) {
	//			
	//			
	//			
	//			View v = vg.getChildAt(i);
	//			
	//			if (v instanceof android.widget.Button
	//					|| v.getClass().toString().contains("android.widget.DateTimeView")) {
	//					continue;
	//			}
	//
	//			if (v instanceof TextView) {
	//				CharSequence viewSeq = texts.get(v.getId());
	//				if (viewSeq == null)
	//					continue;
	//				String viewText = viewSeq.toString();
	//				
	//				if (viewText.equals("...")
	//						|| isInteger(viewText)
	//						|| text.contains(viewText)) {
	//					continue;
	//				}
	//				
	//				viewText = viewText.trim();
	//				
	//				if (v.getId() == android.R.id.title)
	//				{
	//					if (title == null || title.length() < viewText.length())
	//						title = viewText;
	//				}
	//				else
	//					text += viewText + "\n";
	//			}
	//			if (v instanceof ViewGroup) {
	//				dumpViewGroup(depth + 1, (ViewGroup) v, texts);
	//			}
	//		}
	//	}

//	private SparseArray<CharSequence> getRemoteViewData(RemoteViews views)
//	{
//		SparseArray<CharSequence> viewText = new SparseArray<CharSequence>();
//
//		try {
//			Class secretClass = views.getClass();
//
//			Field actionsField = secretClass.getDeclaredField("mActions");
//
//			actionsField.setAccessible(true);
//
//			ArrayList<Object> actions = (ArrayList<Object>) actionsField.get(views);
//			for (Object action : actions) {			
//				if (!action.getClass().getName().contains("$ReflectionAction"))
//					continue;
//
//				Field typeField = action.getClass().getDeclaredField("type");
//				typeField.setAccessible(true);
//				int type = typeField.getInt(action);
//				if (type != 9 && type != 10)
//					continue;
//
//
//				Field idField = action.getClass().getSuperclass().getDeclaredField("viewId");
//				idField.setAccessible(true);
//				int viewId = idField.getInt(action);
//
//				Field valueField = action.getClass().getDeclaredField("value");
//				valueField.setAccessible(true);
//				CharSequence value = (CharSequence) valueField.get(action);
//
//				System.out.println(value);
//				
//				viewText.append(viewId, value);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return viewText;
//	}

	public static boolean isInteger(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
