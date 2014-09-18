package com.matejdro.pebblenotificationcenter.tasker;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.ui.SettingsActivity;
import java.lang.reflect.Field;

/**
  * Created by Matej on 18.9.2014.
  */
 public class TaskerGlobalSettingsActivity extends SettingsActivity
 {
     private Bundle storage;

     @Override
     public void init()
     {
         storage = new Bundle();

         loadIntent();

         replaceSharedPreferences();
         Preference p;

         super.init();
     }

     protected void loadIntent() {
         Intent intent = getIntent();

         if (intent == null)
             return;

         Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
         if (bundle == null)
             return;

         int action = bundle.getInt("action");
         if (action != 1)
             return;

         storage.putAll(bundle);
         for (String key : bundle.keySet())
         {
             if (!key.startsWith("setting_"))
             {
                 storage.remove(key);
             }
         }
     }

     public void onBackPressed()
     {
         Intent intent = new Intent();

         Bundle bundle = new Bundle();

         bundle.putInt("action", 1);

         String description = getString(R.string.taskerDescriptionGlobalSetting);

         bundle.putAll(storage);

         intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", description);
         intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", bundle);

         setResult(RESULT_OK, intent);

         super.onBackPressed();
     }

     private void replaceSharedPreferences()
     {
         PreferenceManager manager = getPreferenceManager();
         BundleSharedPreferences bundleSharedPreferences = new BundleSharedPreferences(manager.getSharedPreferences(), storage);

         try
         {
            Field field = PreferenceManager.class.getDeclaredField("mSharedPreferences");
            field.setAccessible(true);
            field.set(manager, bundleSharedPreferences);

             field = PreferenceManager.class.getDeclaredField("mEditor");
             field.setAccessible(true);
             field.set(manager, bundleSharedPreferences.edit());

         } catch (Exception e)
         {
             e.printStackTrace();
         }
     }

     public static String bundle2string(Bundle bundle) {
               String string = "Bundle{";
               for (String key : bundle.keySet()) {
                   string += " " + key + " => " + bundle.get(key) + ";";
               }
               string += " }Bundle";
               return string;
           }
 }