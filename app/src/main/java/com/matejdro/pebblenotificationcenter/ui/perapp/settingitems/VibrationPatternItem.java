package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.matejdro.pebblecommons.vibration.VibrationPatternPicker;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public class VibrationPatternItem extends BaseSettingItem
{
    private int textResource;
    private int descriptionResource;
    private VibrationPatternPicker vibrationPatternPicker;
    private Context context;

    public VibrationPatternItem(AppSettingStorage settingsStorage, int textResource, int descriptionResource)
    {
        super(settingsStorage, AppSetting.VIBRATION_PATTERN);
        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
    }

    @Override
    public View getView(PerAppActivity activity) {
        this.context = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_customview, null);
        TextView nameText = (TextView) view.findViewById(R.id.name);
        TextView descriptionText = (TextView) view.findViewById(R.id.description);
        FrameLayout settingContainer = (FrameLayout) view.findViewById(R.id.settingContainer);

        nameText.setText(textResource);
        descriptionText.setText(descriptionResource);

        vibrationPatternPicker = new VibrationPatternPicker(activity);
        vibrationPatternPicker.setCurrentPattern(settingsStorage.getString(AppSetting.VIBRATION_PATTERN));
        settingContainer.addView(vibrationPatternPicker);

        return view;
    }

    @Override
    public boolean onClose()
    {
        String vibrationPattern = vibrationPatternPicker.validateAndGetCurrentPattern();

        if (vibrationPattern == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.invalidVibrationPattern);
            builder.setPositiveButton(R.string.ok, null);
            builder.show();
            return false;
        }

        settingsStorage.setString(AppSetting.VIBRATION_PATTERN, vibrationPattern);
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (vibrationPatternPicker != null)
            vibrationPatternPicker.setEnabled(enabled);
    }

}
