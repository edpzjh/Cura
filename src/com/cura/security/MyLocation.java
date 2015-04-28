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

package com.cura.security;

/*
 * Description: This class is used to determine the user's location using GPS. It's here so it can be easily used later on in
 * other activities when the need to send an SMS with the user's location arises due to the security feature being triggered.
 */

import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class MyLocation {
 Timer timer1;
 LocationManager lm;
 LocationResult locationResult;
 boolean gps_enabled = false;
 boolean network_enabled = false;

 public boolean getLocation(Context context, LocationResult result) {
  locationResult = result;
  if(lm == null)
   lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

  try {
   gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
  }
  catch (Exception ex) {
  }
  try {
   network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
  }
  catch (Exception ex) {
  }

  if(!gps_enabled && !network_enabled)
   return false;

  if(gps_enabled)
   lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
  if(network_enabled)
   lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
  timer1 = new Timer();
  timer1.schedule(new GetLastLocation(), 20000);
  return true;
 }

 LocationListener locationListenerGps = new LocationListener() {
  public void onLocationChanged(Location location) {
   timer1.cancel();
   locationResult.gotLocation(location);
   lm.removeUpdates(this);
   lm.removeUpdates(locationListenerNetwork);
  }

  public void onProviderDisabled(String provider) {
  }

  public void onProviderEnabled(String provider) {
  }

  public void onStatusChanged(String provider, int status, Bundle extras) {
  }
 };

 LocationListener locationListenerNetwork = new LocationListener() {
  public void onLocationChanged(Location location) {
   timer1.cancel();
   locationResult.gotLocation(location);
   lm.removeUpdates(this);
   lm.removeUpdates(locationListenerGps);
  }

  public void onProviderDisabled(String provider) {
  }

  public void onProviderEnabled(String provider) {
  }

  public void onStatusChanged(String provider, int status, Bundle extras) {
  }
 };

 class GetLastLocation extends TimerTask {
  @Override
  public void run() {
   lm.removeUpdates(locationListenerGps);
   lm.removeUpdates(locationListenerNetwork);

   Location net_loc = null, gps_loc = null;
   if(gps_enabled)
	gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
   if(network_enabled)
	net_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

   if(gps_loc != null && net_loc != null) {
	if(gps_loc.getTime() > net_loc.getTime())
	 locationResult.gotLocation(gps_loc);
	else
	 locationResult.gotLocation(net_loc);
	return;
   }

   if(gps_loc != null) {
	locationResult.gotLocation(gps_loc);
	return;
   }
   if(net_loc != null) {
	locationResult.gotLocation(net_loc);
	return;
   }
   locationResult.gotLocation(null);
  }
 }

 public static abstract class LocationResult {
  public abstract void gotLocation(Location location);
 }
}
