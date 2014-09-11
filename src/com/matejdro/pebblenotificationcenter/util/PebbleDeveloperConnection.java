package com.matejdro.pebblenotificationcenter.util;

import android.os.Handler;
import android.os.Looper;
import com.getpebble.android.kit.Constants;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class PebbleDeveloperConnection extends WebSocketClient
{
    private List<DeveloperConnectionResult> waitingTasks = Collections.synchronizedList(new LinkedList<DeveloperConnectionResult>());

    public PebbleDeveloperConnection() throws URISyntaxException
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

    private static CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
    public static String getPebbleStringFromByteBuffer(ByteBuffer buffer, int limit)
    {
        StringBuilder builder = new StringBuilder();

        ByteBuffer newBuffer = buffer.duplicate();
        newBuffer.limit(newBuffer.position() + 32);
        buffer.position(buffer.position() + 32);
        String string = "";
        try
        {

            string = decoder.decode(newBuffer).toString();
            int end = string.indexOf(0);
            if (end < 0)
                return "";

            string = string.substring(0, end);
        } catch (CharacterCodingException e)
        {
            e.printStackTrace();
        }

        return string;

    }

}
