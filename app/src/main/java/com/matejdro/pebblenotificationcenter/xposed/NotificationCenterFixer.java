package com.matejdro.pebblenotificationcenter.xposed;

import android.annotation.SuppressLint;
import android.content.Context;

import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.ui.XposedSettingsActivity;

import java.util.Map;
import java.util.UUID;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class NotificationCenterFixer implements IXposedHookLoadPackage {
	public final String MODULE_NAME = "com.matejdro.pebblenotificationcenter.xposed.NotificationCenterFixer";

	private XSharedPreferences xSharedPreferences;

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		switch (lpparam.packageName)
		{
			case "com.getpebble.android.basalt":
				hookPebbleTimeApp(lpparam);
				break;
			case PebbleNotificationCenter.PACKAGE:
				hookNC(lpparam);
				break;
		}


	}

    @SuppressLint("SetWorldReadable")
	private void hookNC(XC_LoadPackage.LoadPackageParam lpparam)
    {
		// Hook into isXposedModuleRunning() to indicate whether module is running or not
        findAndHookMethod("com.matejdro.pebblenotificationcenter.PebbleNotificationCenter", lpparam.classLoader, "isXposedModuleRunning", XC_MethodReplacement.returnConstant(true));

		initPreferences();
		xSharedPreferences.getFile().setReadable(true, false);
    }

    private void hookPebbleTimeApp(final XC_LoadPackage.LoadPackageParam lpparam)
    {
		xSharedPreferences = new XSharedPreferences(PebbleNotificationCenter.PACKAGE, XposedSettingsActivity.SHARED_PREFERENCES_NAME);

		//Failed error fix
		Class wClass = findClass("com.getpebble.android.framework.l.a.w", lpparam.classLoader);
		findAndHookMethod("com.getpebble.android.framework.g.ag", lpparam.classLoader, "a", wClass, new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				xSharedPreferences.reload();
				if (!xSharedPreferences.getBoolean(XposedSettingsActivity.SETTING_FIX_FAILED_ERROR, true))
					return;

				Object as = param.args[0];
				UUID notificationUUID = (UUID) XposedHelpers.getObjectField(as, "a");

				long firstByte = notificationUUID.getMostSignificantBits();
				long secondByte = notificationUUID.getLeastSignificantBits();

				// Notification Center notifications have both longs equal and both have last 4 bytes equal to 0 (or sometimes equal to FFFFFFFF for some reason).
				if (firstByte == secondByte )
				{
					long last4Bytes = firstByte & 0x00000000FFFFFFFFL;
					if (last4Bytes == 0 || last4Bytes == 0xFFFFFFFFL)
					{
						//Block this method call if NC notification is detected.
						param.setResult(null);
					}
				}
			}
		});

		// Developer connection fix
		findAndHookMethod("com.getpebble.android.framework.e.d", lpparam.classLoader, "f", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				xSharedPreferences.reload();
				if (!xSharedPreferences.getBoolean(XposedSettingsActivity.SETTING_FIX_DEVELOPER_CONNECTION, false))
					return;

				//Prevent timeout from starting
				param.setResult(null);

			}
		});
		findAndHookMethod("com.getpebble.android.PebbleApplication", lpparam.classLoader, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				xSharedPreferences.reload();
				if (!xSharedPreferences.getBoolean(XposedSettingsActivity.SETTING_FIX_DEVELOPER_CONNECTION, false))
					return;

				// Kickstart developer connection on startup
				Object pebbleFrameworkObject = XposedHelpers.getStaticObjectField(findClass("com.getpebble.android.PebbleApplication", lpparam.classLoader), "g");
				XposedHelpers.callMethod(pebbleFrameworkObject, "d");
			}
		});

		// Disable all native notifications
		Class notificationClass = XposedHelpers.findClass("com.getpebble.android.notifications.a.b", lpparam.classLoader);
		findAndHookMethod("com.getpebble.android.framework.i.b", lpparam.classLoader, "a", notificationClass, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				xSharedPreferences.reload();
				if (!xSharedPreferences.getBoolean(XposedSettingsActivity.SETTING_BLOCK_PEBBLE_NOTIFICATIONS, false))
					return;

				Object notification = param.args[0];
				Enum notificationTypeEnum = (Enum) XposedHelpers.getObjectField(notification, "m");
				String notificationType = notificationTypeEnum.name();

				if (notificationType.equals("DEMO") || notificationType.equals("JSKIT"))
					return;

				param.setResult(null);
			}
		});

		// Disable native music app
		Class pebbleDeviceClass = XposedHelpers.findClass("com.getpebble.android.bluetooth.PebbleDevice", lpparam.classLoader);
		findAndHookMethod("com.getpebble.android.framework.g.t", lpparam.classLoader, "a", pebbleDeviceClass, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				xSharedPreferences.reload();
				if (!xSharedPreferences.getBoolean(XposedSettingsActivity.SETTING_DISABLE_MUSIC_JUMP, false))
					return;

				param.setResult(false);
			}
		});
    }

	private void initPreferences()
	{
		xSharedPreferences = new XSharedPreferences(PebbleNotificationCenter.PACKAGE, XposedSettingsActivity.SHARED_PREFERENCES_NAME);
	}
}