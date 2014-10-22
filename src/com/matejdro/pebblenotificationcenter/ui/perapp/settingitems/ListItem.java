package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 19.10.2014.
 */
public abstract class ListItem extends BaseSettingItem
{
    private AppSetting associatedSetting;
    private int textResource;
    private int descriptionResource;

    private TextView nameText;
    private TextView descriptionText;
    protected boolean enabled = true;

    protected PerAppActivity activity;

    private DisplayMetrics displayMetrics;

    private List<String> storage = new ArrayList<String>();
    private List<TextView> listViews = new ArrayList<TextView>();
    private List<View> listSeparators = new ArrayList<View>();

    private LinearLayout listContainer;
    private TextView listEmptyText;
    private Button addButton;


    public ListItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int textResource, int descriptionResource)
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
        displayMetrics = activity.getResources().getDisplayMetrics();

        View view = activity.getLayoutInflater().inflate(getLayout(), null);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);
        addButton = (Button) view.findViewById(R.id.addButton);
        listContainer = (LinearLayout) view.findViewById(R.id.listContainer);
        listEmptyText = (TextView) view.findViewById(R.id.listEmptyText);

        nameText.setText(textResource);
        descriptionText.setText(descriptionResource);

        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openAddDialog("");
            }
        });

        load();
        setEnabled(enabled);

        return view;
    }

    private TextView createViewItem(String text)
    {
        TextView view = new TextView(activity);
        view.setText(text);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!enabled)
                    return;

                int id = listViews.indexOf(view);
                openEditDialog(id, storage.get(id));
            }
        });

        view.setBackgroundResource(R.drawable.list_background);

        return view;
    }

    private View createSeparatorView()
    {
        View view = new View(activity);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPixels(2));
        params.setMargins(dpToPixels(30), dpToPixels(2), dpToPixels(30), dpToPixels(10));
        view.setLayoutParams(params);
        view.setBackgroundColor(0xFFDDDDDD);
        return view;
    }

    protected abstract void openAddDialog(String text);
    protected abstract void openEditDialog(final int id, String text);

    public void remove(int id)
    {
        View textView = listViews.get(id);
        listContainer.removeView(textView);
        listViews.remove(id);

        if (storage.size() > 1)
        {
            int separatorToRemove = id;
            if (separatorToRemove >= storage.size())
                separatorToRemove--;

            View separatorView = listSeparators.get(separatorToRemove);
            listSeparators.remove(separatorToRemove);
            listContainer.removeView(separatorView);
        }

        storage.remove(id);

        if (storage.isEmpty())
            listEmptyText.setVisibility(View.VISIBLE);
    }

    public void add(String text)
    {
        TextView listItem = createViewItem(text);
        View separator = createSeparatorView();

        storage.add(text);
        listViews.add(listItem);
        listSeparators.add(separator);

        if (storage.size() > 1)
            listContainer.addView(separator);
        listContainer.addView(listItem);

        listEmptyText.setVisibility(View.GONE);
    }

    public void update(int id, String text)
    {
        storage.set(id, text);
        listViews.get(id).setText(text);
    }

    private int dpToPixels(int dp)
    {
        return (int)((dp * displayMetrics.density) + 0.5);
    }


    protected int getLayout()
    {
        return R.layout.setting_list;
    }

    protected void load()
    {
        List<String> entries = settingsStorage.getStringList(associatedSetting);
        for (String item : entries)
            add(item);
    }

    @Override
    public boolean onClose()
    {
        settingsStorage.setStringList(associatedSetting, storage);
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
            listEmptyText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
            for (TextView view : listViews)
                view.setTextColor(activity.getResources().getColor(R.color.text_enabled));

        }
        else
        {
            nameText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            descriptionText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            listEmptyText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            for (TextView view : listViews)
                view.setTextColor(activity.getResources().getColor(R.color.text_disabled));
        }

        addButton.setEnabled(enabled);
    }
}
