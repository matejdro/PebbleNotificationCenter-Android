package com.matejdro.pebblenotificationcenter.xposed;

import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;

import java.util.UUID;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class NotificationCenterFixer implements IXposedHookLoadPackage {
	public final String MODULE_NAME = "com.matejdro.pebblenotificationcenter.xposed.NotificationCenterFixer";

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		XposedBridge.log("Loaded app: " + lpparam.packageName);

		switch (lpparam.packageName)
		{
			case "com.getpebble.android":
				hookPebbleApp(lpparam);
				break;
			case "com.getpebble.android.basalt":
				hookPebbleTimeApp(lpparam);
				break;
			case PebbleNotificationCenter.PACKAGE:
				hookNC(lpparam);
				break;
		}


	}

    private void hookNC(XC_LoadPackage.LoadPackageParam lpparam)
    {
		// Hook into isXposedModuleRunning() to indicate whether module is running or not
        findAndHookMethod("com.matejdro.pebblenotificationcenter.PebbleNotificationCenter", lpparam.classLoader, "isXposedModuleRunning", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				param.setResult(true);
			}
		});
    }

	/**
	 * @author Ben Grynhaus
	 */
    private void hookPebbleApp(XC_LoadPackage.LoadPackageParam lpparam)
    {
		// Get the classes of parameters that the method expects.
		final Class<?> pmParamClass = findClass("com.getpebble.android.bluetooth.protocol.ProtocolMessage", lpparam.classLoader);
		if (pmParamClass == null)
			XposedBridge.log(String.format("%s: com.getpebble.android.bluetooth.protocol.ProtocolMessage NOT FOUND!", MODULE_NAME));
		else {
			XposedBridge.log(String.format("%s: com.getpebble.android.bluetooth.protocol.ProtocolMessage FOUND!", MODULE_NAME));

			findAndHookMethod("com.getpebble.android.framework.endpoint.NotificationEndpoint", lpparam.classLoader, "onReceive", pmParamClass, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
					final boolean result = true;
					methodHookParam.setResult(result);

					XposedBridge.log(String.format("%s: Fixed a pebble reply. Set the return value to: " + result + ".", MODULE_NAME));

					return result;
				};
			});
		}
    }

    private void hookPebbleTimeApp(final XC_LoadPackage.LoadPackageParam lpparam)
    {
		Class ap = findClass("com.getpebble.android.framework.l.a.ap", lpparam.classLoader);
		findAndHookMethod("com.getpebble.android.framework.g.cc", lpparam.classLoader, "a", ap, new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				Object apObject = param.args[0];

				UUID uuid = (UUID) getObjectField(apObject, "a");

				long firstByte = uuid.getMostSignificantBits();
				long secondByte = uuid.getLeastSignificantBits();

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
    }
}