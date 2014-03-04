package com.matejdro.pebblenotificationcenter.location;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.util.TimeUtil;

public class LocationLookup {
  static private LocationLookup _instance = null;
  private LocationListener locationListener = null;
  private Context context;
  private SharedPreferences settings;
  private Date gotLastLocation;

  public LocationLookup(Context activity) {
    this.context = activity;
    _instance = this;
    settings = PreferenceManager.getDefaultSharedPreferences(activity);
  }

  static public synchronized LocationLookup getInstance() {
    return _instance;
  }

  public void lookup() {
    boolean backlight =
        settings.getBoolean(PebbleNotificationCenter.LIGHT_SCREEN_ON_SUNSET_NOTIFICATION, false);
    // will lookup time once a hour
    if (backlight && TimeUtil.hasTimePassed(gotLastLocation, 60 * 60 * 1000)) {
      // Acquire a reference to the system Location Manager
      LocationManager locationManager =
          (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      String locationProvider = LocationManager.NETWORK_PROVIDER;
      // Or use LocationManager.GPS_PROVIDER

      Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
      if (lastKnownLocation == null) {
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
          public void onLocationChanged(Location location) {
            // Called when a new location is found by the network
            // location provider.
            Editor edit = settings.edit();
            edit.putFloat(PebbleNotificationCenter.PreferencesLatitude,
                (float) location.getLatitude());
            edit.putFloat(PebbleNotificationCenter.PreferencesLongitude,
                (float) location.getLongitude());
            edit.putFloat(PebbleNotificationCenter.PreferencesAltitude,
                (float) location.getAltitude());
            edit.commit();
            gotLastLocation = new Date();
          }

          public void onStatusChanged(String provider, int status, Bundle extras) {}

          public void onProviderEnabled(String provider) {}

          public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive
        // location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
            locationListener);
      } else {
        Editor edit = settings.edit();
        edit.putFloat("latitude", (float) lastKnownLocation.getLatitude());
        edit.putFloat("longitude", (float) lastKnownLocation.getLongitude());
        edit.commit();
      }
    }
  }

  public void close() {
    if (locationListener != null) {
      LocationManager locationManager =
          (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      locationManager.removeUpdates(locationListener);
    }
    _instance = null;
  }
}
