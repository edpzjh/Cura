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
package com.cura.maps;

/*
 * Description: In this activity, we offer Google Maps access so the user would be able to view their own location and the
 * location of the server they're connected to when accessing this Module. Added to which, an option menu provides the user
 * with the ability to view the location of all the other servers that they've added to Cura.
 */

import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Message;
import android.provider.Settings;
import android.widget.Toast;

import com.cura.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MapsActivity extends MapActivity {
	public static final String TAG = "GoogleMapsActivity";
	private LocationManager locationManager;
	Geocoder geocoder;
	Location location;
	LocationListener locationListener;
	CountDownTimer locationtimer;
	MapController mapController;
	MapOverlay mapOverlay = new MapOverlay();

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.maps);
		enableGPS();

		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setSatellite(true);
		mapController = mapView.getController();
		mapController.setZoom(5);

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (locationManager == null) {
			Toast.makeText(MapsActivity.this, "Location Manager Not Available",
					Toast.LENGTH_SHORT).show();
			return;
		}

		location = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (location == null)
			location = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		if (location != null) {
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			GeoPoint point = new GeoPoint((int) (latitude * 1E6),
					(int) (longitude * 1E6));
			mapController.animateTo(point, new Message());
			mapOverlay.setPointToDraw(point);
			List<Overlay> listOfOverlays = mapView.getOverlays();
			listOfOverlays.clear();
			listOfOverlays.add(mapOverlay);
		}

		locationListener = new LocationListener() {
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

			}

			public void onProviderEnabled(String arg0) {

			}

			public void onProviderDisabled(String arg0) {

			}

			public void onLocationChanged(Location loc) {

			}
		};

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 1000, 10f, locationListener);
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 1000, 10f, locationListener);
		locationtimer = new CountDownTimer(30000, 5000) {
			@Override
			public void onTick(long millisUntilFinished) {
				if (location != null)
					locationtimer.cancel();
			}

			@Override
			public void onFinish() {
				if (location == null) {
				}
			}
		};
		locationtimer.start();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	class MapOverlay extends Overlay {
		private GeoPoint pointToDraw;
		Point screenPts = new Point();

		public void setPointToDraw(GeoPoint point) {
			pointToDraw = point;
		}

		public GeoPoint getPointToDraw() {
			return pointToDraw;
		}

		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
				long when) {
			super.draw(canvas, mapView, shadow);

			// Point screenPts = new Point();
			mapView.getProjection().toPixels(pointToDraw, screenPts);

			Bitmap bmp = BitmapFactory.decodeResource(getResources(),
					R.drawable.pingreen);
			canvas.drawBitmap(bmp, screenPts.x, screenPts.y - 24, null);
			return true;
		}
	}

	public void enableGPS() {
		String provider = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (!provider.contains("gps")) {
			// if gps is disabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			// enable it
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			sendBroadcast(poke);
		}
	}
}