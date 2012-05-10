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

package com.cura.ServerStats;

/*
 * Description: Server Stats are general server information like its Vitals, Mounted Filesystems, Memory information,
 * Process Status and so on. The user will be able to refresh these stats while in the activity by going to Menu > Refresh. 
 */

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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.cura.R;
import com.cura.User;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;

public class ServerStatsActivity extends Activity {

	private final int REFRESH = 1;
	// the refresh interval
	private final int WAIT = 2;
	// the waiting interval

	private ProgressDialog loader;
	// appears upon creating the activity
	private String loader_message = "";

	private String hostnameResult, listeningIPResult, kernelversionResult,
			uptimeResult, lastbootResult, currentusersResult,
			loadaveragesResult, memoryoutputResult, filesystemsoutputResult,
			processstatusoutputResult;

	private User userTemp;

	private CommunicationInterface conn;

	TextView hostname, listeningIP, kernelVersion, distribution, uptime,
			lastBoot, currentUsers, loadAverages, memoryOutput,
			filesystemsOuput, processStatusOutput;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			Log.d("ConnectionService", "Connected");
			conn = CommunicationInterface.Stub.asInterface(service);
			// bind to the Connection Service
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			conn = null;
			// when the service is disconnected, null the conn
			// ServiceConnection and display an error message
			Toast.makeText(ServerStatsActivity.this, "Service Disconnected",
					Toast.LENGTH_LONG);
		}
	};

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
		// connect to the SSH service (Connection)
		bindService(i, connection, Context.BIND_AUTO_CREATE);
	}

	public synchronized String sendAndReceive(String command) {

		try {
			String result = conn.executeCommand(command);
			return result;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.serverstats);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userTemp = extras.getParcelable("user");
			// gets the username on entry
		}
		initView();
		// initialize the view
		doBindService();
		// bind to the Connection service
		getStats();
		// get the stats
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, REFRESH, 0, R.string.refresh).setIcon(
				R.drawable.ic_menu_rotate);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case REFRESH:
			getStats();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case WAIT:
			loader = new ProgressDialog(this);
			loader.setMessage(loader_message);
			loader.setCancelable(false);
			return loader;
		}
		return super.onCreateDialog(id);
	}

	protected void getStats() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				loader_message = getString(R.string.loader_message);
				showDialog(WAIT);
				hostname.setText("Hostname: ");
				listeningIP.setText("Listening IP: ");
				kernelVersion.setText("Kernel version: ");
				uptime.setText("Uptime: ");
				lastBoot.setText("Last boot: ");
				currentUsers.setText("Current users: ");
				loadAverages.setText("Load averages: ");
//				memoryOutput.setText("");
				filesystemsOuput.setText("");
				processStatusOutput.setText("");
				super.onPreExecute();
			}

			@Override
			protected Void doInBackground(Void... params) {
				while (true) {
					if (conn != null) {
						hostnameResult = sendAndReceive("hostname");
						listeningIPResult = userTemp.getDomain();
						kernelversionResult = sendAndReceive("uname -mrsv");
						uptimeResult = sendAndReceive("uptime | awk '{print $2 \"\t \" $3 \" \" $4 \" \" $5}'");
						lastbootResult = sendAndReceive("last reboot | head -1 | awk '{print $5 \" \" $6 \" \" $7 \" \" $8 \" \" $9 \" \" $10 \" \" $11}'");
						currentusersResult = sendAndReceive("who | wc -l");
						loadaveragesResult = sendAndReceive("uptime | awk '{print $10 \" \" $11 \" \" $12}'");
						memoryoutputResult = sendAndReceive("free | awk '{if (NR > 1) m = 4;else m = 3;l = $0;for (i = 1; i <= m; i++) {o[i] = index(l,$i) + length($i) - 1; l = substr(l,o[i] - 1)} for (i = 1; i <= m; i++) printf(\"%*s--\",o[i],$i);print \"\"}'");
						//"free | awk '{if (NR > 1) m = 4;else m = 3;l = $0;for (i = 1; i <= m; i++) {o[i] = index(l,$i) + length($i) - 1; l = substr(l,o[i] - 1)} for (i = 1; i <= m; i++) printf(\"%*s--\",o[i],$i);print \"\"}'");
						filesystemsoutputResult = sendAndReceive("df -h");
						processstatusoutputResult = sendAndReceive("ps axo pid,user,pmem,pcpu,comm | { IFS= read -r header; echo \"$header\"; sort -k 3,3nr; } | head -7");
						return null;
					}
				}
			}

			@Override
			protected void onPostExecute(Void result) {
				loader.cancel();
				// finish loading
				hostname.append(hostnameResult);
				listeningIP.append(listeningIPResult);
				kernelVersion.append(kernelversionResult);
				uptime.append(uptimeResult);
				lastBoot.append(lastbootResult);
				currentUsers.append(currentusersResult);
				loadAverages.append(loadaveragesResult);
				//memoryOutput.append(memoryoutputResult);
				createTableLayout(memoryoutputResult);
				filesystemsOuput.append(filesystemsoutputResult);
				processStatusOutput.append(processstatusoutputResult);
				super.onPostExecute(result);
			}
		}.execute();
	}

	protected void initView() {
		hostname = (TextView) findViewById(R.id.hostname);
		listeningIP = (TextView) findViewById(R.id.listeningip);
		kernelVersion = (TextView) findViewById(R.id.kernelversion);
		uptime = (TextView) findViewById(R.id.uptime);
		lastBoot = (TextView) findViewById(R.id.lastboot);
		currentUsers = (TextView) findViewById(R.id.currentusers);
		loadAverages = (TextView) findViewById(R.id.loadaverages);
		memoryOutput = (TextView) findViewById(R.id.memoryoutput);
		filesystemsOuput = (TextView) findViewById(R.id.filesystemsoutput);
		processStatusOutput = (TextView) findViewById(R.id.processstatusoutput);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}
	
	public void createTableLayout(String s){
		String data[] = s.split("--");
		TextView tv = (TextView) findViewById(R.id.totalMem);
		tv.setText("Total: "+data[4].replaceAll("\\s", ""));
		tv = (TextView) findViewById(R.id.usedMem);
		tv.setText("Used: "+data[5].replaceAll("\\s", ""));
		tv = (TextView) findViewById(R.id.freeMem);
		tv.setText("Free: "+data[6].replaceAll("\\s", ""));
	}
}
