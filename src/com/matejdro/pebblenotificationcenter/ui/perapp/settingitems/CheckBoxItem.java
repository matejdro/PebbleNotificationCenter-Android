package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public class CheckBoxItem extends BaseSettingItem
{
    private AppSetting associatedSetting;
    private int textResource;
    private int descriptionResource;

    private TextView nameText;
    private TextView descriptionText;
    private CheckBox checkBox;
    private boolean enabled = true;

    protected PerAppActivity activity;

    public CheckBoxItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int textResource, int descriptionResource)
    {
        super(settingsStorage);

        this.associatedSetting = associatedSetting;
        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
    }

    @Override
    public View getView(PerAppActivity activity)
    {
        this.activity = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_checkbox, null);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);
        checkBox = (CheckBox) view.findViewById(R.id.checkbox);

        nameText.setText(textResource);
        descriptionText.setText(descriptionResource);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (enabled)
                    checkBox.toggle();
            }
        });

        checkBox.setChecked(getSavedValue());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                settingChanged(isChecked);
            }
        });

        setEnabled(enabled);

        return view;
    }

    protected boolean getSavedValue()
    {
        return settingsStorage.getBoolean(associatedSetting);
    }

    protected void settingChanged(boolean change)
    {
        settingsStorage.setBoolean(associatedSetting, change);
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

        checkBox.setEnabled(enabled);
    }
}
