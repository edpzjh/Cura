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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cura.CuraActivity;
import com.cura.LoginScreenActivity;
import com.cura.R;
import com.cura.ScreenCapture;
import com.cura.User;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;
import com.google.analytics.tracking.android.EasyTracker;

public class ServerStatsActivity extends Activity {

	private final int REFRESH = 1;
	private final int WAIT = 2;
	private final int SCREENCAPTURE = 3;
	private ProgressDialog loader;
	private String loader_message = "";

	private String hostnameResult, listeningIPResult, kernelversionResult,
			uptimeResult, lastbootResult, currentusersResult, nameOfUsersResult,
			loadaveragesResult, memoryoutputResult, filesystemsoutputResult,
			processstatusoutputResult;
	private String[] processIDs;
	private String processIDsingular;
	private String totalMem, freeMem, usedMem;

	private User userTemp;

	private CommunicationInterface conn;

	private TextView hostname, listeningIP, kernelVersion, uptime, lastBoot,
			currentUsers, loadAverages, memoryOutput, filesystemsOuput,
			processStatusOutput;

	private Button killProcessesButton;
	private NotificationManager mNotificationManager;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			Log.d("ConnectionService", "Connected");
			conn = CommunicationInterface.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName name) {
			conn = null;
			Toast.makeText(ServerStatsActivity.this, "Service Disconnected",
					Toast.LENGTH_LONG);
		}
	};

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
		getApplicationContext()
				.bindService(i, connection, Context.BIND_AUTO_CREATE);
	}

	public synchronized String sendAndReceive(String command) {
		try {
			String result = conn.executeCommand(command);
			return result;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.serverstats);
		this.setTitle(R.string.ServerStatsTitle);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userTemp = extras.getParcelable("user");
		}
		initView();
		doBindService();
		getStats();
		killProcessesButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						ServerStatsActivity.this);
				builder.setTitle("Pick a process");
				builder.setItems(processIDs, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						sendAndReceive("kill `pidof " + processIDs[item] + "`");
						getStats();
					}

				});
				builder.show();
			}
		});
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, REFRESH, 0, R.string.refresh).setIcon(
				R.drawable.ic_menu_refresh);
		menu.add(0, SCREENCAPTURE, 0, R.string.menuSnapshot).setIcon(
				android.R.drawable.ic_menu_camera);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case REFRESH:
			getStats();
			break;
		case SCREENCAPTURE:
			new AsyncTask<Void, Void, Boolean>() {
				String title = "Server-Stats_Snap_";

				@Override
				protected Boolean doInBackground(Void... params) {
					try {
						ScreenCapture sc = new ScreenCapture();
						Date date = new Date();
						String dateString = date.getMonth() + "_" + date.getDay() + "_"
								+ date.getHours() + "_" + date.getMinutes() + "_"
								+ date.getSeconds();
						title += dateString;
						sc.capture(
								getWindow().getDecorView().findViewById(android.R.id.content),
								title, getContentResolver());
					} catch (Exception ex) {
						return false;
					}
					return true;
				}

				@Override
				protected void onPostExecute(Boolean result) {
					if (result)
						Toast.makeText(ServerStatsActivity.this,
								title + " " + getString(R.string.screenCaptureSaved),
								Toast.LENGTH_LONG).show();
					super.onPostExecute(result);
				}
			}.execute();
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
				super.onPreExecute();
				loader_message = getString(R.string.loader_message);
				showDialog(WAIT);
				System.gc();
				hostname.setText("Hostname: ");
				listeningIP.setText("Listening IP: ");
				kernelVersion.setText("Kernel version: ");
				uptime.setText("Uptime: ");
				lastBoot.setText("Last boot: ");
				currentUsers.setText("Current users: ");
				loadAverages.setText("Load averages: ");
				filesystemsOuput.setText("");
				processStatusOutput.setText("");
			}

			@Override
			protected Void doInBackground(Void... params) {
				while (true) {
					if (conn != null) {
						listeningIPResult = userTemp.getDomain();
						kernelversionResult = sendAndReceive("uname -mrsv");
						uptimeResult = sendAndReceive("uptime | awk '{print $2 \"\t \" $3 \" \" $4 \" \" $5}'");
						lastbootResult = sendAndReceive("last reboot | head -1 | awk '{print $5 \" \" $6 \" \" $7 \" \" $8 \" \" $9 \" \" $10 \" \" $11}'");
						currentusersResult = sendAndReceive("who | awk '{print $1}' | uniq | wc -l | xargs /bin/echo -n");
						nameOfUsersResult = sendAndReceive("who | awk '{print $1}' | uniq | xargs /bin/echo -n");
						loadaveragesResult = sendAndReceive("uptime | awk '{print $10 \" \" $11 \" \" $12}'");
						memoryoutputResult = sendAndReceive("free | awk '{if (NR > 1) m = 4;else m = 3;l = $0;for (i = 1; i <= m; i++) {o[i] = index(l,$i) + length($i) - 1; l = substr(l,o[i] - 1)} for (i = 1; i <= m; i++) printf(\"%*s--\",o[i],$i);print \"\"}'");
						hostnameResult = sendAndReceive("hostname");
						filesystemsoutputResult = sendAndReceive("df -h");
						processstatusoutputResult = sendAndReceive("ps axo pid,user,pmem,pcpu,comm | { IFS= read -r header; echo \"$header\"; sort -k 3,3nr; } | head -7");
						processIDsingular = sendAndReceive("ps axo pid,user,pmem,pcpu,comm | { IFS= read -r header; sort -k 3,3nr; } | head -7 | awk '{print $5}'");
						return null;
					}
				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				loader.cancel();
				hostname.append(hostnameResult);
				listeningIP.append(listeningIPResult);
				kernelVersion.append(kernelversionResult);
				uptime.append(uptimeResult);
				lastBoot.append(lastbootResult);
				String usersResultsForAppending = currentusersResult + " ( "
						+ nameOfUsersResult + " )";
				currentUsers.append(usersResultsForAppending);
				loadAverages.append(loadaveragesResult);
				createChartLayout(memoryoutputResult);
				filesystemsOuput.append(filesystemsoutputResult);
				processStatusOutput.append(processstatusoutputResult);
				processIDs = processIDsingular.split("\n");
				Log.d("account id", "wselet post");
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
		filesystemsOuput = (TextView) findViewById(R.id.filesystemsoutput);
		processStatusOutput = (TextView) findViewById(R.id.processstatusoutput);
		killProcessesButton = (Button) findViewById(R.id.killprocessbutton);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getApplicationContext().unbindService(connection);
	}

	public void createChartLayout(String s) {
		String data[] = s.split("--");
		try {
			totalMem = String.format("%.2f GB",
					Double.parseDouble(data[4].replaceAll("\\s", "")) / (1024 * 1024));
			usedMem = String.format("%.2f GB",
					Double.parseDouble(data[5].replaceAll("\\s", "")) / (1024 * 1024));
			freeMem = String.format("%.2f GB",
					Double.parseDouble(data[6].replaceAll("\\s", "")) / (1024 * 1024));
			TextView tv = (TextView) findViewById(R.id.totalMem);
			tv.setText("Total: " + totalMem);
			tv = (TextView) findViewById(R.id.usedMem);
			tv.setText("Used: " + usedMem);
			tv = (TextView) findViewById(R.id.freeMem);
			tv.setText("Free: " + freeMem);
			LinearLayout memoryPieChartView = (LinearLayout) (findViewById(R.id.memoryPieChartView));
			memoryPieChartView.removeAllViews();
			memoryPieChartView.addView(new MemoryStatsPieChart().execute(this, data),
					new LayoutParams(300, 300));
			Log.d("account id", "wselet pie");
		} catch (Exception e) {
			TextView tv = (TextView) findViewById(R.id.totalMem);
			tv.setText("Total: " + totalMem);
			tv = (TextView) findViewById(R.id.usedMem);
			tv.setText("Used: " + usedMem);
			tv = (TextView) findViewById(R.id.freeMem);
			tv.setText("Free: " + freeMem);
		}
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
							Intent closeAllActivities = new Intent(ServerStatsActivity.this,
									LoginScreenActivity.class);
							closeAllActivities.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							ServerStatsActivity.this.startActivity(closeAllActivities);

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