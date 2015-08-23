package com.matejdro.pebblenotificationcenter.pebble;

import android.graphics.Color;

import com.matejdro.pebblecommons.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblecommons.pebble.PebbleImageToolkit;
import com.matejdro.pebblecommons.util.TextUtil;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.notifications.actions.DismissOnPebbleAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.DismissOnPhoneAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.WearVoiceAction;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;

public class NotificationCenterDeveloperConnection extends PebbleDeveloperConnection
{
    private NativeNotificationActionHandler notificationActionHandler;
    private NCTalkerService service;

    public NotificationCenterDeveloperConnection(NCTalkerService service) throws URISyntaxException
    {
        super(service);
        this.service = service;
    }

    @Override
    public void onMessage(ByteBuffer bytes)
    {
        int source = bytes.get();
        if (source == 0) //Message from watch
        {
            short size = bytes.getShort();
            short endpoint = bytes.getShort();
            if (endpoint == 3010) //SDK2 Actions
            {
                if (notificationActionHandler != null)
                    notificationActionHandler.handleSdk2(bytes);
            }
            else if (endpoint == 0x2CB0) //SDK3 Actions
            {
                if (notificationActionHandler != null)
                    notificationActionHandler.handleSdk3(bytes);
            }
        }

        bytes.rewind();
        super.onMessage(bytes);
    }

    public void registerActionHandler(NativeNotificationActionHandler handler)
    {
        this.notificationActionHandler = handler;
    }

    public void sendNotification(ProcessedNotification notification)
    {
        if (!isOpen())
            return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(stream);

        int numOfActions = 0;
        if (notification.source.getActions() != null)
            numOfActions = notification.source.getActions().size();

        try
        {
            dataStream.writeByte(1); //Message goes from phone to watch
            dataStream.writeShort(0); //Size of the messages (placeholder)
            dataStream.writeShort(3010); //Endpoint - EXTENSIBLE_NOTIFICATION
            dataStream.writeByte(0); //ADD_NOTIFICATION type
            dataStream.writeByte(1); //ADD_NOTIFICATION command
            writeUnsignedIntLittleEndian(dataStream, 0); //flags (none for now)
            writeUnsignedIntLittleEndian(dataStream, notification.id); //notificaiton id
            writeUnsignedIntLittleEndian(dataStream, 0); //?
            writeUnsignedIntLittleEndian(dataStream, (int) (notification.source.getPostTime() / 1000)); //post time
            dataStream.writeByte(1); //DEFAULT layout
            dataStream.writeByte(2); //Size of attributes
            dataStream.writeByte(numOfActions); //Number of actions

            // Write attributes
            dataStream.writeByte(1); //Title
            writeUTFPebbleString(dataStream, notification.source.getTitle(), 64);

            String body = notification.source.getText();
            if (!notification.source.getSubtitle().isEmpty())
                body = notification.source.getSubtitle() + "\n" + body;

            dataStream.writeByte(3); //Body
            writeUTFPebbleString(dataStream, body, 512);

            // Write actions
            if (notification.source.getActions() != null)
            {
                for (int i = 0; i < numOfActions; i++)
                {
                    NotificationAction action = notification.source.getActions().get(i);

                    dataStream.writeByte(i + 1);
                    if (action instanceof WearVoiceAction)
                    {
                        WearVoiceAction voiceAction = (WearVoiceAction) action;
                        voiceAction.populateCannedList(service, notification, true);

                        dataStream.writeByte(3); //Action type. 3 = text
                        dataStream.writeByte(2); //2 attributes

                        //Text attribute
                        dataStream.writeByte(1); //Attribute Type = 1 (title)
                        writeUTFPebbleString(dataStream, TextUtil.prepareString(action.getActionText(), 64), 64);

                        //Responses attribute
                        dataStream.writeByte(8); //Attribute Type = 8 (canned responses)
                        int size = 0;
                        List<String> responses = voiceAction.getCannedResponseList();
                        for (String response : responses)
                        {
                            size += response.getBytes().length + 1;
                        }
                        size = Math.min(size, 128);

                        writeUnsignedShortLittleEndian(dataStream, size); //Size of canned response list
                        writeNullTerminatedPebbleStringList(dataStream, responses, 128); //Write responses
                    }
                    else
                    {
                        dataStream.writeByte(2); //Action type. 2 = normal
                        dataStream.writeByte(1); //1 attribute
                        dataStream.writeByte(1); //Attribute Type = 1 (title)
                        writeUTFPebbleString(dataStream, action.getActionText(), 64);
                    }
                }

            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }

        //Insert size
        int size = stream.size() - 5; //First 5 bytes do not count
        byte[] message = stream.toByteArray();
        message[1] = (byte) (size >> 8);
        message[2] = (byte) size;

        send(message);
    }

    public void sendSDK3Notification(ProcessedNotification notification, boolean dismissable)
    {
        if (!isOpen())
            return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(stream);

        short token = (short) (Math.random() * Short.MAX_VALUE);

        int notificationSizeFirstByte = 0;
        int payloadSizeFirstByte = 0;
        try
        {
            dataStream.writeByte(1); //Message goes from phone to watch
            dataStream.writeShort(0); //Size of the messages (placeholder)
            dataStream.writeShort(0xb1db); //Endpoint - Blob DB
            dataStream.writeByte(1); //Insert command
            writeUnsignedShortLittleEndian(dataStream, token); //Command token (randomized)
            dataStream.writeByte(4); //Notification database

            //Notification key = UUID
            dataStream.writeByte(16); //Key size = 16 bytes (2x long)
            writeUnsignedLongLittleEndian(dataStream, notification.id); //First long
            writeUnsignedLongLittleEndian(dataStream, notification.id); //Second long

            //Notification object
            notificationSizeFirstByte = stream.size();
            dataStream.writeShort(0); //Size of notification object (placeholder)
            writeUnsignedLongLittleEndian(dataStream, notification.id); //Notification ID, First Long
            writeUnsignedLongLittleEndian(dataStream, notification.id); //Second long
            dataStream.writeLong(0xED429C16F6744220L); //Magic number
            dataStream.writeLong(0x95DA454F303F15E2L); //Magic number
            writeUnsignedIntLittleEndian(dataStream, (int) (notification.source.getRawPostTime() / 1000)); //Notification timestamp
            dataStream.writeShort(0); //Duration of the item (not used for notifications, always 0)
            dataStream.writeByte(1); //Item type to insert = Notification (1)
            dataStream.writeShort(dismissable ? 0x0001 : 0x1001); //Flags (magic value, depends on whether notification is dismissable or not)
            dataStream.writeByte(4); //Layout (always 4)

            boolean hasColor = notification.source.getColor() != Color.TRANSPARENT;
            int numOfActions = 0;
            if (notification.source.getActions() != null)
                numOfActions = notification.source.getActions().size();

            payloadSizeFirstByte = stream.size();
            dataStream.writeShort(0); //Size of notification object payload (placeholder)
            dataStream.writeByte(hasColor ? 4 : 3); //Attribute count
            dataStream.writeByte(numOfActions); //Action count

            //ATTRIBUTES
            //Title attribute
            dataStream.writeByte(0x01);
            writeUTFPebbleString(dataStream, notification.source.getTitle(), 64);
            //Subtitle attribute
            dataStream.writeByte(0x02);
            writeUTFPebbleString(dataStream, notification.source.getSubtitle(), 64);
            //Body attribute
            dataStream.writeByte(0x03);
            writeUTFPebbleString(dataStream, notification.source.getText(), 512);
            //Color attribute
            if (hasColor)
            {
                dataStream.writeByte(0x1c); //Attribute ID
                writeUnsignedShortLittleEndian(dataStream, 1); //Attribute size
                dataStream.writeByte(PebbleImageToolkit.getGColor8FromRGBColor(notification.source.getColor())); //Color in GColor8 format
            }
            //Icon attribute
            /*writeUnsignedShortLittleEndian(dataStream, 4); //Attribute size
            writeUnsignedIntLittleEndian(dataStream, 0x80000010); //Icon?
            dataStream.writeByte(0x1c);*/


            //Actions
            if (notification.source.getActions() != null)
            {
                //Determine which action is dismiss action (used in Pebble's "Dismiss all" option)
                int dismissAction = -1;
                for (int i = 0; i < notification.source.getActions().size(); i++)
                {
                    NotificationAction action = notification.source.getActions().get(i);

                    if (action instanceof DismissOnPhoneAction)
                    {
                        dismissAction = i;
                        break;
                    }
                    else if (action instanceof DismissOnPebbleAction)
                    {
                        dismissAction = i;
                    }
                }


                for (int i = 0; i < numOfActions; i++)
                {
                    NotificationAction action = notification.source.getActions().get(i);

                    dataStream.writeByte(i + 1); //Action ID
                    if (action instanceof WearVoiceAction)
                    {
                        WearVoiceAction voiceAction = (WearVoiceAction) action;
                        voiceAction.populateCannedList(service, notification, true);

                        dataStream.writeByte(3); //Action type. 3 = reply
                        dataStream.writeByte(2); //2 attributes

                        //Text attribute
                        dataStream.writeByte(1); //Attribute Type = 1 (title)
                        writeUTFPebbleString(dataStream, TextUtil.prepareString(action.getActionText(), 64), 64);

                        //Responses attribute
                        dataStream.writeByte(8); //Attribute Type = 8 (canned responses)
                        int size = 0;
                        List<String> responses = voiceAction.getCannedResponseList();
                        for (String response : responses)
                        {
                            size += response.getBytes().length + 1;
                        }
                        size = Math.min(size, 128);

                        writeUnsignedShortLittleEndian(dataStream, size); //Size of canned response list
                        writeNullTerminatedPebbleStringList(dataStream, responses, 128); //Write responses
                    }
                    else
                    {
                        int actionType = i == dismissAction ? 4 : 2;
                        dataStream.writeByte(actionType); //Action type. 2 = normal action, 4 = dismiss action
                        dataStream.writeByte(1); //1 attribute
                        dataStream.writeByte(1); //Attribute Type = 1 (title)
                        writeUTFPebbleString(dataStream, action.getActionText(), 64);
                    }
                }

            }
        } catch (
                IOException e
                )

        {
            e.printStackTrace();
        }

        //Insert sizes

        int notificationObjectSize = stream.size() - notificationSizeFirstByte - 2; //First 2 bytes are part of size number so they don't count
        int notificationPayloadSize = stream.size() - payloadSizeFirstByte - 2; //First 2 bytes are part of size number so they don't count
        int globalSize = stream.size() - 5; //First 5 bytes do not count
        byte[] message = stream.toByteArray();
        message[1] = (byte) (globalSize >> 8);
        message[2] = (byte) globalSize;

        message[notificationSizeFirstByte] = (byte) notificationObjectSize;
        message[notificationSizeFirstByte + 1] = (byte) (notificationObjectSize >> 8);
        message[payloadSizeFirstByte] = (byte) notificationPayloadSize;
        message[payloadSizeFirstByte + 1] = (byte) (notificationPayloadSize >> 8);

        send(message);

    }

    public static NotificationCenterDeveloperConnection fromDevConn(PebbleDeveloperConnection pebbleDeveloperConnection)
    {
        return (NotificationCenterDeveloperConnection) pebbleDeveloperConnection;
    }
}
