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

package com.cura.sysmonitor;

/*
 * Description: This is the SysMonitor module Activity. In here we construct an ongoing chart that tracks down the exact
 * percentages of CPU and RAM usage for a pleasant and accurate server-monitoring experience. The menu options available 
 * for this activity are Pause (where the monitoring pauses at the last fetched values) and Resume (where it resumes).
 */

import java.util.Date;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cura.LoginScreenActivity;
import com.cura.R;
import com.cura.ScreenCapture;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;
import com.google.analytics.tracking.android.EasyTracker;

public class SysMonitorActivity extends Activity {

	private final int PAUSE = 1;
	private final int START = 2;
	private final int SCREENCAPTURE = 3;

	private static TimeSeries timeSeriesCPU, timeSeriesRAM;
	private static XYMultipleSeriesDataset dataset;
	private static XYMultipleSeriesRenderer renderer;
	private static XYSeriesRenderer rendererSeriesCPU, rendererSeriesRAM;
	private static GraphicalView view;

	private static Thread mThread;
	private static boolean state = true;
	private NotificationManager mNotificationManager;
	private CommunicationInterface conn;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			Log.d("ConnectionService", "Connected");
			conn = CommunicationInterface.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName name) {
			conn = null;
			Toast.makeText(SysMonitorActivity.this, "Service Disconnected",
					Toast.LENGTH_LONG);
		}
	};

	public synchronized void sendAndReceive() {
		String resultCPU = "";
		String resultRAM = "";

		try {
			resultCPU = conn
					.executeCommand("ps aux | awk '{sum+=$3} END {print sum}'");
			resultRAM = conn
					.executeCommand("ps aux | awk '{sum+=$4} END {print sum}'");
			if (!resultCPU.equalsIgnoreCase("") && !resultRAM.equalsIgnoreCase("")) {
				if (Double.parseDouble(resultCPU) > 100)
					resultCPU = "100";
				if (Double.parseDouble(resultRAM) > 100)
					resultRAM = "100";
				timeSeriesCPU.add(new Date(), Double.parseDouble(resultCPU));
				timeSeriesRAM.add(new Date(), Double.parseDouble(resultRAM));
				view.repaint();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
		getApplicationContext()
				.bindService(i, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		doBindService();

		dataset = new XYMultipleSeriesDataset();

		renderer = new XYMultipleSeriesRenderer();
		renderer.setAxesColor(Color.BLUE);
		renderer.setAxisTitleTextSize(16);
		renderer.setChartTitle("System Monitor");
		renderer.setChartTitleTextSize(25);
		renderer.setFitLegend(false);
		renderer.setGridColor(Color.LTGRAY);
		renderer.setPanEnabled(true, false);
		renderer.setPointSize(5);
		renderer.setXTitle("Time");
		renderer.setYTitle("Number");
		renderer.setYAxisMax(100);
		renderer.setYAxisMin(0);
		renderer.setMargins(new int[] { 20, 30, 20, 30 });
		renderer.setZoomButtonsVisible(true);
		renderer.setBarSpacing(20);
		renderer.setAntialiasing(true);
		renderer.setShowGrid(true);

		rendererSeriesCPU = new XYSeriesRenderer();
		rendererSeriesCPU.setColor(Color.RED);
		rendererSeriesCPU.setFillPoints(true);
		rendererSeriesCPU.setPointStyle(PointStyle.CIRCLE);

		rendererSeriesRAM = new XYSeriesRenderer();
		rendererSeriesRAM.setColor(Color.GREEN);
		rendererSeriesRAM.setFillPoints(true);
		rendererSeriesRAM.setPointStyle(PointStyle.X);

		renderer.addSeriesRenderer(rendererSeriesCPU);
		renderer.addSeriesRenderer(rendererSeriesRAM);
		timeSeriesCPU = new TimeSeries("CPU");
		timeSeriesRAM = new TimeSeries("RAM");

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

	}

	public void startThread() {
		mThread = new Thread() {
			public void run() {
				while (state) {
					try {
						Thread.sleep(1000);
						sendAndReceive();
					} catch (InterruptedException IE) {
						IE.printStackTrace();
					}
				}
			}
		};
		mThread.start();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, PAUSE, 0, R.string.SysMonitorPause).setIcon(
				android.R.drawable.ic_media_pause);
		menu.add(0, START, 0, R.string.SysMonitorStart).setIcon(
				android.R.drawable.ic_media_play);
		menu.add(0, SCREENCAPTURE, 0, R.string.menuSnapshot).setIcon(
				android.R.drawable.ic_menu_camera);
		return result;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case PAUSE:
			state = false;
			mThread = null;
			return true;
		case START:
			state = true;
			startThread();
			return true;
		case SCREENCAPTURE:
			new AsyncTask<Void, Void, Boolean>() {
				String title = "SysMonitor_Snap_";

				@Override
				protected Boolean doInBackground(Void... params) {
					try {
						ScreenCapture sc = new ScreenCapture();
						Date date = new Date();
						String dateString = date.getMonth() + "_" + date.getDay() + "_"
								+ date.getHours() + "_" + date.getMinutes() + "_"
								+ date.getSeconds();
						title += dateString;
						sc.capture(
								getWindow().getDecorView().findViewById(android.R.id.content),
								title, getContentResolver());
					} catch (Exception ex) {
						return false;
					}
					return true;
				}

				@Override
				protected void onPostExecute(Boolean result) {
					if (result)
						Toast.makeText(SysMonitorActivity.this,
								title + " " + getString(R.string.screenCaptureSaved),
								Toast.LENGTH_LONG).show();
					super.onPostExecute(result);
				}
			}.execute();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		state = false;
		mThread = null;
		getApplicationContext().unbindService(connection);
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		doBindService();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			new AlertDialog.Builder(this).setTitle("Logout Confirmation")
					.setMessage(R.string.logoutConfirmationDialog)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							try {
								Log.d("Connection", "connection closed");
							} catch (Exception e) {
								Log.d("Connection", e.toString());
							}
							Intent closeAllActivities = new Intent(SysMonitorActivity.this,
									LoginScreenActivity.class);
							closeAllActivities.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							SysMonitorActivity.this.startActivity(closeAllActivities);

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

	@Override
	protected void onStart() {
		super.onStart();

		startThread();
		dataset.addSeries(timeSeriesCPU);
		dataset.addSeries(timeSeriesRAM);
		view = ChartFactory
				.getTimeChartView(this, dataset, renderer, "Consumption");
		view.refreshDrawableState();
		view.repaint();
		setContentView(view);
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
}