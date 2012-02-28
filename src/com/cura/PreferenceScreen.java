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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.cura.security.SMSService;

public class PreferenceScreen extends PreferenceActivity implements
		OnPreferenceClickListener {
	private CheckBoxPreference cp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferencescreen);
		cp = (CheckBoxPreference) findPreference("enableSMS");
		cp.setOnPreferenceClickListener(this);

	}

	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equalsIgnoreCase("enableSMS") && cp.isChecked()) {
			// if the "enable SMS" checkbox is checked, enable the listening
			// (for SMS) service
			startService(new Intent(this, SMSService.class));
			return true;
		}
		if (preference.getKey().equalsIgnoreCase("enableSMS")
				&& !cp.isChecked()) {
			// else if the "enable SMS" key is present while the box is not
			// checked, stop the listening (for SMS) service
			stopService(new Intent(this, SMSService.class));
			return true;
		}
		return false;
	}

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
}