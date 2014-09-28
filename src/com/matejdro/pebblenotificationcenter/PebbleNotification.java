package com.matejdro.pebblenotificationcenter;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import java.util.ArrayList;

/**
 * Created by Matej on 22.9.2014.
 */
public class PebbleNotification implements Parcelable
{
    private int androidID;
    private String pkg;
    private String tag;
    private String title;
    private String subtitle;
    private String text;
    private boolean dismissable;
    private long postTime;
    private AppSettingStorage settingStorage;
    private ArrayList<NotificationAction> actions;
    private boolean noHistory;
    private boolean noActions;
    private boolean forceSwitch;
    private boolean listNotification;
    private PendingIntent openAction;

    public PebbleNotification(String title, String text, String pkg)
    {
        this.title = title == null ? "" : title;
        this.text = text == null ? "" : text;
        this.pkg = pkg;
        this.subtitle = "";


        androidID = -1;
        postTime = System.currentTimeMillis();
        dismissable = false;
        noHistory = false;
        noActions = false;
        forceSwitch = false;
        listNotification = false;
    }

    public Integer getAndroidID()
    {
        return androidID;
    }

    public void setAndroidID(Integer androidID)
    {
        this.androidID = androidID;
    }

    public String getPackage()
    {
        return pkg;
    }

    public void setPackage(String pkg)
    {
        this.pkg = pkg;
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title == null ? "" : title;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public void setSubtitle(String subtitle)
    {
        this.subtitle = subtitle == null ? "" : subtitle;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text == null ? "" : text;
    }

    public boolean isDismissable()
    {
        return dismissable;
    }

    public void setDismissable(boolean dismissable)
    {
        this.dismissable = dismissable;
    }

    public long getPostTime()
    {
        return postTime;
    }

    public void setPostTime(long postTime)
    {
        this.postTime = postTime;
    }

    public AppSettingStorage getSettingStorage(Context context)
    {
        if (settingStorage == null)
        {
            if (pkg == null)
                settingStorage = PebbleNotificationCenter.getInMemorySettings().getDefaultSettingsStorage();
            else
                settingStorage = new SharedPreferencesAppStorage(context, pkg, PebbleNotificationCenter.getInMemorySettings().getDefaultSettingsStorage(), true);
        }

        return settingStorage;
    }

    public ArrayList<NotificationAction> getActions()
    {
        return actions;
    }

    public void setActions(ArrayList<NotificationAction> actions)
    {
        this.actions = actions;
    }

    public boolean isHistoryDisabled()
    {
        return noHistory;
    }

    public void setNoHistory(boolean noHistory)
    {
        this.noHistory = noHistory;
    }

    public boolean areActionsDisabled()
    {
        return noActions;
    }

    public void setNoActions(boolean noActions)
    {
        this.noActions = noActions;
    }

    public boolean isListNotification()
    {
        return listNotification;
    }

    public void setListNotification(boolean listNotification)
    {
        this.listNotification = listNotification;
    }

    public PendingIntent getOpenAction()
    {
        return openAction;
    }

    public void setOpenAction(PendingIntent openAction)
    {
        this.openAction = openAction;
    }

    public boolean shouldNCForceSwitchToThisNotification()
    {
        return forceSwitch;
    }

    public void setForceSwitch(boolean forceSwitch)
    {
        this.forceSwitch = forceSwitch;
    }

    public boolean isSameNotification(PebbleNotification comparing)
    {
        return androidID == comparing.androidID && comparing.text.equals(text) && comparing.title.equals(title) && comparing.subtitle.equals(subtitle);
    }

    public boolean isSameNotification(Integer androidId, String pkg, String text)
    {
        return this.androidID != -1 && this.androidID == androidId.intValue() && this.pkg != null && this.pkg.equals(pkg) && (this.tag == null || this.tag.equals(tag));
    }


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeValue(title);
        parcel.writeValue(text);
        parcel.writeValue(pkg);
        parcel.writeInt(androidID);
        parcel.writeByte((byte) (dismissable ? 1 : 0));
        parcel.writeByte((byte) (noHistory ? 1 : 0));
        parcel.writeByte((byte) (noActions ? 1 : 0));
        parcel.writeByte((byte) (listNotification ? 1 : 0));
        parcel.writeByte((byte) (forceSwitch ? 1 : 0));
        parcel.writeLong(postTime);
        parcel.writeValue(tag);
        parcel.writeValue(subtitle);
        parcel.writeValue(text);
        parcel.writeValue(actions);
        parcel.writeValue(openAction);
    }

    public static final Creator<PebbleNotification> CREATOR = new Creator<PebbleNotification>()
    {
        @Override
        public PebbleNotification createFromParcel(Parcel parcel)
        {
            String title = (String) parcel.readValue(String.class.getClassLoader());
            String text = (String) parcel.readValue(String.class.getClassLoader());
            String pkg = (String) parcel.readValue(String.class.getClassLoader());

            PebbleNotification notification = new PebbleNotification(title, text, pkg);
            notification.androidID = parcel.readInt();
            notification.dismissable = parcel.readByte() == 1;
            notification.noHistory = parcel.readByte() == 1;
            notification.noActions = parcel.readByte() == 1;
            notification.listNotification = parcel.readByte() == 1;
            notification.forceSwitch = parcel.readByte() == 1;
            notification.postTime = parcel.readLong();
            notification.tag = (String) parcel.readValue(getClass().getClassLoader());
            notification.subtitle = (String) parcel.readValue(getClass().getClassLoader());
            notification.text = (String) parcel.readValue(getClass().getClassLoader());
            notification.actions = (ArrayList) parcel.readValue(getClass().getClassLoader());
            notification.openAction = (PendingIntent) parcel.readValue(getClass().getClassLoader());

            return notification;
        }

        @Override
        public PebbleNotification[] newArray(int i)
        {
            return new PebbleNotification[i];
        }
    };
}
