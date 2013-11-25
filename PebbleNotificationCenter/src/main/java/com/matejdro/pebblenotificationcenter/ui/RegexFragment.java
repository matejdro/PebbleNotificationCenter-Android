package com.matejdro.pebblenotificationcenter.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.matejdro.pebblenotificationcenter.Preferences;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.util.ListSerialization;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jbergler on 25/11/2013.
 */
public class RegexFragment extends Fragment {
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    private ListView regexListView;
    private View regexListViewHeader;
    private RegexListAdapter regexAdapter;
    private List<String> regexList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = preferences.edit();

        regexList = new ArrayList<String>();
        ListSerialization.loadCollection(preferences, regexList, "BlacklistRegexes");

        return inflater.inflate(R.layout.fragment_regex_list, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Spinner regexModeSpinner = (Spinner) getView().findViewById(R.id.regexModePickerSpinner);
        ArrayAdapter<CharSequence> regexModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.regexModePickerModes, android.R.layout.simple_spinner_item);
        regexModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        regexModeSpinner.setAdapter(regexModeAdapter);

        boolean regexMode = preferences.getBoolean(Preferences.REGEX_INCLUSION_MODE, false);
        regexModeSpinner.setSelection(regexMode ? 1 : 0);

        regexModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putBoolean(Preferences.REGEX_INCLUSION_MODE, position == 1); //Only two items, 1 => true
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        //Header for the Regex List, this is what we use to have an 'Add new entry' item
        regexListViewHeader = View.inflate(getActivity(), R.layout.fragment_regex_list_header, null);
        regexListViewHeader.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View view) {
                add();
            }
        });

        //Setup the List<String> adapter, map it to the ListView and add the header
        regexAdapter = new RegexListAdapter();
        regexListView  = (ListView) getView().findViewById(R.id.regexList);
        regexListView.addHeaderView(regexListViewHeader);
        regexListView.setAdapter(regexAdapter);

        //Click handler for list items.
        regexListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int headerCount = regexListView.getHeaderViewsCount(); //Make sure to take header into account.
                if (position >= headerCount) edit(position - headerCount);
            }
        });

        super.onActivityCreated(savedInstanceState);
    }

    private void saveList()
    {
        ListSerialization.saveCollection(editor, regexList, "BlacklistRegexes");
        regexAdapter.notifyDataSetChanged();
    }

    /**
     * Build a dialog to handle adding a new regex
     * TODO: this should do some error checking
     */
    private void add()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText editField = new EditText(getActivity());

        builder.setTitle("Add Filter");
        builder.setView(editField);

        builder.setMessage("Enter keyword/regex for this filter");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                regexList.add(editField.getText().toString());
                saveList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Build a dialog to handle editing/deleting a regex already in the list
     * TODO: this should do some error checking (like add())
     * @param position index in regexList to edit
     */
    private void edit(final int position)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText editField = new EditText(getActivity());
        editField.setText(regexList.get(position));

        builder.setTitle("Editing Filter");
        builder.setView(editField);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                regexList.set(position, editField.getText().toString());
                saveList();
            }
        });

        builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                regexList.remove(position);
                saveList();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private class RegexListAdapter extends BaseAdapter
    {

        @Override
        public int getCount() {
            return regexList.size();
        }

        @Override
        public Object getItem(int position) {
            return regexList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getActivity().getLayoutInflater().inflate(R.layout.fragment_regex_list_item, null);

            TextView text = (TextView) convertView.findViewById(R.id.regex);
            text.setText(regexList.get(position));

            return convertView;
        }
    }
}
