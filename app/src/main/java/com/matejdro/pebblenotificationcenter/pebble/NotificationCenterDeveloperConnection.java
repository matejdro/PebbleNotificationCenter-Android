package com.matejdro.pebblenotificationcenter.pebble;

import android.content.Context;

import com.matejdro.pebblecommons.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.WearVoiceAction;
import com.matejdro.pebblecommons.util.TextUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;

import timber.log.Timber;

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
            if (endpoint == 3010) //Actions
            {
                if (notificationActionHandler != null)
                    notificationActionHandler.handle(bytes);
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

        Timber.d("Sending native notification " + LogWriter.bytesToHex(message));

        send(message);
    }

    public static NotificationCenterDeveloperConnection fromDevConn(PebbleDeveloperConnection pebbleDeveloperConnection)
    {
        return (NotificationCenterDeveloperConnection) pebbleDeveloperConnection;
    }
}
