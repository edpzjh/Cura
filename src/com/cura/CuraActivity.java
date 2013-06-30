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

public class CuraActivity extends TabActivity implements OnClickListener, OnTouchListener {

 private final int LOGOUT = 0;
 private final int SERVER_INFO = 2;
 private final int WAIT = 100;
 TableRow terminalRow, sysMonitorRow, sysLogRow, nessusRow, nmapRow, serverStatsRow;

 User userTemp;
 private String uname = "";
 private String uptime = "";
 private String location = "";
 private String loader_message = "";
 private String finalResultForDialog = "";
 private CommunicationInterface conn;
 private ProgressDialog loader;
 private NotificationManager mNotificationManager;
 boolean connectionTrigger = true;
 private AlertDialog alert;

 private ServiceConnection connection = new ServiceConnection() {
  public void onServiceConnected(ComponentName arg0, IBinder service) {
   conn = CommunicationInterface.Stub.asInterface(service);
  }

  public void onServiceDisconnected(ComponentName name) {
   conn = null;
   Toast.makeText(CuraActivity.this, "Service Disconnected", Toast.LENGTH_LONG).show();
  }
 };

 public synchronized String getUname() {
  String resultUNAME = "";
  try {
   resultUNAME = conn.executeCommand("uname -a");
  }
  catch (RemoteException e) {
   e.printStackTrace();
  }
  return resultUNAME;
 }

 public synchronized String getUptime() {
  String resultUPTIME = "";
  try {
   resultUPTIME = conn.executeCommand("uptime");
  }
  catch (RemoteException e) {
   e.printStackTrace();
  }
  return resultUPTIME;
 }

 public synchronized String getLocation() {
  String resultLocation = "";
  try {
   resultLocation = conn.executeCommand("geoiplookup " + userTemp.getDomain() + " | awk '{print $4, $5}'");
  }
  catch (RemoteException e) {
   e.printStackTrace();
  }
  return resultLocation;
 }

 public synchronized String getHostname() {
  String resultHostname = "";
  try {
   resultHostname = conn.executeCommand("hostname");
  }
  catch (RemoteException e) {
   e.printStackTrace();
  }
  return resultHostname;
 }

 public void doBindService() {
  Intent i = new Intent(this, ConnectionService.class);
  bindService(i, connection, Context.BIND_AUTO_CREATE);
 }

 @Override
 public void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.main);
  Log.d("Cura Activity", "finished");
  doBindService();
  Bundle extras = getIntent().getExtras();
  if(extras != null) {
   userTemp = extras.getParcelable("user");
  }

  this.setTitle(userTemp.getUsername() + "'s Control Box");

  TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);

  TabSpec serverstats = tabHost.newTabSpec("Server Stats");
  serverstats.setIndicator("", getResources().getDrawable(R.drawable.serverstats_tab_selector));
  Intent photosIntent = new Intent(this, ServerStatsActivity.class);
  photosIntent.putExtra("user", userTemp);
  serverstats.setContent(photosIntent);
  tabHost.addTab(serverstats);

  TabSpec sysLogSpec = tabHost.newTabSpec("SysLog");
  sysLogSpec.setIndicator("", getResources().getDrawable(R.drawable.syslog_tab_selector));
  Intent sysLogIntent = new Intent(this, SysLogActivity.class);
  sysLogIntent.putExtra("user", userTemp);
  sysLogSpec.setContent(sysLogIntent);
  tabHost.addTab(sysLogSpec);

  TabSpec sysMonitorSpec = tabHost.newTabSpec("sysMonitor");
  sysMonitorSpec.setIndicator("", getResources().getDrawable(R.drawable.sysmonitor_tab_selector));
  Intent sysMonitorIntent = new Intent(this, SysMonitorActivity.class);
  sysMonitorIntent.putExtra("user", userTemp);
  sysMonitorSpec.setContent(sysMonitorIntent);
  tabHost.addTab(sysMonitorSpec);

  TabSpec NmapSpec = tabHost.newTabSpec("Nmap");
  NmapSpec.setIndicator("", getResources().getDrawable(R.drawable.nmap_tab_selector));
  Intent nmapIntent = new Intent(this, NmapActivity.class);
  nmapIntent.putExtra("user", userTemp);
  NmapSpec.setContent(nmapIntent);
  tabHost.addTab(NmapSpec);

  TabSpec TerminalSpec = tabHost.newTabSpec("Terminal");
  TerminalSpec.setIndicator("", getResources().getDrawable(R.drawable.terminal_tab_selector));
  Intent terminalIntent = new Intent(this, TerminalActivity.class);
  terminalIntent.putExtra("user", userTemp);
  TerminalSpec.setContent(terminalIntent);
  tabHost.addTab(TerminalSpec);

  mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

 @Override
 public boolean onCreateOptionsMenu(Menu menu) {
  boolean result = super.onCreateOptionsMenu(menu);
  menu.add(0, LOGOUT, 10, R.string.logout).setIcon(R.drawable.ic_lock_power_off);
  return result;
 }

 public boolean onOptionsItemSelected(MenuItem item) {
  switch (item.getItemId()) {
  case LOGOUT:
   new AlertDialog.Builder(this).setTitle("Logout Confirmation").setMessage(R.string.logoutConfirmationDialog)
	 .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

	  public void onClick(DialogInterface dialog, int which) {
	   try {
		Log.d("Connection", "connection closed");
	   }
	   catch (Exception e) {
		Log.d("Connection", e.toString());
	   }
	   Intent closeAllActivities = new Intent(CuraActivity.this, LoginScreenActivity.class);
	   closeAllActivities.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	   CuraActivity.this.startActivity(closeAllActivities);
	   mNotificationManager.cancelAll();
	  }
	 }).setNegativeButton("No", new DialogInterface.OnClickListener() {
	  public void onClick(DialogInterface dialog, int which) {
	   dialog.dismiss();
	  }
	 }).show();
   break;
  }
  return super.onOptionsItemSelected(item);
 }

 @Override
 protected void onResume() {
  super.onResume();
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
  return super.onKeyDown(keyCode, event);
 }

 public boolean onTouch(View v, MotionEvent event) {
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
  new AsyncTask<String, String, String>() {

   @Override
   protected void onPreExecute() {
	super.onPreExecute();
	loader_message = getString(R.string.initializing);
   }

   @Override
   protected String doInBackground(String... arg0) {
	while(connectionTrigger) {
	 if(conn != null) {
	  uptime = getUptime();
	  uname = getUname();
	  location = getLocation();
	  connectionTrigger = false;
	 }
	}
	return null;
   }

   @Override
   protected void onPostExecute(String result) {
	super.onPostExecute(result);
   }
  }.execute();
 }

 @Override
 public void onClick(View arg0) {
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
