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
package com.jwork.spycamera.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.webkit.WebView;
import android.widget.Toast;

import com.jwork.spycamera.R;

/**
 * @author Jimmy Halim
 */
public class Utility {

	private static final CharSequence MODEL_NAME_HTC_WILDFIRE = "HTC Wildfire";
	private static LogUtility log = LogUtility.getInstance();

	public static String[] cameraSizeSupport(Camera.Parameters cameraParameters, StringBuffer data) {
		List<Size> sizes = cameraParameters.getSupportedPreviewSizes();
		List<Size> sizes2 = cameraParameters.getSupportedPictureSizes();
		List<String> imageList = new ArrayList<String>();
		for (Size size:sizes2) {
			String temp = size.width + "x" + size.height+"*";
			data.append(temp);
			data.append("#");
			imageList.add(temp);
		}
		for (Size size:sizes) {
			if (android.os.Build.MODEL.contains(MODEL_NAME_HTC_WILDFIRE) && size.width==1280) {
				continue;
			}
			String temp = size.width + "x" + size.height;
			data.append(temp);
			data.append("#");
			imageList.add(temp);
		}
		data.delete(data.length()-1, data.length());
		String[] imageArray = new String[imageList.size()];
		return imageList.toArray(imageArray);
	}

	@TargetApi(11)
	public static String[] camcorderProfileSupport(int cameraId, StringBuffer data) {
		ArrayList<String> list = new ArrayList<String>();

		if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB || CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW)) {
			list.add(""+CamcorderProfile.QUALITY_LOW);
			data.append(""+CamcorderProfile.QUALITY_LOW);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF)) {
			list.add(""+CamcorderProfile.QUALITY_QCIF);
			data.append("#"+CamcorderProfile.QUALITY_QCIF);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 && CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA)) {
			list.add(""+CamcorderProfile.QUALITY_QVGA);
			data.append("#"+CamcorderProfile.QUALITY_QVGA);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_CIF)) {
				list.add(""+CamcorderProfile.QUALITY_CIF);
				data.append("#"+CamcorderProfile.QUALITY_CIF);
			}
			if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
				list.add(""+CamcorderProfile.QUALITY_480P);
				data.append("#"+CamcorderProfile.QUALITY_480P);
			}
			if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
				list.add(""+CamcorderProfile.QUALITY_720P);
				data.append("#"+CamcorderProfile.QUALITY_720P);
			}
			if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
				list.add(""+CamcorderProfile.QUALITY_1080P);
				data.append("#"+CamcorderProfile.QUALITY_1080P);
			}
		}
		if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB || CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH)) {
			list.add(""+CamcorderProfile.QUALITY_HIGH);
			data.append("#"+CamcorderProfile.QUALITY_HIGH);
		}
		String[] qualities = new String[list.size()];
		list.toArray(qualities);
		return qualities;
	}

	public static String createErrorReport(Throwable ex, Activity activity, String additionalInfo) {
		StringBuffer report = new StringBuffer();
		StackTraceElement[] arr = ex.getStackTrace();
		report.append("Please write description. ");
		report.append("Example: The app crashed when I just start the application or It crash when I press 'capture' button");
		report.append("\n\n-------------------------------\n");
		report.append(ex.toString()+"\n\n");
		report.append("--------- Stack trace ---------\n");
		for (int i=0; i<arr.length; i++)
		{
			report.append("    "+arr[i].toString()+"\n");
		}
		report.append("-------------------------------\n\n");

		report.append("--------- Cause ---------\n");
		Throwable cause = ex.getCause();
		if(cause != null) {
			report.append(cause.toString() + "\n");
			arr = cause.getStackTrace();
			for (int i=0; i<arr.length; i++)
			{
				report.append("    "+arr[i].toString()+"\n");
			}
		}
		report.append("-------------------------------\n\n");
		report.append(getPhoneInfo(activity));
		report.append("-------------------------------\n");
		report.append(additionalInfo+"\n");
		report.append("-------------------------------\n");
		return report.toString();
	}

	public static String getPhoneInfo(Context activity) {
		StringBuffer info = new StringBuffer();
		info.append("App version: " + activity.getString(R.string.app_versionName) + "(" + getVersion(activity) + ")" + '\n');
		info.append("Phone Model: " + android.os.Build.MODEL + '\n');
		info.append("Android Version: " + android.os.Build.VERSION.RELEASE + '\n');
		info.append("Board: " + android.os.Build.BOARD + '\n');
		info.append("Brand: " + android.os.Build.BRAND + '\n');
		info.append("Device: " + android.os.Build.DEVICE + '\n');
		info.append("Host: " + android.os.Build.HOST + '\n');
		info.append("ID: " + android.os.Build.ID + '\n');
		info.append("Product: " + android.os.Build.PRODUCT + '\n');
		info.append("Type: " + android.os.Build.TYPE + '\n');
		return info.toString();
	}

	public static int getVersion(Context activity) {
		int v = 0;
		try {
			v = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {}
		return v;
	}

	public static void shareIt(Context context) {
		Intent intentText = new Intent(Intent.ACTION_SEND);
		intentText.setType("text/plain");
		intentText.putExtra(Intent.EXTRA_SUBJECT, "Spy Camera OS (Open Source)"); 
		intentText.putExtra(Intent.EXTRA_TEXT, 
				"Check out Spy Camera OS (Open Source) on Google Play! https://play.google.com/store/apps/details?id="+context.getPackageName());
		try {
			context.startActivity(Intent.createChooser(intentText, "Select your favorite Social Media network"));
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, "Failed to share this application", Toast.LENGTH_LONG).show();
		}
	}

	public static boolean showChangelogNew(boolean check, final Activity activity, boolean resetConfig) {
		final ConfigurationUtility config = ConfigurationUtility.getInstance(activity);
		String version = config.getAppVersion();
		if (!check || (check && !version.equals(activity.getString(R.string.app_versionName)))) {
			if (resetConfig) {
				config.newVersionSetup();
			}

			AlertDialog dialog = new AlertDialog.Builder(activity).create();
			dialog.setTitle("Changelog");

			WebView wv = new WebView(activity.getApplicationContext());
			wv.loadData(activity.getString(R.string.changelog_dialog_text), "text/html", "utf-8");
			wv.setScrollContainer(true);
			dialog.setView(wv);

			dialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					config.setAppVersion(activity.getString(R.string.app_versionName));
					dialog.dismiss();
				}
			});
			dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Rate It", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					config.setAppVersion(activity.getString(R.string.app_versionName));
					dialog.dismiss();
					Uri uri = Uri.parse("market://details?id=" + activity.getPackageName());
					Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
					try {
						activity.startActivity(myAppLinkToMarket);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(activity, "Failed to find Market application", Toast.LENGTH_LONG).show();
					}
				}
			});
			dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Donate", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					config.setAppVersion(activity.getString(R.string.app_versionName));
					dialog.dismiss();
					//					Utility.shareIt(activity);
					openDonation(activity);
				}
			});
			dialog.show();
			return true;
		}
		return false;
	}

	public static String getExternalSDCardPath() {
		BufferedReader br = null;
		String primary = Environment.getExternalStorageDirectory().getPath();
		String result = null;
		try {
			br = new BufferedReader(new FileReader(new File("/etc/vold.fstab")));
			String line;
			while ((line=br.readLine())!=null) {
				line = line.trim();
				if (line.startsWith("#")){
					continue;
				} else {
					String[] data = line.split(" ");
					if (data.length<3) {
						continue;
					}
					if (!data[0].equalsIgnoreCase("dev_mount")) {
						continue;
					}
					if (!data[1].contains("sdcard")) {
						continue;
					}
					if (data[2].equalsIgnoreCase(primary)) {
						continue;
					}
					result = data[2];
					break;
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (Exception e) {}
		}
		log.v(Utility.class, "getExternalSDCardPath():"+result);
		return result;
	}

	public static void setSound(Context context, int mode) {
		log.d(Utility.class, "setSound(mode:"+mode+")");
		if (context!=null) {
			AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			manager.setRingerMode(mode);
		}
	}

	public static int getSound(Context context) {
		log.d(Utility.class, "getSound()");
		AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return manager.getRingerMode();
	}

	public static void openDonation(Context context) {
		Intent i = new Intent(Intent.ACTION_VIEW, 
				Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ZPQBLUCBZ99Z4"));
		context.startActivity(i);
	}

	public static boolean isOnline(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo netInfo = connectivity.getActiveNetworkInfo();
		    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
		        return true;
		    }
		}
		return false;
	}
	
	private static Context context;
	public static void setGlobalContext(Context context) {
		Utility.context = context;
	}
	public static Context getGlobalContext() {
		return context;
	}

}
