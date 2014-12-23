package com.matejdro.pebblenotificationcenter.pebble;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import com.getpebble.android.kit.Constants;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.lists.NotificationListAdapter;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.WearVoiceAction;
import com.matejdro.pebblenotificationcenter.util.LogWriter;
import com.matejdro.pebblenotificationcenter.util.TextUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import timber.log.Timber;

public class PebbleDeveloperConnection extends WebSocketClient
{
    private List<DeveloperConnectionResult> waitingTasks = Collections.synchronizedList(new LinkedList<DeveloperConnectionResult>());
    private NativeNotificationActionHandler notificationActionHandler;
    private Context context;

    public PebbleDeveloperConnection(Context context) throws URISyntaxException
    {
        super(new URI("ws://127.0.0.1:9000"));
    }

    @Override
    public void onOpen(ServerHandshake handshakedata)
    {
    }

    @Override
    public void onMessage(String message)
    {
    }

    @Override
    public void onMessage(ByteBuffer bytes)
    {
        int source = bytes.get();
        if (source == 0) //Message from watch
        {
            short size = bytes.getShort();
            short endpoint = bytes.getShort();
            if (endpoint == 6000) //APP_INSTALL_MANAGER
            {
                int cmd = bytes.get();
                if (cmd == 7) //UUID of the active app
                {
                    UUID receivedUUID = new UUID(bytes.getLong(), bytes.getLong());
                    completeWaitingTasks(DeveloperConnectionTaskType.GET_CURRENT_RUNNING_APP, receivedUUID);
                }
                else if (cmd == 1) //List of all installed apps
                {
                    List<PebbleApp> installedApps = PebbleApp.getFromByteBuffer(bytes);
                    completeWaitingTasks(DeveloperConnectionTaskType.GET_ALL_INSTALLED_APP_META, installedApps);
                }
                else if (cmd == 5) //List of UUIDs of all installed apps
                {
                    List<UUID> installedUUIDs = PebbleApp.getUUIDListFromByteBuffer(bytes);
                    completeWaitingTasks(DeveloperConnectionTaskType.GET_ALL_INSTALLED_APP_UUID, installedUUIDs);
                }
            }
            else if (endpoint == 3010) //Actions
            {
                if (notificationActionHandler != null)
                    notificationActionHandler.handle(bytes);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote)
    {
    }

    @Override
    public void onError(Exception ex)
    {
        ex.printStackTrace();
    }

    public UUID getCurrentRunningApp()
    {
        if (!isOpen())
            return null;

        DeveloperConnectionResult<UUID> result = new DeveloperConnectionResult(DeveloperConnectionTaskType.GET_CURRENT_RUNNING_APP);
        waitingTasks.add(result);

        //0x01 = CMD (PHONE_TO_WATCH)
        //0x00 0x01 = Data length (short) - 1
        //0x17 0x70 = Endpoint (6000 - APP_MANAGER)
        //0x07 = Data (7)
        byte[] requestCurrentApp = new byte[]{0x1, 0x0, 0x1, 0x17, 0x70, 0x7};
        send(requestCurrentApp);

        return result.get(5, TimeUnit.SECONDS);
    }

    public List<PebbleApp> getInstalledPebbleApps()
    {
        if (!isOpen())
            return null;

        DeveloperConnectionResult<List<PebbleApp>> resultAppMeta = new DeveloperConnectionResult(DeveloperConnectionTaskType.GET_ALL_INSTALLED_APP_META);
        DeveloperConnectionResult<List<UUID>> resultAppUUID = new DeveloperConnectionResult(DeveloperConnectionTaskType.GET_ALL_INSTALLED_APP_UUID);

        waitingTasks.add(resultAppMeta);
        waitingTasks.add(resultAppUUID);


        //0x01 = CMD (PHONE_TO_WATCH)
        //0x00 0x01 = Data length (short) - 1
        //0x17 0x70 = Endpoint (6000 - APP_MANAGER)
        //0x01 = Data (1 = get apps meta, 5 = get apps UUID)
        byte[] request = new byte[]{0x1, 0x0, 0x1, 0x17, 0x70, 0x1};
        send(request);
        request = new byte[]{0x1, 0x0, 0x1, 0x17, 0x70, 0x5};
        send(request);


        List<PebbleApp> appList = resultAppMeta.get(5, TimeUnit.SECONDS);
        if (appList == null)
            return null;

        List<UUID> uuidList = resultAppUUID.get(5, TimeUnit.SECONDS);
        if (uuidList == null)
            return null;

        for (int i = 0; i < appList.size(); i++)
        {
            appList.get(i).uuid = uuidList.get(i);
        }

        appList.add(new PebbleApp("Sports app", Constants.SPORTS_UUID));
        appList.add(new PebbleApp("Golf app", Constants.GOLF_UUID));

        return appList;
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
                        voiceAction.populateCannedList(context, notification);

                        dataStream.writeByte(3); //Action type. 3 = text
                        dataStream.writeByte(2); //2 attributes

                        //Text attribute
                        dataStream.writeByte(1); //Attribute Type = 1 (title)
                        writeUTFPebbleString(dataStream, TextUtil.prepareString(action.getActionText(), 64), 64);

                        //Responses attribute
                        dataStream.writeByte(8); //Attribute Type = 8 (canned responses)
                        int size = 0;
                        List<String> responses = new ArrayList<String>(voiceAction.getCannedResponseList().size());
                        for (String response : voiceAction.getCannedResponseList())
                        {
                            response = TextUtil.prepareString(response, 20);
                            responses.add(response);

                            size += Math.min(20, response.getBytes().length) + 1;
                        }
                        writeUnsignedShortLittleEndian(dataStream, size); //Size of canned response list

                        for (String response : responses)
                        {
                            writeNullTerminatedPebbleString(dataStream, response, 20); //Write responses
                        }
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

    public void sendNotificationDismiss(int id)
    {
        if (!isOpen())
            return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(stream);

        try
        {
            dataStream.writeByte(1); //Message goes from phone to watch
            dataStream.writeShort(0); //Size of the messages (placeholder)
            dataStream.writeShort(3010); //Endpoint - EXTENSIBLE_NOTIFICATION
            dataStream.writeByte(1); //REMOVE_NOTIFICATION type
            writeUnsignedIntLittleEndian(dataStream, id); //notificaiton id

        } catch (IOException e)
        {
            e.printStackTrace();
        }

        //Insert size
        int size = stream.size() - 5; //First 5 bytes do not count
        byte[] message = stream.toByteArray();
        message[1] = (byte) (size >> 8);
        message[2] = (byte) size;

        //Disabled until it actually works just to prevent any problems
       // send(message);
    }


    public void sendActionACKNACKCheckmark(int notificationId, int actionId, String text)
    {
        if (!isOpen())
            return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(stream);

        try
        {
            dataStream.writeByte(1); //Message goes from phone to watch
            dataStream.writeShort(0); //Size of the messages (placeholder)
            dataStream.writeShort(3010); //Endpoint - EXTENSIBLE_NOTIFICATION
            dataStream.writeByte(17); //ACKNACK type
            writeUnsignedIntLittleEndian(dataStream, notificationId); //notificaiton id
            dataStream.writeByte(actionId); //Action ID
            dataStream.writeByte(00); //Icon attribute
            dataStream.writeByte(01); //Checkmark icon
            dataStream.writeByte(02); //Subtitle attribute
            writeUTFPebbleString(dataStream, text, 32);
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


    public void sendBasicNotification(String title, String message)
    {
        if (!isOpen())
            return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(stream);

        Calendar localCalendar = Calendar.getInstance();
        String date = new SimpleDateFormat().format(localCalendar.getTime());

        int sizeTitle = Math.min(255, title.getBytes().length) + 1;
        int sizeMessage = Math.min(255, message.getBytes().length) + 1;
        int sizeDate = Math.min(255, date.getBytes().length) + 1;

        try
        {
            dataStream.writeByte(1); //Message goes from phone to watch
            dataStream.writeShort(1 + sizeTitle + sizeMessage + sizeDate); //Size of the messages(3 strings and one byte)
            dataStream.writeShort(3000); //Endpoint - NOTIFICATIONS
            dataStream.writeByte(1); //SMS Notification command


            writeLegacyPebbleString(dataStream, title);
            writeLegacyPebbleString(dataStream, message);
            writeLegacyPebbleString(dataStream, date);

        } catch (IOException e)
        {
            e.printStackTrace();
        }


        send(stream.toByteArray());
    }

    public void registerActionHandler(NativeNotificationActionHandler handler)
    {
        this.notificationActionHandler = handler;
    }


    private void completeWaitingTasks(DeveloperConnectionTaskType type, Object result)
    {
        Iterator<DeveloperConnectionResult> iterator = waitingTasks.iterator();
        while (iterator.hasNext())
        {
            DeveloperConnectionResult task = iterator.next();
            if (task.getType() != type)
                continue;

            task.finished(result);
            iterator.remove();
        }
    }

    private class DeveloperConnectionResult<T>
    {
        private Handler timeoutCallbackHandler;
        private Thread timeoutThread;
        private T result;
        private CountDownLatch handlerReadyLatch;
        private CountDownLatch waitingLatch;
        private boolean isDone;
        private DeveloperConnectionTaskType type;

        private DeveloperConnectionResult(DeveloperConnectionTaskType type)
        {
            handlerReadyLatch = new CountDownLatch(1);
            timeoutThread = new Thread()
            {
                @Override
                public void run()
                {
                    Looper.prepare();

                    timeoutCallbackHandler = new Handler();
                    handlerReadyLatch.countDown();

                    Looper.loop();
                }
            };
            timeoutThread.start();

            waitingLatch = new CountDownLatch(1);
            isDone = false;
            this.type = type;
        }

        public DeveloperConnectionTaskType getType()
        {
            return type;
        }

        protected void finished(T result)
        {
            isDone = true;
            this.result = result;
            timeoutCallbackHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    Looper.myLooper().quit();
                }
            });
            timeoutCallbackHandler.removeCallbacksAndMessages(null);

            waitingLatch.countDown();
        }

        public void cancel()
        {
            finished(null);
        }

        public boolean isCancelled()
        {
            return isDone && result == null;
        }

        public boolean isDone()
        {
            return isDone;
        }

        public T get()
        {
            try
            {
                waitingLatch.await();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            return result;
        }


        public T get(long l, TimeUnit timeUnit)
        {
            try
            {
                handlerReadyLatch.await();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            timeoutCallbackHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    cancel();
                }
            }, timeUnit.toMillis(l));

            return get();
        }
    }

    private static enum DeveloperConnectionTaskType
    {
        GET_CURRENT_RUNNING_APP,
        GET_ALL_INSTALLED_APP_META,
        GET_ALL_INSTALLED_APP_UUID
    }

    private static void writeUnsignedIntLittleEndian(DataOutputStream stream, int number) throws IOException
    {
        number = number & 0xFFFFFFFF;

        stream.write((byte) number);
        stream.write((byte) (number >> 8));
        stream.write((byte) (number >> 16));
        stream.write((byte) (number >> 24));
    }

    private static void writeUnsignedShortLittleEndian(DataOutputStream stream, int number) throws IOException
    {
        number = number & 0xFFFF;

        stream.write((byte) number);
        stream.write((byte) (number >> 8));
    }

    public static String getPebbleStringFromByteBuffer(ByteBuffer buffer, int limit)
    {
        byte[] stringData = new byte[limit];

        try
        {
            buffer.get(stringData);
            String string = new String(stringData, "UTF-8");

            int end = string.indexOf(0);
            if (end >= 0)
                string = string.substring(0, end);

            return string;
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return "[ERROR]";
    }

    public static void writeLegacyPebbleString(DataOutputStream stream, String string)
    {
        string = TextUtil.trimString(string, 255, true);
        byte[] stringData = string.getBytes();

        try
        {
            stream.writeByte(stringData.length & 0xFF);
            stream.write(stringData);

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void writeNullTerminatedPebbleString(DataOutputStream stream, String string, int limit) throws IOException
    {
        string = TextUtil.trimString(string, limit, true);
        byte[] stringData = string.getBytes();

        stream.write(stringData);
        stream.write(0);
    }


    public static void writeUTFPebbleString(DataOutputStream stream, String string, int limit) throws IOException
    {
        string = TextUtil.trimString(string, limit, true);
        byte[] stringData = string.getBytes();

        writeUnsignedShortLittleEndian(stream, stringData.length);
        stream.write(stringData);
    }

}
