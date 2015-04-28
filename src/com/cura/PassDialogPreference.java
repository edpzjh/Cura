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
 * Description: This class is used to implement the part of the Settings Activity (Login Screen > Menu > Settings) that 
 * deals with changing the password that allows the user to access the Settings Activity (by default it's "default").
 * 
 * Here we use the "Jasypt" library to encrypt the password with BasicPasswordEncryptor and not not StrongPasswordEncryptor
 * because of a large lagging time that we experienced due to using the latter.
 * More information about this library can be found here: http://www.jasypt.org/
 */

import org.jasypt.util.password.BasicPasswordEncryptor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PassDialogPreference extends DialogPreference {
 private String oldPassword;
 private EditText oldPassED, newPassED, confPassED;
 private SharedPreferences prefs;
 private BasicPasswordEncryptor passwordEncryptor;
 private Context context;

 public PassDialogPreference(Context context, AttributeSet attrs) {
  super(context, attrs);
  this.context = context;
  setDialogLayoutResource(R.layout.prefpassdialog);
  prefs = PreferenceManager.getDefaultSharedPreferences(context);
  oldPassword = prefs.getString("myPass", "");

  passwordEncryptor = new BasicPasswordEncryptor();
 }

 @Override
 protected void onBindDialogView(View view) {
  super.onBindDialogView(view);
  oldPassED = (EditText) view.findViewById(R.id.oldPassEditText);
  newPassED = (EditText) view.findViewById(R.id.newPassEditText);
  confPassED = (EditText) view.findViewById(R.id.confirmPassEditText);
 }

 @Override
 protected void onDialogClosed(boolean positiveResult) {
  super.onDialogClosed(positiveResult);
  persistBoolean(positiveResult);
  if(positiveResult) {
   if(passwordEncryptor.checkPassword(oldPassED.getText().toString(), oldPassword))
	if((newPassED.getText().toString()).compareTo(confPassED.getText().toString()) == 0 && (newPassED.getText().toString()).compareTo("") != 0
	  && (confPassED.getText().toString()).compareTo("") != 0) {
	 SharedPreferences.Editor editor = prefs.edit();
	 editor.putString("myPass", passwordEncryptor.encryptPassword(newPassED.getText().toString()));
	 editor.commit();
	 Toast.makeText(context, R.string.passwordChanged, Toast.LENGTH_SHORT).show();
	}
	else
	 Toast.makeText(context, R.string.wrongConfPass, Toast.LENGTH_SHORT).show();
   else
	Toast.makeText(context, R.string.wrongPassword, Toast.LENGTH_SHORT).show();
  }
 }
}