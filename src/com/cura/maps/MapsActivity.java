package com.cura.maps;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.cura.R;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MapsActivity extends MapActivity {
	LocationManager locMgr;
	LocationListener locListener;
	double latitude;
	double longitude;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maps);
		gpsLocation();
		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		List<Overlay> mapOverlays = mapView.getOverlays();
		Drawable drawable = this.getResources().getDrawable(
				R.drawable.androidmarker);
		ItemizedOverlay itemizedoverlay = new ItemizedOverlay(drawable, this);
		

		// GeoPoint point = new GeoPoint(latitude, longitude);
		// OverlayItem overlayitem = new OverlayItem(point, "Hola, Mundo!",
		// "You are here!");
		// itemizedoverlay.addOverlay(overlayitem);
		// mapOverlays.add(itemizedoverlay);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public void gpsLocation() {
		locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locListener = new LocationListener() {

			public void onLocationChanged(Location location) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				Toast.makeText(MapsActivity.this, "" + latitude + longitude,
						Toast.LENGTH_LONG).show();
			}

			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub

			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub

			}

		};
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
				locListener);
	}
}
