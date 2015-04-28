package com.cura.rate;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.cura.R;

public class AppRater {
 private final static String APP_TITLE = "Cura";
 private final static String APP_PNAME = "com.cura";

 private final static int DAYS_UNTIL_PROMPT = 7;
 private final static int LAUNCHES_UNTIL_PROMPT = 7;

 public static void app_launched(Context mContext) {
  SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
  if(prefs.getBoolean("dontshowagain", false)) {
   return;
  }

  SharedPreferences.Editor editor = prefs.edit();

  // Increment launch counter
  long launch_count = prefs.getLong("launch_count", 0) + 1;
  editor.putLong("launch_count", launch_count);

  // Get date of first launch
  Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
  if(date_firstLaunch == 0) {
   date_firstLaunch = System.currentTimeMillis();
   editor.putLong("date_firstlaunch", date_firstLaunch);
  }

  // Wait at least n days before opening
  if(launch_count >= LAUNCHES_UNTIL_PROMPT) {
   if(System.currentTimeMillis() >= date_firstLaunch + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
	showRateDialog(mContext, editor);
   }
  }

  editor.commit();
 }

 public static void showRateDialog(final Context mContext, final SharedPreferences.Editor editor) {
  final Dialog dialog = new Dialog(mContext);
  dialog.setContentView(R.layout.ratingdialog);
  dialog.setTitle("Rate " + APP_TITLE);
  Button rateButton = (Button) dialog.findViewById(R.id.rateNowBTN);
  Button remindMeButton = (Button) dialog.findViewById(R.id.remindBTN);
  Button noThanksButton = (Button) dialog.findViewById(R.id.nothanksBTN);

  rateButton.setOnClickListener(new OnClickListener() {
   public void onClick(View v) {
	mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
	dialog.dismiss();
   }
  });

  remindMeButton.setOnClickListener(new OnClickListener() {
   public void onClick(View v) {
	dialog.dismiss();
   }
  });

  noThanksButton.setOnClickListener(new OnClickListener() {
   public void onClick(View v) {
	if(editor != null) {
	 editor.putBoolean("dontshowagain", true);
	 editor.commit();
	}
	dialog.dismiss();
   }
  });

  dialog.show();
 }
}