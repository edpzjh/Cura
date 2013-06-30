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

public class LoginScreenActivity extends Activity implements android.view.View.OnClickListener {

 private final String connected = "cura.connected";
 private final String notConnected = "cura.not.connected";
 private final int ADD_USER = 1;
 private final int MODIFY_SERVER = 4;
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
  prefs = PreferenceManager.getDefaultSharedPreferences(this);
  rv = new regexValidator();

  selectServer = (Button) findViewById(R.id.selectServer);
  newServer = (Button) findViewById(R.id.newServer);
  selectServer.setOnClickListener(this);
  newServer.setOnClickListener(this);

  br = new BroadcastReceiver() {

   @Override
   public void onReceive(Context context, Intent intent) {
	Bundle extras = intent.getExtras();
	setProgressBarIndeterminateVisibility(false);
	if(extras != null) {
	 userTemp = extras.getParcelable("user");
	}
	if(intent.getAction().compareTo(connected) == 0) {
	 isConnected = true;
	 goToMainActivity = new Intent(LoginScreenActivity.this, CuraActivity.class);
	 goToMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	 goToMainActivity.putExtra("user", userTemp);
	 startActivity(goToMainActivity);
	}
	else {
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
  registerReceiver(br, intentFilter);
  vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
  buttonsLayout = (LinearLayout) findViewById(R.id.ButtonsLayout);
 }

 @Override
 protected void onStart() {
  super.onStart();
  if(isConnected) {
   goToMainActivity = new Intent(LoginScreenActivity.this, CuraActivity.class);
   goToMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
   goToMainActivity.putExtra("user", userTemp);
   startActivity(goToMainActivity);
  }
 }

 @Override
 public void onClick(View arg0) {
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
	 accounts.dismiss();
	 position = arg2;
	 if(user.length == 1 && user[0].getUsername().equalsIgnoreCase("username") && user[0].getDomain().equalsIgnoreCase("domain")) {
	  Toast.makeText(LoginScreenActivity.this, R.string.addServerHint, Toast.LENGTH_LONG).show();
	 }
	 else {
	  AlertDialog.Builder passwordAlert = new AlertDialog.Builder(LoginScreenActivity.this);

	  passwordAlert.setTitle("Login");

	  LayoutInflater li = LayoutInflater.from(LoginScreenActivity.this);
	  View view = li.inflate(R.layout.password_dialog, null);
	  passwordAlert.setView(view);
	  final EditText passField = (EditText) view.findViewById(R.id.passwordprompt);
	  CheckBox showPass = (CheckBox) view.findViewById(R.id.showPassword);
	  showPass.setOnCheckedChangeListener(new OnCheckedChangeListener() {

	   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(isChecked)
		 passField.setTransformationMethod(null);
		else
		 passField.setTransformationMethod(PasswordTransformationMethod.getInstance());
	   }
	  });
	  passwordAlert.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
	   public void onClick(final DialogInterface dialog, int whichButton) {
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
		  userTemp = user[position];
		  passUserObjToService = new Intent(LoginScreenActivity.this, ConnectionService.class);
		  passUserObjToService.putExtra("user", userTemp);
		  passUserObjToService.putExtra("pass", pass);
		  return null;
		 }

		 @Override
		 protected void onPostExecute(String result) {
		  startService(passUserObjToService);
		 }
		};
		task.execute();

	   }
	  });
	  passwordAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	   public void onClick(DialogInterface dialog, int which) {
		return;
	   }
	  });
	  passField.setOnEditorActionListener(new TextView.OnEditorActionListener() {

	   @Override
	   public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
		alert.getButton(Dialog.BUTTON1).performClick();
		return false;
	   }
	  });
	  alert = passwordAlert.create();
	  alert.show();
	  passField.addTextChangedListener(new TextWatcher() {
	   public void onTextChanged(CharSequence s, int start, int before, int count) {
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
	 accounts.dismiss();
	}
   });
   accounts.show();
   break;
  case R.id.newServer:
   addUser();
   break;
  }
 }

 public User[] getUser() {
  dbHelper = new DbHelper(this);
  db = dbHelper.getReadableDatabase();

  Cursor c = db.rawQuery("select * from user", null);
  if(c.getCount() == 0) {
   user = new User[1];
   user[0] = new User("username", "domain", 22);
  }
  else {
   user = new User[c.getCount()];

   int counter = 0;

   if(c != null) {
	if(c.moveToFirst()) {
	 do {
	  String username = c.getString(c.getColumnIndex("username"));
	  String domain = c.getString(c.getColumnIndex("domain"));
	  int port = Integer.parseInt(c.getString(c.getColumnIndex("port")));
	  user[counter] = new User(username, domain, port);
	  counter++;
	 } while(c.moveToNext());
	}
   }
  }
  c.close();
  db.close();
  dbHelper.close();

  return user;
 }

 @Override
 public boolean onCreateOptionsMenu(Menu menu) {
  boolean result = super.onCreateOptionsMenu(menu);
  menu.add(0, MODIFY_SERVER, 0, R.string.ModifyServerInfo).setIcon(android.R.drawable.ic_menu_edit);
  menu.add(0, SETTINGS, 0, R.string.preferenceSettings).setIcon(R.drawable.ic_menu_preferences);
  menu.add(0, ABOUT, 0, R.string.aboutString).setIcon(android.R.drawable.ic_dialog_info);
  return result;
 }

 @Override
 public boolean onOptionsItemSelected(MenuItem item) {
  switch (item.getItemId()) {
  case ADD_USER:
   addUser();
   return true;
  case MODIFY_SERVER:
   user = getUser();
   if(user.length == 1 && user[0].getUsername().equalsIgnoreCase("username") && user[0].getDomain().equalsIgnoreCase("domain"))
	Toast.makeText(this, getString(R.string.noServersFound), Toast.LENGTH_LONG).show();
   else
	startActivity(new Intent(this, AccountsListActivity.class));
   break;
  case SETTINGS:
   final AlertDialog.Builder alert = new AlertDialog.Builder(this);
   alert.setTitle(R.string.settingsPassDialogTitle);
   alert.setMessage(R.string.settignsScreenPasswordPrompt);
   final EditText passField = new EditText(this);
   passField.setTransformationMethod(PasswordTransformationMethod.getInstance());
   alert.setView(passField);
   alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	public void onClick(DialogInterface dialog, int whichButton) {
	 BasicPasswordEncryptor passwordEncryptor = new BasicPasswordEncryptor();
	 String curaPass = prefs.getString("myPass", "");
	 String passfield = passField.getText().toString();
	 if(passwordEncryptor.checkPassword(passfield, curaPass))
	  startActivity(new Intent(LoginScreenActivity.this, PreferenceScreen.class));
	 else
	  Toast.makeText(LoginScreenActivity.this, R.string.wrongPassword, Toast.LENGTH_SHORT).show();
	 return;
	}
   });
   alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	public void onClick(DialogInterface dialog, int which) {
	 return;
	}
   });
   final AlertDialog settingsPassAlert = alert.create();
   settingsPassAlert.show();
   passField.addTextChangedListener(new TextWatcher() {

	public void onTextChanged(CharSequence s, int start, int before, int count) {
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
  final Dialog myDialog;
  myDialog = new Dialog(LoginScreenActivity.this);
  myDialog.setContentView(R.layout.adduserscreen);
  myDialog.setTitle(R.string.DialogTitle);
  myDialog.setCancelable(true);
  myDialog.setCanceledOnTouchOutside(true);

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
   }

   public void beforeTextChanged(CharSequence s, int start, int count, int after) {
   }

   public void onTextChanged(CharSequence s, int start, int before, int count) {
	String username = usernameInput.getText().toString();
	String domain = domainInput.getText().toString();
	String port = portInput.getText().toString();
	if(rv.validateUsername(username) && !domain.equalsIgnoreCase("") && !port.equalsIgnoreCase(""))
	 AddUserButton.setEnabled(true);
	else
	 AddUserButton.setEnabled(false);
   }

  };
  usernameInput.addTextChangedListener(watcher);
  domainInput.addTextChangedListener(watcher);
  portInput.addTextChangedListener(watcher);

  AddUserButton.setOnClickListener(new View.OnClickListener() {
   public void onClick(View v) {
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
	 DbHelper dbHelper = new DbHelper(LoginScreenActivity.this);
	 SQLiteDatabase db = dbHelper.getWritableDatabase();

	 ContentValues values = new ContentValues();

	 values.put(dbHelper.C_USERNAME, usern);
	 values.put(dbHelper.C_DOMAIN, domain);
	 values.put(dbHelper.C_PORT, port);

	 try {
	  db.insertOrThrow(dbHelper.userTableName, null, values);
	 }
	 catch (Exception e) {
	  Log.d("SQL", e.toString());
	 }

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
	myDialog.cancel();
   }
  });
  myDialog.show();
 }

 public boolean isFound(String username, String domain) {
  String userValue = "";
  String dom = "";
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
  super.onResume();

 }

 @Override
 protected void onDestroy() {
  super.onDestroy();
  unregisterReceiver(br);
 }
}
