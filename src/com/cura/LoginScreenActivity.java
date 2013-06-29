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
 * Description: This is the login screen and this is Cura's main first screen where the user will be dropped to upon accessing the 
 * application. Here is where we offer the user the ability to select a server account from the ones that they've added, add new ones or
 * modify existing server accounts. Also offered in this screen (through the menu) is the Settings tab and the About tab.
 */

import org.jasypt.util.password.BasicPasswordEncryptor;

import com.cura.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.cura.Connection.ConnectionService;
import com.cura.about.aboutActivity;
import com.cura.validation.regexValidator;
//import com.google.ads.AdRequest;
//import com.google.ads.AdView;

public class LoginScreenActivity extends Activity implements android.view.View.OnClickListener {

 private final String connected = "cura.connected";
 private final String notConnected = "cura.not.connected";
 private final int ADD_USER = 1;
 private final int SETTINGS = 2;
 private final int ABOUT = 3;
 private CustomArrayAdapter array;
 private BroadcastReceiver br;
 private Intent goToMainActivity;
 private Button selectServer, newServer, modifyServers;
 private LinearLayout buttonsLayout;
 private User user[];
 private User userTemp;
 private DbHelper dbHelper;
 private SQLiteDatabase db;
 int position;
 private Vibrator vibrator;
 private SharedPreferences prefs;
 private regexValidator rv;
 private boolean isConnected = false;
 // private AdView adView;
 private AlertDialog alert;
 private AsyncTask<String, String, String> task;

 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
  setContentView(com.cura.R.layout.loginscreen);

  ((TextView) findViewById(R.id.connecting)).setVisibility(View.GONE);
  // this "Connecting..." textview is shown whenever the user has selected
  // a server and chose to connect, it is invisible in the beginning but
  // it is still accounted for in XML (hidden) so it has to be hidden in
  // code as well
  prefs = PreferenceManager.getDefaultSharedPreferences(this);
  rv = new regexValidator();

  selectServer = (Button) findViewById(R.id.selectServer);
  newServer = (Button) findViewById(R.id.newServer);
  modifyServers = (Button) findViewById(R.id.modifyServers);
  selectServer.setOnClickListener(this);
  newServer.setOnClickListener(this);
  modifyServers.setOnClickListener(this);

  br = new BroadcastReceiver() {

   @Override
   public void onReceive(Context context, Intent intent) {
	// TODO Auto-generated method stub
	Bundle extras = intent.getExtras();
	setProgressBarIndeterminateVisibility(false);
	if(extras != null) {
	 userTemp = extras.getParcelable("user");
	 // find out who the user is
	}
	if(intent.getAction().compareTo(connected) == 0) {
	 // if they are connected, take them to the main activity
	 // (CuraActivity)
	 isConnected = true;
	 goToMainActivity = new Intent(LoginScreenActivity.this, CuraActivity.class);
	 goToMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	 goToMainActivity.putExtra("user", userTemp);
	 startActivity(goToMainActivity);
	}
	else {
	 // else if they are not connected, meaning that the
	 // username/password combination was incorrect (or some
	 // other reason which will be dealt with later on) show this
	 // the appropriate error dialog
	 isConnected = false;
	 Toast.makeText(context, R.string.credentialsWrong, Toast.LENGTH_LONG).show();
	 ((ImageView) findViewById(R.id.server)).setImageResource(R.drawable.serveroffline);
	 ((TextView) findViewById(R.id.connecting)).setVisibility(View.GONE);
	 buttonsLayout.setVisibility(View.VISIBLE);

	 stopService(new Intent(LoginScreenActivity.this, ConnectionService.class));
	}
   }
  };

  IntentFilter intentFilter = new IntentFilter();
  intentFilter.addAction(connected);
  intentFilter.addAction(notConnected);
  // this intent filter listens to when the user is actually connected to
  // the server and sends a message through the broadcast receiver
  registerReceiver(br, intentFilter);
  vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
  buttonsLayout = (LinearLayout) findViewById(R.id.ButtonsLayout);

  // Ads
  // adView = (AdView)this.findViewById(R.id.adView);
  // adView.loadAd(new AdRequest());
 }

 @Override
 protected void onStart() {
  // TODO Auto-generated method stub
  super.onStart();
  // here it is received and the user is then dropped into their main
  // CuraActivity where they can go about their business
  if(isConnected) {
   goToMainActivity = new Intent(LoginScreenActivity.this, CuraActivity.class);
   goToMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
   goToMainActivity.putExtra("user", userTemp);
   startActivity(goToMainActivity);
  }
 }

 @Override
 public void onClick(View arg0) {
  // TODO Auto-generated method stub
  switch (arg0.getId()) {
  case R.id.selectServer:

   final Dialog accounts = new Dialog(this);
   accounts.setContentView(R.layout.list);
   accounts.setTitle(R.string.selectServer);
   user = getUser();
   array = new CustomArrayAdapter(this, user);
   ListView mlistView = new ListView(this);
   mlistView.setOnItemClickListener(new OnItemClickListener() {
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	 // TODO Auto-generated method stub
	 accounts.dismiss();
	 position = arg2;
	 if(user.length == 1 && user[0].getUsername().equalsIgnoreCase("username") && user[0].getDomain().equalsIgnoreCase("domain")) {
	  Toast.makeText(LoginScreenActivity.this, R.string.addServerHint, Toast.LENGTH_LONG).show();
	 }
	 else {
	  AlertDialog.Builder passwordAlert = new AlertDialog.Builder(LoginScreenActivity.this);

	  // set an alert dialog to prompt the user for their
	  // password to
	  // login.
	  passwordAlert.setTitle("Login");

	  LayoutInflater li = LayoutInflater.from(LoginScreenActivity.this);
	  View view = li.inflate(R.layout.password_dialog, null);
	  passwordAlert.setView(view);
	  final EditText passField = (EditText) view.findViewById(R.id.passwordprompt);
	  CheckBox showPass = (CheckBox) view.findViewById(R.id.showPassword);
	  // this is for the "Show password" checkbox that allows
	  // the user to
	  // see their password in the clear
	  showPass.setOnCheckedChangeListener(new OnCheckedChangeListener() {

	   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		if(isChecked)
		 passField.setTransformationMethod(null);
		// if that checkbox is checked, do the
		// transformation
		else
		 passField.setTransformationMethod(PasswordTransformationMethod.getInstance());
		// if it isn't, leave it as is

	   }
	  });
	  passwordAlert.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
	   // if the textfield is now filled with a
	   // password, allow
	   // the "Connect button" to be clickable
	   public void onClick(final DialogInterface dialog, int whichButton) {
		// UPON CLICKING "OK" IN THE DIALOG BOX
		// (ALERT)
		InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		keyboard.hideSoftInputFromWindow(passField.getWindowToken(), 0);
		task = new AsyncTask<String, String, String>() {
		 Intent passUserObjToService;

		 @Override
		 protected void onPreExecute() {
		  dialog.dismiss();

		  setProgressBarIndeterminateVisibility(true);
		  ((ImageView) findViewById(R.id.server)).setImageResource(R.drawable.serverconnecting);
		  ((TextView) findViewById(R.id.connecting)).setVisibility(View.VISIBLE);
		  buttonsLayout.setVisibility(View.GONE);
		 }

		 @Override
		 protected String doInBackground(String... params) {
		  String pass = passField.getText().toString();
		  user[position].setPassword(pass);
		  // store the user's password
		  // according to
		  // their determined position
		  userTemp = user[position];
		  passUserObjToService = new Intent(LoginScreenActivity.this, ConnectionService.class);
		  // initiate the Connection
		  // intent and send
		  // the user's password and the
		  // user object
		  // along with it
		  passUserObjToService.putExtra("user", userTemp);
		  passUserObjToService.putExtra("pass", pass);
		  return null;
		 }

		 @Override
		 protected void onPostExecute(String result) {
		  // causing the loader circle to
		  // stop
		  // spinning
		  // starts the connection service
		  startService(passUserObjToService);
		  // removeDialog(WAIT);
		 }
		};
		task.execute();

	   }
	  });
	  passwordAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	   // UPON CLICKING "CANCEL" IN THE DIALOG BOX
	   // (ALERT)
	   public void onClick(DialogInterface dialog, int which) {
		return;
	   }
	  });
	  passField.setOnEditorActionListener(new TextView.OnEditorActionListener() {

	   @Override
	   public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
		// TODO Auto-generated method stub
		alert.getButton(Dialog.BUTTON1).performClick();
		return false;
	   }
	  });
	  alert = passwordAlert.create();
	  alert.show();
	  passField.addTextChangedListener(new TextWatcher() {
	   // this TextWatcher watches for changes in text at
	   // the password prompt, if there are no characters
	   // entered yet, do not enable the "OK" button which
	   // allows the user to login, else display that
	   // button
	   public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		String pass = passField.getText().toString();
		if(pass.length() > 0)
		 alert.getButton(Dialog.BUTTON1).setEnabled(true);
		else
		 if(pass.length() == 0)
		  alert.getButton(Dialog.BUTTON1).setEnabled(false);
	   }

	   public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	   }

	   public void afterTextChanged(Editable s) {
	   }
	  });
	  alert.getButton(Dialog.BUTTON1).setEnabled(false);
	 }
	}
   });
   mlistView.setAdapter(array);
   accounts.addContentView(mlistView, new LayoutParams());
   accounts.setOnCancelListener(new OnCancelListener() {

	@Override
	public void onCancel(DialogInterface arg0) {
	 // TODO Auto-generated method stub
	 accounts.dismiss();
	}
   });
   accounts.show();
   break;
  case R.id.newServer:
   addUser();
   break;
  case R.id.modifyServers:
   user = getUser();
   // if the Modify Servers button is tapped and there are no servers,
   // display that message
   if(user.length == 1 && user[0].getUsername().equalsIgnoreCase("username") && user[0].getDomain().equalsIgnoreCase("domain"))
	Toast.makeText(this, getString(R.string.noServersFound), Toast.LENGTH_LONG).show();
   else
	startActivity(new Intent(this, AccountsListActivity.class));
   break;
  }
 }

 public User[] getUser() {
  dbHelper = new DbHelper(this);
  db = dbHelper.getReadableDatabase();

  // select all users.
  Cursor c = db.rawQuery("select * from user", null);
  // create an array of users
  if(c.getCount() == 0) {
   user = new User[1];
   user[0] = new User("username", "domain", 22);
  }
  else {
   user = new User[c.getCount()];

   int counter = 0;

   // get info and store them into objects
   if(c != null) {
	if(c.moveToFirst()) {
	 do {
	  String username = c.getString(c.getColumnIndex("username"));
	  String domain = c.getString(c.getColumnIndex("domain"));
	  int port = Integer.parseInt(c.getString(c.getColumnIndex("port")));
	  // fetch the user's data.
	  user[counter] = new User(username, domain, port);
	  counter++;
	 } while(c.moveToNext());
	}
   }
  }
  // close database
  c.close();
  db.close();
  dbHelper.close();

  return user;
 }

 // MENU STUFF IS IMPLEMENTED BELOW, FIRST THE REGULAR MENU THAT APPEARS WHEN
 // A USER CLICKS ON THE MENU BUTTON, THEN THE CONTEXT MENU WHEN A USER
 // LONG-CLICKS ON ONE OF THE USERS IN THE LIST
 @Override
 public boolean onCreateOptionsMenu(Menu menu) {
  boolean result = super.onCreateOptionsMenu(menu);
  // Add a button to menu
  menu.add(0, SETTINGS, 0, R.string.preferenceSettings).setIcon(R.drawable.ic_menu_preferences);
  menu.add(0, ABOUT, 0, R.string.aboutString).setIcon(android.R.drawable.ic_dialog_info);
  return result;
 }

 // THE FIRST MENU
 @Override
 public boolean onOptionsItemSelected(MenuItem item) {
  switch (item.getItemId()) {
  // if "Add new user" button is pressed from the menu
  case ADD_USER:
   addUser();
   return true;
  case SETTINGS:
   final AlertDialog.Builder alert = new AlertDialog.Builder(this);
   // set an alert dialog to prompt the user for their password to
   // login.
   alert.setTitle(R.string.settingsPassDialogTitle);
   alert.setMessage(R.string.settignsScreenPasswordPrompt);
   final EditText passField = new EditText(this);
   passField.setTransformationMethod(PasswordTransformationMethod.getInstance());
   // make it turn into stars, as available from the API.
   alert.setView(passField);
   // show the alert.
   alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	public void onClick(DialogInterface dialog, int whichButton) {
	 // UPON CLICKING "OK" IN THE DIALOG BOX (ALERT)
	 BasicPasswordEncryptor passwordEncryptor = new BasicPasswordEncryptor();
	 String curaPass = prefs.getString("myPass", "");
	 String passfield = passField.getText().toString();
	 // encrypt the given password
	 // if (passfield.compareTo(curaPass) == 0)
	 if(passwordEncryptor.checkPassword(passfield, curaPass))
	  // if it matches the password that we have for
	  // the user, take them to where they should go
	  startActivity(new Intent(LoginScreenActivity.this, PreferenceScreen.class));
	 else
	  // else, prompt for wrong password
	  Toast.makeText(LoginScreenActivity.this, R.string.wrongPassword, Toast.LENGTH_SHORT).show();
	 return;
	}
   });
   alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	// UPON CLICKING "CANCEL" IN THE DIALOG BOX (ALERT)
	public void onClick(DialogInterface dialog, int which) {
	 return;
	}
   });
   final AlertDialog settingsPassAlert = alert.create();
   settingsPassAlert.show();
   passField.addTextChangedListener(new TextWatcher() {

	public void onTextChanged(CharSequence s, int start, int before, int count) {
	 // TODO Auto-generated method stub
	 String pass = passField.getText().toString();
	 if(pass.length() > 0)
	  settingsPassAlert.getButton(Dialog.BUTTON1).setEnabled(true);
	 else
	  if(pass.length() == 0)
	   settingsPassAlert.getButton(Dialog.BUTTON1).setEnabled(false);
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	public void afterTextChanged(Editable s) {
	}
   });
   settingsPassAlert.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
   return true;
  case ABOUT:
   Intent aboutIntent = new Intent(LoginScreenActivity.this, aboutActivity.class);
   startActivity(aboutIntent);
   return true;
  }
  return super.onOptionsItemSelected(item);
 }

 protected void addUser() {
  // display dialog box
  final Dialog myDialog;
  myDialog = new Dialog(LoginScreenActivity.this);
  myDialog.setContentView(R.layout.adduserscreen);
  myDialog.setTitle(R.string.DialogTitle);
  myDialog.setCancelable(true);
  myDialog.setCanceledOnTouchOutside(true);

  // constructed out of all the fields below
  final Button AddUserButton = (Button) myDialog.findViewById(R.id.button1);
  AddUserButton.setEnabled(false);
  Button cancelButton = (Button) myDialog.findViewById(R.id.button2);
  TextWatcher watcher = null;
  final EditText usernameInput = (EditText) myDialog.findViewById(R.id.usernameTextField);

  final EditText domainInput = (EditText) myDialog.findViewById(R.id.domainTextField);

  final EditText portInput = (EditText) myDialog.findViewById(R.id.portTextField);

  final TextView userExists = (TextView) myDialog.findViewById(R.id.serverExists);
  watcher = new TextWatcher() {

   public void afterTextChanged(Editable s) {
	// TODO Auto-generated method stub
   }

   public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	// TODO Auto-generated method stub

   }

   public void onTextChanged(CharSequence s, int start, int before, int count) {
	// TODO Auto-generated method stub
	String username = usernameInput.getText().toString();
	String domain = domainInput.getText().toString();
	String port = portInput.getText().toString();
	if(rv.validateUsername(username) && !domain.equalsIgnoreCase("") && !port.equalsIgnoreCase(""))
	 // if all the textfields are filled, enable the Add
	 // button
	 AddUserButton.setEnabled(true);
	else
	 // else, disable it
	 AddUserButton.setEnabled(false);
   }

  };
  usernameInput.addTextChangedListener(watcher);
  // adding listeners
  domainInput.addTextChangedListener(watcher);
  portInput.addTextChangedListener(watcher);
  // cannot click "Add" if all of the above textfields are empty

  AddUserButton.setOnClickListener(new View.OnClickListener() {
   public void onClick(View v) {
	// get username, domain and port from EditText
	String usern = usernameInput.getText().toString();
	String domain = domainInput.getText().toString();
	int port;
	try {
	 port = Integer.parseInt(portInput.getText().toString());
	}
	catch (Exception e) {
	 port = 22;
	 Toast.makeText(LoginScreenActivity.this, R.string.portError, Toast.LENGTH_LONG).show();
	}
	if(!isFound(usern, domain)) {
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
	 }
	 catch (Exception e) {
	  Log.d("SQL", e.toString());
	 }

	 // close database
	 db.close();
	 dbHelper.close();

	 myDialog.cancel();
	}
	else {
	 LoginScreenActivity.this.vibrator.vibrate(300);
	 userExists.setText(R.string.serverExists);
	 usernameInput.setText("");
	 domainInput.setText("");
	}
   }
  });

  cancelButton.setOnClickListener(new View.OnClickListener() {
   @Override
   public void onClick(View v) {
	// close dialog box
	myDialog.cancel();
   }
  });
  myDialog.show();
 }

 public boolean isFound(String username, String domain) {
  String userValue = "";
  String dom = "";
  // if the same username and domain are found and have already been
  // added, return the result here so that the device can vibrate and
  // display an error to the user
  user = getUser();
  for (int i = 0; i < user.length; i++) {
   userValue = user[i].getUsername();
   dom = user[i].getDomain();
   if(userValue.compareTo(username) == 0 && dom.compareTo(domain) == 0)
	return true;
  }
  return false;
 }

 @Override
 protected void onResume() {
  // TODO Auto-generated method stub
  super.onResume();

 }

 @Override
 protected void onDestroy() {
  super.onDestroy();
  unregisterReceiver(br);
 }
}
