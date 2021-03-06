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
package com.cura.Terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;

import android.util.Log;

import com.cura.User;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Terminal extends Thread {

	private JSch jsch;
	private Session session;
	private Channel channel;

	private String username;
	private String host;
	private String password;
	private int port;
	private StringWriter writer; 
	private InputStream in;
	private String result = "";
	int i = 0;
	

	public Terminal(User user) throws JSchException {
		// TODO Auto-generated method stub
		writer = new StringWriter();
		jsch = new JSch();
		username = user.getUsername();
		host = user.getDomain();
		this.password = user.getPassword();
		port = user.getPort();

		session = jsch.getSession(username, host, port);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		Log.i("Terminal", "connected");
		channel = session.openChannel("exec");

	}

	public synchronized String ExecuteCommand(String command) {
		
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			channel.connect();

			// get output from server
			in = channel.getInputStream();

			// convert output to string
			writer.getBuffer().setLength(0);
			IOUtils.copy(in, writer);
			result = writer.toString();
			
			System.gc();

		} catch (IOException i) {
			Log.d("terminal", i.toString());
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			Log.d("terminal", e.toString());
		}
		return result;
	}
	public void close(){
		session.disconnect();
		channel.disconnect();
	}
	public boolean connected(){
		return true;
	}
}
