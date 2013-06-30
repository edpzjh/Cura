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

/*
 * Description: In this Activity, we implement the means of talking to the server in a custom Terminal emulator that allows
 * the user to run any command that can be run on a live Linux command line screen. We also add a Favorites Screen here that 
 * will allow the user to add any number of their favorite commands to be executed when they choose one of them from the list.
 */

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cura.DbHelper;
import com.cura.LoginScreenActivity;
import com.cura.R;
import com.cura.ScreenCapture;
import com.cura.User;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;

public class TerminalActivity extends Activity {
 private final int FAVORITES = 1;
 private final int CLEAR_EDITTEXT = 2;
 private final int SCREENCAPTURE = 3;

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

 private GestureDetector gd;
 private NotificationManager mNotificationManager;
 private CommunicationInterface conn;

 private ServiceConnection connection = new ServiceConnection() {
  public void onServiceConnected(ComponentName arg0, IBinder service) {
   conn = CommunicationInterface.Stub.asInterface(service);
   sendAndReceive();
  }

  public void onServiceDisconnected(ComponentName name) {
   conn = null;
   Toast.makeText(TerminalActivity.this, "Service Disconnected", Toast.LENGTH_LONG);
  }
 };

 public void doBindService() {
  Intent i = new Intent(this, ConnectionService.class);
  getApplicationContext().bindService(i, connection, Context.BIND_AUTO_CREATE);
 }

 public void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.terminal);
  doBindService();

  Bundle extras = getIntent().getExtras();
  if(extras != null) {
   userTemp = extras.getParcelable("user");
  }
  if(userTemp.getUsername().compareTo("root") == 0) {
   username = userTemp.getUsername() + "@" + userTemp.getDomain() + ":~# ";
  }
  else {
   username = userTemp.getUsername() + "@" + userTemp.getDomain() + ":~$ ";
  }
  this.setTitle("Welcome to the terminal, " + userTemp.getUsername());
  execute = (Button) findViewById(R.id.executeButton);
  commandLine = (EditText) findViewById(R.id.commandLine);
  commandLine.setOnEditorActionListener(new TextView.OnEditorActionListener() {

   @Override
   public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
	if(actionId == EditorInfo.IME_ACTION_GO) {
	 execute.performClick();
	 return true;
	}
	return false;
   }
  });
  result = (EditText) findViewById(R.id.result);
  result.append(username);
  result.setTextColor(Color.GREEN);
  mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

 }

 public void sendAndReceive() {
  execute.setOnClickListener(new OnClickListener() {
   public void onClick(View arg0) {
	execute.setEnabled(false);
	String command = commandLine.getText().toString();
	result.setTextColor(Color.GREEN);
	result.append(command + "\n");
	String res = "";
	try {
	 res = conn.executeCommand(command);
	}
	catch (Exception e) {
	 Log.d("Terminal", e.toString());
	}
	result.append(res);
	result.append(username);
	execute.setEnabled(true);
	commandLine.setText("");
   }
  });

  favoritesButton = (Button) findViewById(R.id.favoritesButton);

  favoritesButton.setOnClickListener(new OnClickListener() {
   public void onClick(View v) {
	dbHelper = new DbHelper(TerminalActivity.this);
	db = dbHelper.getReadableDatabase();

	Cursor c = db.rawQuery("select * from commandTable", null);
	favoriteCommands = new String[c.getCount()];
	int counter = 0;

	if(c != null) {
	 if(c.moveToFirst()) {
	  do {
	   favoriteCommands[counter] = c.getString(c.getColumnIndex("command"));
	   counter++;
	  } while(c.moveToNext());
	 }
	}
	if(counter == 0) {
	 Toast.makeText(TerminalActivity.this, R.string.noFavoritesFound, Toast.LENGTH_SHORT).show();
	}
	else {
	 AlertDialog.Builder builder = new AlertDialog.Builder(TerminalActivity.this);
	 builder.setTitle("Pick a command");
	 builder.setItems(favoriteCommands, new DialogInterface.OnClickListener() {
	  public void onClick(DialogInterface dialog, int item) {
	   commandLine.append(favoriteCommands[item]);
	  }

	 });
	 builder.show();
	}
	c.close();
	db.close();
	dbHelper.close();
   }
  });
 }

 @Override
 public boolean onCreateOptionsMenu(Menu menu) {
  boolean result = super.onCreateOptionsMenu(menu);
  menu.add(0, FAVORITES, 0, R.string.addNewFavoriteCommand).setIcon(android.R.drawable.ic_input_add);
  menu.add(0, CLEAR_EDITTEXT, 0, R.string.clearTerminal).setIcon(android.R.drawable.ic_notification_clear_all);
  menu.add(0, SCREENCAPTURE, 0, R.string.menuSnapshot).setIcon(android.R.drawable.ic_menu_camera);
  return result;
 }

 @Override
 public boolean onOptionsItemSelected(MenuItem item) {
  switch (item.getItemId()) {
  case FAVORITES:
   AlertDialog.Builder addUser = new AlertDialog.Builder(TerminalActivity.this);
   addUser.setMessage(R.string.addNewCommandToFavoritesprompt);
   final EditText commandText = new EditText(this);
   addUser.setView(commandText);
   addUser.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	public void onClick(DialogInterface dialog, int whichButton) {
	 String command = commandText.getText().toString();
	 addCommandToDatabase(command);
	 return;
	}
   });
   addUser.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	public void onClick(DialogInterface dialog, int which) {

	 return;
	}
   });
   final AlertDialog alert = addUser.create();
   alert.show();
   commandText.addTextChangedListener(new TextWatcher() {
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	 if(commandText.getText().length() > 0)
	  alert.getButton(Dialog.BUTTON1).setEnabled(true);
	 else
	  alert.getButton(Dialog.BUTTON1).setEnabled(false);
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	public void afterTextChanged(Editable s) {
	}
   });
   alert.getButton(Dialog.BUTTON1).setEnabled(false);
   break;
  case CLEAR_EDITTEXT:
   result.setText("");
   if(userTemp.getUsername().compareTo("root") == 0) {
	username = userTemp.getUsername() + "@" + userTemp.getDomain() + ":~# ";
   }
   else {
	username = userTemp.getUsername() + "@" + userTemp.getDomain() + ":~$ ";
   }
   result.append(username);
   break;
  case SCREENCAPTURE:
   new AsyncTask<Void, Void, Boolean>() {
	String title = "Terminal_Snap_";

	@Override
	protected Boolean doInBackground(Void... params) {
	 try {
	  ScreenCapture sc = new ScreenCapture();
	  Date date = new Date();
	  String dateString = date.getMonth() + "_" + date.getDay() + "_" + date.getHours() + "_" + date.getMinutes() + "_" + date.getSeconds();
	  title += dateString;
	  sc.capture(getWindow().getDecorView().findViewById(android.R.id.content), title, getContentResolver());
	 }
	 catch (Exception ex) {
	  return false;
	 }
	 return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
	 if(result)
	  Toast.makeText(TerminalActivity.this, title + " " + getString(R.string.screenCaptureSaved), Toast.LENGTH_LONG).show();
	 super.onPostExecute(result);
	}
   }.execute();
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
   db.insertOrThrow(DbHelper.commandTableName, null, values);
   Toast.makeText(this, "Command added successfully!", Toast.LENGTH_SHORT).show();
  }
  catch (Exception e) {
   Toast.makeText(this, "Command could not be added!", Toast.LENGTH_SHORT).show();
   Log.d("SQL", e.toString());
  }

  db.close();
  dbHelper.close();
 }

 @Override
 protected void onStop() {
  super.onStop();
 }

 @Override
 protected void onDestroy() {
  super.onStop();
  getApplicationContext().unbindService(connection);
 }

 public boolean onDown(MotionEvent e) {
  return false;
 }

 public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
  return false;
 }

 public void onLongPress(MotionEvent e) {
 }

 public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
  return false;
 }

 public void onShowPress(MotionEvent e) {
 }

 public boolean onSingleTapUp(MotionEvent e) {
  return false;
 }

 @Override
 public boolean onKeyDown(int keyCode, KeyEvent event) {
  if((keyCode == KeyEvent.KEYCODE_BACK)) {
   new AlertDialog.Builder(this).setTitle("Logout Confirmation").setMessage(R.string.logoutConfirmationDialog)
	 .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

	  public void onClick(DialogInterface dialog, int which) {
	   try {
		Log.d("Connection", "connection closed");
	   }
	   catch (Exception e) {
		Log.d("Connection", e.toString());
	   }
	   Intent closeAllActivities = new Intent(TerminalActivity.this, LoginScreenActivity.class);
	   closeAllActivities.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	   TerminalActivity.this.startActivity(closeAllActivities);

	   mNotificationManager.cancelAll();
	  }
	 }).setNegativeButton("No", new DialogInterface.OnClickListener() {

	  public void onClick(DialogInterface dialog, int which) {
	   dialog.dismiss();
	  }
	 }).show();
  }
  return super.onKeyDown(keyCode, event);
 }
}