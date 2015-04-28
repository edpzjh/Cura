/*
 Copyright© 2010, 2011 WJHolden

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

package com.cura.nmap;

/*
 * Description: This is the implementation of Nmap for Android. Its source can be found here:
 * http://nmap.wjholden.com/src/
 * Nmap (“Network Mapper”) is an open source tool for network exploration and security auditing. It was designed to rapidly
 * scan large networks, although it works fine against single hosts. Nmap uses raw IP packets in novel ways to determine
 * what hosts are available on the network, what services (application name and version) those hosts are offering, what
 * operating systems (and OS versions) they are running, what type of packet filters/firewalls are in use, and dozens
 * of other characteristics. While Nmap is commonly used for security audits, many systems and network administrators
 * find it useful for routine tasks such as network inventory, managing service upgrade schedules, and monitoring host
 * or service uptime.
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cura.LoginScreenActivity;
import com.cura.R;
import com.cura.User;
import com.google.analytics.tracking.android.EasyTracker;

public class NmapActivity extends Activity {

	/* --- view resources --- */
	private TextView mResults, mTarget;
	private EditText mArguments;
	private Button mStart, saveResults;// , mHelp, mShare, mExit;
	private Spinner mCommandSpinner, mOutputSpinner;
	private File NmapDir;
	private FileWriter target;
	private NotificationManager mNotificationManager;

	/*
	 * --- program variables - these need to be backed up for configuration
	 * changes ---
	 */
	private static boolean installationVerified = false;
	private static boolean hasRunOneScan = false;
	public static boolean canGetRoot = false;
	public static String bindir = null;
	public static String outputArgs;

	/* --- program variables that don't need to be backed up --- */
	public static int scanType;
	private AsyncTask<Void, Void, Void> vTask = null;
	private AsyncTask<String, Void, Void> sTask = null;

	/* --- constants --- */
	private static final int INSTALL_NO_ROOT = 0;
	private static final int INSTALL_ERROR = 1;
	private static final int INSTALL_GOOD = 2;
	private static final int RUN_LINE = 3;
	private static final int RUN_COMPLETE = 4;
	private static final int RUN_ERROR = 5;
	private static final int THREAD_ERROR = 6;
	private static final int SCANTYPE_NMAP = 7;
	private static final int SCANTYPE_NPING = 8;
	private static final int SCANTYPE_NCAT = 9;
	private static final int SCANTYPE_NDIFF = 10;
	private static final String tag = "Nmap";
	/* --- User --- */
	private User user;
	/* --- Menu Items --- */
	private final int SHARE = 11;
	private final int HELP = 12;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nmap);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Bundle extras = getIntent().getExtras();
		user = (User) extras.get("user");
		mResults = (TextView) findViewById(R.id.results);
		mArguments = (EditText) findViewById(R.id.Arguments);
		mTarget = (TextView) findViewById(R.id.Target);
		mTarget.setText(user.getDomain());
		mStart = (Button) findViewById(R.id.Start);
		saveResults = (Button) findViewById(R.id.saveNmapResults);
		NmapDir = new File("/sdcard/Cura/Nmap");

		try {
			int curVersion = getPackageManager().getPackageInfo("com.cura", 0).versionCode;
			// get the current version of Nmap that's installed
			Log.d(tag, "Nmap version: " + curVersion);
		} catch (NameNotFoundException e) {
			Log.d(tag, e.toString());
		}

		if (!installationVerified && vTask == null) {
			vTask = new verifyInstallation().execute();
		}

		mCommandSpinner = (Spinner) findViewById(R.id.CommandSpinner);
		ArrayAdapter<CharSequence> adapterCommand = ArrayAdapter
				.createFromResource(this, R.array.commands,
						android.R.layout.simple_spinner_item);
		adapterCommand
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCommandSpinner.setAdapter(adapterCommand);
		// construct the commands drop-down list

		mCommandSpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
							long arg3) {
						scanType = mCommandSpinner.getSelectedItemPosition()
								+ SCANTYPE_NMAP;
						// when selected, store the type of scan

						if (mCommandSpinner.getSelectedItem().toString().equals("nmap")
								&& !mArguments.getText().toString().contains("--system-dns"))
							mArguments.setText("--system-dns "
									+ mArguments.getText().toString());
						// if nothing was changed in the first screen that was
						// shown to the user, set it as the one that's selected
						Log.d(tag, mCommandSpinner.getSelectedItem().toString()
								+ " selected.");
					}

					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}
				});

		mOutputSpinner = (Spinner) findViewById(R.id.OutputSpinner);
		ArrayAdapter<CharSequence> adapterOutput = ArrayAdapter.createFromResource(
				this, R.array.output, android.R.layout.simple_spinner_item);
		adapterOutput
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mOutputSpinner.setAdapter(adapterOutput);

		mOutputSpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
							long arg3) {
						if (hasRunOneScan) {
							mResults.setText("");
							saveResults.setVisibility(Button.INVISIBLE);
							h.sendEmptyMessage(RUN_COMPLETE);
						}
					}

					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}
				});

		mStart.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				mResults.setText("");
				saveResults.setVisibility(Button.INVISIBLE);
				String s = mTarget.getText().toString() + " "
						+ mArguments.getText().toString();
				if (s == null || s.length() == 0)
					s = "";
				if (sTask == null || sTask.getStatus() == AsyncTask.Status.FINISHED
						|| sTask.getStatus() == AsyncTask.Status.RUNNING) {
					sTask = new scan().execute(s);
				} else {
					if (!sTask.cancel(false)) {
						h.sendEmptyMessage(THREAD_ERROR);
					}
				}
			}
		});

		saveResults.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!NmapDir.exists()) {
					NmapDir.mkdir();
				}
				try {
					Date date = new Date();
					String dateString = date.getMonth() + "_" + date.getDay() + "_"
							+ date.getHours() + "_" + date.getMinutes();
					String fileName = user.getUsername() + "_"
							+ mCommandSpinner.getSelectedItem().toString() + "_"
							+ mOutputSpinner.getSelectedItem().toString() + "_" + dateString
							+ ".txt";
					Log.d("filename", fileName);
					target = new FileWriter("/sdcard/Cura/Nmap/" + fileName);
					target.append(mResults.getText());
					target.flush();
					target.close();
					Toast.makeText(
							NmapActivity.this,
							getString(R.string.nmapResultsSaved) + " \"/Nmap/" + fileName
									+ "\"", Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(NmapActivity.this, R.string.resultsNotSaved,
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, SHARE, 0, R.string.Share).setIcon(
				android.R.drawable.ic_menu_share);
		// creates the options menu that includes "Server Info" and "Logout"
		menu.add(0, HELP, 0, R.string.Help)
				.setIcon(android.R.drawable.ic_menu_help);
		return result;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case SHARE:
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
					"Nmap Scan Results");
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, mResults
					.getText().toString());
			NmapActivity.this.startActivity(emailIntent);
			break;
		case HELP:
			mResults.setText("");
			saveResults.setVisibility(Button.INVISIBLE);
			if (sTask == null || sTask.getStatus() == AsyncTask.Status.FINISHED
					|| sTask.getStatus() == AsyncTask.Status.RUNNING) {
				sTask = new scan().execute("-h");
			} else {
				if (!sTask.cancel(false)) {
					h.sendEmptyMessage(THREAD_ERROR);
				}
			}

			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public static void setBinDir(String b) {
		bindir = new String(b);
	}

	private class scan extends AsyncTask<String, Void, Void> {
		private ProgressDialog pd;
		private String command;

		protected void onPreExecute() {
			switch (NmapActivity.scanType) {
			case SCANTYPE_NMAP:
				command = "nmap ";
				break;
			// if one of the following was chosen, store it as the command and
			// leave a space so that the command can be constructed from it
			case SCANTYPE_NPING:
				command = "nping ";
				break;
			case SCANTYPE_NDIFF:
				command = "ndiff ";
				break;
			case SCANTYPE_NCAT:
				command = "ncat ";
				break;
			}

			command += NmapActivity.outputArgs;
			// add to it whatever was input in the "Output Arguments" textfield
			Log.d(tag, "Selected scan type: " + command + " ("
					+ NmapActivity.scanType + ")");
			// output the scan type complete, for debugging purposes

			pd = new ProgressDialog(NmapActivity.this);
			pd.setMessage("Please wait...");
			pd.show();
			// show the progress dialog
		}

		protected void onPostExecute(Void v) {
			pd.dismiss();
			NmapActivity.hasRunOneScan = true;
			// when finished, finish it and set that the AsyncTask status was
			// finished
		}

		@Override
		protected Void doInBackground(String... params) {
			for (int i = 0; i < params.length; i++)
				Log.d(tag, "Execution Parameters [" + i + "]: " + params[i]);

			if (canGetRoot)
				Log.d(tag, "Getting root...");

			Process p = null;
			// initialize a Process p
			try {
				if (canGetRoot)
					// if it was able to get root privileges...
					p = Runtime.getRuntime().exec("su");
				// issue the command "su"
				else
					// if not..
					p = Runtime.getRuntime().exec("sh");
				// issue the command "sh"
			} catch (IOException e) {
				Message msg = Message.obtain();
				msg.obj = "Unable to start shell: " + e.toString();
				// that's if "sh" command was not able to run, ya3ne ma mishe el
				// shell
				msg.what = RUN_ERROR;
				h.sendMessage(msg);
				return (null);
			}
			if (canGetRoot && p != null)
				// if root was gotten...
				Log.d(tag, "Got root!");

			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader err = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			// this is the way that they send messages to the server, as opposed
			// to our way

			try {
				os.writeBytes("cd " + bindir + "\n");
				os.flush();
				if (params[0].equals("-h"))
					os.writeBytes("./" + command + " -h\n");
				else if (canGetRoot) {
					os.writeBytes("./" + command + params[0] + "\n");
					Log.d(tag, "./" + command + params[0]);
				} else {
					os.writeBytes("./" + command + params[0] + "\n");
					Log.d(tag, "./" + command + params[0]);
				}
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				// Catch the errors first:
				String error, errLine;
				error = errLine = "";
				while ((errLine = err.readLine()) != null) {
					Log.d(tag, errLine);
					error += errLine;
				}
				if (error.length() > 0) {
					Message msg1 = Message.obtain();
					msg1.obj = "Error detected at runtime: " + error;
					msg1.what = RUN_ERROR;
					h.sendMessage(msg1);
				}
			} catch (IOException e) {
				Message msg = Message.obtain();
				msg.obj = "Unable to execute command: " + e.toString();
				msg.what = RUN_ERROR;
				h.sendMessage(msg);
			}

			String line;
			try {
				while ((line = in.readLine()) != null) {
					Message msg = Message.obtain();
					msg.obj = line;
					msg.what = RUN_LINE;
					h.sendMessage(msg);
				}
			} catch (IOException e) {
				Message msg = Message.obtain();
				msg.obj = "Unable to read command output: " + e.toString();
				msg.what = RUN_ERROR;
				h.sendMessage(msg);
			}
			try {
				in.close();
				err.close();
			} catch (IOException e) {
				Message msg = Message.obtain();
				msg.obj = "Unable to close command output: " + e.toString();
				msg.what = RUN_ERROR;
				h.sendMessage(msg);
			}

			p.destroy();

			if (!params[0].equals("-h"))
				h.sendEmptyMessage(RUN_COMPLETE);
			return (null);
		}
	}

	private class verifyInstallation extends AsyncTask<Void, Void, Void> {
		private final String progressMessage = "Verifying installation. Select \"Allow\" if prompted.";
		private String installationResults;
		private ProgressDialog pd;
		private String filenames[] = { "ndiff", "nping", "nmap-services",
				"nmap-mac-prefixes" };

		protected void onPreExecute() {

			File su = new File("/system/bin/su");
			canGetRoot = su.exists();
			if (canGetRoot)
				Log.d(tag, "su command found - will run with root permissions.");
			else
				Log.d(tag,
						"su NOT found - will attempt to run without root permissions.");

			if (canGetRoot)
				NmapActivity.setBinDir("/data/local/bin/");
			else
				try {
					NmapActivity.setBinDir((NmapActivity.this.getPackageManager()
							.getApplicationInfo("com.cura", 0).dataDir + "/bin/"));
				} catch (NameNotFoundException e) {
					Log.d(tag, "Unable to set bindir: " + e.toString());
				}
		}

		protected void onPostExecute(Void v) {
			// pd.dismiss();
			NmapActivity.installationVerified = true;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				// First we build a results folder
				File instdir = new File(NmapActivity.this.getPackageManager()
						.getApplicationInfo("com.cura", 0).dataDir + "/install/");
				NmapActivity.outputArgs = new String(" -oA "
						+ NmapActivity.this.getPackageManager().getApplicationInfo(
								"com.cura", 0).dataDir + "/tmp/scan ");

				Log.d(
						tag,
						"Will attempt to uncompress binaries to "
								+ instdir.getCanonicalPath());

				if (!instdir.exists() && !instdir.mkdir()) {
					Message msg = Message.obtain();
					msg.obj = "Unable to create " + instdir.getCanonicalPath();
					msg.what = INSTALL_ERROR;
					h.sendMessage(msg);
					return (null);
				}

				File tmpdir = new File(NmapActivity.this.getPackageManager()
						.getApplicationInfo("com.cura", 0).dataDir + "/tmp/");
				if (!tmpdir.exists() && !tmpdir.mkdir()) {
					Message msg = Message.obtain();
					msg.obj = "Unable to create " + tmpdir.getCanonicalPath();
					msg.what = INSTALL_ERROR;
					h.sendMessage(msg);
					return (null);
				}

				// We also need /data/local/bin, but that requires root to
				// create.
				Process p = canGetRoot ? Runtime.getRuntime().exec("su") : Runtime
						.getRuntime().exec("sh");
				DataOutputStream os = new DataOutputStream(p.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				BufferedReader err = new BufferedReader(new InputStreamReader(
						p.getErrorStream()));

				int requiredResources[] = { R.raw.ndiff, R.raw.nping,
						R.raw.nmap_services, R.raw.nmap_mac_prefixes };

				if (!(new File(bindir)).exists()) {
					if (canGetRoot) {
						os.writeBytes("mkdir " + bindir.toString() + "\n");
						char[] mkdirError = new char[1024];
						if (err.read(mkdirError) > 0) {
							Log.d(tag, "Unable to create " + bindir.toString());
							Message msg = Message.obtain();
							msg.obj = "Unable to create " + bindir.toString();
							msg.what = INSTALL_ERROR;
							h.sendMessage(msg);
							return (null);
						}
					} else {
						if (!(new File(bindir).mkdir())) {
							Log.d(tag, "Unable to create " + bindir.toString());
							Message msg = Message.obtain();
							msg.obj = "Unable to create " + bindir.toString();
							msg.what = INSTALL_ERROR;
							h.sendMessage(msg);
							return (null);
						}
					}
				}

				os.writeBytes("cd " + instdir + "\n");

				for (int k = 0; k < requiredResources.length; k++) {
					// First we're going to attempt to retrieve from resources.
					if (!(new File(filenames[k]).exists())) {
						InputStream in1 = getResources().openRawResource(
								requiredResources[k]);
						OutputStream out = new FileOutputStream(instdir + "/"
								+ filenames[k]);
						byte[] buf = new byte[8192];
						while (in1.read(buf) > 0) {
							out.write(buf);
						}
						in1.close();
						out.close();
						os.writeBytes("cat " + instdir.toString() + "/" + filenames[k]
								+ " > " + bindir + filenames[k] + "\n");
						Log.d(tag, "cat " + instdir.toString() + "/" + filenames[k] + " > "
								+ bindir + filenames[k]);
						if (canGetRoot) {
							os.writeBytes("chown root.root " + bindir + filenames[k] + "\n");
							Log.d(tag, "chown root.root " + bindir + filenames[k]);
						}
						os.writeBytes("chmod 777 " + bindir + filenames[k] + "\n");
						Log.d(tag, "chmod 777 " + bindir + filenames[k]);
					}
				}

				if (!(new File("ncat").exists())) {
					InputStream in1 = getResources().openRawResource(R.raw.ncat_a);
					InputStream in2 = getResources().openRawResource(R.raw.ncat_b);
					OutputStream out = new FileOutputStream(instdir + "/" + "ncat");
					byte[] buf = new byte[8192];
					while (in1.read(buf) > 0)
						out.write(buf);
					in1.close();
					while (in2.read(buf) > 0)
						out.write(buf);
					in2.close();
					out.close();
					os.writeBytes("cat " + instdir + "/" + "ncat > " + bindir + "ncat\n");
					Log.d(tag, "cat " + instdir + "/" + "ncat > " + bindir + "ncat\n");
					if (canGetRoot) {
						os.writeBytes("chown root.root " + bindir + "ncat\n");
						Log.d(tag, "chown root.root " + bindir + "ncat");
					}
					os.writeBytes("chmod 777 " + bindir + "ncat\n");
					Log.d(tag, "chmod 777 " + bindir + "ncat");
				}

				if (!(new File("nmap").exists())) {
					// nmap is split because resources can only be 1MB
					InputStream in1 = getResources().openRawResource(R.raw.nmap_a);
					InputStream in2 = getResources().openRawResource(R.raw.nmap_b);
					InputStream in3 = getResources().openRawResource(R.raw.nmap_c);
					OutputStream out = new FileOutputStream(instdir + "/" + "nmap");
					byte[] buf = new byte[8192];
					while (in1.read(buf) > 0)
						out.write(buf);
					in1.close();
					while (in2.read(buf) > 0)
						out.write(buf);
					in2.close();
					while (in3.read(buf) > 0)
						out.write(buf);
					in3.close();
					out.close();
					os.writeBytes("cat " + instdir + "/" + "nmap > " + bindir + "nmap\n");
					Log.d(tag, "cat " + instdir + "/" + "nmap > " + bindir + "nmap\n");
					if (canGetRoot) {
						os.writeBytes("chown root.root " + bindir + "nmap\n");
						Log.d(tag, "chown root.root " + bindir + "nmap");
					}
					os.writeBytes("chmod 777 " + bindir + "nmap\n");
					Log.d(tag, "chmod 777 " + bindir + "nmap");
				}

				os.writeBytes("exit\n");
				os.flush();

				String s, e;
				while ((s = in.readLine()) != null) {
					installationResults += s + "\n";
				}
				while ((e = err.readLine()) != null) {
					installationResults += e + "\n";
				}

				os.close();
				in.close();
				err.close();
				p.waitFor();
			} catch (Exception e) {
				Message msg = Message.obtain();
				msg.obj = e.toString();
				msg.what = INSTALL_ERROR;
				h.sendMessage(msg);
				return (null);
			}

			if (installationResults != null && !installationResults.equals("")) {
				Message msg = Message.obtain();
				msg.obj = installationResults;
				msg.what = INSTALL_ERROR;
				h.sendMessage(msg);
			}

			// if you make to here that means there's absolutely no error so
			// blast that message up and keep going.
			h.sendEmptyMessage(INSTALL_GOOD);
			return (null);
		}
	}

	private Handler h = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			AlertDialog.Builder alert = new AlertDialog.Builder(NmapActivity.this);

			Log.d(tag, msg.what + (String) msg.obj);
			switch (msg.what) {
			case INSTALL_NO_ROOT:
			case RUN_ERROR:
			case INSTALL_ERROR:
				// mResults.setText((String) msg.obj);
				// alert.setMessage((String) msg.obj);
				// alert.show();
				break;
			case INSTALL_GOOD:
				// no actions necessary
				break;
			case RUN_LINE:
				mResults.setText(mResults.getText() + "\n" + (String) msg.obj);
				saveResults.setVisibility(Button.VISIBLE);
				break;
			case RUN_COMPLETE:
				try {
					String type = mOutputSpinner.getSelectedItem().toString();
					if (type.equals("Normal"))
						type = new String("nmap");
					else if (type.equals("XML"))
						type = new String("xml");
					else if (type.equals("Grepable"))
						type = new String("gnmap");
					else {
						Log.d(tag,
								"Something went wrong with mOutputSpinner and received type: "
										+ type);
						type = new String("nmap");
					}
					BufferedReader b = new BufferedReader(new FileReader(
							NmapActivity.this.getPackageManager().getApplicationInfo(
									"com.cura", 0).dataDir
									+ "/tmp/scan." + type));
					String l;
					while ((l = b.readLine()) != null) {
						mResults.setText(mResults.getText() + l + "\n");
					}
					saveResults.setVisibility(Button.VISIBLE);
					b.close();
				} catch (FileNotFoundException e) {
					Message msg1 = Message.obtain();
					msg1.obj = e.toString();
					msg1.what = RUN_ERROR;
					h.sendMessage(msg1);
				} catch (NameNotFoundException e) {
					Message msg1 = Message.obtain();
					msg1.obj = e.toString();
					msg1.what = RUN_ERROR;
					h.sendMessage(msg1);
				} catch (IOException e) {
					Message msg1 = Message.obtain();
					msg1.obj = e.toString();
					msg1.what = RUN_ERROR;
					h.sendMessage(msg1);
				}
				break;
			case THREAD_ERROR:
				alert.setMessage("Unable to cancel task.");
				alert.show();
				break;
			default:
				// shouldn't be an unhandled case here
				break;
			}
		}
	};

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (vTask != null)
			vTask.cancel(true);
		if (sTask != null)
			sTask.cancel(true);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		installationVerified = savedInstanceState
				.getBoolean("installationVerified");
		hasRunOneScan = savedInstanceState.getBoolean("hasRunOneScan");
		canGetRoot = savedInstanceState.getBoolean("canGetRoot");
		bindir = savedInstanceState.getString("bindir");
		outputArgs = savedInstanceState.getString("outputArgs");
		if (vTask != null)
			vTask.cancel(true);
		if (sTask != null)
			sTask.cancel(true);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("installationVerified", installationVerified);
		outState.putBoolean("hasRunOneScan", hasRunOneScan);
		outState.putBoolean("canGetRoot", canGetRoot);
		outState.putString("bindir", bindir);
		outState.putString("outputArgs", outputArgs);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		finish();
		EasyTracker.getInstance().activityStop(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			// if the back button is pressed when the user is in this (Cura
			// Activity)
			new AlertDialog.Builder(this).setTitle("Logout Confirmation")
					// confirm logout
					.setMessage(R.string.logoutConfirmationDialog)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							try {
								// close connection
								// conn.close();
								Log.d("Connection", "connection closed");
							} catch (Exception e) {
								Log.d("Connection", e.toString());
							}
							Intent closeAllActivities = new Intent(NmapActivity.this,
									LoginScreenActivity.class);
							// just close everything
							closeAllActivities.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							NmapActivity.this.startActivity(closeAllActivities);

							mNotificationManager.cancelAll();
						}
					}).setNegativeButton("No",
					// if No is selected, dismiss the dialog
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).show();
		}
		return super.onKeyDown(keyCode, event);
	}
}