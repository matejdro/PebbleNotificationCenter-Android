package com.matejdro.pebblenotificationcenter.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.matejdro.pebblecommons.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblecommons.util.RootUtil;
import com.matejdro.pebblenotificationcenter.GeneralNCDatabase;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblecommons.pebble.PebbleApp;
import com.matejdro.pebblenotificationcenter.appsetting.PebbleAppNotificationMode;
import com.matejdro.pebblenotificationcenter.pebble.appretrieval.AppRetrievalCallback;
import com.matejdro.pebblenotificationcenter.pebble.appretrieval.RootAppRetrieval;
import com.matejdro.pebblenotificationcenter.pebble.appretrieval.Sdk2AppRetrieval;
import com.matejdro.pebblenotificationcenter.pebble.appretrieval.Sdk3AppRetrieval;
import com.matejdro.pebblenotificationcenter.pebble.modules.SystemModule;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PebbleAppListFragment extends Fragment implements AppRetrievalCallback
{
	private ListView listView;
	private PebbleAppListAdapter listViewAdapter;
    private List<PebbleApp> apps = Collections.emptyList();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_pebble_app_list, null);

		listView = (ListView) view.findViewById(R.id.pebbleAppList);
		listViewAdapter = new PebbleAppListAdapter();
		listView.setAdapter(listViewAdapter);

		this.setHasOptionsMenu(true);

		return view;
	}

	@Override
	public void onStart()
	{
		super.onStart();
		reloadApps();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.pebbleapps, menu);
	}

	private void reloadApps()
	{
		apps = GeneralNCDatabase.getInstance().getPebbleApps();
		listViewAdapter.notifyDataSetChanged();
	}

	private void setAll(int option)
	{
		GeneralNCDatabase.getInstance().setAllPebbleAppNotificationMode(option);
		reloadApps();
	}

	public void retrievePebbleApps()
	{
		if (!PebbleKit.isWatchConnected(getActivity()))
		{
			new AlertDialog.Builder(getActivity()).setMessage(R.string.error_pebble_disconnected).setPositiveButton(R.string.ok, null).show();
			return;
		}

		PebbleKit.FirmwareVersionInfo pebbleFirmwareVersion = PebbleKit.getWatchFWVersion(getActivity());

		if (pebbleFirmwareVersion.getMajor() >= 3)
		{
			if (RootUtil.isDeviceRooted())
			{
				new RootAppRetrieval(getActivity(), this).retrieveApps();
			}
			else
			{
				new Sdk3AppRetrieval(getActivity(), this).retrieveApps();
			}
		}
		else
		{
			new Sdk2AppRetrieval(getActivity(), this).retrieveApps();
		}
	}

	@Override
	public void addApps(Collection<PebbleApp> apps)
	{
		GeneralNCDatabase.getInstance().addPebbleApps(apps);
		reloadApps();
	}

	public void deleteAll()
	{
		GeneralNCDatabase.getInstance().deleteAllPebbleApps();
		reloadApps();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
			case R.id.addPebbleApps:
				retrievePebbleApps();
				break;
			case R.id.allNC:
				setAll(PebbleAppNotificationMode.OPEN_IN_NOTIFICATION_CENTER);
				return true;
			case R.id.allPebble:
				setAll(PebbleAppNotificationMode.SHOW_NATIVE_NOTIFICATION);
				return true;
			case R.id.allNone:
				setAll(PebbleAppNotificationMode.DISABLE_NOTIFICATION);
				return true;
			case R.id.action_delete_all:
				deleteAll();
				break;

		}

		return false;
	}

	private class PebbleAppListAdapter extends BaseAdapter
	{
        private ArrayAdapter<CharSequence> appModePickerAdapter;

        public PebbleAppListAdapter()
        {
            appModePickerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.pebble_app_options, android.R.layout.simple_spinner_item);
            appModePickerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
		public int getCount() {
			return apps.size();
		}

		@Override
		public Object getItem(int position) {
			return apps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			final ViewHolder holder;

			if (convertView == null)
			{
				convertView = getActivity().getLayoutInflater().inflate(R.layout.pebble_app_list_item, parent, false);
				holder = new ViewHolder();

				holder.name = (TextView) convertView.findViewById(R.id.appName);
				holder.spinner = (Spinner) convertView.findViewById(R.id.appOption);

				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder) convertView.getTag();
			}

			final PebbleApp pebbleApp = apps.get(position);

			holder.name.setText(pebbleApp.getName());

            holder.spinner.setAdapter(appModePickerAdapter);
            holder.spinner.setSelection(pebbleApp.getNotificationMode());

            holder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
				{
					pebbleApp.setNotificationMode(position);
					GeneralNCDatabase.getInstance().setPebbleAppNotificationMode(pebbleApp.getUuid(), position);
				}

				@Override
				public void onNothingSelected(AdapterView<?> adapterView)
				{

				}
			});

			if (!pebbleApp.getUuid().equals(SystemModule.UNKNOWN_UUID))
			{
				convertView.setOnLongClickListener(new View.OnLongClickListener()
				{
					@Override
					public boolean onLongClick(View v)
					{

						new AlertDialog.Builder(getActivity()).setMessage(R.string.pebble_apps_delete_prompt).setNegativeButton(R.string.no, null)
								.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialog, int which)
									{
										GeneralNCDatabase.getInstance().deletePebbleApp(pebbleApp.getUuid());
										apps.remove(position);
										listViewAdapter.notifyDataSetChanged();
									}
								}).show();

						return false;
					}
				});
			}



			return convertView;
		}
	}

	private class AppLoadingTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected void onPreExecute()
        {
        }

		@Override
		protected Void doInBackground(Void... params) {
			if (getActivity() == null)
				return null;

            Context context = getActivity();

            PebbleDeveloperConnection connection = null;
            try
            {
                connection = new PebbleDeveloperConnection(getActivity());
                connection.connectBlocking();
				apps = connection.getInstalledPebbleApps();

			} catch (URISyntaxException e)
            {
                e.printStackTrace();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

           /* if (apps != null)
            {
                //Remove Notification Center from the list
                Iterator<PebbleApp> iterator = apps.iterator();
                while (iterator.hasNext())
                {
                    PebbleApp app = iterator.next();
                    if (app.getUuid().equals(PebbleNotificationCenter.WATCHAPP_UUID))
                        iterator.remove();
                }

                apps.addAll(PebbleApp.getSystemApps(context));

                Collections.sort(apps, new PebbleAppComparator());

                PebbleApp otherApp = new PebbleApp(context.getString(R.string.PebbleAppsOther), SystemModule.UNKNOWN_UUID);
                apps.add(otherApp);

                for (PebbleApp app : apps)
                {
                    app.setNotificationMode(PebbleAppNotificationSettings.getPebbleAppNotificationMode(preferences, app.getUuid()));
                }
            }*/

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
		}
	}

	private static class PebbleAppComparator implements Comparator<PebbleApp>
	{

		@Override
		public int compare(PebbleApp lhs, PebbleApp rhs) {
			return lhs.getName().compareTo(rhs.getName());
		}

	}

	private static class ViewHolder
	{
		TextView name;
		Spinner spinner;
	}
}
