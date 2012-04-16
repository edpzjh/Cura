package com.cura;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SplashScreen extends Activity {

	private final int DELAY = 1500;

	private String currentVers = "";
	private String currentDate = "";
	public static String EXT_CURRENT_VERSION = "currentVersion";
	public static String EXT_CURRENT_DATE = "currentDate";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splashscreen);
		// FileUtils.createAppDir();

		// getCurrentVersion();
		// getCurrentTime();
		hideSplashScreen();
	}

	private void hideSplashScreen() {
		new Handler().postDelayed(new Runnable() {
			public void run() {
				/* Create an Intent that will start the Menu-Activity. */
				Intent mainIntent = new Intent(SplashScreen.this,
						LoginScreenActivity.class); // SignUpActivity.class);
				mainIntent.putExtra(EXT_CURRENT_VERSION, currentVers);
				// mainIntent.putExtra(EXT_CURRENT_DATE, currentDate);
				SplashScreen.this.startActivity(mainIntent);
				SplashScreen.this.finish();
			}
		}, DELAY);
	}

	private void getCurrentVersion() {
		try {
			currentVers = getPackageManager().getPackageInfo("com.cura",
					PackageManager.GET_INSTRUMENTATION).versionName;
			Log.d("Current Application version: ", currentVers);
		} catch (NameNotFoundException e) {
			Log.d("IL", e.toString());
		}
	}

	private void getCurrentTime() {
		SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
		currentDate = format.format(new Timestamp(Calendar.getInstance()
				.getTime().getTime()));
	}
}
