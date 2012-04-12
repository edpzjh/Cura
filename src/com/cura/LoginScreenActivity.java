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

import org.jasypt.util.password.BasicPasswordEncryptor;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.Toast;

import com.cura.Connection.ConnectionService;

public class LoginScreenActivity extends ListActivity {
	private final String connected = "cura.connected";
	private final String notConnected = "cura.not.connected";
	private final int ADD_USER = 1;
	private final int SETTINGS = 2;
	private final int REFRESH = 3;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.LoginScreenName);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		user = getUser();
		// create the listView

		if (user.length == 1
				&& user[0].getUsername().equalsIgnoreCase("username")
				&& user[0].getDomain().equalsIgnoreCase("domain")) {
			showDialog(DIALOG_YES_NO_LONG_MESSAGE);
		}

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
					// find out who the user is
				}
				if (intent.getAction().compareTo(connected) == 0) {
					// if they are connected, take them to the main activity
					// (CuraActivity)
					loader.dismiss();
					goToMainActivity = new Intent(LoginScreenActivity.this,
							CuraActivity.class);
					goToMainActivity.putExtra("user", userTemp);
					startActivity(goToMainActivity);
				} else {
					loader.dismiss();
					// else if they are not connected, meaning that the
					// username/password combination was incorrect (or some
					// other reason which will be dealt with later on) show this
					// the appropriate error dialog
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
				// this is the screen that shows up with the user installs Cura
				// for the very first time
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
			AlertDialog.Builder passwordAlert = new AlertDialog.Builder(this);

			// set an alert dialog to prompt the user for their password to
			// login.
			passwordAlert.setTitle("Login");

			LayoutInflater li = LayoutInflater.from(this);
			View view = li.inflate(R.layout.password_dialog, null);
			passwordAlert.setView(view);
			final EditText passField = (EditText) view
					.findViewById(R.id.passwordprompt);

			CheckBox showPass = (CheckBox) view.findViewById(R.id.showPassword);
			// this is for the "Show password" checkbox that allows the user to
			// see their password in the clear
			showPass.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					// TODO Auto-generated method stub
					if (isChecked)
						passField.setTransformationMethod(null);
					// if that checkbox is checked, do the transformation
					else
						passField
								.setTransformationMethod(PasswordTransformationMethod
										.getInstance());
					// if it isn't, leave it as is

				}
			});
			passwordAlert.setPositiveButton("Connect",
					new DialogInterface.OnClickListener() {
						// if the textfield is now filled with a password, allow
						// the "Connect button" to be clickable
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
									// show this dialog to signify that the user
									// is being connected to their server
								}

								@Override
								protected String doInBackground(
										String... params) {
									String pass = passField.getText()
											.toString();
									user[position].setPassword(pass);
									// store the user's password according to
									// their determined position
									userTemp = user[position];
									passUserObjToService = new Intent(
											LoginScreenActivity.this,
											ConnectionService.class);
									// initiate the Connection intent and send
									// the user's password and the user object
									// along with it
									passUserObjToService.putExtra("user",
											userTemp);
									passUserObjToService.putExtra("pass", pass);
									return null;
								}

								@Override
								protected void onPostExecute(String result) {
									//causing the loader circle to stop spinning
									//starts the connection service
									startService(passUserObjToService);
								}
							};
							task.execute();
						}
					});
			passwordAlert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						// UPON CLICKING "CANCEL" IN THE DIALOG BOX (ALERT)
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					});
			final AlertDialog alert = passwordAlert.create();
			alert.show();
			passField.addTextChangedListener(new TextWatcher() {

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// TODO Auto-generated method stub
					String pass = passField.getText().toString();
					if (pass.length() > 0)
						alert.getButton(Dialog.BUTTON1).setEnabled(true);
					else if (pass.length() == 0)
						alert.getButton(Dialog.BUTTON1).setEnabled(false);
				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				public void afterTextChanged(Editable s) {
				}
			});
			alert.getButton(Dialog.BUTTON1).setEnabled(false);
		}
	}

	// MENU STUFF IS IMPLEMENTED BELOW, FIRST THE REGULAR MENU THAT APPEARS WHEN
	// A USER CLICKS ON THE MENU BUTTON, THEN THE CONTEXT MENU WHEN A USER
	// LONG-CLICKS ON ONE OF THE USERS IN THE LIST
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		// Add a button to menu
		menu.add(0, ADD_USER, 0, R.string.no_users).setIcon(
				R.drawable.ic_menu_add);
		menu.add(0, SETTINGS, 0, R.string.preferenceSettings).setIcon(
				R.drawable.ic_menu_preferences);
		menu.add(1, REFRESH, 1, "Refresh").setIcon(R.drawable.ic_menu_rotate);
		return result;
	}

	// THE FIRST MENU
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// if "Add new user" button is pressed from the menu
		case ADD_USER:
			// display dialog box
			final Dialog myDialog;
			myDialog = new Dialog(LoginScreenActivity.this);
			myDialog.setContentView(R.layout.adduserscreen);
			myDialog.setTitle(R.string.DialogTitle);
			myDialog.setCancelable(true);
			myDialog.setCanceledOnTouchOutside(true);

			final Button AddUserButton = (Button) myDialog
					.findViewById(R.id.button1);
			AddUserButton.setEnabled(false);
			Button cancelButton = (Button) myDialog.findViewById(R.id.button2);
			TextWatcher watcher = null;
			final EditText usernameInput = (EditText) myDialog
					.findViewById(R.id.usernameTextField);

			final EditText domainInput = (EditText) myDialog
					.findViewById(R.id.domainTextField);

			final EditText portInput = (EditText) myDialog
					.findViewById(R.id.portTextField);

			watcher = new TextWatcher() {

				public void afterTextChanged(Editable s) {
					// TODO Auto-generated method stub
				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub

				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// TODO Auto-generated method stub
					String username = usernameInput.getText().toString();
					String domain = domainInput.getText().toString();
					String port = portInput.getText().toString();
					if (!username.isEmpty() && !domain.isEmpty()
							&& !port.isEmpty())
						//if all the textfields are filled, enable the Add button
						AddUserButton.setEnabled(true);
					else
						//else, disable it
						AddUserButton.setEnabled(false);
				}

			};
			usernameInput.addTextChangedListener(watcher);
			//adding listeners
			domainInput.addTextChangedListener(watcher);
			portInput.addTextChangedListener(watcher);
			// cannot click "Add" if all of the above textfields are empty

			AddUserButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// get username, domain and port from EditText
					String usern = usernameInput.getText().toString();
					String domain = domainInput.getText().toString();
					int port = Integer.parseInt(portInput.getText().toString());

					// open writable database
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
		case SETTINGS:
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
							BasicPasswordEncryptor passwordEncryptor = new BasicPasswordEncryptor();
							String curaPass = prefs.getString("myPass", "");
							String passfield = passField.getText().toString();
							// encrypt the given password
							// if (passfield.compareTo(curaPass) == 0)
							if (passwordEncryptor.checkPassword(passfield,
									curaPass))
								// if it matches the password that we have for
								// the user, take them to where they should go
								startActivity(new Intent(
										LoginScreenActivity.this,
										PreferenceScreen.class));
							else
								// else, prompt for wrong password
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
		case REFRESH:
			// this button from the Login Screen's menu items is used to refresh
			// that specific screen in order to instantly see that Cura's
			// database has been wiped due to an emergency SMS having been sent
			// to the phone
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(br);
	}
}