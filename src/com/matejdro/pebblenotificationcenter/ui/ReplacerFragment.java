package com.matejdro.pebblenotificationcenter.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
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
import android.widget.Toast;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.ui.ReplacerEditDialog.ReplacerDialogResult;
import com.matejdro.pebblenotificationcenter.ui.ReplacerFilePickerDialog.FilePickerDialogResult;
import com.matejdro.pebblenotificationcenter.util.PreferencesUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplacerFragment extends Fragment {
	public static Pattern UNICODE_PATTERN = Pattern.compile("U\\+([0-9a-fA-F]+)");
	private static SharedPreferences preferences;
	private static SharedPreferences.Editor editor;

	private ListView listView;
	private ReplacerListAdapter listViewAdapter;

	private List<String> characters = new ArrayList<String>();
	private List<String> replacements = new ArrayList<String>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	
		this.setHasOptionsMenu(true);

		listViewAdapter = new ReplacerListAdapter();

		View view = inflater.inflate(R.layout.fragment_replacement, null);
		listView = (ListView) view.findViewById(R.id.replacementPairList);
		listView.setAdapter(listViewAdapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, final int position,
					long arg3) {
				ReplacerEditDialog dialog = new ReplacerEditDialog(getActivity(), characters.get(position), replacements.get(position));

				dialog.setOKListener(new ReplacerDialogResult() {

					@Override
					public void dialogFinished(CharSequence character, CharSequence replacement) {
						characters.set(position, convertToCharacter(character));
						replacements.set(position, replacement.toString());

						saveData();
					}
				});

				dialog.setDeleteListener(new ReplacerDialogResult() {
					@Override
					public void dialogFinished(CharSequence character, CharSequence replacement) {
						characters.remove(position);
						replacements.remove(position);

						saveData();
					}
				});

				dialog.show();

			}
		});

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = preferences.edit();

		PreferencesUtil.loadCollection(preferences, characters, PebbleNotificationCenter.REPLACING_KEYS_LIST);
		PreferencesUtil.loadCollection(preferences, replacements, PebbleNotificationCenter.REPLACING_VALUES_LIST);

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.replacer, menu);
	}

	private static String convertToCharacter(CharSequence string)
	{
		Matcher matcher = UNICODE_PATTERN.matcher(string);
		if (!matcher.matches())
			return string.toString();

        char[] chars = Character.toChars(Integer.parseInt(matcher.group(1), 16));

		return String.valueOf(chars);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
		case R.id.action_new:
			showNewDialog();
			return true;
		case R.id.action_delete_all:
			deleteAll();
			return true;
		case R.id.action_import:
			loadFromFile();
			return true;
		case R.id.action_export:
			saveToFile();
			return true;
		}

		return false;
	}


	private void showNewDialog()
	{
		ReplacerEditDialog dialog = new ReplacerEditDialog(getActivity());

		dialog.setOKListener(new ReplacerDialogResult() {

			@Override
			public void dialogFinished(CharSequence character, CharSequence replacement) {
				characters.add(convertToCharacter(character));
				replacements.add(replacement.toString());

				saveData();
			}
		});

		dialog.show();
	}

	private void saveData()
	{
		PreferencesUtil.saveCollection(editor, characters, PebbleNotificationCenter.REPLACING_KEYS_LIST);
		PreferencesUtil.saveCollection(editor, replacements, PebbleNotificationCenter.REPLACING_VALUES_LIST);

		listViewAdapter.notifyDataSetChanged();
		PebbleNotificationCenter.getInMemorySettings().markDirty();
	}

	private void deleteAll()
	{
		new AlertDialog.Builder(getActivity())
		.setTitle("Delete all")
		.setMessage("Are you sure you want to delete all replacement pairs?")
		.setNegativeButton("No", null)
		.setPositiveButton("Yes", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				characters.clear();
				replacements.clear();

				saveData();
			}
		})
		.show();

	}

	private void loadFromFile()
	{
		File folder = Environment.getExternalStoragePublicDirectory("NotificationCenter");
		if (!folder.exists())
			folder.mkdir();

		File[] fileList = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getName().endsWith(".txt");
			}
		});

		if (fileList.length < 1)
		{
			new AlertDialog.Builder(getActivity()).setTitle("Import").setMessage(R.string.importNoFiles).setPositiveButton("OK", null).show();
			return;
		}
		ReplacerFilePickerDialog dialog = new ReplacerFilePickerDialog(getActivity(), fileList);
		
		dialog.setFilePickListener(new FilePickerDialogResult() {
			
			@Override
			public void dialogFinished(File file) {
				try
				{
					BufferedReader reader = new BufferedReader(new FileReader(file));
					while (true)
					{
						String line = reader.readLine();
						if (line == null)
							break;
						
						if (!line.contains(">"))
							continue;
						
						String[] split = line.split(">");
						
						String left = split[0].trim();
						left = convertToCharacter(left);
						if (left == null)
							continue;
						
						String right = split[1].trim();
						
						characters.add(left);
						replacements.add(right);
					}
					
					reader.close();
					saveData();
					
					Toast.makeText(getActivity(), "File imported successfully!", Toast.LENGTH_SHORT).show();
				}
				catch (IOException e)
				{
					Toast.makeText(getActivity(), "Import failed - " + e.getMessage() + "!", Toast.LENGTH_SHORT).show();
					return;
				}
			}
		});
		
		dialog.show();
	}

	private void saveToFile()
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

		alert.setTitle("Export");
		alert.setMessage("Enter exporting file name:");

		final EditText input = new EditText(getActivity());
		alert.setView(input);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText();
				
				File folder = Environment.getExternalStoragePublicDirectory("NotificationCenter");
				if (!folder.exists())
					folder.mkdir();

				File file = new File(folder, value.toString() + ".txt");
				
				try
				{
					BufferedWriter writer = new BufferedWriter(new FileWriter(file));
					for (int i = 0; i < characters.size(); i++)
					{
						int charId = Character.codePointAt(characters.get(i), 0);
						
						writer.write("U+");
						writer.write(String.format("%04x", charId & 0xFFFFFFFF));
						writer.write(" > ");
						writer.write(replacements.get(i));
						writer.newLine();
					}
					
					writer.close();
				}
				catch (IOException e)
				{
					Toast.makeText(getActivity(), "Export failed - " + e.getMessage() + "!", Toast.LENGTH_SHORT).show();
					return;
				}
				
				Toast.makeText(getActivity(), "Replacement pairs was successfully exported to NotificationCenter/" + file.getName(), Toast.LENGTH_LONG).show();
			}
		});

		alert.setNegativeButton("Cancel", null);

		alert.show();
	}

	private class ReplacerListAdapter extends BaseAdapter
	{

		@Override
		public int getCount() {
			return characters.size();
		}

		@Override
		public Object getItem(int position) {
			return characters.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = getActivity().getLayoutInflater().inflate(R.layout.fragment_regex_list_item, null);

			((TextView) convertView.findViewById(R.id.regex)).setText(characters.get(position).concat(" => ").concat(replacements.get(position)));

			return convertView;
		}


	}
}
