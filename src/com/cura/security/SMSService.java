/*
 Copyright© 2010, 2011 Ahmad Balaa, Oday Maleh

 This file is part of Cura.

	Cura is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cura is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Cura.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cura.security;

/*
 * Description: This class implements the Service and is used to listen for when the pattern that the user chooses is sent
 * to the phone that has been compromised (this pattern is set in the Settings menu item from the Login Screen) upon which
 * it sends an SMS to the emergency phone number that the user specified in that same Settings menu item, containing the 
 * location of the compromised phone. We used GPS location to determine that.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteDatabase;
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

import com.cura.DbHelper;
import com.cura.R;
import com.cura.security.MyLocation.LocationResult;

public class SMSService extends Service implements OnSharedPreferenceChangeListener {

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
 double latitude;
 double longitude;
 long time;
 String securityMessageBody = "Click the link below to see the location of your device \n" + "http://maps.google.com/maps?q=" + latitude + ","
   + longitude + "&t=k";

 @Override
 public void onCreate() {
  super.onCreate();
  enableGps();
  LocationResult locationResult = new LocationResult() {
   @Override
   public void gotLocation(Location location) {
	try {
	 latitude = location.getLatitude();
	 longitude = location.getLongitude();
	 Log.d("Location", latitude + " - " + longitude);
	}
	catch (Exception e) {
	 latitude = 0;
	 longitude = 0;
	 Toast.makeText(SMSService.this, R.string.unableToLocate, Toast.LENGTH_LONG).show();
	}
   }
  };
  MyLocation myLocation = new MyLocation();
  myLocation.getLocation(this, locationResult);

 }

 @Override
 public void onStart(Intent intent, int startId) {
  super.onStart(intent, startId);
  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
  prefs.registerOnSharedPreferenceChangeListener(this);
  pattern = prefs.getString("securityPattern", "");
  alternativePhoneNo = prefs.getString("alternativePhoneNo", "");
  alternativeEmail = prefs.getString("alternativeEmail", "");
  telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

  sms = new BroadcastReceiver() {

   @Override
   public void onReceive(Context context, Intent intent) {
	dbHelper = new DbHelper(context);
	db = dbHelper.getWritableDatabase();
	Bundle bundle = intent.getExtras();

	if(bundle != null) {
	 Object[] pdus = (Object[]) bundle.get("pdus");
	 messages = new SmsMessage[pdus.length];
	 for (int i = 0; i < pdus.length; i++) {
	  messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
	 }
	 messageBody = messages[0].getMessageBody();
	}
	if(pattern.compareTo(messages[0].getMessageBody()) == 0) {
	 db.delete(DbHelper.userTableName, "", null);
	 Intent i = new Intent();
	 i.setAction("database.delete");
	 sendBroadcast(i);
	 Log.d("SMSservice", "received");
	 Toast.makeText(context, "Cura's database has been deleted!", Toast.LENGTH_LONG).show();

	 if(telMgr.getSimState() == TelephonyManager.SIM_STATE_READY && latitude != 0.0 && longitude != 0.0) {
	  sendSMS(alternativePhoneNo, "Click the link below to see the location of your device \n" + "http://maps.google.com/maps?q=" + latitude + ","
		+ longitude + "&t=k");
	  Toast.makeText(context, "A message has been sent to the owner of this device informing them of your location.", Toast.LENGTH_LONG).show();
	 }
	 internet = new BroadcastReceiver() {

	  @Override
	  public void onReceive(Context context, Intent intent) {
	   boolean connectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
	   if(!connectivity) {
		sendEmail();
	   }
	  }

	 };
	}
	db.close();
	dbHelper.close();
	IntentFilter NETintentFilter = new IntentFilter();
	NETintentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	registerReceiver(internet, NETintentFilter);
   }

  };
  IntentFilter SMSintentFilter = new IntentFilter();
  SMSintentFilter.addAction(SMS_RECEIVED);
  registerReceiver(sms, SMSintentFilter);
 }

 @Override
 public IBinder onBind(Intent arg0) {
  return null;
 }

 public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
  pattern = sharedPreferences.getString("securityPattern", "");
 }

 private void sendSMS(String phoneNumber, String message) {
  SmsManager sms = SmsManager.getDefault();
  sms.sendTextMessage(phoneNumber, null, message, null, null);
 }

 private void sendEmail() {
  Log.d("Email", "wait to send email");
  try {
   GMailSender sender = new GMailSender("cura.app@gmail.com", "CURAapplication1+2+3+");
   sender.sendMail("Cura: Device location", "Click the link below to see the location of your device \n" + "http://maps.google.com/maps?q="
	 + latitude + "," + longitude + "&t=k", "cura.app@gmail.com", alternativeEmail);
   Log.d("Email", "email has been sent");
  }
  catch (Exception e) {
   Log.e("SendMail", e.getMessage(), e);
  }
 }

 private void getFirstLocation() {
  Location location = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
  if(location != null) {
   latitude = location.getLatitude();
   longitude = location.getLongitude();
  }
 }

 public void enableGps() {
  String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

  if(!provider.contains("gps")) {
   final Intent poke = new Intent();
   poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
   poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
   poke.setData(Uri.parse("3"));
   sendBroadcast(poke);
  }
 }

 @Override
 public void onDestroy() {
  super.onDestroy();
  unregisterReceiver(sms);

 }
}
