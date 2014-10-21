package com.matejdro.pebblenotificationcenter;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.regex.Pattern;

/**
 * Created by Matej on 18.10.2014.
 */
public class NotificationKey implements Parcelable
{
    private String lolipopKey;

    private String pkg;
    private Integer androidId;
    private String tag;

    public NotificationKey(String lolipopKey)
    {
        this.lolipopKey = lolipopKey;

        String[] split = lolipopKey.split(Pattern.quote("|"));

        pkg = split[1];
        androidId = Integer.parseInt(split[2]);

        if (!split[3].equals("null"))
            tag = split[3];
    }

    public NotificationKey(String pkg, Integer androidId, String tag)
    {
        this.pkg = pkg;
        this.androidId = androidId;
        this.tag = tag;
    }

    public String getLolipopKey()
    {
        return lolipopKey;
    }

    public String getPackage()
    {
        return pkg;
    }

    public Integer getAndroidId()
    {
        return androidId;
    }

    public String getTag()
    {
        return tag;
    }

    public boolean equals(NotificationKey comparing)
    {
        if (this.lolipopKey != null && comparing.lolipopKey != null)
            return lolipopKey.equals(comparing.lolipopKey);

        return pkg.equals(comparing.pkg) && androidId.equals(comparing.androidId) && ((tag == null && comparing.tag == null) || (tag != null && tag.equals(comparing.tag)));
    }

    @Override
    public String toString()
    {
        if (lolipopKey != null)
            return lolipopKey;
        else
            return pkg + " " + androidId + " " + getTag();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        if (lolipopKey != null)
        {
            dest.writeByte((byte) 0);
            dest.writeValue(lolipopKey);
        }
        else
        {
            dest.writeByte((byte) 1);
            dest.writeValue(pkg);
            dest.writeValue(androidId);
            dest.writeValue(tag);
        }
    }

    public static final Creator<NotificationKey> CREATOR = new Creator<NotificationKey>()
    {
        @Override
        public NotificationKey createFromParcel(Parcel parcel)
        {
            boolean hasLollipopKey = parcel.readByte() == 0;

            if (hasLollipopKey)
            {
                String lollipopKey = (String) parcel.readValue(getClass().getClassLoader());
                return new NotificationKey(lollipopKey);
            }
            else
            {
                String pkg = (String) parcel.readValue(getClass().getClassLoader());
                Integer androidId = (Integer) parcel.readValue(getClass().getClassLoader());
                String tag = (String) parcel.readValue(getClass().getClassLoader());

                return new NotificationKey(pkg, androidId, tag);

            }
        }

        @Override
        public NotificationKey[] newArray(int i)
        {
            return new NotificationKey[i];
        }
    };
}
