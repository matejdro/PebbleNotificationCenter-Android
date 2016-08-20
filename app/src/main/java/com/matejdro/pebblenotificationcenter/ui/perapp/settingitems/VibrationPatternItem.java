package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.app.AlertDialog;
import android.text.InputType;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblecommons.pebble.PebbleVibrationPattern;

/**
 * Created by Matej on 19.10.2014.
 */
public class VibrationPatternItem extends EditTextItem
{
    public VibrationPatternItem(AppSettingStorage settingsStorage, int textResource, int descriptionResource)
    {
        super(settingsStorage, AppSetting.VIBRATION_PATTERN, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL, textResource, descriptionResource);
    }

    @Override
    public boolean onClose()
    {
        if (!changed)
            return true                     ;

        String vibrationPattern = editText.getText().toString();

        if (!PebbleVibrationPattern.validateVibrationPattern(vibrationPattern))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(R.string.invalidVibrationPattern);
            builder.setPositiveButton(R.string.ok, null);
            builder.show();
            return false;
        }

        settingsStorage.setString(AppSetting.VIBRATION_PATTERN, vibrationPattern);
        return true;
    }

}
