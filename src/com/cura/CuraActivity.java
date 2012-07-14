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
 * Description: This is the activity that the user gets dropped to after they have logged in successfully and this is where
 * all of Cura's facilities are shown (Terminal, SysLog, SysMonitor, etc...). The options provided in this activity are
 * Server Info and Logout. 
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.TabActivity;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;
import com.cura.ServerStats.ServerStatsActivity;
import com.cura.Terminal.TerminalActivity;
import com.cura.nmap.NmapActivity;
import com.cura.syslog.SysLogActivity;
import com.cura.sysmonitor.SysMonitorActivity;

public class CuraActivity extends TabActivity implements OnClickListener,
		OnTouchListener {

	private final int LOGOUT = 0;
	private final int SERVER_INFO = 2;
	private final int WAIT = 100;
	TableRow terminalRow, sysMonitorRow, sysLogRow, nessusRow, nmapRow,
			serverStatsRow;
	// menu buttons

	User userTemp;
	// user object
	private String uname = "";
	private String uptime = "";
	private String location = "";
	private String loader_message = "";
	private String finalResultForDialog = "";
	private CommunicationInterface conn;
	// private String page = "";
	private ProgressDialog loader;
	// to fetch the GET request of the server's location
	private NotificationManager mNotificationManager;
	boolean connectionTrigger = true;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			// TODO Auto-generated method stub
			conn = CommunicationInterface.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			conn = null;
			Toast.makeText(CuraActivity.this, "Service Disconnected",
					Toast.LENGTH_LONG).show();
		}
	};

	public synchronized String getUname() {
		String resultUNAME = "";
		// produces the result of "uname -a"
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
		// produces the output of "uptime"
		try {
			resultUPTIME = conn.executeCommand("uptime");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultUPTIME;
	}

	public synchronized String getLocation() {
		String resultLocation = "";
		// produces the output of "geoiplookup domain" and prints the 4th and
		// 5th column from that. This produces the locale of a given domain name
		try {
			resultLocation = conn.executeCommand("geoiplookup "
					+ userTemp.getDomain() + " | awk '{print $4, $5}'");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultLocation;
	}

	public synchronized String getHostname() {
		String resultHostname = "";
		try {
			resultHostname = conn.executeCommand("hostname");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultHostname;
	}

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
		bindService(i, connection, Context.BIND_AUTO_CREATE);
		// function needed for binding to the Connection service
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d("Cura Activity", "finished");
		doBindService();
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userTemp = extras.getParcelable("user");
			// gets the username on entry
		}

		this.setTitle(userTemp.getUsername() + "'s Control Box");
		// welcoming the user in this activity's title.

		TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
		
		TabSpec sysLogSpec = tabHost.newTabSpec("SysLog");
		// setting Title and Icon for the Tab
		sysLogSpec.setIndicator("SysLog", getResources().getDrawable(R.drawable.syslog_tab_selector));
		Intent sysLogIntent = new Intent(this, SysLogActivity.class);
		sysLogIntent.putExtra("user", userTemp);
		sysLogSpec.setContent(sysLogIntent);
		tabHost.addTab(sysLogSpec);
		
		// Tab for Photos
        TabSpec serverstats = tabHost.newTabSpec("Server Stats");
        // setting Title and Icon for the Tab
        serverstats.setIndicator("Stats", getResources().getDrawable(R.drawable.serverstats_tab_selector));
        Intent photosIntent = new Intent(this, ServerStatsActivity.class);
        photosIntent.putExtra("user", userTemp);
        serverstats.setContent(photosIntent);
        tabHost.addTab(serverstats);
        
        
        TabSpec sysMonitorSpec = tabHost.newTabSpec("sysMonitor");
        // setting Title and Icon for the Tab
        sysMonitorSpec.setIndicator("SysMonitor", getResources().getDrawable(R.drawable.sysmonitor_tab_selector));
        Intent sysMonitorIntent = new Intent(this, SysMonitorActivity.class);
        sysMonitorIntent.putExtra("user", userTemp);
        sysMonitorSpec.setContent(sysMonitorIntent);
        tabHost.addTab(sysMonitorSpec);
        
        TabSpec NmapSpec = tabHost.newTabSpec("Nmap");
        // setting Title and Icon for the Tab
        NmapSpec.setIndicator("Nmap", getResources().getDrawable(R.drawable.nmap_tab_selector));
        Intent nmapIntent = new Intent(this, NmapActivity.class);
        nmapIntent.putExtra("user", userTemp);
        NmapSpec.setContent(nmapIntent);
        tabHost.addTab(NmapSpec);
        
        TabSpec TerminalSpec = tabHost.newTabSpec("Terminal");
        // setting Title and Icon for the Tab
        TerminalSpec.setIndicator("Terminal", getResources().getDrawable(R.drawable.terminal_tab_selector));
        Intent terminalIntent = new Intent(this, TerminalActivity.class);
        terminalIntent.putExtra("user", userTemp);
        TerminalSpec.setContent(terminalIntent);
        tabHost.addTab(TerminalSpec);
		
		// SETTING CLICK-LISTENERS FOR ALL OF THE BUTTONS
//		terminalRow = (TableRow) findViewById(R.id.TerminalRow);
//		terminalRow.setOnClickListener(this);
//		terminalRow.setOnTouchListener(this);
//
//		sysMonitorRow = (TableRow) findViewById(R.id.SysMonitorRow);
//		sysMonitorRow.setOnClickListener(this);
//		sysMonitorRow.setOnTouchListener(this);
//
//		sysLogRow = (TableRow) findViewById(R.id.SysLogRow);
//		sysLogRow.setOnClickListener(this);
//		sysLogRow.setOnTouchListener(this);
//
//		nmapRow = (TableRow) findViewById(R.id.NMapRow);
//		nmapRow.setOnClickListener(this);
//		nmapRow.setOnTouchListener(this);
//
//		serverStatsRow = (TableRow) findViewById(R.id.ServerStatsRow);
//		serverStatsRow.setOnClickListener(this);
//		serverStatsRow.setOnTouchListener(this);

		// get Server info after service binding
		initServerInfo();
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case WAIT:
			loader = new ProgressDialog(this);
			loader.setMessage(loader_message);
			loader.setCancelable(false);
			// this shows up when the user is logged in and at the main screen;
			// it is used to make the user wait a second while we fetch
			// information that appears in Menu > Server Info
			return loader;
		}
		return super.onCreateDialog(id);
	}

//	public void onClick(View v) {
//		switch (v.getId()) {
//		case R.id.TerminalRow:
//			// when the row entitled "Terminal" is clicked, take the user to the
//			// terminal activity.
//			// terminalRow.setBackgroundColor(R.drawable.moduleselectedhighlight);
//			Intent terminalIntent = new Intent(this, TerminalActivity.class);
//			terminalIntent.putExtra("user", userTemp);
//
//			// send userTemp (the current user)'s name along with the intent so
//			// that the name can be displayed there as well
//			startActivity(terminalIntent);
//			break;
//		case R.id.SysMonitorRow:
//			// when the row entitled "SysMonitor" is clicked, take the user the
//			// SysMonitor activity.
//			Intent sysMonitorIntent = new Intent(this, SysMonitorActivity.class);
//			startActivity(sysMonitorIntent);
//			break;
//		case R.id.SysLogRow:
//			// when the row entitled "SysLog" is clicked, take the user to
//			// the SysLog activity
//			Intent sysLogIntent = new Intent(this, SysLogActivity.class);
//			sysLogIntent.putExtra("user", userTemp);
//			startActivity(sysLogIntent);
//			break;
//		case R.id.NMapRow:
//			// when the row entitled "Nmap" is clicked, take the user
//			// to the Nmap activity
//			// if ((userTemp.getUsername()).compareTo("root") == 0) {
//			// if the user is root, allow them to access this activity
//			// if they are not, don't allow them
//			Intent nmapIntent = new Intent(this, NmapActivity.class);
//			nmapIntent.putExtra("user", userTemp);
//			startActivity(nmapIntent);
//			// } else {
//			// Toast.makeText(
//			// CuraActivity.this,
//			// "Error! You are not allowed to access the Nmap module if you do not have root privileges over this server.",
//			// Toast.LENGTH_LONG).show();
//			// }
//			break;
//		case R.id.ServerStatsRow:
//			// when the row entitled "Server Stats" is clicked, take the user
//			// the Server Stats activity.
//			Intent serverStatsIntent = new Intent(this,
//					ServerStatsActivity.class);
//			serverStatsIntent.putExtra("user", userTemp);
//			startActivity(serverStatsIntent);
//			break;
//		}
//	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, SERVER_INFO, 0, R.string.GetServerInfoOptionMenu).setIcon(
				android.R.drawable.ic_menu_info_details);
		// creates the options menu that includes "Server Info" and "Logout"
		menu.add(0, LOGOUT, 0, R.string.logout).setIcon(
				R.drawable.ic_lock_power_off);
		return result;
	}

	// actions for the above menu list
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case LOGOUT:
			// Confirm logout for user
			new AlertDialog.Builder(this)
					.setTitle("Logout Confirmation")
					.setMessage(R.string.logoutConfirmationDialog)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									try {
										// close the connection
										// conn.close();
										Log.d("Connection", "connection closed");
									} catch (Exception e) {
										Log.d("Connection", e.toString());
									}
									Intent closeAllActivities = new Intent(
											CuraActivity.this,
											LoginScreenActivity.class);
									// return the user to the login screen
									// activity just close everything
									closeAllActivities
											.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
									CuraActivity.this
											.startActivity(closeAllActivities);
									mNotificationManager.cancelAll();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								// if No is chosen, don't do anything and
								// dismiss the dialog
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
			break;
		case SERVER_INFO:
			// if "Server info" is selected, produce the output of Uptime and
			// Uname, concatenate them into a paragraph and display it for the
			// user
			if (location.equalsIgnoreCase("")) {
				location = getString(R.string.unableToGetLocation);
			}
			finalResultForDialog = uname + "\n"
					+ getString(R.string.uptimeText) + uptime + "\n"
					+ getString(R.string.userLocation) + location;
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			// build the above information into a dialog and display it
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

	@Override
	protected void onResume() {
		super.onResume();
		//doBindService();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopService(new Intent(CuraActivity.this, ConnectionService.class));
		unbindService(connection);
		Log.d("CuraActivity", "Stop");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			// if the back button is pressed when the user is in this (Cura
			// Activity)
			new AlertDialog.Builder(this).setTitle("Logout Confirmation")
					// confirm logout
					.setMessage(R.string.logoutConfirmationDialog)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									try {
										// close connection
										// conn.close();
										Log.d("Connection", "connection closed");
									} catch (Exception e) {
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
								
									mNotificationManager.cancelAll();
									// stopService(new Intent(CuraActivity.this,
									// ConnectionService.class));
								}
							}).setNegativeButton("No",
					// if No is selected, dismiss the dialog
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
		}
		return super.onKeyDown(keyCode, event);
	}

	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		// this is for when the user touches the row of one of the modules; this
		// is for highlighting that row and removing the highlighting when the
		// user touches away from it
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			v.setBackgroundResource(R.drawable.moduleselectedhighlight);
			break;
		case MotionEvent.ACTION_UP:
			v.setBackgroundResource(0);
			break;
		case MotionEvent.ACTION_CANCEL:
			v.setBackgroundResource(0);
			break;
		}

		return false;
	}

	public void initServerInfo() {
		// This asyncTask is for the server info that are going to be fetched
		// upon having the user log in and arrive at the main screen
		new AsyncTask<String, String, String>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				loader_message = getString(R.string.initializing);
				showDialog(WAIT);
			}

			@Override
			protected String doInBackground(String... arg0) {
				while (connectionTrigger) {
					if (conn != null) {
						uptime = getUptime();
						uname = getUname();
						location = getLocation();
						
						// just a safe-hold value
						connectionTrigger = false;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				loader.dismiss();
			}
		}.execute();
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}
	
	private View prepareTabView(String text, int resId) {
	    View view = LayoutInflater.from(this).inflate(R.layout.tablayout, null);
	    ImageView iv = (ImageView) view.findViewById(R.id.TabImageView);
	    TextView tv = (TextView) view.findViewById(R.id.TabTextView);
	    iv.setImageResource(resId);
	    tv.setText(text);
	    return view;
	}
}