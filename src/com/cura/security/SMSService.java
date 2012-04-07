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
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
	String alternativeEmail;
	String messageBody;
	SmsMessage[] messages;
	DbHelper dbHelper;
	SQLiteDatabase db;
	BroadcastReceiver internet;
	BroadcastReceiver sms;
	TelephonyManager telMgr;
	LocationManager locMgr;
	LocationListener locListener;
	Context c;
	double latitude;
	double longitude;
	String securityMessageBody = "Click the link below to see the location of your device \n" +
			"http://maps.google.com/maps?q=" + latitude
			+ "," + longitude + "&t=k";
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
		gpsLocation();
		pattern = prefs.getString("securityPattern", "");
		alternativePhoneNo = prefs.getString("alternativePhoneNo", "");
		alternativeEmail = prefs.getString("alternativeEmail", "");
		telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		sms = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				dbHelper = new DbHelper(context);
				db = dbHelper.getWritableDatabase();
				c = context;
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
					enableGps();
					getFirstLocation();
					if (telMgr.getSimState() == TelephonyManager.SIM_STATE_READY && latitude!=0.0 && longitude!=0.0) {
						sendSMS(alternativePhoneNo,"Click the link below to see the location of your device \n" +
						"http://maps.google.com/maps?q=" + latitude
						+ "," + longitude + "&t=k");
						Toast.makeText(
								c,
								"A message has been sent to the owner of this device informing them of your location.",
								Toast.LENGTH_LONG).show();
					}
					if(!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)){
						sendEmail();
					}
					//internet broadcast receiver
					internet = new BroadcastReceiver(){

						@Override
						public void onReceive(Context context, Intent intent) {
							// TODO Auto-generated method stub
							boolean connectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
							if(!connectivity){
								sendEmail();
							}
						}
						
					};
				}
				db.close();
				dbHelper.close();
				//registering internet broadcast receiver
				IntentFilter NETintentFilter = new IntentFilter();
				NETintentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				registerReceiver(internet, NETintentFilter);
			}
			
		};
		//Registering sms broadcast receiver
		IntentFilter SMSintentFilter = new IntentFilter();
		SMSintentFilter.addAction(SMS_RECEIVED);
		registerReceiver(sms, SMSintentFilter);
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
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1000,
				locListener);
	}

	private void sendSMS(String phoneNumber, String message) {
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, null, null);
	}
	private void sendEmail(){
		Log.d("Email","wait to send email");
		//send email
		try {   
            GMailSender sender = new GMailSender("cura.app@gmail.com", "CURAapplication1+2+3+");
            sender.sendMail("Cura: Device location", 
            	"Click the link below to see the location of your device \n" +
            			"http://maps.google.com/maps?q=" + latitude
		+ "," + longitude + "&t=k", "cura.app@gmail.com", alternativeEmail);   
            Log.d("Email","email has been sent");
		} catch (Exception e) {   
            Log.e("SendMail", e.getMessage(), e);   
        } 
	}
	private void getFirstLocation(){
		Location location = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(location!=null){
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		}
	}
	public void enableGps(){
		String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

	    if(!provider.contains("gps")){ //if gps is disabled
	        final Intent poke = new Intent();
	        poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); 
	        poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
	        poke.setData(Uri.parse("3")); 
	        sendBroadcast(poke);
	    }
	}
}
