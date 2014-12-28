package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public class EditTextItem extends BaseSettingItem
{
    private AppSetting associatedSetting;
    private int textResource;
    private int descriptionResource;
    private int inputType;

    private TextView nameText;
    private TextView descriptionText;
    protected EditText editText;
    private boolean enabled = true;
    protected boolean changed = false;

    protected PerAppActivity activity;

    public EditTextItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int inputType, int textResource, int descriptionResource)
    {
        super(settingsStorage);

        this.associatedSetting = associatedSetting;
        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
        this.inputType = inputType;
    }

    @Override
    public View getView(PerAppActivity activity)
    {
        this.activity = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_edittext, null);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);
        editText = (EditText) view.findViewById(R.id.editText);

        nameText.setText(textResource);
        descriptionText.setText(descriptionResource);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (enabled)
                    editText.requestFocus();
            }
        });

        editText.setInputType(inputType);
        editText.setText(settingsStorage.getString(associatedSetting));
        editText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                changed = true;
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
        if (changed)
        {
            settingsStorage.setString(associatedSetting, editText.getText().toString());
        }
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

        editText.setEnabled(enabled);
    }
}
