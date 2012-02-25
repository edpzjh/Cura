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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.cura.R;
import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;

public class SysMonitorActivity extends Activity {

	private static TimeSeries timeSeries;
	private static XYMultipleSeriesDataset dataset;
	private static XYMultipleSeriesRenderer renderer;
	private static XYSeriesRenderer rendererSeries;
	private static GraphicalView view;

	// private static Thread mThread;

	private AsyncTask<String, String, String> thread;
	private CommunicationInterface conn;

	int i = 0;

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			Log.d("ConnectionService", "Connected");
			conn = CommunicationInterface.Stub.asInterface(service);
			// bind to the Connection Service
			sendAndReceive();
			// this is the only place we can issue sendAndReceive() and get
			// output because this is were we are sure that "conn" is not null
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
		// now the thread sendAndReceive can go out as an asyncTask instead of a
		// solitary thread on the main UI thread (good practice)
		thread = new AsyncTask<String, String, String>() {
			String res = "";

			@Override
			protected String doInBackground(String... params) {
				// this is the second method that AsyncTask invokes

				try {
					Log.d("Async", "doInBackground");
					// logs to the LogCat
					res = conn
							.executeCommand("ps aux | awk '{ sum+=$3 } END {print sum }'");
					// this line causes a NullPointerException because "conn" is
					// null..
					// it's null because onServiceConnected() has not yet been
					// called and already the AsyncTask has been executed and
					// went to the server bas mafi conn..
					// stores the output of our command in the "res" String
					return res;
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return "";
			}

			@Override
			protected void onPostExecute(String result) {
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				// this is the final method that AsyncTask invokes
				if (!result.equalsIgnoreCase("")) {
					// if result is not empty
					timeSeries.add(new Date(), Double.parseDouble(result));
					// add it as a point to our graph
					view.repaint();
					// repaint the view
					Log.d("Async", "" + i);
					i++;
					// iterate
				}
			}

			@Override
			protected void onPreExecute() {
				// TODO Auto-generated method stub
				// this is the first method that AsyncTask invokes
				super.onPreExecute();
			}
		};
		thread.execute();
		// run this AsyncTask
	}

	public void doBindService() {
		Intent i = new Intent(this, ConnectionService.class);
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
		renderer.setChartTitleTextSize(15);
		renderer.setFitLegend(true);
		renderer.setGridColor(Color.LTGRAY);
		renderer.setPanEnabled(true, true);
		renderer.setPointSize(10);
		renderer.setXTitle("Time");
		renderer.setYTitle("Number");
		renderer.setYAxisMax(100);
		renderer.setMargins(new int[] { 20, 30, 30, 30 });
		renderer.setZoomButtonsVisible(true);
		renderer.setBarSpacing(10);
		renderer.setShowGrid(true);

		rendererSeries = new XYSeriesRenderer();
		rendererSeries.setColor(Color.RED);
		rendererSeries.setFillPoints(true);
		rendererSeries.setPointStyle(PointStyle.CIRCLE);

		renderer.addSeriesRenderer(rendererSeries);

		timeSeries = new TimeSeries("CPU");

		// mThread = new Thread() {
		// public void run() {
		// while (true) {
		// try {
		// Thread.sleep(1500L);
		// sendAndReceive();
		// // view.repaint();
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// }
		// };
		// mThread.start();
	}

	@Override
	protected void onStart() {
		super.onStart();
		dataset.addSeries(timeSeries);
		view = ChartFactory.getTimeChartView(this, dataset, renderer, "Test");
		view.refreshDrawableState();
		view.repaint();
		setContentView(view);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
}