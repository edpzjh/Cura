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
 * Description: This is just a User class and it's used to receive all of the user accounts' information and encapsulate 
 * them in an object.
 */

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
	private String username;
	private String domain;
	private int port;
	private String password;

	// The User class, used to store and send the user's information through
	// activities

	public User(String usern, String dom, int port) {
		username = usern;
		domain = dom;
		this.port = port;
	}

	public User(Parcel in) {
		// TODO Auto-generated constructor stub
		username = in.readString();
		domain = in.readString();
		port = in.readInt();
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public String getDomain() {
		return domain;
	}

	public int getPort() {
		return port;
	}

	public String getPassword() {
		return password;
	}

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(username);
		dest.writeString(domain);
		dest.writeInt(port);
	}

	public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
		public User createFromParcel(Parcel in) {
			return new User(in);
		}

		public User[] newArray(int size) {
			return new User[size];
		}
	};
}