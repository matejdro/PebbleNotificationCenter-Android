package com.matejdro.pebblenotificationcenter.pebble.appretrieval;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.View;
import android.widget.EditText;

import com.matejdro.pebblecommons.pebble.PebbleApp;
import com.matejdro.pebblecommons.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblecommons.pebble.PebbleSiteAPI;
import com.matejdro.pebblecommons.util.AccountRetreiver;
import com.matejdro.pebblecommons.util.RootUtil;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.ui.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RootAppRetrieval
{
    private Context context;
    private AppRetrievalCallback appRetrievalCallback;

    public RootAppRetrieval(Context context, AppRetrievalCallback appRetrievalCallback)
    {
        this.context = context;
        this.appRetrievalCallback = appRetrievalCallback;
    }

    public void retrieveApps()
    {
        new AlertDialog.Builder(context)
                .setMessage(R.string.root_app_retrieval_intro)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_continue, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        checkForRootPermissions();
                    }
                })
                .setNeutralButton(R.string.button_manual, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        new Sdk3AppRetrieval(context, appRetrievalCallback).retrieveApps();
                    }
                }).show();
    }

    private void checkForRootPermissions()
    {
        if (!RootUtil.isRootAccessible())
        {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.error_no_root)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            new Sdk3AppRetrieval(context, appRetrievalCallback).retrieveApps();
                        }
                    })
                    .show();

            return;
        }

        new GetLockerAppsTask().execute((Void[]) null);
    }

    private class GetLockerAppsTask extends AsyncTask<Void, Void, List>
    {
        private ProgressDialog progressDialog;


        @Override
        protected void onPreExecute()
        {
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(context.getString(R.string.retrieving_apps));
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialog)
                {
                    cancel(false);
                }
            });

            progressDialog.show();
        }

        @Override
        protected List doInBackground(Void... params)
        {
            File accountsDbFile = AccountRetreiver.copyAccounts(context);
            if (accountsDbFile == null)
                return null;

            String token = AccountRetreiver.getPebbleAccountToken(accountsDbFile);
            //noinspection ResultOfMethodCallIgnored
            accountsDbFile.delete();
            if (token == null)
                return null;

            String jsonString = PebbleSiteAPI.getLockerJson(token);
            if (jsonString == null)
                return null;


            try {
                JSONObject object = new JSONObject(jsonString);
                JSONArray applications = object.getJSONArray("applications");
                List<PebbleApp> apps = new ArrayList<>(applications.length());

                for (int i = 0; i < applications.length(); i++)
                {
                    JSONObject app = (JSONObject) applications.get(i);

                    String name = app.getString("title");
                    String uuid = app.getString("uuid");

                    PebbleApp pebbleApp = new PebbleApp(name, UUID.fromString(uuid));
                    apps.add(pebbleApp);
                }

                return apps;
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final List list)
        {
            progressDialog.hide();

            if (list == null)
            {
                new AlertDialog.Builder(context).setMessage(R.string.error_internet).setPositiveButton(R.string.ok, null).show();
                return;
            }


            appRetrievalCallback.addApps(list);


        }
    }
}
