package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public class SpinnerItem extends BaseSettingItem
{
    private AppSetting associatedSetting;
    private int textResource;
    private int descriptionResource;
    private int spinnerItemsListResource;
    private Integer spinnerItemValuesResource;


    private TextView nameText;
    private TextView descriptionText;
    private Spinner spinner;
    private boolean enabled = true;

    protected PerAppActivity activity;

    public SpinnerItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int spinnerItemsListResource, int textResource, int descriptionResource, Integer spinnerItemValuesResource)
    {
        super(settingsStorage);

        this.spinnerItemsListResource = spinnerItemsListResource;
        this.associatedSetting = associatedSetting;
        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
        this.spinnerItemValuesResource = spinnerItemValuesResource;
    }

    @Override
    public View getView(final PerAppActivity activity)
    {
        this.activity = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_spinner, null);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);
        spinner = (Spinner) view.findViewById(R.id.spinner);

        nameText.setText(textResource);
        if (descriptionResource == 0)
            descriptionText.setVisibility(View.GONE);
        else
            descriptionText.setText(descriptionResource);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (enabled)
                    spinner.performClick();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity, spinnerItemsListResource, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int selection = settingsStorage.getInt(associatedSetting);
        if (spinnerItemValuesResource != null)
        {
            int[] values = activity.getResources().getIntArray(spinnerItemValuesResource);
            for (int i = 0; i < values.length; i++)
            {
                if (values[i] == selection)
                {
                    selection = i;
                    break;
                }
            }
        }
        if (selection >= adapter.getCount())
            selection = 0;

        spinner.setSelection(selection);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                int value = i;

                if (spinnerItemValuesResource != null)
                    value = activity.getResources().getIntArray(spinnerItemValuesResource)[value];

                settingsStorage.setInt(associatedSetting, value);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });

        setEnabled(enabled);

        return view;
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

        spinner.setEnabled(enabled);
    }
}
