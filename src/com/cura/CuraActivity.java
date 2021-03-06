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

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;
import com.cura.Terminal.TerminalActivity;
import com.cura.security.SMSService;
import com.cura.syslog.SysLogActivity;
import com.cura.sysmonitor.SysMonitorActivity;

public class CuraActivity extends Activity implements OnClickListener {

	TableRow terminalRow, sysMonitorRow, sysLogRow, nessusRow, nmapRow,
			sysConnectRow;
	// menu buttons

	User userTemp;
	// user object
	private CommunicationInterface conn;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			// TODO Auto-generated method stub
			conn = CommunicationInterface.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			conn = null;
			Toast.makeText(CuraActivity.this, "Service Disconnected",
					Toast.LENGTH_LONG);
		}
	};

	public synchronized String getUname() {
		String resultUNAME = "";
		try {
			resultUNAME = conn.executeCommand("uname -a");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultUNAME;
	}

	public synchronized String getUptime() {
		String resultUPTIME = "";
		try {
			resultUPTIME = conn.executeCommand("uptime");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultUPTIME;
	}

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
		bindService(i, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		doBindService();
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userTemp = extras.getParcelable("user");
		}
		this.setTitle(userTemp.getUsername() + "'s Control Box");
		// welcoming the user in this activity's title.

		// SETTING CLICK-LISTENERS FOR ALL OF THE BUTTONS
		terminalRow = (TableRow) findViewById(R.id.TerminalRow);
		terminalRow.setOnClickListener(this);

		sysMonitorRow = (TableRow) findViewById(R.id.SysMonitorRow);
		sysMonitorRow.setOnClickListener(this);

		sysLogRow = (TableRow) findViewById(R.id.SysLogRow);
		sysLogRow.setOnClickListener(this);

		nessusRow = (TableRow) findViewById(R.id.NessusRow);
		nessusRow.setOnClickListener(this);

		nmapRow = (TableRow) findViewById(R.id.NMapRow);
		nmapRow.setOnClickListener(this);

		sysConnectRow = (TableRow) findViewById(R.id.SysConnectRow);
		sysConnectRow.setOnClickListener(this);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.TerminalRow:
			// when the row entitled "Terminal" is clicked, take the user to the
			// terminal activity.
			Intent terminalIntent = new Intent(this, TerminalActivity.class);
			terminalIntent.putExtra("user", userTemp);
			// send userTemp (the current user)'s name along with the intent so
			// that the name can be displayed there as well
			startActivity(terminalIntent);
			break;
		case R.id.SysMonitorRow:
			// when the row entitled "SysMonitor" is clicked, take the user to
			// the
			// SysMonitor activity.
			Intent sysMonitorIntent = new Intent(this, SysMonitorActivity.class);
			startActivity(sysMonitorIntent);
			break;
		case R.id.SysLogRow:
			Intent sysLogIntent = new Intent(this, SysLogActivity.class);
			startActivity(sysLogIntent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, 2, 0, R.string.GetServerInfoOptionMenu).setIcon(
				android.R.drawable.ic_menu_info_details);
		menu.add(0, Menu.FIRST, 0, R.string.logout).setIcon(
				R.drawable.ic_lock_power_off);
		return result;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST:
			// Confirm logout for user
			new AlertDialog.Builder(this)
					.setTitle("Logout Confirmation")
					.setMessage(R.string.logoutConfirmationDialog)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									try {
										conn.close();
										Log.d("Connection", "connection closed");
									} catch (RemoteException e) {
										Log.d("Connection", e.toString());
									}
									Intent closeAllActivities = new Intent(
											CuraActivity.this,
											LoginScreenActivity.class);
									// just close everything
									closeAllActivities
											.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
									CuraActivity.this
											.startActivity(closeAllActivities);
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
			break;
		case 2:
			String finalResultForDialog = "";
			finalResultForDialog = getUname() + "\n"
					+ getString(R.string.uptimeText) + getUptime();
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.ServerInfoDialog);
			final TextView infoArea = new TextView(this);
			infoArea.setText(finalResultForDialog);
			alert.setView(infoArea);
			alert.setNegativeButton("Ok",
					new DialogInterface.OnClickListener() {
						// UPON CLICKING "CANCEL" IN THE DIALOG BOX (ALERT)
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					});
			alert.show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
