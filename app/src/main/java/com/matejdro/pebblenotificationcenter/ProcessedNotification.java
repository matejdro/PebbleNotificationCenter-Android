package com.matejdro.pebblenotificationcenter;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ProcessedNotification implements Parcelable
{
	public int id;
    public int prevId = 0;
	public List<byte[]> textChunks = new ArrayList<>(13);
    public short firstSubtitleIndex = Short.MAX_VALUE;
    public short firstTextIndex = Short.MAX_VALUE;
    public short textLength;

    public boolean vibrated = false;
    public int nextChunkToSend = -1;
    public boolean nativeNotification;

    public PebbleNotification source;

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeInt(id);
        parcel.writeInt(prevId);
        parcel.writeValue(textChunks);
        parcel.writeByte((byte) (vibrated ? 1 : 0));
        parcel.writeInt(nextChunkToSend);
        parcel.writeByte((byte) (nativeNotification ? 1 : 0));
        parcel.writeValue(source);
    }

    public static final Creator<ProcessedNotification> CREATOR = new Creator<ProcessedNotification>()
    {
        @Override
        public ProcessedNotification createFromParcel(Parcel parcel)
        {
            ProcessedNotification notification = new ProcessedNotification();
            notification.id = parcel.readInt();
            notification.prevId = parcel.readInt();
            notification.textChunks = (List<byte[]>) parcel.readValue(getClass().getClassLoader());
            notification.vibrated = parcel.readByte() == 1;
            notification.nextChunkToSend = parcel.readInt();
            notification.nativeNotification = parcel.readByte() == 1;
            notification.source = (PebbleNotification) parcel.readValue(getClass().getClassLoader());

            return notification;
        }

        @Override
        public ProcessedNotification[] newArray(int i)
        {
            return new ProcessedNotification[0];
        }
    };

}
