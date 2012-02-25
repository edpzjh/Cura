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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.cura.User;
import com.cura.Terminal.Terminal;

public class ConnectionService extends Service {
	User user;
	SSHConnection sshconnection;
	Terminal terminal;
	Intent i = new Intent();

	// turning the SSH connection into a Service that other activities can bind
	// to

	private final CommunicationInterface.Stub mBinder = new CommunicationInterface.Stub() {
		public synchronized String executeCommand(String command)
				throws RemoteException {
			String result = "";
			try {
				result = sshconnection.messageSender(command);
			} catch (Exception e) {
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

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		user = (User) intent.getParcelableExtra("user");
		String password = intent.getStringExtra("pass");
		user.setPassword(password);
		sshconnection = (SSHConnection) new SSHConnection().execute(user);
		try {
			i.setAction(sshconnection.get());
		} catch (Exception e) {
			Log.d("Connection", e.toString());
		}
		i.putExtra("user", user);
		sendBroadcast(i);

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