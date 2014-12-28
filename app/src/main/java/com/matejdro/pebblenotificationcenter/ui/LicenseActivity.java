package com.matejdro.pebblenotificationcenter.ui;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.matejdro.pebblenotificationcenter.R;

public class LicenseActivity extends Activity {
	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		webView = new WebView(this);
		setContentView(webView);
		
		webView.loadData(getResources().getString(R.string.notifierLicense), "text/html", "utf-8");
		super.onCreate(savedInstanceState);
	}	
}
