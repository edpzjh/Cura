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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cura.R;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;

public class SysMonitorActivity extends Activity {

	private final int PAUSE = 1;
	private final int START = 2;

	private static TimeSeries timeSeriesCPU, timeSeriesRAM;
	private static XYMultipleSeriesDataset dataset;
	private static XYMultipleSeriesRenderer renderer;
	private static XYSeriesRenderer rendererSeriesCPU, rendererSeriesRAM;
	private static GraphicalView view;

	private static Thread mThread;
	private static boolean state = true;
	private CommunicationInterface conn;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			Log.d("ConnectionService", "Connected");
			conn = CommunicationInterface.Stub.asInterface(service);
			// bind to the Connection Service
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			conn = null;
			// when the services is disconnected, null the conn
			// ServiceConnection and display an error message
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
			// the command that fetches the CPU usage percentage
			Log.d("CPUValue", resultCPU);
			resultRAM = conn
					.executeCommand("ps aux | awk '{sum+=$4} END {print sum}'");
			// the command that fetches the RAM usage percentage
			Log.d("RAMValue", resultRAM);
			if (!resultCPU.equalsIgnoreCase("")
					&& !resultRAM.equalsIgnoreCase("")) {
				if (Double.parseDouble(resultCPU) > 100)
					// even if the results go over the 100 mark, floor them so
					// that they stick to the 100% limit
					resultCPU = "100";
				if (Double.parseDouble(resultRAM) > 100)
					resultRAM = "100";
				// if results are not empty
				timeSeriesCPU.add(new Date(), Double.parseDouble(resultCPU));
				timeSeriesRAM.add(new Date(), Double.parseDouble(resultRAM));
				// add them as points to our graph
				view.repaint();
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
		// connect to the SSH service (Connection)
		bindService(i, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		doBindService();
		// bind now to the Connection service

		dataset = new XYMultipleSeriesDataset();
		// this method and the methods below indicate the usage of AChartEngine
		// library to modify the value/settings of each graph line

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

		// the renderer for the CPU percentage
		rendererSeriesCPU = new XYSeriesRenderer();
		rendererSeriesCPU.setColor(Color.RED);
		rendererSeriesCPU.setFillPoints(true);
		rendererSeriesCPU.setPointStyle(PointStyle.CIRCLE);

		// the renderer for the RAM percentage
		rendererSeriesRAM = new XYSeriesRenderer();
		rendererSeriesRAM.setColor(Color.GREEN);
		rendererSeriesRAM.setFillPoints(true);
		rendererSeriesRAM.setPointStyle(PointStyle.X);

		renderer.addSeriesRenderer(rendererSeriesCPU);
		renderer.addSeriesRenderer(rendererSeriesRAM);
		// add these values to the graph
		timeSeriesCPU = new TimeSeries("CPU");
		timeSeriesRAM = new TimeSeries("RAM");

	}

	public void startThread() {
		mThread = new Thread() {
			public void run() {
				while (state) {
					try {
						// hold off for 1 second between each fetch of the
						// CPU/RAM usage percentages
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

	@Override
	protected void onStart() {
		super.onStart();

		startThread();
		// add all of the above variables to the chart and construct it
		dataset.addSeries(timeSeriesCPU);
		dataset.addSeries(timeSeriesRAM);
		view = ChartFactory.getTimeChartView(this, dataset, renderer,
				"Consumption");
		view.refreshDrawableState();
		view.repaint();
		setContentView(view);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		// Add a button to menu
		menu.add(0, PAUSE, 0, R.string.SysMonitorPause).setIcon(
				android.R.drawable.ic_media_pause);
		menu.add(0, START, 0, R.string.SysMonitorStart).setIcon(
				android.R.drawable.ic_media_play);
		return result;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case PAUSE:
			state = false;
			mThread = null;
			return true;
			// when paused
		case START:
			state = true;
			startThread();
			return true;
			// when resumed
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// unbind and set state to false
		state = false;
		mThread = null;
		unbindService(connection);
		finish();
	}
	@Override
	protected void onResume() {
		super.onResume();
		doBindService();
	}
}