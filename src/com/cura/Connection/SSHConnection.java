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
 * Description: This is where the Connection Service is implemented. It's where we construct this service so that other 
 * activities can bind to it and..be connected via SSH to the server requested. We send it in an AsyncTask to establish that.
 */

import android.os.AsyncTask;
import android.util.Log;

import com.cura.User;
import com.cura.Terminal.Terminal;
import com.jcraft.jsch.JSchException;

public class SSHConnection extends AsyncTask<User, String, String> {

	private final String connected = "cura.connected";
	private final String notConnected = "cura.not.connected";
	String result;
	Terminal terminal;

	@Override
	protected String doInBackground(User... user) {
		// TODO Auto-generated method stub
		try {
			terminal = new Terminal(user[0]);
			result = connected;
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			Log.d("Connection", e.toString());
			result = notConnected;
		}
		return result;
	}
	@Override
	protected void onPostExecute(String result) {
		// TODO Auto-generated method stub
		Log.d("Connection", result);
		// just for checking in the LogCat
	}

	public synchronized String messageSender(String message) {
		return terminal.ExecuteCommand(message);
	}

	public boolean connected() {
		return terminal.connected();
	}

	public void closeConnection() {
		if (terminal != null)
			terminal.close();
	}
}