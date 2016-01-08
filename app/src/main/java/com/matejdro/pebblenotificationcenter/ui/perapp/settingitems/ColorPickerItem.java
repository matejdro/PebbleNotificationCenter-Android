package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.github.danielnilsson9.colorpickerview.dialog.ColorPickerDialogFragment;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public class ColorPickerItem extends BaseSettingItem implements ColorPickerDialogFragment.ColorPickerDialogListener
{
    private static final int DIALOG_ID = 1;

    private AppSetting associatedSetting;
    private int textResource;
    private int descriptionResource;

    private TextView nameText;
    private TextView descriptionText;

    private View colorView;
    private Button resetButton;
    private boolean enabled = true;

    protected PerAppActivity activity;

    private int curColor;

    public ColorPickerItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int textResource, int descriptionResource)
    {
        super(settingsStorage);

        this.associatedSetting = associatedSetting;
        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
    }

    @Override
    public View getView(final PerAppActivity activity)
    {
        this.activity = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_color, null);

        colorView = view.findViewById(R.id.colorDisplay);
        resetButton = (Button) view.findViewById(R.id.resetButton);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);

        nameText.setText(textResource);
        descriptionText.setText(descriptionResource);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (enabled)
                    colorView.performClick();
            }
        });

        curColor = settingsStorage.getInt(associatedSetting);
        colorView.setBackgroundColor(curColor);
        colorView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openColorPicker(activity);
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                set((Integer) associatedSetting.getDefault());
            }
        });

        setEnabled(enabled);

        return view;
    }

    private void openColorPicker(Activity activity)
    {
        ColorPickerDialogFragment colorPicker = ColorPickerDialogFragment.newInstance(DIALOG_ID, null, null, curColor | 0xFF000000, false);
        colorPicker.show(activity.getFragmentManager(), null);
    }

    private void set(int color)
    {
        colorView.setBackgroundColor(color);
        settingsStorage.setInt(associatedSetting, color);
        curColor = color;
    }

    @Override
    public boolean onClose()
    {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        if (activity == null)
            return;

        if (enabled)
        {
            nameText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
            descriptionText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
        }
        else
        {
            nameText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            descriptionText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
        }

        colorView.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    @Override
    public void onColorSelected(int i, int color)
    {
        set(color);
    }

    @Override
    public void onDialogDismissed(int i)
    {
    }
}
