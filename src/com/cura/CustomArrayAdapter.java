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
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomArrayAdapter extends ArrayAdapter {
	Context context;
	User user[];
	DbHelper dbHelper;
	SQLiteDatabase db;

	@SuppressWarnings("unchecked")
	public CustomArrayAdapter(Context context, User user[]) {
		super(context, R.layout.loginscreen, user);
		this.context = context;
		this.user = user;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.loginscreen, parent, false);

		TextView userAndDomain = (TextView) rowView.findViewById(R.id.label);
		TextView port = (TextView) rowView.findViewById(R.id.label2);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		userAndDomain.setText(user[position].getUsername() + "@"
				+ user[position].getDomain());
		port.setText("Connects through port " + user[position].getPort());

		imageView.setImageResource(R.drawable.usersfoldericon);
		return rowView;
	}
}
