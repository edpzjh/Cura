package com.cura.security;

import com.cura.DbHelper;
import com.cura.LoginScreenActivity;
import com.cura.User;
import com.cura.Connection.SSHConnection;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SMSService extends Service implements
		OnSharedPreferenceChangeListener {

	private final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	String pattern;
	String alternativePhoneNo;
	String messageBody;
	SmsMessage[] messages;
	DbHelper dbHelper;
	SQLiteDatabase db;
	BroadcastReceiver internet;
	BroadcastReceiver sms;
	TelephonyManager telMgr;
	LocationManager locMgr;
	LocationListener locListener;
	double latitude;
	double longitude;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		// register preference change listener
		prefs.registerOnSharedPreferenceChangeListener(this);
		// and set remembered preferences
		pattern = prefs.getString("securityPattern", "");
		alternativePhoneNo = prefs.getString("alternativePhoneNo", "");
		telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		gpsLocation();
		sms = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				dbHelper = new DbHelper(context);
				db = dbHelper.getWritableDatabase();

				Bundle bundle = intent.getExtras();

				if (bundle != null) {
					Object[] pdus = (Object[]) bundle.get("pdus");
					messages = new SmsMessage[pdus.length];
					for (int i = 0; i < pdus.length; i++) {
						messages[i] = SmsMessage
								.createFromPdu((byte[]) pdus[i]);
					}
					messageBody = messages[0].getMessageBody();
				}
				if (pattern.compareTo(messages[0].getMessageBody()) == 0) {
					db.delete(DbHelper.userTableName, "", null);
					Toast.makeText(context,
							"Cura's database has been deleted!",
							Toast.LENGTH_LONG).show();

					if (telMgr.getSimState() == TelephonyManager.SIM_STATE_READY) {
						sendSMS(alternativePhoneNo,
								"http://maps.google.com/maps?q=" + latitude
										+ "," + longitude + "&t=k");
						Toast.makeText(
								context,
								"A message has been sent to the owner of this device informing them of your location.",
								Toast.LENGTH_LONG).show();
					}
				}
				db.close();
				dbHelper.close();

			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SMS_RECEIVED);
		registerReceiver(sms, intentFilter);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		pattern = sharedPreferences.getString("securityPattern", "");
	}

	public void gpsLocation() {
		locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locListener = new LocationListener() {

			public void onLocationChanged(Location location) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				Log.d("Location", latitude + " - " + longitude);
			}

			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub

			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub

			}

		};
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
				locListener);
	}

	private void sendSMS(String phoneNumber, String message) {
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, null, null);
	}

}
