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

package com.cura.syslog;

/*
 * Description: This is the actual SysLog module Activity. Here is where the user can choose from a drop-down list the file
 * that they choose to view from the list of files that SysLog usually dumps to on any Linux machine. After choosing that, 
 * the user can then choose whether to Tail (last 10 lines) or Head (first 10 lines) that file. Added to which, they can 
 * choose to Tail or Head that file according to a user-specified number (e.g. the first 45 lines, the last 20 lines).
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.cura.LoginScreenActivity;
import com.cura.R;
import com.cura.User;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;
import com.google.analytics.tracking.android.EasyTracker;

public class SysLogActivity extends Activity implements
		android.view.View.OnClickListener {

	private String menu1[] = new String[] { "Head", "Tail" };
	private String menu2[] = new String[] { "errors.log", "kernel.log", "boot",
			"auth.log", "daemon.log", "dmesg.log", "crond.log", "user.log",
			"Xorg.0.log" };
	private Spinner position, logFile;
	private CheckBox checkBox;
	private EditText lineNumbers;
	private Button sysLogButton;
	private Button sysLogSaveButton;
	private static final int WAIT = 100;
	private String loader_message = "";
	private ProgressDialog loader;
	private User user;
	private File syslogDir;
	private FileWriter target;
	private NotificationManager mNotificationManager;
	private CommunicationInterface conn;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			conn = CommunicationInterface.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName name) {
			conn = null;
		}
	};

	public String sendAndReceive(String command) {
		String result = "";

		try {
			result = conn.executeCommand(command);
		} catch (RemoteException e) {
			Log.d("SysLog", e.toString());
		}
		return result;
	}

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
		getApplicationContext()
				.bindService(i, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.syslog);
		Bundle extras = getIntent().getExtras();
		if (extras != null)
			user = (User) extras.get("user");
		doBindService();
		initSpinners();
		sysLogButton = (Button) findViewById(R.id.sysLogButton);
		sysLogButton.setOnClickListener(this);
		sysLogSaveButton = (Button) findViewById(R.id.sysLogSaveLogsButton);
		sysLogSaveButton.setOnClickListener(this);
		checkBox = (CheckBox) findViewById(R.id.EnableLineNumber);
		checkBox.setOnClickListener(this);
		checkBox.setChecked(false);
		lineNumbers = (EditText) findViewById(R.id.LinesNumber);
		lineNumbers.setEnabled(false);
		syslogDir = new File("/sdcard/Cura/Syslog");
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case WAIT:
			loader = new ProgressDialog(this);
			loader.setMessage(loader_message);
			loader.setCancelable(false);
			loader.show();
			break;
		}
		return super.onCreateDialog(id);
	}

	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.sysLogButton:
			getLogs(true);
			break;
		case R.id.sysLogSaveLogsButton:
			getLogs(false);
			break;
		case R.id.EnableLineNumber:
			lineNumbers.setEnabled(checkBox.isChecked());
			break;
		}
	}

	public void initSpinners() {
		position = (Spinner) findViewById(R.id.HeadTailSpinner);
		ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, menu1);
		adapter1
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		position.setAdapter(adapter1);

		logFile = (Spinner) findViewById(R.id.LogFileSpinner);
		ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, menu2);
		adapter2
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		logFile.setAdapter(adapter2);
	}

	public void getLogs(final boolean inDialog) {
		new AsyncTask<String, String, Boolean>() {
			String command;
			String pos;
			String file;
			String result;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				if (inDialog)
					loader_message = getString(R.string.fetchingLogs);
				else
					loader_message = getString(R.string.savingLogs);
				showDialog(WAIT);
				command = "";
				pos = position.getSelectedItemPosition() == 0 ? "head" : "tail";
				file = menu2[logFile.getSelectedItemPosition()];
			}

			@Override
			protected Boolean doInBackground(String... params) {
				if (!checkBox.isChecked()) {
					command = pos + " /var/log/" + file;
				} else {
					try {
						int i = Integer.parseInt(lineNumbers.getText().toString());
						command = pos + " -n " + i + " /var/log/" + file;
					} catch (NumberFormatException e) {
						Toast.makeText(SysLogActivity.this,
								R.string.SysLogLineNumberprompt, Toast.LENGTH_SHORT).show();
						return false;
					}

				}
				result = sendAndReceive(command);
				return true;
			}

			@Override
			protected void onPostExecute(Boolean booleanResult) {
				super.onPostExecute(booleanResult);
				super.onPostExecute(booleanResult);
				loader.dismiss();
				if (booleanResult) {
					if (result.equalsIgnoreCase(""))
						result = getString(R.string.SysLogNoLogsFoundprompt);
					if (inDialog) {
						Intent res = new Intent(SysLogActivity.this, LogsDialog.class);
						res.putExtra("LogsResult", result);
						startActivity(res);
					} else {
						if (!syslogDir.exists()) {
							syslogDir.mkdir();
						}
						try {
							if (!result
									.equalsIgnoreCase(getString(R.string.SysLogNoLogsFoundprompt))) {
								Date date = new Date();
								String dateString = date.getMonth() + "_" + date.getDay() + "_"
										+ date.getHours() + "_" + date.getMinutes();
								String fileName = user.getUsername() + "_"
										+ menu2[logFile.getSelectedItemPosition()] + "_"
										+ dateString + ".txt";
								target = new FileWriter("/sdcard/Cura/SysLog/" + fileName);
								target.append(result);
								target.flush();
								target.close();
								Toast.makeText(
										SysLogActivity.this,
										getString(R.string.logsSaved) + " \"/SysLog/" + fileName
												+ "\"", Toast.LENGTH_LONG).show();
							} else
								Toast.makeText(SysLogActivity.this,
										getString(R.string.SysLogNoLogsFoundprompt),
										Toast.LENGTH_LONG).show();
						} catch (IOException e) {
							Toast.makeText(SysLogActivity.this, R.string.logsNotSaved,
									Toast.LENGTH_LONG).show();
						}
					}
				} else
					Toast.makeText(SysLogActivity.this, R.string.logsNotSaved,
							Toast.LENGTH_LONG).show();
			}
		}.execute();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getApplicationContext().unbindService(connection);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			new AlertDialog.Builder(this).setTitle("Logout Confirmation")
					.setMessage(R.string.logoutConfirmationDialog)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							try {
								Log.d("Connection", "connection closed");
							} catch (Exception e) {
								Log.d("Connection", e.toString());
							}
							Intent closeAllActivities = new Intent(SysLogActivity.this,
									LoginScreenActivity.class);
							closeAllActivities.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							SysLogActivity.this.startActivity(closeAllActivities);

							mNotificationManager.cancelAll();
						}
					}).setNegativeButton("No", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).show();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
}
