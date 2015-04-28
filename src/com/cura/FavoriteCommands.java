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
 * Description: This is the Favorite Commands activity which exists in the Terminal module. Here is where the user can add
 * new commands to their list of favorite commands and be able to select one of them and run it in the terminal.
 */

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.cura.Connection.SSHConnection;
import com.google.analytics.tracking.android.EasyTracker;

public class FavoriteCommands extends ListActivity {

	String favoriteCommands[];
	DbHelper dbHelper;
	SQLiteDatabase db;
	User userTemp;
	SSHConnection sshconnection;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.favoritecommands);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userTemp = extras.getParcelable("user");
		}
		this.setTitle(R.string.favoritesWelcome + userTemp.getUsername());

		dbHelper = new DbHelper(this);
		db = dbHelper.getReadableDatabase();

		Cursor c = db.rawQuery("select * from commandTable", null);

		favoriteCommands = new String[c.getCount()];
		int counter = 0;

		if (c != null) {
			if (c.moveToFirst()) {
				do {
					favoriteCommands[counter] = c.getString(c.getColumnIndex("command"));
					counter++;
				} while (c.moveToNext());
			}
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, favoriteCommands);
		setListAdapter(adapter);
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
		menu.add(0, Menu.FIRST + 1, 0, R.string.deleteFavoriteCommand);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		String command = favoriteCommands[info.position];

		dbHelper = new DbHelper(FavoriteCommands.this);
		db = dbHelper.getWritableDatabase();

		switch (item.getItemId()) {
		case Menu.FIRST + 1:
			try {

				String where = "command = ?";
				String[] whereArgs = { command };
				db.delete(DbHelper.commandTableName, where, whereArgs);

				startActivity(getIntent());
				finish();
			} catch (Exception e) {
				Log.d("SQL", e.toString());
			}

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this); // Add this method.
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this); // Add this method.
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		db.close();
		dbHelper.close();
	}

}