package com.matejdro.pebblenotificationcenter.tasker;

import android.content.SharedPreferences;
 import android.os.Bundle;
 import java.util.Map;
 import java.util.Set;

/**
  * Created by Matej on 18.9.2014.
  */
 public class BundleSharedPreferences implements SharedPreferences
 {
     private Bundle storage;
     private SharedPreferences original;

     public BundleSharedPreferences(SharedPreferences originalValues, Bundle storage)
     {
         this.storage = storage;
         original = originalValues;
     }

     @Override
     public Map<String, ?> getAll()
     {
         throw new UnsupportedOperationException();
     }

     @Override
     public String getString(String s, String s2)
     {
         if (storage.containsKey("setting_".concat(s)))
             return storage.getString("setting_".concat(s));

         return original.getString(s, s2);
     }

     @Override
     public Set<String> getStringSet(String s, Set<String> strings)
     {
         throw new UnsupportedOperationException();
     }

     @Override
     public int getInt(String s, int i)
     {
         if (storage.containsKey("setting_".concat(s)))
             return storage.getInt("setting_".concat(s));

         return original.getInt(s, i);

     }

     @Override
     public long getLong(String s, long l)
     {
         if (storage.containsKey("setting_".concat(s)))
             return storage.getLong("setting_".concat(s));

         return original.getLong(s, l);
     }

     @Override
     public float getFloat(String s, float v)
     {
         if (storage.containsKey("setting_".concat(s)))
             return storage.getFloat("setting_".concat(s));

         return original.getFloat(s, v);
     }

     @Override
     public boolean getBoolean(String s, boolean b)
     {
         if (storage.containsKey("setting_".concat(s)))
             return storage.getBoolean("setting_".concat(s));

         return original.getBoolean(s, b);
     }

     @Override
     public boolean contains(String s)
     {
         return original.contains(s) || storage.containsKey(s);
     }

     @Override
     public Editor edit()
     {
         return new Editor();
     }

     @Override
     public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener)
     {
         throw new UnsupportedOperationException();
     }

     @Override
     public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener)
     {
         throw new UnsupportedOperationException();
     }

     public class Editor implements SharedPreferences.Editor
     {
         @Override
         public SharedPreferences.Editor putString(String s, String s2)
         {
             storage.putString("setting_".concat(s), s2);
             return this;
         }

         @Override
         public SharedPreferences.Editor putStringSet(String s, Set<String> strings)
         {
             throw new UnsupportedOperationException();
         }

         @Override
         public SharedPreferences.Editor putInt(String s, int i)
         {
             storage.putInt("setting_".concat(s), i);
             return this;
         }

         @Override
         public SharedPreferences.Editor putLong(String s, long l)
         {
             storage.putLong("setting_".concat(s), l);
             return this;
         }

         @Override
         public SharedPreferences.Editor putFloat(String s, float v)
         {
             storage.putFloat("setting_".concat(s), v);
             return this;
         }

         @Override
         public SharedPreferences.Editor putBoolean(String s, boolean b)
         {
             storage.putBoolean("setting_".concat(s), b);
             return this;
         }

         @Override
         public SharedPreferences.Editor remove(String s)
         {
             storage.remove("setting_".concat(s));
             return this;
         }

         @Override
         public SharedPreferences.Editor clear()
         {
             storage.clear();
             return this;
         }

         @Override
         public boolean commit()
         {
             return false;
         }

         @Override
         public void apply()
         {

         }
     }
 }
