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

package com.cura.about;

/*
 * Description: This activity includes the About section of the application which is listed under the Menus option in the 
 * Login Screen. It mentions the Author of this application, the application's version and its Changelog, the License which 
 * this application is under, a means of e-mailing and the developers and a link to the application's website.
 */

import java.util.Vector;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class aboutActivity extends ListActivity {
	
	private final int LICENSE = 5;
	private final int EMAIL = 7;
	private final int WEBSITE = 8;
	private aboutAdapter aboutAdapter;
	private Vector<AboutClass> info = new Vector<AboutClass>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		fillInfotoVector();
		aboutAdapter = new aboutAdapter(aboutActivity.this, info);
		setListAdapter(aboutAdapter);
		ListView list = getListView();
		list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {

				switch (position) {
				case LICENSE:
					Intent licenseIntent = new Intent(aboutActivity.this,
							LicenseActivity.class);
					startActivity(licenseIntent);
					break;
				case EMAIL:
					Intent emailIntent = new Intent(
							android.content.Intent.ACTION_SEND);

					String EmailValue[] = { "cura.app@gmail.com" };

					emailIntent.setType("plain/text");
					emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
							EmailValue);
					emailIntent.putExtra(android.content.Intent.EXTRA_CC, "");
					emailIntent.putExtra(android.content.Intent.EXTRA_BCC, "");
					emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
							"");
					emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");
					startActivity(emailIntent);
					break;
				case WEBSITE:
					Uri uriUrl = Uri.parse("http://odaym.github.com/Cura/");
					Intent launchBrowser = new Intent(Intent.ACTION_VIEW,
							uriUrl);
					startActivity(launchBrowser);
					break;
				}
			}
		});
	}

	protected void fillInfotoVector() {
		// SECTION SEPARATOR
		AboutClass ab = new AboutClass("separator", "Application Information");
		info.add(ab);
		// STARTING WITH AUTHOR
		ab = new AboutClass("Author", "TTCO Development Team");
		info.add(ab);
		// VERSION NUMBER
		ab = new AboutClass("Version", "1.4");
		info.add(ab);
		// LAST UPDATE
		ab = new AboutClass("Last Update", "20/06/2012");
		info.add(ab);
		// Changelog
		ab = new AboutClass("Changelog", "See the changelog of the app");
		info.add(ab);
		// LICENSE
		ab = new AboutClass("License", "Affero General Public License v3");
		info.add(ab);
		// SECTION SEPARATOR
		ab = new AboutClass("separator", "Contact Information");
		info.add(ab);
		// Email
		ab = new AboutClass("E-mail", "E-mail us");
		info.add(ab);
		// Website
		ab = new AboutClass("Website", "Visit our website");
		info.add(ab);
	}
}