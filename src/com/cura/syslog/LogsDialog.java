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
package com.cura.syslog;

/*
 * Description: This Activity is for the dialog creation that happens when a user chooses to select a certain chunk of data
 * from a certain file in the SysLog repository available on said server. This pops up a dialog containing the text that the
 * user requested to see.
 */

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.cura.R;

public class LogsDialog extends Activity implements OnClickListener {
	private Button close;
	private TextView t;
	private String Logs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logsdialog);
		close = (Button) findViewById(R.id.closeLogsDialog);
		close.setOnClickListener(this);
		Bundle extra = getIntent().getExtras();
		if (extra != null)
			// grab the contents that were sent along with the intent that lead
			// to this activity
			Logs = extra.getString("LogsResult");
		t = (TextView) findViewById(R.id.logsView);
		// set the text to the display
		t.setText(Logs);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.closeLogsDialog:
			// close
			this.finish();
			break;
		}

	}

}
