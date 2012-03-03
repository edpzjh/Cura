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

import com.cura.Connection.CommunicationInterface;
import com.cura.Connection.ConnectionService;

public class SysMonitorActivity extends Activity {

	private static TimeSeries timeSeriesCPU, timeSeriesRAM;
	private static XYMultipleSeriesDataset dataset;
	private static XYMultipleSeriesRenderer renderer;
	private static XYSeriesRenderer rendererSeriesCPU, rendererSeriesRAM;
	private static GraphicalView view;

	private static Thread mThread;

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
			Log.d ("CPUValue", resultCPU);
			resultRAM = conn
					.executeCommand("ps aux | awk '{sum+=$4} END {print sum}'");
			Log.d ("RAMValue", resultRAM);
			if (!resultCPU.equalsIgnoreCase("")
					&& !resultRAM.equalsIgnoreCase("")) {
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
		renderer.setPanEnabled(true, true);
		renderer.setPointSize(5);
		renderer.setXTitle("Time");
		renderer.setYTitle("Number");
		renderer.setYAxisMax(100);
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

		mThread = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1500);
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
		dataset.addSeries(timeSeriesCPU);
		dataset.addSeries(timeSeriesRAM);
		view = ChartFactory.getTimeChartView(this, dataset, renderer,
				"Consumption");
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