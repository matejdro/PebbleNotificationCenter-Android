package com.matejdro.pebblenotificationcenter.xposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * @author Ben Grynhaus
 * 
 */
public class NotificationCenterFixer implements IXposedHookLoadPackage {
	public final String MODULE_NAME = "com.matejdro.pebblenotificationcenter.xposed.NotificationCenterFixer";

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		XposedBridge.log("Loaded app: " + lpparam.packageName);

		// If we are not in the Pebble package, do nothing.
		if (!lpparam.packageName.equals("com.getpebble.android"))
			return;

		// Get the classes of parameters that the method expects.
		final Class<?> pmParamClass = lpparam.classLoader.loadClass("com.getpebble.android.bluetooth.protocol.ProtocolMessage");
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
}