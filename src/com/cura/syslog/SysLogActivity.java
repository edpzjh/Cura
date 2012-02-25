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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;

public class SysLogActivity extends Activity implements
		android.view.View.OnClickListener {
	private String menu1[] = new String[] { "Head", "Tail" };
	// GUI menu where "Head" and "Tail" appear
	private String menu2[] = new String[] { "alternatives.log", "auth.log",
			"boot.log", "daemon.log", "dpkg.log", "jockey.log", "kern.log",
			"user.log" };
	// GUI menu where the drop-down list of available log files appears
	private Spinner position, logFile;
	private CheckBox checkBox;
	private EditText lineNumbers;
	private Button sysLogButton;
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
		doBindService();
		initSpinners();
		sysLogButton = (Button) findViewById(R.id.sysLogButton);
		sysLogButton.setOnClickListener(this);
		checkBox = (CheckBox) findViewById(R.id.EnableLineNumber);
		checkBox.setOnClickListener(this);
		checkBox.setChecked(false);
		lineNumbers = (EditText) findViewById(R.id.LinesNumber);
		lineNumbers.setEnabled(false);
		// all of the above sets up the shape of this activity
	}

	public void onClick(View v) {
		String command;
		switch (v.getId()) {
		case R.id.sysLogButton:
			// when the "Get Logs" button is pressed
			command = "";
			String pos = position.getSelectedItemPosition() == 0 ? "head"
					: "tail";
			// if the position in the spinner is 0 then the user has selected
			// "Head", else the user has selected "Tail"
			String file = menu2[logFile.getSelectedItemPosition()];
			// get the name of the log file that the user has selected

			if (!checkBox.isChecked()) {
				// if the Enable Line Numbers is NOT checked, construct a simple
				// command that does "either Head/Tail" plus "/var/log/" plus
				// the name of the file from the drop-down list
				command = pos + " /var/log/" + file;
			} else {
				// if the Enable Line Numbers checkbox IS checked
				try {
					int i = Integer.parseInt(lineNumbers.getText().toString());
					// number and then construct the same command as in the case
					// above, only this time add the "-n int" to it to get a
					// specific number of lines output for you
					command = pos + " -n " + i + " /var/log/" + file;
				} catch (NumberFormatException e) {
					// if the line numbers checkbox is checked but no numbers
					// have been entered in the textfield, display this
					Toast.makeText(this, "Please enter a number",
							Toast.LENGTH_SHORT).show();
					return;
				}
			}
			String result = sendAndReceive(command);
			// fill the result string with the output of our command (mentioned
			// above)
			if (result.equalsIgnoreCase(""))
				// if it doesn't produce anything, display this
				result = "No logs in this file";
			// else, take the user to the Dialog activity which displays the
			// results of the result variable
			Intent res = new Intent(this, LogsDialog.class);
			// send the actual result with the intent
			res.putExtra("LogsResult", result);
			// start it
			startActivity(res);
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
}
