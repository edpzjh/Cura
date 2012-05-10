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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.cura.R;
import com.cura.User;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;

public class SysLogActivity extends Activity implements
		android.view.View.OnClickListener {

	private String menu1[] = new String[] { "Head", "Tail" };
	// GUI menu where "Head" and "Tail" appear
	private String menu2[] = new String[] { "errors.log", "kernel.log", "boot",
			"auth.log", "daemon.log", "dmesg.log", "crond.log", "user.log",
			"Xorg.0.log" };
	// GUI menu where the drop-down list of available log files appears
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
	private CommunicationInterface conn;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			conn = CommunicationInterface.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
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
		bindService(i, connection, Context.BIND_AUTO_CREATE);
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
		// all of the above sets up the shape of this activity
		syslogDir = new File("/sdcard/Cura/Syslog");
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
			// display logs in dialog
			getLogs(true);
			break;
		case R.id.sysLogSaveLogsButton:
			// save logs in file
			getLogs(false);
			break;
		case R.id.EnableLineNumber:
			// if this checkbox is checked, set the lineNumbers EditText to
			// enabled (so it can be used as a validator later on)
			lineNumbers.setEnabled(checkBox.isChecked());
			break;
		}
	}

	public void initSpinners() {
		// position Spinner
		position = (Spinner) findViewById(R.id.HeadTailSpinner);
		ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, menu1);
		// initialize the Position (Head/Tail) spinner (radio buttons)
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		position.setAdapter(adapter1);

		// Log File Spinner
		logFile = (Spinner) findViewById(R.id.LogFileSpinner);
		ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, menu2);
		// initialize the logFile spinner (drop-down list of all the log files)
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
				// when the "Get Logs" button is pressed
				command = "";
				pos = position.getSelectedItemPosition() == 0 ? "head" : "tail";
				// if the position in the spinner is 0 then the user has
				// selected
				// "Head", else the user has selected "Tail"
				file = menu2[logFile.getSelectedItemPosition()];
				// get the name of the log file that the user has selected
			}

			@Override
			protected Boolean doInBackground(String... params) {
				if (!checkBox.isChecked()) {
					// if the Enable Line Numbers is NOT checked, construct a
					// simple
					// command that does "either Head/Tail" plus "/var/log/"
					// plus
					// the name of the file from the drop-down list
					command = pos + " /var/log/" + file;
				} else {
					// if the Enable Line Numbers checkbox IS checked
					try {
						int i = Integer.parseInt(lineNumbers.getText()
								.toString());
						// number and then construct the same command as in the
						// case
						// above, only this time add the "-n int" to it to get a
						// specific number of lines output for you
						command = pos + " -n " + i + " /var/log/" + file;
					} catch (NumberFormatException e) {
						// if the line numbers checkbox is checked but no
						// numbers
						// have been entered in the textfield, display this
						Toast.makeText(SysLogActivity.this,
								R.string.SysLogLineNumberprompt,
								Toast.LENGTH_SHORT).show();
						return false;
					}

				}
				result = sendAndReceive(command);
				// fill the result string with the output of our command
				// (mentioned
				// above)
				return true;
			}

			@Override
			protected void onPostExecute(Boolean booleanResult) {
				super.onPostExecute(booleanResult);
				super.onPostExecute(booleanResult);
				loader.dismiss();
				if (booleanResult) {
					if (result.equalsIgnoreCase(""))
						// if it doesn't produce anything, display this
						result = getString(R.string.SysLogNoLogsFoundprompt);
					// else, take the user to the Dialog activity which displays
					// the
					// results of the result variable
					if (inDialog) {
						Intent res = new Intent(SysLogActivity.this,
								LogsDialog.class);
						// send the actual result with the intent
						res.putExtra("LogsResult", result);
						// start it
						startActivity(res);
					} else {
						if (!syslogDir.exists()) {
							syslogDir.mkdir();
						}
						try {
							if (!result
									.equalsIgnoreCase(getString(R.string.SysLogNoLogsFoundprompt))) {
								Date date = new Date();
								String dateString = date.getMonth()+"_"+date.getDay()+"_"+date.getHours()+"_"+date.getMinutes(); 
								String fileName = user.getUsername()
										+ "_"
										+ menu2[logFile
												.getSelectedItemPosition()]
										+ "_"+dateString+".txt";
								target = new FileWriter("/sdcard/Cura/SysLog/"
										+ fileName);
								target.append(result);
								target.flush();
								target.close();
								Toast.makeText(
										SysLogActivity.this,
										getString(R.string.logsSaved) + " \"/SysLog/"
												+ fileName + "\"",
										Toast.LENGTH_LONG).show();
							} else
								Toast.makeText(
										SysLogActivity.this,
										getString(R.string.SysLogNoLogsFoundprompt),
										Toast.LENGTH_LONG).show();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							Toast.makeText(SysLogActivity.this,
									R.string.logsNotSaved, Toast.LENGTH_LONG)
									.show();
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
		unbindService(connection);
	}
}
