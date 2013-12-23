package com.matejdro.pebblenotificationcenter.ui;


import java.io.File;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.matejdro.pebblenotificationcenter.R;

public class ReplacerFilePickerDialog extends Dialog {
	private FilePickerDialogResult listener;
	private File[] fileList;

	private ArrayAdapter<File> fileAdapter;

	public ReplacerFilePickerDialog(Context context, File[] fileList) {
		super(context);
		this.fileList = fileList;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dialog_replacer_file_picker);

		setTitle("Import");

		((Button) findViewById(R.id.buttonCancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		fileAdapter = new FileListAdapter(getContext(), R.layout.fragment_regex_list_item, fileList);
		
		ListView view = (ListView) findViewById(R.id.listView1);
		view.setAdapter(fileAdapter);
		
		view.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				if (listener != null)
				{
					listener.dialogFinished(fileList[pos]);
				}
				
				dismiss();
			}
		});
	}

	public void setFilePickListener(FilePickerDialogResult listener)
	{
		this.listener = listener;
	}

	public static abstract class FilePickerDialogResult
	{
		public abstract void dialogFinished(File name);
	}
	
	private class FileListAdapter extends ArrayAdapter<File>
	{
		public FileListAdapter(Context context, int resource, File[] objects) {
			super(context, resource, objects);
		}

		@Override
		public View getView(int position, View convertView,
				ViewGroup parent) {
			if (convertView == null)
				convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.fragment_regex_list_item, null);
			
			((TextView) convertView.findViewById(R.id.regex)).setText(fileList[position].getName());
			
			return convertView;
		}
	}
}
