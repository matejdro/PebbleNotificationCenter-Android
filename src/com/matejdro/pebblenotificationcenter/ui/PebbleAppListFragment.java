package com.matejdro.pebblenotificationcenter.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.DataReceiver;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.pebble.PebbleApp;
import com.matejdro.pebblenotificationcenter.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblenotificationcenter.util.PreferencesUtil;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class PebbleAppListFragment extends Fragment {

	private static SharedPreferences preferences;
	private static SharedPreferences.Editor editor;

	private ListView listView;
	private AppListAdapter listViewAdapter;
    private List<PebbleApp> apps;


	private boolean showOnResume = false;
    private boolean error = false;

	@Override
	public void onResume() {
		super.onResume();

		if (showOnResume && apps.size() > 0)
		{
			showOnResume = false;
			showList();
		}
        else if (error)
        {
            error = false;

            showLoading();
            new AppLoadingTask().execute();
        }
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		apps = Collections.synchronizedList(new ArrayList<PebbleApp>());

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = preferences.edit();
		
		new AppLoadingTask().execute();

		return inflater.inflate(R.layout.fragment_pebble_app_list, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

    private void showLoading()
    {
        View view = getView().findViewById(R.id.loadingBar);
        view.setVisibility(View.VISIBLE);
        view = getView().findViewById(R.id.loadingErrorText);
        view.setVisibility(View.GONE);
        view = getView().findViewById(R.id.pebbleAppList);
        view.setVisibility(View.GONE);
    }

	private void showList()
	{
        error = false;

		if (!isResumed())
		{
			showOnResume = true;
			return;
		}

		View view = getView().findViewById(R.id.loadingBar);
		view.setVisibility(View.GONE);
        view = getView().findViewById(R.id.loadingErrorText);
        view.setVisibility(View.GONE);

        listView = (ListView) getView().findViewById(R.id.pebbleAppList);
		listViewAdapter = new AppListAdapter();
		listView.setAdapter(listViewAdapter);
		listView.setVisibility(View.VISIBLE);
		listView.setScrollingCacheEnabled(true);
	}

    private void showError()
    {
        error = true;

        View view = getView().findViewById(R.id.loadingBar);
        view.setVisibility(View.GONE);
        view = getView().findViewById(R.id.loadingErrorText);
        view.setVisibility(View.VISIBLE);
        view = getView().findViewById(R.id.pebbleAppList);
        view.setVisibility(View.GONE);
    }

	private class AppListAdapter extends BaseAdapter
	{
        private ArrayAdapter<CharSequence> appModePickerAdapter;

        public AppListAdapter()
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
				convertView = getActivity().getLayoutInflater().inflate(R.layout.pebble_app_list_item, null);
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
                    PreferencesUtil.setPebbleAppNotificationMode(editor, pebbleApp.getUuid(), position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {

                }
            });

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
                connection = new PebbleDeveloperConnection();
                connection.connectBlocking();

            } catch (URISyntaxException e)
            {
                e.printStackTrace();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            apps = connection.getInstalledPebbleApps();
            if (apps != null)
            {
                //Remove Notification Center from the list
                Iterator<PebbleApp> iterator = apps.iterator();
                while (iterator.hasNext())
                {
                    PebbleApp app = iterator.next();
                    if (app.getUuid().equals(DataReceiver.pebbleAppUUID))
                        iterator.remove();
                }

                apps.addAll(PebbleApp.getSystemApps(context));

                Collections.sort(apps, new PebbleAppComparator());

                PebbleApp otherApp = new PebbleApp(context.getString(R.string.PebbleAppsOther), PebbleTalkerService.UNKNOWN_UUID);
                apps.add(otherApp);

                for (PebbleApp app : apps)
                {
                    app.setNotificationMode(PreferencesUtil.getPebbleAppNotificationMode(preferences, app.getUuid()));
                }
            }

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
            if (getActivity() == null || !isResumed())
                return;

            if (apps == null)
                showError();
            else
                showList();
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
