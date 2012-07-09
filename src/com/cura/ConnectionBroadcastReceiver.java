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
 * Description: This class is for use in detecting the availability of a connection while using Cura. When the phone loses
 * connection to the Internet due to an error or a user choice, the user will be kicked back to the login screen and a popup
 * dialog will appear informing the user that they have lost Internet connection.
 */

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Vibrator;
import android.widget.Toast;

import com.cura.Connection.ConnectionService;

public class ConnectionBroadcastReceiver extends BroadcastReceiver {
	Vibrator vibrator;

	@Override
	public void onReceive(Context context, Intent intent) {
		boolean noConnectivity = intent.getBooleanExtra(
				ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		// initialize the noConnectivity flag
		vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);
		// initialize the vibrator
		if (noConnectivity && isMyServiceRunning(context)) {
			context.stopService(new Intent(context, ConnectionService.class));
			// if connection is lost while the user is still inside Cura
			Toast.makeText(context, R.string.connectionTimeoutMessage,
					Toast.LENGTH_LONG).show();
			// pop-up a message saying that the connection was lost
			vibrator.vibrate(300);
			// vibrate for this much time
			Intent closeAllActivities = new Intent(
					context.getApplicationContext(), LoginScreenActivity.class);
			// close all activities
			closeAllActivities.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			closeAllActivities.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.getApplicationContext().startActivity(closeAllActivities);
			// and logout.
		}
	}

	private boolean isMyServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			//function to check if a given Service is running
			if ("com.cura.Connection.ConnectionService".equals(service.service
					.getClassName())) {
				return true;
			}
		}
		return false;
	}
}