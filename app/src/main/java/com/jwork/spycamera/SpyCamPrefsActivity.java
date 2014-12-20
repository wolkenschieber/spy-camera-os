/* ========================================================================
 * Copyright 2013 Jimmy Halim
 * Licensed under the Creative Commons Attribution-NonCommercial 3.0 license 
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://creativecommons.org/licenses/by-nc/3.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */
package com.jwork.spycamera;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.CamcorderProfile;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import com.jwork.spycamera.utility.ConfigurationUtility;
import com.jwork.spycamera.utility.LogUtility;
import com.jwork.spycamera.utility.Utility;

/**
 * @author Jimmy Halim
 */
public class SpyCamPrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener {
	
	private static final String TAG = SpyCamPrefsActivity.class.getSimpleName();
	private String[][] sizes;
	private ConfigurationUtility config;
	private Preference feedbackPlay;
	private Preference feedbackEmail;
	private Preference aboutSourceCode;
	private Preference aboutChangelog;
	private Preference generalHideFolder;
	private Preference supportDonate;
	private Preference startupBlackMode;
	private Preference shareIt;
	private LogUtility log;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.setting);
		log = LogUtility.getInstance();

		config = ConfigurationUtility.getInstance(this);
		if (config.getVideoQualityList(0)==null) {
			log.w(this, "video quality empty, recreate config");
			config = ConfigurationUtility.createInstance(this);
		}
		
		config.registerOnSharedPreferenceChangeListener(this);
		log.v(this, "onCreate()");
		
		// Set camera available
		int cameraNumber = getIntent().getIntExtra("cameraNumber", 1);
		String[] cameraPrefsOptions = new String[cameraNumber];
		String[] cameraPrefsValues = new String[cameraNumber];
		String[] cameraOptions = getResources().getStringArray(R.array.cameraOptions);
		String[] cameraValues = getResources().getStringArray(R.array.cameraValues);
		for (int i=0;i<cameraNumber;i++) {
			cameraPrefsOptions[i]=cameraOptions[i];
			cameraPrefsValues[i]=cameraValues[i];
		}
		ListPreference listPreferenceCamera = (ListPreference) findPreference("cameraId");
		listPreferenceCamera.setEntries(cameraPrefsOptions);
		listPreferenceCamera.setEntryValues(cameraPrefsValues);

		sizes = new String[cameraNumber][];
		sizes[0] = getIntent().getStringArrayExtra("cameraPreviewSizes0");
		if (sizes.length>1) {
			sizes[1] = getIntent().getStringArrayExtra("cameraPreviewSizes1");
		}

		for (int id=0;id<sizes.length;id++) {
			ListPreference listPreferenceCategory = (ListPreference) findPreference(ConfigurationUtility.PREFS_IMAGE_SIZE+id);
			if (listPreferenceCategory != null && sizes[id]!=null) {
				CharSequence entries[] = new String[sizes[id].length];
				CharSequence entryValues[] = new String[sizes[id].length];
				int i = 0;
				for (String size : sizes[id]) {
					if (size.endsWith("*")) {
						entries[i] = "[High] " + size;
					} else {
						entries[i] = "[Low] " + size;
					}
					entryValues[i] = size;
					i++;
				}
				listPreferenceCategory.setEntries(entries);
				listPreferenceCategory.setEntryValues(entryValues);
			}
		}
		
		if (sizes.length==1) {
			ListPreference listPreferenceCategory = (ListPreference) findPreference(ConfigurationUtility.PREFS_IMAGE_SIZE_1);
			listPreferenceCategory.setEnabled(false);
		}
		
		//Video Quality
		for (int id=0;id<sizes.length;id++) {
			ListPreference listVideoQuality = (ListPreference) findPreference(ConfigurationUtility.PREFS_VIDEO_QUALITY+id);
			try {
				String[] entryValues = config.getVideoQualityList(id).split("#");
				String[] entries = new String[entryValues.length];

				for (int i=0;i<entryValues.length;i++) {
					if (entryValues[i].equals(""+CamcorderProfile.QUALITY_LOW)) {
						entries[i] = "Lowest";
					} else if (entryValues[i].equals(""+CamcorderProfile.QUALITY_QCIF)) {
						entries[i] = "QCIF (176x144)";
					} else if (entryValues[i].equals(""+CamcorderProfile.QUALITY_QVGA)) {
						entries[i] = "QVGA (320x240)";
					} else if (entryValues[i].equals(""+CamcorderProfile.QUALITY_CIF)) {
						entries[i] = "CIF (352x288)";
					} else if (entryValues[i].equals(""+CamcorderProfile.QUALITY_480P)) {
						entries[i] = "480p (720x480)";
					} else if (entryValues[i].equals(""+CamcorderProfile.QUALITY_720P)) {
						entries[i] = "720p (1280x720)";
					} else if (entryValues[i].equals(""+CamcorderProfile.QUALITY_1080P)) {
						entries[i] = "1080p (1920x1080)";
					} else if (entryValues[i].equals(""+CamcorderProfile.QUALITY_HIGH)) {
						entries[i] = "Highest";
					} else {
						entries[i] = "Unknown : " + entryValues[i];
					}
				}
				listVideoQuality.setEntries(entries);
				listVideoQuality.setEntryValues(entryValues);
			} catch (RuntimeException re) {
				log.e(this, re);
				Toast.makeText(this, "Error when loading video quality available", Toast.LENGTH_SHORT).show();
				listVideoQuality.setEnabled(false);
			}
		}

		if (sizes.length==1) {
			ListPreference listPreferenceCategory = (ListPreference) findPreference(ConfigurationUtility.PREFS_VIDEO_QUALITY_1);
			listPreferenceCategory.setEnabled(false);
		}
		
		ListPreference savingPath = (ListPreference) findPreference(ConfigurationUtility.PREFS_SAVING_PATH);
		String external = config.getSavingPathExternal();
		String[] entryValues = new String[external==null?1:2];
		entryValues[0] = config.getSavingPathPrimary();
		if (external!=null) {
			entryValues[1] = config.getSavingPathExternal();
			savingPath.setEntries(R.array.savingPathOptions);
		} else {
			savingPath.setEntries(new String[] {"Phone"});
		}
		savingPath.setEntryValues(entryValues);
		savingPath.setDefaultValue(entryValues[0]);
		
		feedbackPlay = (Preference) findPreference("feedbackPlay");
		feedbackPlay.setOnPreferenceClickListener(this);
		feedbackEmail = (Preference) findPreference("feedbackEmail");
		feedbackEmail.setOnPreferenceClickListener(this);
		aboutSourceCode = (Preference) findPreference("aboutSourceCode");
		aboutSourceCode.setOnPreferenceClickListener(this);
		aboutChangelog = (Preference) findPreference("aboutChangelog");
		aboutChangelog.setOnPreferenceClickListener(this);
		generalHideFolder = (Preference) findPreference("hideFolder");
		generalHideFolder.setOnPreferenceClickListener(this);
		shareIt = (Preference) findPreference("shareIt");
		shareIt.setOnPreferenceClickListener(this);
		supportDonate = (Preference) findPreference("supportDonate");
		supportDonate.setOnPreferenceClickListener(this);
		startupBlackMode = (Preference) findPreference("startupBlackMode");
		startupBlackMode.setOnPreferenceClickListener(this);
		((Preference) findPreference("autoEmailGmailCreate")).setOnPreferenceClickListener(this);
		((Preference) findPreference(ConfigurationUtility.PREFS_IMAGE_ROTATION)).setOnPreferenceClickListener(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		config.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		log.d(this, "onSharedPreferenceChanged(key:"+key+")");
		if (key.equals(ConfigurationUtility.PREFS_CAMERA_ID)) {
			int activeCamera = config.getCurrentCamera();
			if (sizes!=null && sizes[activeCamera]!=null) {
				ListPreference listPreferenceCategory = (ListPreference) findPreference("imageSize");
				if (listPreferenceCategory != null) {
					CharSequence entries[] = new String[sizes[activeCamera].length];
					CharSequence entryValues[] = new String[sizes[activeCamera].length];
					int i = 0;
					for (String size : sizes[activeCamera]) {
						entries[i] = size;
						entryValues[i] = size;
						i++;
					}
					listPreferenceCategory.setEntries(entries);
					listPreferenceCategory.setEntryValues(entryValues);
				}
			}
		} else if (key.toLowerCase(Locale.getDefault()).contains(ConfigurationUtility.PREFS_IMAGE_SIZE.toLowerCase(Locale.getDefault()))) {
			String size = sharedPreferences.getString(key, "");
			if (size.endsWith("*")) {
				final AlertDialog ad = new AlertDialog.Builder(this).create();
				ad.setMessage(getString(R.string.warning_image_resolution));
				ad.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ad.dismiss();
					}
				});
				ad.show();
			}
		} else if (key.toLowerCase(Locale.getDefault()).contains(ConfigurationUtility.PREFS_SAVING_PATH.toLowerCase(Locale.getDefault()))) {
			File directory = new File(config.getSavingPath());
			if (!directory.exists()) {
				directory.mkdir();
			}
			if (!directory.exists() || !directory.canWrite()) {
				final AlertDialog ad = new AlertDialog.Builder(this).create();
				ad.setMessage("Unable to write in directory : " + config.getSavingPath() +"\nCanceling changes");
				ad.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ad.dismiss();
					}
				});
				ad.show();
				config.setSavingPath(config.getSavingPathPrimary());
			}
		} else if (key.toLowerCase(Locale.getDefault()).contains(ConfigurationUtility.PREFS_STARTUP_BLACK_MODE.toLowerCase(Locale.getDefault()))) {
			boolean blackMode = sharedPreferences.getBoolean(key, false);
			if (blackMode) {
				final AlertDialog ad = new AlertDialog.Builder(this).create();
				ad.setMessage(getString(R.string.hint_blackmode));
				ad.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ad.dismiss();
					}
				});
				ad.show();
			}
		} else if (key.toLowerCase(Locale.getDefault()).contains(ConfigurationUtility.PREFS_IMAGE_ROTATION.toLowerCase(Locale.getDefault()))) {
			int rotation = config.getImageRotation();
			if (rotation!=0) {
				final AlertDialog ad = new AlertDialog.Builder(this).create();
				ad.setMessage(getString(R.string.warning_image_rotation));
				ad.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ad.dismiss();
					}
				});
				ad.show();
			}
		} else if (key.toLowerCase(Locale.getDefault()).contains(ConfigurationUtility.PREFS_AUTO_EMAIL_GMAIL_ENABLE.toLowerCase(Locale.getDefault()))) {
			if (sharedPreferences.getBoolean(ConfigurationUtility.PREFS_AUTO_EMAIL_GMAIL_ENABLE, false)) {
				final AlertDialog ad = new AlertDialog.Builder(this).create();
				ad.setMessage(getString(R.string.warning_enable_auto_email));
				ad.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ad.dismiss();
					}
				});
				ad.show();
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference==feedbackPlay) {
			Uri uri = Uri.parse("market://details?id=" + getPackageName());
		    Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
		    try {
		        startActivity(myAppLinkToMarket);
		    } catch (ActivityNotFoundException e) {
		        Toast.makeText(this, "Failed to find Market application", Toast.LENGTH_LONG).show();
		    }
			return true;
		} else if (preference==feedbackEmail) {
			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			String subject = "[Spy Camera OS] Feedback";
			sendIntent.putExtra(Intent.EXTRA_EMAIL,
					new String[] {"jwork.spy.camera.os@gmail.com"});
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
			sendIntent.putExtra(Intent.EXTRA_TEXT, "\n-------------------------------\n"+Utility.getPhoneInfo(this));
			LogUtility log = LogUtility.getInstance();
			log.flushLogging(this);
			File logFile = new File(config.getSavingPathPrimary() + File.separator + "logging.txt"); 
			if (config.isLogging() && logFile.exists()) {
				sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));
			}
			
			sendIntent.setType("message/rfc822");
			startActivity(Intent.createChooser(sendIntent, "Email:"));
			return true;
		} else if (preference==aboutSourceCode) {
			Intent i = new Intent(Intent.ACTION_VIEW, 
					Uri.parse("https://bitbucket.org/jimmod/spycamera"));
			startActivity(i);
			return true;
		} else if (preference==supportDonate) {
			Utility.openDonation(this);
			return true;
		} else if (preference==shareIt) {
			Utility.shareIt(this);
			return true;
		} else if (preference==aboutChangelog) {
			Utility.showChangelogNew(false, this, false);
			return true;
		} else if (preference==generalHideFolder) {
			File file = new File(config.getSavingPath()+"/.nomedia");
			if (config.isHideFolder()) {
				if (!file.exists()) {
					try {
						System.out.println("Create:"+file.createNewFile());
					} catch (IOException e) {
						Log.w(TAG, e);
					}
				}
			} else {
				if (file.exists()) {
					System.out.println("Del:"+file.delete());
				}
			}
			//Rescan media
			log.i(this, "Trigger media scan");
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
			MediaScannerConnection.scanFile(this, new String[]{config.getSavingPathExternal(), config.getSavingPathPrimary()}, null, new OnScanCompletedListener() {
				
				@Override
				public void onScanCompleted(String path, Uri uri) {
					log.d(this, "onScanCompleted(path:"+path+"|uri:"+uri+")");
				}
			});
			return true;
		} else if (preference.getKey().equals("autoEmailGmailCreate")) {
			Uri uri = Uri.parse("https://accounts.google.com/SignUp");
		    Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
		    try {
		        startActivity(myAppLinkToMarket);
		    } catch (ActivityNotFoundException e) {
		        Toast.makeText(this, "Failed to browser application", Toast.LENGTH_LONG).show();
		    }
			return true;
		}
		return false;
	}
}

/**
 * We have found a new way to generate some money from this free app. using this new search tool, we can keep creating apps and give them to you completely free forever! This search is from our search partner and give you access to great web search via a search icon, bookmark link and homepage. You may remove them at your choice. Thanks.
*/