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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cura.DbHelper;
import com.cura.LoginScreenActivity;
import com.cura.R;
import com.cura.User;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;

public class TerminalActivity extends Activity {
<<<<<<< HEAD
	
	private final int FAVORITES = 1;
	private final int LOGOUT = 2;
	
=======

>>>>>>> 6a153732e1c44efa6d7510225b4715c94e0546eb
	EditText result;
	EditText commandLine;
	Button execute;
	Button favoritesButton;

	Terminal terminal;
	User userTemp;

	DbHelper dbHelper;
	SQLiteDatabase db;

	String favoriteCommands[];
	String username;

	private CommunicationInterface conn;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			// TODO Auto-generated method stub
			conn = CommunicationInterface.Stub.asInterface(service);
			sendAndReceive();
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			conn = null;
			Toast.makeText(TerminalActivity.this, "Service Disconnected",
					Toast.LENGTH_LONG);
		}
	};

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
		bindService(i, connection, Context.BIND_AUTO_CREATE);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.terminal);
		doBindService();
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userTemp = extras.getParcelable("user");
		}
		if (userTemp.getUsername().compareTo("root") == 0) {
			username = userTemp.getUsername() + "@" + userTemp.getDomain()
					+ ":~# ";	
		} else {
			username = userTemp.getUsername() + "@" + userTemp.getDomain()
					+ ":~$ ";
		}
		this.setTitle("Welcome to the terminal, " + userTemp.getUsername());
		commandLine = (EditText) findViewById(R.id.commandLine);
		execute = (Button) findViewById(R.id.executeButton);
		result = (EditText) findViewById(R.id.result);
		result.append(username);
		result.setTextColor(Color.GREEN);
	}

	public void sendAndReceive() {
		execute.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				String command = commandLine.getText().toString();
				result.setTextColor(Color.GREEN);
				result.append(command + "\n");
				String res = "";
				try {
					res = conn.executeCommand(command);
				} catch (Exception e) {
					Log.d("Terminal", e.toString());
				}
				result.append(res);
				result.append(username);
			}

		});

		favoritesButton = (Button) findViewById(R.id.favoritesButton);

		favoritesButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dbHelper = new DbHelper(TerminalActivity.this);
				db = dbHelper.getReadableDatabase();
				// instantiate the instance of SQLite database and DBHelper

				// select all commands.
				Cursor c = db.rawQuery("select * from commandTable", null);
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

				AlertDialog.Builder builder = new AlertDialog.Builder(
						TerminalActivity.this);
				builder.setTitle("Pick a command");
				builder.setItems(favoriteCommands,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								commandLine.append(favoriteCommands[item]);
							}

						});
				builder.show();
				db.close();
				dbHelper.close();
			}
		});
	}

	// MENU STUFF IS IMPLEMENTED BELOW
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		// Add a button to menu
		menu.add(0, FAVORITES, 0, R.string.addNewFavoriteCommand).setIcon(android.R.drawable.ic_input_add);
		menu.add(1, LOGOUT,0,R.string.logout).setIcon(R.drawable.ic_lock_power_off);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// if "Add new command" button is pressed from the menu
		case FAVORITES:
			// display dialog box
			AlertDialog.Builder addUser = new AlertDialog.Builder(
					TerminalActivity.this);
			addUser.setMessage(R.string.addNewCommandToFavoritesprompt);
			final EditText commandText = new EditText(this);
			addUser.setView(commandText);

			addUser.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String command = commandText.getText().toString();
							addCommandToDatabase(command);
							return;
						}
					});
			addUser.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							return;
						}
					});
			addUser.show();
			break;
		case LOGOUT:
				new AlertDialog.Builder(this)
				.setTitle("Logout Confirmation")
				.setMessage(R.string.logoutConfirmationDialog)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						try {
						conn.close();
						Log.d("Connection","connection closed");
						} catch (RemoteException e) {
							Log.d("Connection",e.toString());
						}
						Intent closeAllActivities = new Intent(TerminalActivity.this, LoginScreenActivity.class);
						closeAllActivities.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						TerminalActivity.this.startActivity(closeAllActivities);	
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
				
					public void onClick(DialogInterface dialog, int which) {	
						dialog.dismiss();	
					}
				}).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void addCommandToDatabase(String command) {
		DbHelper dbHelper = new DbHelper(TerminalActivity.this);
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		ContentValues values = new ContentValues();

		values.put(DbHelper.C_COMMAND, command);

		try {
			// insert into database a new command
			db.insertOrThrow(DbHelper.commandTableName, null, values);
			Toast.makeText(this, "Command added successfully!", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(this, "Command could not be added!", Toast.LENGTH_SHORT).show();
			Log.d("SQL", e.toString());
		}

		// close database
		db.close();
		dbHelper.close();
		startActivity(getIntent());
		finish();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
<<<<<<< HEAD
		unbindService(connection);
		finish();
	}
	
	@Override
	protected void onDestroy() {
		super.onStop();
//		unbindService(connection);
	}
	
=======
		finish();
	}
>>>>>>> 6a153732e1c44efa6d7510225b4715c94e0546eb
}
