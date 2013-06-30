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

package com.cura.Connection;

/*
 * Description: This is where the Connection service functionality is constructed. Meaning that this is where we implement
 * functions like executeCommand() which is used to execute a command at the terminal.
 */

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cura.CuraActivity;
import com.cura.R;
import com.cura.User;
import com.cura.Terminal.Terminal;

public class ConnectionService extends Service {

 private User user;
 private SSHConnection sshconnection;
 private Terminal terminal;
 private Intent i = new Intent();
 private Handler mHandler = new Handler();
 private boolean run = true;
 private int usersNo = 0;
 private NotificationManager mNotificationManager;
 private Notification notification;
 private CharSequence contentTitle;
 private CharSequence contentText;
 private Context context;
 private int icon;
 private SharedPreferences prefs;
 private long timeInterval;

 private final CommunicationInterface.Stub mBinder = new CommunicationInterface.Stub() {
  public synchronized String executeCommand(String command) throws RemoteException {
   String result = "";
   try {
	result = sshconnection.messageSender(command);
   }
   catch (Exception e) {
	Log.d("ConnectionService", e.toString());
   }
   return result;
  }

  public void close() {
   sshconnection.closeConnection();
  }

  public boolean connected() {
   return terminal.connected();
  }
 };

 @Override
 public void onCreate() {
  super.onCreate();
 }

 @SuppressLint("NewApi")
 @Override
 public int onStartCommand(Intent intent, int flags, int startId) {
  super.onStartCommand(intent, flags, startId);
  user = (User) intent.getParcelableExtra("user");
  String password = intent.getStringExtra("pass");
  user.setPassword(password);
  sshconnection = (SSHConnection) new SSHConnection().execute(user);
  try {
   i.setAction(sshconnection.get());
  }
  catch (Exception e) {
   Log.d("Connection", e.toString());
  }
  i.putExtra("user", user);
  sendBroadcast(i);
  new Thread(new Runnable() {

   public void run() {
	prefs = PreferenceManager.getDefaultSharedPreferences(ConnectionService.this);
	timeInterval = Long.parseLong(prefs.getString("minutes", "0"));
	if(timeInterval != 0) {
	 try {
	  usersNo = Integer.parseInt(mBinder.executeCommand("who | awk '{print $1}' | uniq | wc -l | xargs /bin/echo -n"));

	 }
	 catch (Exception e) {
	  e.printStackTrace();
	 }
	 while(run) {
	  try {
	   Thread.sleep(3000);
	   if(run) {
		mHandler.post(new Runnable() {

		 public void run() {
		  try {
		   int users = usersNo;
		   try {
			users = Integer.parseInt(mBinder.executeCommand("who | awk '{print $1}' | uniq | wc -l | xargs /bin/echo -n"));
		   }
		   catch (NumberFormatException e) {
		   }
		   if(users > usersNo) {
			Log.d("Notification", "" + users);
			int newUsers = users - usersNo;
			usersNo = users;
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			icon = R.drawable.curalogo;
			CharSequence tickerText = "Login Notification";
			long when = System.currentTimeMillis();

			notification = new Notification(icon, tickerText, when);
			context = getApplicationContext();
			contentTitle = "Cura";
			String msg;
			if(newUsers == 1)
			 msg = "1 user has just logged in to " + user.getDomain();
			else
			 msg = newUsers + " users has just logged in to " + user.getDomain();
			contentText = msg;

			Intent notificationIntent = new Intent(ConnectionService.this, CuraActivity.class);
			notificationIntent.putExtra("user", user);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent contentIntent = PendingIntent.getActivity(ConnectionService.this, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			notification.defaults = Notification.DEFAULT_SOUND;
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			mNotificationManager.notify(1, notification);
			System.gc();
		   }
		   else
			usersNo = users;
		  }
		  catch (RemoteException e) {
		   e.printStackTrace();
		  }
		 }
		});
	   }
	  }
	  catch (Exception e) {
	  }
	 }
	}
   }
  }).start();
  return START_STICKY;
 }

 @Override
 public void onDestroy() {
  super.onDestroy();
  try {
   run = false;
   mBinder.close();
  }
  catch (RemoteException e) {
   e.printStackTrace();
  }
  sshconnection = null;

  Log.d("Connection Service", "connection stopped ");
 }

 @Override
 public IBinder onBind(Intent intent) {
  return mBinder;
 }

 @Override
 public boolean onUnbind(Intent intent) {
  return super.onUnbind(intent);
 }
}