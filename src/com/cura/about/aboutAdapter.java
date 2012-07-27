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
 * Description: This class is used to automatically construct a list of information items for the About Activity activity.
 */

import java.util.Vector;

import com.cura.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

@SuppressWarnings("rawtypes")
public class aboutAdapter extends ArrayAdapter {
	Context context;
	Vector<AboutClass> aboutVector;

	@SuppressWarnings("unchecked")
	public aboutAdapter(Context context, Vector aboutV) {
		super(context, R.layout.about, aboutV);
		this.context = context;
		aboutVector = aboutV;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.about, parent, false);
		if (aboutVector.get(position).getTitle().compareTo("separator") == 0) {
			rowView = inflater.inflate(R.layout.seperator, null);

			rowView.setOnClickListener(null);
			rowView.setOnLongClickListener(null);
			rowView.setLongClickable(false);

			final TextView sectionView = (TextView) rowView
					.findViewById(R.id.list_item_section_text);
			sectionView.setText(aboutVector.get(position).getSubtitle());
		} else {
			TextView title = (TextView) rowView.findViewById(R.id.label);
			TextView subTitle = (TextView) rowView.findViewById(R.id.label2);
			title.setText(aboutVector.get(position).getTitle());
			subTitle.setText(aboutVector.get(position).getSubtitle());
		}

		return rowView;
	}
}
