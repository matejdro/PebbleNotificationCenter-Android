package com.matejdro.pebblenotificationcenter.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.util.ListSerialization;

public class BlacklistRegexesFragment extends Fragment {
	private static SharedPreferences preferences;
	private static SharedPreferences.Editor editor;
	private ListView listView;
	private RegexListAdapter adapter;

	private List<String> regexes;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		this.setHasOptionsMenu(true);

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = preferences.edit();

		regexes = new ArrayList<String>();
		ListSerialization.loadCollection(preferences, regexes, "BlacklistRegexes");

		return inflater.inflate(R.layout.fragment_blacklist_regexes, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		adapter = new RegexListAdapter();
		listView  = (ListView) getActivity().findViewById(R.id.regexList);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				edit(position);
			}
		});

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.regex, menu);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_content_new)
		{
			add();
			return true;
		}

		return false;
	}

	private void saveList()
	{
		ListSerialization.saveCollection(editor, regexes, "BlacklistRegexes");
		adapter.notifyDataSetChanged();
	}

	private void add()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final EditText editField = new EditText(getActivity());

		builder.setTitle("Adding regex");
		builder.setView(editField);

		builder.setMessage("Enter keyword or regex that notification should not have:");

		builder.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				regexes.add(editField.getText().toString());
				saveList();
			}
		});

		builder.setNegativeButton("Cancel", null);

		builder.show();
	}

	private void edit(final int position)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final EditText editField = new EditText(getActivity());
		editField.setText(regexes.get(position));

		builder.setTitle("Editing regex");
		builder.setView(editField);

		builder.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				regexes.set(position, editField.getText().toString());
				saveList();
			}
		});

		builder.setNeutralButton("Delete", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				regexes.remove(position);
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
			return regexes.size();
		}

		@Override
		public Object getItem(int position) {
			return regexes.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = getActivity().getLayoutInflater().inflate(R.layout.regex_list_item, null);

			TextView text = (TextView) convertView.findViewById(R.id.regex);
			text.setText(regexes.get(position));

			return convertView;
		}


	}

}
