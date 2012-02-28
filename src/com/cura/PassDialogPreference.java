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
	// old password to be held
	private EditText oldPassED, newPassED, confPassED;
	// 3 editTexts to appear in the prompt for changing the password
	private SharedPreferences prefs;
	// to be able to access the content in PreferenceScreen
	private Context context;

	public PassDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		setDialogLayoutResource(R.layout.prefpassdialog);
		//set the password dialog from the 
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		oldPassword = prefs.getString("myPass", "");
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		// set the layout of the password dialog
		oldPassED = (EditText) view.findViewById(R.id.oldPassEditText);
		newPassED = (EditText) view.findViewById(R.id.newPassEditText);
		confPassED = (EditText) view.findViewById(R.id.confirmPassEditText);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		persistBoolean(positiveResult);
		if (positiveResult) {
			// if the fields are set
			if ((oldPassED.getText().toString()).compareTo(oldPassword) == 0)
				// and if the old password that was typed in matches the actual
				// oldPassword value
				if ((newPassED.getText().toString()).compareTo(confPassED
						.getText().toString()) == 0
				// if the new password is equal to the password in confirm
				// Password text
						&& (newPassED.getText().toString()).compareTo("") != 0
						// if the new password editText is not empty
						&& (confPassED.getText().toString()).compareTo("") != 0) {
					// if the confirm password editText is not empty
					SharedPreferences.Editor editor = prefs.edit();
					// open preferences
					editor.putString("myPass", newPassED.getText().toString());
					// save the new password
					editor.commit();
					// commit the changes
					Toast.makeText(context, R.string.passwordChanged,
							Toast.LENGTH_SHORT).show();
					// show the password changed successfully dialog
				} else
					Toast.makeText(context, R.string.wrongConfPass,
							Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(context, R.string.wrongPassword,
						Toast.LENGTH_SHORT).show();
			// else show the wrong password dialog
		}
	}
}