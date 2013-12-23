package com.matejdro.pebblenotificationcenter.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import android.os.Handler;
import android.os.HandlerThread;

public class PebbleDeveloperConnection extends WebSocketClient {
	
	
	
	private UUID receivedUUID;
	private HandlerThread timeoutThread;
	private Handler timeoutThreadHandler;
	public CountDownLatch uuidWaitLatch;
	
	
	public PebbleDeveloperConnection() throws URISyntaxException {
		super(new URI("ws://127.0.0.1:9000"));		
	}
	
	@Override
	public void onOpen(ServerHandshake handshakedata) {
	}

	@Override
	public void onMessage(String message) {
	}

	@Override
	public void onMessage(ByteBuffer bytes) {		
		if (timeoutThread == null || !timeoutThread.isAlive())
			return;
		
		int source = bytes.get();
		if (source != 0)
			return;

		short size = bytes.getShort();
		short endpoint = bytes.getShort();
		
		if (size != 17 || endpoint != 6000)
			return;
		
		int cmd = bytes.get();
		if (cmd != 7)
			return;
			
		receivedUUID = new UUID(bytes.getLong(), bytes.getLong());
		
		returnId();
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}
	
	private void returnId()
	{
		timeoutThreadHandler.removeCallbacksAndMessages(null);
		timeoutThread.quit();
		uuidWaitLatch.countDown();
	}

	public synchronized UUID getCurrentRunningApp()
	{
		if (!isOpen())
			return null;
		
		try
		{
			receivedUUID = null;
			uuidWaitLatch = new CountDownLatch(1);

			timeoutThread = new HandlerThread("timeoutThread");
			timeoutThread.start();
			
			timeoutThreadHandler = new Handler(timeoutThread.getLooper());
			
			timeoutThreadHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					returnId();
				}
			}, 5000);

			//0x01 = CMD (PHONE_TO_WATCH)
			//0x00 0x01 = Data length (short) - 1
			//0x17 0x70 = Endpoint (6000 - APP_MANAGER)
			//0x07 = Data (7)
			
			byte[] requestCurrentApp = new byte[] { 0x1,0x0,0x1,0x17,0x70,0x7 };
			send(requestCurrentApp);			
			
			uuidWaitLatch.await();

			return receivedUUID;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return null;
	}

}
