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

import java.lang.Thread.UncaughtExceptionHandler;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Surface;

import com.jwork.spycamera.utility.ConfigurationUtility;
import com.jwork.spycamera.utility.CrashHandler;
import com.jwork.spycamera.utility.LogUtility;
import com.jwork.spycamera.utility.Utility;

/**
 * @author Jimmy Halim
 */
public class SpyCamActivity extends FragmentActivity {

	public static final String[] ACTION_WIDGET = new String[] {
		"WIDGET_LAUNCH_APP", "WIDGET_SINGLE_IMAGE", "WIDGET_AUTO_MODE", "WIDGET_FACE_MODE", "WIDGET_VIDEO_MODE"};

	private LogUtility log;
	private MainFragment fragment;
	private UncaughtExceptionHandler defaultUEH;
	private CrashHandler crashHandler;
	private int defaultOrientation;
	
	public SpyCamActivity() {
//		Factory.reset();
		log = LogUtility.getInstance();
		log.v(this, "constructor()");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    int v = 0;
	    try {
	        v = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
	    } catch (NameNotFoundException e) {}
		log.v(this, "onCreate(): " +  this.getString(R.string.app_versionName) + "(" + v + ")");
		Utility.setGlobalContext(this);
		
		defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		crashHandler = CrashHandler.getInstance(this, defaultUEH);
		Thread.setDefaultUncaughtExceptionHandler(crashHandler);
		setContentView(R.layout.activity_main);
		fragment = (MainFragment)getSupportFragmentManager().findFragmentById(R.id.fragmentMain);

		getDefaultOrientation();
		String action = getIntent().getAction();
		log.d(this, "action:"+action);
		if (action==null || action.equals("android.intent.action.MAIN") || action.equals(ACTION_WIDGET[0])) {
			fragment.setVisible();
		} else {
			for (int i=1;i<ACTION_WIDGET.length;i++) {
				if (action.equals(ACTION_WIDGET[i])) {
					if (ConfigurationUtility.getInstance(this).isDisableBackgroundService()) {
						fragment.setVisible();
					} else {
						fragment.setVisibleForWidget();
					}
					fragment.callWidgetAction(i);
					return;
				}
			}
			fragment.setVisible();
		}
	}


	private void getDefaultOrientation() {
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		//If Naturally landscape (tablets)
		log.v(this, "Display pixels: " + dm.widthPixels + "x" + dm.heightPixels + "|Rotation:" + rotation);
		if (
				((rotation==Surface.ROTATION_0 || rotation==Surface.ROTATION_180) &&  dm.widthPixels>dm.heightPixels)
				|| ((rotation==Surface.ROTATION_90 || rotation==Surface.ROTATION_270) &&  dm.widthPixels<dm.heightPixels)
				) {
			rotation += 1;
			if (rotation>3) {
				rotation = 0;
			}
		}
		switch (rotation) {
		case Surface.ROTATION_0: 
			defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			break;
		case Surface.ROTATION_90: 
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
			} else {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				setRequestedOrientation(defaultOrientation);
			}
			break;
		case Surface.ROTATION_180: 
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
			} else {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				setRequestedOrientation(defaultOrientation);
			}
			break;
		case Surface.ROTATION_270: 
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
			} else {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				setRequestedOrientation(defaultOrientation);
			}
			break;
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		log.v(this, "onResume()");
		super.onResume();
	}

	@Override
	protected void onPause() {
		log.v(this, "onPause()");
		super.onPause();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (defaultUEH!=null) {
			Thread.setDefaultUncaughtExceptionHandler(defaultUEH);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		log.d(this, "onConfigurationChanged");
		setRequestedOrientation(defaultOrientation);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		log.v(this, "onKeyDown(keycode:"+keyCode+")");
		if (fragment.onKeyDown(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		log.v(this, "onActivityResult(requestCode:"+requestCode+"|resultCode:"+resultCode+")");
	}
	
}
