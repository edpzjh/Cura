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
package com.cura;

/*
 * Description: In this class we implement the screen that the user sees upon going to Login Screen > Menu > Settings.
 * In here, the user is offered to enable the SMS/E-mail security feature. When this feature is enabled, the user is requested
 * to enter their alternative phone number or their alternative e-mail address along with a specific pattern.
 * 
 * This security feature allows users to have Cura's database completely wiped upon them sending the compromised phone 
 * a secret pattern of their choosing. (ex: send an SMS message containing "phone has been stolen!" to your Android phone to
 * wipe Cura's database and receive the location of the compromised phone as an SMS to your emergency phone number or as an 
 * e-mail to your emergency e-mail address).
 * 
 * Here the user is also allowed to change the password they need to allow them access to this very activity (as we explained
 * in "PassDialogPreference.java") (this password defaults to "default" when Cura is first installed).
 */

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.cura.security.SMSService;
import com.cura.validation.regexValidator;

public class PreferenceScreen extends PreferenceActivity implements
		OnPreferenceClickListener, OnSharedPreferenceChangeListener {
	private CheckBoxPreference cp;
	private regexValidator rv;
	private String email;
	private SharedPreferences sharedPreferences;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferencescreen);
		cp = (CheckBoxPreference) findPreference("enableSMS");
		cp.setOnPreferenceClickListener(this);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		rv = new regexValidator();
	}

	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equalsIgnoreCase("enableSMS") && cp.isChecked()) {
			// if the "enable SMS" checkbox is checked, enable the listening
			// (for SMS) service
			enableGps();
			startService(new Intent(this, SMSService.class));
			return true;
		}
		if (preference.getKey().equalsIgnoreCase("enableSMS")
				&& !cp.isChecked()) {
			// else if the "enable SMS" key is present while the box is not
			// checked, stop the listening (for SMS) service
			disableGps();
			stopService(new Intent(this, SMSService.class));
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	private boolean isMyServiceRunning() {
		// checks for the status of the above service so that it can be used
		// later to enable the service once the phone is started
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.cura.security.SMSSecurity".equals(service.service
					.getClassName())) {
				Log.d("Running", service.service.getClassName());
				return true;
			}
		}
		return false;
	}

	public void enableGps() {
		String provider = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (!provider.contains("gps")) {
			// if GPS is disabled, enable it for the user
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			// enable it
			poke.setData(Uri.parse("3"));
			sendBroadcast(poke);
		}
	}

	public void disableGps() {

	}

	public void onSharedPreferenceChanged(SharedPreferences sp,
			String key) {
		if (key.equals("alternativeEmail")) {
	        // email validation
	        String value = sp.getString(key, null);
	        if (!rv.validateEmail(value)) {
	            Toast.makeText(this, R.string.emailNotValid, Toast.LENGTH_SHORT).show();
	        }
	    }
		
	}
}