package com.matejdro.pebblenotificationcenter.ui;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.matejdro.pebblenotificationcenter.R;

public class XposedSettingsActivity extends AppCompatActivity
{
    public static final String SHARED_PREFERENCES_NAME = "xposed";

    public static final String SETTING_FIX_FAILED_ERROR = "fixFailed";
    public static final String SETTING_FIX_DEVELOPER_CONNECTION = "fixDevConnTimeout";
    public static final String SETTING_BLOCK_PEBBLE_NOTIFICATIONS = "blockPebbleNotifications";
    public static final String SETTING_DISABLE_MUSIC_JUMP = "disableMusicJump";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xposed_settings);
    }

    public static class XposedSettingsFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(SHARED_PREFERENCES_NAME);
            //noinspection deprecation

            addPreferencesFromResource(R.xml.settings_xposed);
        }
    }
}
