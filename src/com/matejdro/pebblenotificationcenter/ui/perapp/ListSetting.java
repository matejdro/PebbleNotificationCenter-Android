package com.matejdro.pebblenotificationcenter.ui.perapp;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Matej on 16.9.2014.
 */
public abstract class ListSetting
{
    private DisplayMetrics displayMetrics;

    private List<String> storage = new ArrayList<String>();
    private List<TextView> listViews = new ArrayList<TextView>();
    private List<View> listSeparators = new ArrayList<View>();

    private LinearLayout layout;
    private View listEmptyText;

    protected Activity activity;

    public ListSetting(Activity activity, int listLayoutId, int listAddButtonId, int listEmptyTextId)
    {
        this.activity = activity;
        this.layout = (LinearLayout) activity.findViewById(listLayoutId);
        this.listEmptyText = activity.findViewById(listEmptyTextId);
        displayMetrics = activity.getResources().getDisplayMetrics();

        ((Button) activity.findViewById(listAddButtonId)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                openAddDialog("");
            }
        });
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
                int id = listViews.indexOf(view);
                openEditDialog(id, storage.get(id));
            }
        });

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
        layout.removeView(textView);
        listViews.remove(id);

        if (storage.size() > 1)
        {
            int separatorToRemove = id;
            if (separatorToRemove >= storage.size())
                separatorToRemove--;

            View separatorView = listSeparators.get(separatorToRemove);
            listSeparators.remove(separatorToRemove);
            layout.removeView(separatorView);
        }

        storage.remove(id);
    }

    public void add(String text)
    {
        TextView listItem = createViewItem(text);
        View separator = createSeparatorView();

        storage.add(text);
        listViews.add(listItem);
        listSeparators.add(separator);

        if (storage.size() > 1)
            layout.addView(separator);
        layout.addView(listItem);

        listEmptyText.setVisibility(View.GONE);
    }

    public void update(int id, String text)
    {
        storage.set(id, text);
        listViews.get(id).setText(text);
    }

    public void addAll(List<String> list)
    {
        for (String s : list)
            add(s);
    }

    public List<String> getInternalStorage()
    {
        return storage;
    }

    private static boolean isRegexValid(String text)
    {
        if (text.trim().isEmpty())
            return false;

        try
        {
            Pattern pattern = Pattern.compile(text);

            return true;
        }
        catch (PatternSyntaxException e)
        {
            return false;
        }
    }

    private int dpToPixels(int dp)
    {
        return (int)((dp * displayMetrics.density) + 0.5);
    }
}

