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

import java.io.IOException;

import com.cura.Connection.SSHConnection;
import com.jcraft.jsch.JSchException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class FavoriteCommands extends ListActivity {

	String favoriteCommands[];
	// create an array of strings.
	DbHelper dbHelper;
	// instance of the database helper.
	SQLiteDatabase db;
	// instance of SQLite database.
	User userTemp;
	SSHConnection sshconnection;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.favoritecommands);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userTemp = extras.getParcelable("user");
		}
		this.setTitle("Welcome to your favorites, " + userTemp.getUsername());

		dbHelper = new DbHelper(this);
		db = dbHelper.getReadableDatabase();
		// instantiate the instance of SQLite database and DBHelper

		// select all commands.
		Cursor c = db.rawQuery("select * from commandTable", null);

		// create an array of commands.
		favoriteCommands = new String[c.getCount()];
		int counter = 0;

		if (c != null) {
			if (c.moveToFirst()) {
				do {
					favoriteCommands[counter] = c.getString(c
							.getColumnIndex("command"));
					counter++;
				} while (c.moveToNext());
			}
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, favoriteCommands);
		// display them.
		setListAdapter(adapter);
		//register context menu 
		registerForContextMenu(getListView());
	}
	
	String commandItem;
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		commandItem = (String) getListAdapter().getItem(position);
		Toast.makeText(this, commandItem + " selected", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// add to buttons to context menu "Modify command", "Delete command"
		menu.add(0, Menu.FIRST + 1, 0, R.string.deleteFavoriteCommand);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		String command = favoriteCommands[info.position];

		dbHelper = new DbHelper(FavoriteCommands.this);
		db = dbHelper.getWritableDatabase();

		switch (item.getItemId()) {
		// delete button is pressed
		case Menu.FIRST + 1:
			// Delete command
			try {
				
				String where = "command = ?";
				String[] whereArgs = { command };
				// prepare the query.
				db.delete(DbHelper.commandTableName, where, whereArgs);
				// execute it.

				startActivity(getIntent());
				finish();
				} catch (Exception e) {
					Log.d("SQL", e.toString());
					// so that we can know where to follow the errors (if
					// any).
				}

				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		db.close();
		dbHelper.close();
	}

}