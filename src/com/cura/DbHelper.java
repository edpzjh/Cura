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
 * Description: This class is used to access the database and write to it the information about user accounts and their
 * preferences plus the Favorite commands from the Terminal.
 */

import org.jasypt.util.password.BasicPasswordEncryptor;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class DbHelper extends SQLiteOpenHelper {
	//private static final String TAG = DbHelper.class.getSimpleName();
	public static final String databaseName = "userInfo.db";
	public static final int version = 1;
	public static final String userTableName = "user";
	public static final String commandTableName = "commandTable";
	public static final String C_USERNAME = "username";
	public static final String C_DOMAIN = "domain";
	public static final String C_PORT = "port";
	public static final String C_COMMAND = "command";
	private SharedPreferences prefs;
	private BasicPasswordEncryptor passEncryptor ;
	Context context;

	public DbHelper(Context context) {
		super(context, databaseName, null, version);
		this.context = context;
		passEncryptor = new BasicPasswordEncryptor();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String Create_User_Table = String.format(
				"create table %s (%s TEXT, %s TEXT, %s INT)", userTableName,
				C_USERNAME, C_DOMAIN, C_PORT);
		String Create_Commands_Table = String.format(
				"create table %s (%s TEXT)", commandTableName, C_COMMAND);
		db.execSQL(Create_User_Table);
		db.execSQL(Create_Commands_Table);
		
		//Save default pass
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("myPass", passEncryptor.encryptPassword("default"));
		editor.commit();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}
}