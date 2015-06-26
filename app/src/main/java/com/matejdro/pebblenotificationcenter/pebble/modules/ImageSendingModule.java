package com.matejdro.pebblenotificationcenter.pebble.modules;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleImageToolkit;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.pebble.WatchappHandler;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Matej on 29.11.2014.
 */
public class ImageSendingModule extends CommModule
{
    public static final int MAX_IMAGE_WIDTH = 144;
    public static final int MAX_IMAGE_HEIGHT = 168 - 16;
    public static final int IMAGE_BYTES_PER_MESSAGE = 124 - 8 - 1;
    public static final int MAX_IMAGE_SIZE = 9000;


    public static int MODULE_IMAGE_SENDING = 5;

    private byte[] imageData;
    private int nextByteToSend = -1;

    public ImageSendingModule(PebbleTalkerService service)
    {
        super(service);
    }

    @Override
    public boolean sendNextMessage()
    {
        if (nextByteToSend != -1)
        {
            sendImagePart();
            return true;
        }

        return false;
    }

    public void sendImagePart()
    {
        Timber.d("SendImagePart " + nextByteToSend);

        int bytesToSend = Math.min(imageData.length - nextByteToSend, IMAGE_BYTES_PER_MESSAGE);
        byte[] bytes = new byte[bytesToSend + 1];
        bytes[0] = (byte) ((byte) (nextByteToSend % 256) & 0xFF);

        System.arraycopy(imageData, nextByteToSend, bytes, 1, bytesToSend);

        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 5);
        data.addUint8(1, (byte) 0);

        data.addBytes(2, bytes);

        getService().getPebbleCommunication().sendToPebble(data);

        nextByteToSend += bytesToSend;
        if (nextByteToSend >= imageData.length)
            nextByteToSend = -1;

    }

    public void startSendingImage(ProcessedNotification notification)
    {
        if (notification == null)
        {
            Timber.w("Invalid notification for image!");
            return;
        }

        imageData = notification.source.getPebbleImage();
        if (imageData == null)
        {
            Timber.w("Notification has no image!");
            return;
        }

        nextByteToSend = 0;
        getService().getPebbleCommunication().queueModule(this);
        getService().getPebbleCommunication().sendNext();
    }

    public void gotMessageStartSendingImage(PebbleDictionary message)
    {
        PebbleCommunication pebbleCommunication = getService().getPebbleCommunication();
        if (pebbleCommunication.getConnectedPebblePlatform() != PebbleCommunication.PEBBLE_PLATFORM_BASSALT)
            return;

        int notificationID = message.getInteger(2).intValue();

        ProcessedNotification notification = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications.get(notificationID);
        startSendingImage(notification);
    }

    public static byte[] prepareImage(Bitmap originalImage)
    {
        if (originalImage == null)
            return null;

        Bitmap image = PebbleImageToolkit.resizePreservingRatio(originalImage, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
        PebbleImageToolkit.ditherToPebbleTimeColors(image);
        byte[] imageData = PebbleImageToolkit.getIndexedPebbleImageBytes(image);
        if (imageData.length > MAX_IMAGE_SIZE)
        {
            Timber.w("Too big Pebble image: " + imageData.length + "bytes! Further resizing...");

            float sizeRatio = (float) MAX_IMAGE_SIZE / imageData.length;
            image = PebbleImageToolkit.resizePreservingRatio(originalImage, (int) (MAX_IMAGE_WIDTH * sizeRatio), (int) (MAX_IMAGE_HEIGHT * sizeRatio));
            PebbleImageToolkit.ditherToPebbleTimeColors(image);
            imageData = PebbleImageToolkit.getIndexedPebbleImageBytes(image);
        }

        return imageData;
    }

    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        int id = message.getUnsignedIntegerAsLong(1).intValue();

        Timber.d("image packet " + id);

        switch (id)
        {
            case 0: //Pebble opened
                gotMessageStartSendingImage(message);
                break;

        }
    }

    public static ImageSendingModule get(PebbleTalkerService service)
    {
        return (ImageSendingModule) service.getModule(MODULE_IMAGE_SENDING);
    }
}
