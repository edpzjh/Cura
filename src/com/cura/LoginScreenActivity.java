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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.cura.Connection.ConnectionService;

public class LoginScreenActivity extends ListActivity {
	private final String connected = "cura.connected";
	private final String notConnected = "cura.not.connected";
	TableRow AddUserRow;
	DbHelper dbHelper;
	SQLiteDatabase db;
	User user[];
	User userTemp;
	CustomArrayAdapter array;
	Intent goToMainActivity;
	BroadcastReceiver br;
	ProgressDialog loader;
	private SharedPreferences prefs;
	private static final int DIALOG_YES_NO_LONG_MESSAGE = 99;
	private static final int SET_CURA_PASSWORD = 98;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.LoginScreenName);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		user = getUser();
		// create the listView

		if (user.length == 0) {
			showDialog(DIALOG_YES_NO_LONG_MESSAGE);
		}

		if (prefs.getString("myPass", "").compareTo("") == 0)
			showDialog(SET_CURA_PASSWORD);

		array = new CustomArrayAdapter(this, user);
		setListAdapter(array);
		// enable context menu
		registerForContextMenu(getListView());
		br = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				Bundle extras = intent.getExtras();
				if (extras != null) {
					userTemp = extras.getParcelable("user");
				}
				if (intent.getAction().compareTo(connected) == 0) {
					loader.cancel();
					goToMainActivity = new Intent(LoginScreenActivity.this,
							CuraActivity.class);
					goToMainActivity.putExtra("user", userTemp);
					startActivity(goToMainActivity);
				} else {
					loader.cancel();
					Toast.makeText(context, R.string.credentialsWrong,
							Toast.LENGTH_LONG).show();
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(connected);
		intentFilter.addAction(notConnected);
		registerReceiver(br, intentFilter);
	}

	// new function that fetches users info from database, used in "onCreate()"
	// and to refresh activity
	public User[] getUser() {
		dbHelper = new DbHelper(this);
		db = dbHelper.getReadableDatabase();

		// select all users.
		Cursor c = db.rawQuery("select * from user", null);
		// create an array of users
		if (c.getCount() == 0) {
			user = new User[1];
			user[0] = new User("username", "domain", 22);
			Log.d("HERE", "HERE");
		} else {
			user = new User[c.getCount()];

			int counter = 0;

			// get info and store them into objects
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						String username = c.getString(c
								.getColumnIndex("username"));
						String domain = c.getString(c.getColumnIndex("domain"));
						int port = Integer.parseInt(c.getString(c
								.getColumnIndex("port")));
						// fetch the user's data.
						user[counter] = new User(username, domain, port);
						counter++;
					} while (c.moveToNext());
				}
			}
		}
		// close database
		db.close();
		dbHelper.close();

		return user;
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		return new AlertDialog.Builder(LoginScreenActivity.this)
				// .setIconAttribute(android.R.attr.alertDialogIcon)
				.setTitle(R.string.firstTimeUseMessageTitle)
				.setMessage(R.string.firstTimeUseMessage)
				.setPositiveButton(R.string.firstTimeUseOKButton,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								/* User clicked OK so do some stuff */
							}
						})
				.setNegativeButton(R.string.firstTimeUseCancelButton,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								/* User clicked Cancel so do some stuff */
							}
						}).create();
	}

	@Override
	// UPON CLICKING A USER ITEM FROM THE LIST OF USERS AVAILABLE.
	protected void onListItemClick(ListView l, View v, final int position,
			long id) {
		super.onListItemClick(l, v, position, id);
		if (user.length == 1
				&& user[0].getUsername().equalsIgnoreCase("username")
				&& user[0].getDomain().equalsIgnoreCase("domain")) {
			Toast.makeText(this, R.string.addUserHint, Toast.LENGTH_LONG)
					.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			// set an alert dialog to prompt the user for their password to
			// login.
			alert.setTitle("Login");
			alert.setMessage(R.string.LoginScreenPasswordPrompt);

			final EditText passField = new EditText(this);
			passField.setTransformationMethod(PasswordTransformationMethod
					.getInstance());
			// make it turn into stars, as available from the API.
			alert.setView(passField);
			// show the alert.

			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								int whichButton) {
							// UPON CLICKING "OK" IN THE DIALOG BOX (ALERT)
							AsyncTask<String, String, String> task = new AsyncTask<String, String, String>() {
								Intent passUserObjToService;

								@Override
								protected void onPreExecute() {
									dialog.dismiss();
									loader = ProgressDialog.show(
											LoginScreenActivity.this,
											"Connecting...",
											"Loading, please wait...", true);
								}

								@Override
								protected String doInBackground(
										String... params) {
									String pass = passField.getText()
											.toString();
									user[position].setPassword(pass);
									userTemp = user[position];
									passUserObjToService = new Intent(
											LoginScreenActivity.this,
											ConnectionService.class);
									passUserObjToService.putExtra("user",
											userTemp);
									passUserObjToService.putExtra("pass", pass);
									return null;
								}

								@Override
								protected void onPostExecute(String result) {
									startService(passUserObjToService);
									loader.dismiss();
								}

							};
							task.execute();
							return;
						}
					});
			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						// UPON CLICKING "CANCEL" IN THE DIALOG BOX (ALERT)
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					});
			alert.show();
		}
	}

	// MENU STUFF IS IMPLEMENTED BELOW, FIRST THE REGULAR MENU THAT APPEARS WHEN
	// A USER CLICKS ON THE MENU BUTTON, THEN THE CONTEXT MENU WHEN A USER
	// LONG-CLICKS ON ONE OF THE USERS IN THE LIST
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		// Add a button to menu
		menu.add(0, Menu.FIRST, 0, R.string.no_users).setIcon(
				android.R.drawable.ic_menu_add);
		menu.add(0, 2, 0, R.string.preferenceSettings).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(1, 3, 1, "Refresh").setIcon(android.R.drawable.ic_menu_rotate);
		return result;
	}

	// THE FIRST MENU
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// if "Add new user" button is pressed from the menu
		case Menu.FIRST:
			// display dialog box
			final Dialog myDialog;
			myDialog = new Dialog(LoginScreenActivity.this);
			myDialog.setContentView(R.layout.adduserscreen);
			myDialog.setTitle(R.string.DialogTitle);
			myDialog.setCancelable(true);
			myDialog.setCanceledOnTouchOutside(true);

			Button AddUserButton = (Button) myDialog.findViewById(R.id.button1);
			Button cancelButton = (Button) myDialog.findViewById(R.id.button2);

			final EditText usernameInput = (EditText) myDialog
					.findViewById(R.id.usernameTextField);
			final EditText domainInput = (EditText) myDialog
					.findViewById(R.id.domainTextField);
			final EditText portInput = (EditText) myDialog
					.findViewById(R.id.portTextField);

			AddUserButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// get username, domain and port from EditText
					String usern = usernameInput.getText().toString();
					String domain = domainInput.getText().toString();
					int port = Integer.parseInt(portInput.getText().toString());

					// open database
					DbHelper dbHelper = new DbHelper(LoginScreenActivity.this);
					SQLiteDatabase db = dbHelper.getWritableDatabase();

					ContentValues values = new ContentValues();

					values.put(dbHelper.C_USERNAME, usern);
					values.put(dbHelper.C_DOMAIN, domain);
					values.put(dbHelper.C_PORT, port);

					try {
						// insert into database a new user
						db.insertOrThrow(dbHelper.userTableName, null, values);
					} catch (Exception e) {
						Log.d("SQL", e.toString());
					}

					// close database
					db.close();
					dbHelper.close();

					// CHANGED : refresh list view
					user = getUser();
					array = new CustomArrayAdapter(LoginScreenActivity.this,
							user);
					setListAdapter(array);
					myDialog.cancel();
				}
			});

			cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// close dialog box
					myDialog.cancel();
				}
			});
			myDialog.show();
			return true;
		case 2:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			// set an alert dialog to prompt the user for their password to
			// login.
			alert.setTitle(R.string.settingsPassDialogTitle);
			alert.setMessage(R.string.settignsScreenPasswordPrompt);

			final EditText passField = new EditText(this);
			passField.setTransformationMethod(PasswordTransformationMethod
					.getInstance());
			// make it turn into stars, as available from the API.
			alert.setView(passField);
			// show the alert.

			alert.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// UPON CLICKING "OK" IN THE DIALOG BOX (ALERT)
							String curaPass = prefs.getString("myPass", "");
							String passfield = passField.getText().toString();
							if (passfield.compareTo(curaPass) == 0)
								startActivity(new Intent(
										LoginScreenActivity.this,
										PreferenceScreen.class));
							else
								Toast.makeText(LoginScreenActivity.this,
										R.string.wrongPassword,
										Toast.LENGTH_SHORT).show();
							return;
						}
					});
			alert.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						// UPON CLICKING "CANCEL" IN THE DIALOG BOX (ALERT)
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					});
			alert.show();
			return true;
		case 3:
			user = getUser();
			array = new CustomArrayAdapter(LoginScreenActivity.this, user);
			setListAdapter(array);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// THE SECOND MENU
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// add to buttons to context menu "Modify user Info", "Delete User"
		menu.add(0, Menu.FIRST + 1, 0, R.string.ModifyUserInfo).setIcon(
				R.drawable.ic_menu_edit);
		menu.add(0, Menu.FIRST + 2, 0, R.string.DeleteUser).setIcon(
				R.drawable.ic_menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		int userIDint = (int) info.id;
		final String usernameCode = user[userIDint].getUsername();
		final String domainCode = user[userIDint].getDomain();

		dbHelper = new DbHelper(LoginScreenActivity.this);
		db = dbHelper.getWritableDatabase();

		switch (item.getItemId()) {
		// modify button is pressed
		case Menu.FIRST + 1:
			final Dialog myDialog;
			myDialog = new Dialog(LoginScreenActivity.this);
			myDialog.setContentView(R.layout.adduserscreen);
			myDialog.setTitle(R.string.DialogTitle);
			myDialog.setCancelable(true);
			myDialog.setCanceledOnTouchOutside(true);

			Button modifyUserInfo = (Button) myDialog
					.findViewById(R.id.button1);
			modifyUserInfo.setText(R.string.ModifyUserInfo);
			Button cancelButton = (Button) myDialog.findViewById(R.id.button2);

			final EditText usernameInput = (EditText) myDialog
					.findViewById(R.id.usernameTextField);
			final EditText domainInput = (EditText) myDialog
					.findViewById(R.id.domainTextField);
			final EditText portInput = (EditText) myDialog
					.findViewById(R.id.portTextField);
			// display the initial info to be replaced by the user
			usernameInput.setText(user[userIDint].getUsername());
			domainInput.setText(user[userIDint].getDomain());
			portInput.setText("" + user[userIDint].getPort());

			modifyUserInfo.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// update user info
					String usern = usernameInput.getText().toString();
					String domain = domainInput.getText().toString();
					int port = Integer.parseInt(portInput.getText().toString());

					ContentValues values = new ContentValues();

					values.put(dbHelper.C_USERNAME, usern);
					values.put(dbHelper.C_DOMAIN, domain);
					values.put(dbHelper.C_PORT, port);

					String where = "username = ? AND domain = ?";
					String[] whereArgs = { usernameCode, domainCode };
					try {
						db.update(DbHelper.userTableName, values, where,
								whereArgs);
					} catch (Exception e) {
						Log.d("SQL", e.toString());
					}

					// close database
					db.close();
					dbHelper.close();

					// CHANGED : refresh list view
					user = getUser();
					array = new CustomArrayAdapter(LoginScreenActivity.this,
							user);
					setListAdapter(array);

					myDialog.cancel();
				}
			});

			cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// close dialog box
					myDialog.cancel();
				}
			});
			myDialog.show();
			return true;

		case Menu.FIRST + 2:
			// Delete user
			try {
				String table_name = "user";
				String where = "username = ? AND domain = ?";
				String[] whereArgs = { usernameCode, domainCode };
				// prepare the query.
				db.delete(table_name, where, whereArgs);
				// execute it.

			} catch (Exception e) {
				Log.d("SQL", e.toString());
				// so that we can know where to follow the errors (if any).
			}
			// close database
			db.close();
			dbHelper.close();

			// CHANGED : refresh list view
			user = getUser();
			array = new CustomArrayAdapter(LoginScreenActivity.this, user);
			setListAdapter(array);

			return true;
		}
		return super.onContextItemSelected(item);
	}
}