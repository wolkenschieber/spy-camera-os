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

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.CamcorderProfile;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.jwork.spycamera.R;

/**
 * @author Jimmy Halim
 */
public class ConfigurationUtility {

	public static final String PREFS_PREVIEW_SIZE = "previewSize";
	public static final String PREFS_AUTOSHOT_DELAY = "autoshotDelay";
	public static final String PREFS_SHOW_TOAST = "showToast";
	public static final String PREFS_IMAGE_SIZE = "imageSize-";
	public static final String PREFS_IMAGE_SIZE_0 = "imageSize-0";
	public static final String PREFS_IMAGE_SIZE_1 = "imageSize-1";
	public static final String PREFS_CAMERA_ID = "cameraId";
	public static final String PREFS_PREVIEW_ON_AUTOSHOT = "previewDisplayOnAutoCapture";
	public static final String PREFS_CRASHED = "justCrashed";
	public static final String PREFS_APP_VERSION = "appVersion";
	public static final String PREFS_HIDE_FOLDER = "hideFolder";
	public static final String PREFS_VIBRATION = "vibration";
	public static final String PREFS_CAMERA_PREVIEW_SIZES = "cameraPreviewSizes-";
	public static final String PREFS_ERRORREPORT_SETDISPLAYORIENTATION = "errorReportSetDisplayOrientation";
	public static final String PREFS_VIDEO_QUALITY = "videoQuality-";
	public static final String PREFS_VIDEO_QUALITY_0 = "videoQuality-0";
	public static final String PREFS_VIDEO_QUALITY_1 = "videoQuality-1";
	public static final String PREFS_VIDEO_QUALITY_LIST = "videoQualityList-";
	public static final String PREFS_VIDEO_QUALITY_LIST_0 = "videoQualityList-0";
	public static final String PREFS_VIDEO_QUALITY_LIST_1 = "videoQualityList-1";
//	public static final String PREFS_USE_AUTOFOCUS = "useAutoFocus";
	public static final String PREFS_USE_AUTOFOCUS_2 = "useAutoFocus2";
//	public static final String PREFS_OUTOFMEMORY_ISSUE = "outOfMemoryIssue";
	public static final String PREFS_VIDEO_EXPERIMENTAL_NOTICE = "videoExperimentalNotice";
	public static final String PREFS_VIBRATION_TIME = "vibrationTime";
	public static final String PREFS_SAVING_PATH = "savingPath";
	public static final String PREFS_SAVING_PATH_EXTERNAL = "savingPathExternal";
	public static final String PREFS_VOLUME_UP_ACTION = "volUpAction";
	public static final String PREFS_VOLUME_DOWN_ACTION = "volDownAction";
	public static final String PREFS_LOGGING = "logging";
	public static final String PREFS_STARTUP_BLACK_MODE = "startupBlackMode";
	public static final String PREFS_WIDGET_ACTION = "widgetAction-";
	public static final String PREFS_WIDGET_TEXT = "widgetText-";
	public static final String PREFS_IMAGE_ROTATION = "imageRotation";
	public static final String PREFS_YUV_DECODE_ALT = "yuvDecodeAlternate";
	public static final String PREFS_MINIMIZE_EXPERIMENTAL_NOTICE = "minimizeExperimentalNotice";
	public static final String PREFS_DISABLE_BACKGROUND_SERVICE = "disableBackgroundService";
	public static final String PREFS_AUTO_EMAIL_GMAIL_ENABLE = "autoEmailGmailEnable";
	public static final String PREFS_AUTO_EMAIL_GMAIL_RECEIVER = "autoEmailGmailReceiver";
	public static final String PREFS_AUTO_EMAIL_GMAIL_USERNAME = "autoEmailGmailSenderUsername";
	public static final String PREFS_AUTO_EMAIL_GMAIL_PASSWORD = "autoEmailGmailSenderPassword";
	public static final String PREFS_STARTAPP_LASTINIT = "startappLastInit";
	
	private static final String[] PREFS_CRASH_RESET = new String[] {
		PREFS_PREVIEW_SIZE, PREFS_IMAGE_SIZE_0, PREFS_IMAGE_SIZE_1, PREFS_CAMERA_ID
		, PREFS_VIDEO_QUALITY_0, PREFS_VIDEO_QUALITY_1, PREFS_VIDEO_QUALITY_LIST_0, PREFS_VIDEO_QUALITY_LIST_1
		, PREFS_STARTUP_BLACK_MODE
	};
	
	public static ConfigurationUtility instance;
	private SharedPreferences prefs;
	private Context context;
	private LogUtility log;
	
	private ConfigurationUtility(Context context) {
		this.context = context;
		PreferenceManager.setDefaultValues(context, R.xml.setting, false);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
		this.log = LogUtility.getInstance();
		
		if (getSavingPath()==null) {
			setSavingPath(getSavingPathPrimary());
		}
	}
	
	public synchronized static ConfigurationUtility getInstance(Context context) {
		if (instance==null) {
			instance = new ConfigurationUtility(context);
		}
		return instance;
	}
	
	public synchronized static ConfigurationUtility createInstance(Context context) {
		return new ConfigurationUtility(context);
	}
	
	
	private Integer previewWidthSize = null;
	public int getPreviewWidthSize() {
		if (previewWidthSize==null) {
			previewWidthSize = prefs.getInt(PREFS_PREVIEW_SIZE, 150); 
		}
		log.v(this, "getPreviewWidthSize():"+previewWidthSize);
		return previewWidthSize;
	}
	public void setPreviewWidthSize(int width) {
		log.v(this, "setPreviewWidthSize(width:"+width+")");
		prefs.edit().putInt(PREFS_PREVIEW_SIZE, width).commit();
		previewWidthSize = width;
	}

	public int getCurrentCamera() {
		int data = 0;
		String temp = prefs.getString(PREFS_CAMERA_ID, "0");
		try {
			data = Integer.parseInt(temp);
		} catch (NumberFormatException e) {
		}
		log.v(this, "getCurrentCamera():"+data);
		return data;
	}
	public void setCurrentCamera(int cameraId) {
		log.v(this, "setCurrentCamera(cameraId:"+cameraId+")");
		prefs.edit().putString(PREFS_CAMERA_ID, ""+cameraId).commit();
	}

	public String getCameraPreviewSizes(int cameraId) {
		String data = prefs.getString(PREFS_CAMERA_PREVIEW_SIZES+cameraId, null);
		log.v(this, "getCameraPreviewSizes(cameraId:"+cameraId+"):"+data);
		return data;
	}
	public void setCameraPreviewSizes(int cameraId, String data) {
		log.v(this, "setCameraPreviewSizes(cameraId:"+cameraId+"|data:"+data+")");
		prefs.edit().putString(PREFS_CAMERA_PREVIEW_SIZES+cameraId, data).commit();
	}

	public String getVideoQualityList(int cameraId) {
		String data = prefs.getString(PREFS_VIDEO_QUALITY_LIST+cameraId, null);
		log.v(this, "getVideoQualityList(cameraId:"+cameraId+"):"+data);
		return data;
	}
	public void setVideoQualityList(int cameraId, String data) {
		log.v(this, "setVideoQualityList(cameraId:"+cameraId+"|data:"+data+")");
		prefs.edit().putString(PREFS_VIDEO_QUALITY_LIST+cameraId, data).commit();
	}
	
	public String getImageCaptureSize(int cameraId) {
		String data = prefs.getString(PREFS_IMAGE_SIZE+cameraId, null);
		log.v(this, "getImageCaptureSize(cameraId:"+cameraId+"):"+data);
		return data;
	}
	public void setImageCaptureSize(int cameraId, String data) {
		log.v(this, "setImageCaptureSize(cameraId:"+cameraId+"|data:"+data+")");
		prefs.edit().putString(PREFS_IMAGE_SIZE+cameraId, data).commit();
	}

	public int getVideoRecordingQuality(int cameraId) {
		String temp = prefs.getString(PREFS_VIDEO_QUALITY+cameraId, ""+CamcorderProfile.QUALITY_HIGH);
		int data = CamcorderProfile.QUALITY_HIGH;
		try {
			data = Integer.parseInt(temp);
		} catch (NumberFormatException e) {}
		log.v(this, "getVideoRecordingQuality(cameraId:"+cameraId+"):"+data);
		return data;
	}
	public void setVideoRecordingQuality(int cameraId, int data) {
		log.v(this, "setVideoRecordingQuality(cameraId:"+cameraId+"|data:"+data+")");
		prefs.edit().putString(PREFS_VIDEO_QUALITY+cameraId, ""+data).commit();
	}

//	public boolean isHaveOutOfMemoryIssue() {
//		boolean data = prefs.getBoolean(PREFS_OUTOFMEMORY_ISSUE, false);
//		log.v(this, "isHaveOutOfMemoryIssue():"+data);
//		return data;
//	}

//	public void setOutOfMemoryIssue() {
//		log.v(this, "setOutOfMemoryIssue()");
//		prefs.edit().putBoolean(PREFS_OUTOFMEMORY_ISSUE, true).commit();
//	}

	private Boolean showToast = null;
	public boolean isShowToast() {
		if (showToast==null) {
			showToast = prefs.getBoolean(PREFS_SHOW_TOAST, true);
		}
		log.v(this, "isShowToast():"+showToast);
		return showToast;
	}

	public void clear(boolean isUncaughtExc) {
		Editor editor = prefs.edit();
		for (String key:PREFS_CRASH_RESET) {
			editor.remove(key);
		}
		if (isUncaughtExc) {
			editor.putBoolean(PREFS_CRASHED, true);
		}
		editor.commit();
	}

	public void putBoolean(String id, boolean data) {
		log.v(this, "putBoolean(id:"+id+"|data:"+data+")");
		prefs.edit().putBoolean(id,data).commit();
	}

	public boolean getBoolean(String id, boolean b) {
		boolean data = prefs.getBoolean(id, b);
		log.v(this, "getBoolean(id"+id+"):"+data);
		return data;
	}

	private Boolean useAutoFocus = null;
	public boolean isUseAutoFocus() {
		if (useAutoFocus==null) {
			useAutoFocus = prefs.getBoolean(PREFS_USE_AUTOFOCUS_2, false);
		}
		log.v(this, "isUseAutoFocus():"+useAutoFocus);
		return useAutoFocus;
	}

	private Integer autoCaptureDelay = null;
	public int getAutoCaptureDelay() {
		if (autoCaptureDelay==null) {
			autoCaptureDelay = 2000;
			try {
				autoCaptureDelay = Integer.parseInt(prefs.getString(PREFS_AUTOSHOT_DELAY, "2000"));
			} catch (NumberFormatException e) {}
		}
		return autoCaptureDelay;
	}

	public boolean isDisplayedVideoExperimentalNotice() {
		boolean data = prefs.getBoolean(PREFS_VIDEO_EXPERIMENTAL_NOTICE, false);
		log.v(this, "isDisplayedVideoExperimentalNotice():"+data);
		return data;
	}

	public void setDisplayVideoExperimentalNotice() {
		log.v(this, "setDisplayVideoExperimentalNotice()");
		prefs.edit().putBoolean(PREFS_VIDEO_EXPERIMENTAL_NOTICE, true).commit();
	}
	
	public boolean isCrashed() {
		boolean data = prefs.getBoolean(PREFS_CRASHED, false);
		log.v(this, "isCrashed():"+data);
		return data;
	}
	
	public void clearCrashed() {
		log.v(this, "clearCrashed()");
		prefs.edit().remove(PREFS_CRASHED).commit();
	}
	
	public String getCrashLogFilePath() {
		return getSavingPathPrimary() + "/stack.trace";
	}

	public String getCrashTypeFilePath() {
		return getSavingPathPrimary() + "/SpyCamera/error.trace";
	}

	private Boolean vibrate = null;
	public boolean isVibrate() {
		if (vibrate==null) {
			vibrate = prefs.getBoolean(PREFS_VIBRATION, true);
		}
		log.v(this, "isVibrate():"+vibrate);
		return vibrate;
	}

	private Long vibrateTime = null;
	public long getVibrateTime() {
		if (vibrateTime==null) {
			vibrateTime = prefs.getLong(PREFS_VIBRATION_TIME, 100);
		}
		log.v(this, "getVibrateTime():"+vibrateTime);
		return vibrateTime;
	}

	public boolean isHideFolder() {
		boolean data = prefs.getBoolean(PREFS_HIDE_FOLDER, false);
		log.v(this, "isHideFolder():"+data);
		return data;
	}

	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		prefs.registerOnSharedPreferenceChangeListener(listener);
	}

	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
	}

	public String getAppVersion() {
		String data = prefs.getString(PREFS_APP_VERSION, "");
		log.v(this, "getAppVersion():"+data);
		return data;
	}

	public void newVersionSetup() {
		log.v(this, "newVersionSetup()");
		Editor editor = prefs.edit();
		editor.remove(PREFS_CAMERA_PREVIEW_SIZES + "0");
		editor.remove(PREFS_CAMERA_PREVIEW_SIZES + "1");
		editor.remove(PREFS_VIDEO_QUALITY_LIST + "0");
		editor.remove(PREFS_VIDEO_QUALITY_LIST + "1");
		editor.remove(PREFS_SAVING_PATH_EXTERNAL);
		editor.remove(PREFS_STARTUP_BLACK_MODE);
		editor.commit();		
	}

	public void setAppVersion(String version) {
		log.v(this, "setAppVersion(version:"+version+")");
		prefs.edit().putString(PREFS_APP_VERSION, version).commit();
	}

	private String savingPath = null;
	public String getSavingPath() {
		if (savingPath==null) {
			savingPath = prefs.getString(PREFS_SAVING_PATH, getSavingPathPrimary());
		}
		log.v(this, "getSavingPath():"+savingPath);
		return savingPath;
	}
	public void setSavingPath(String path) {
		log.v(this, "setSavingPath(path:"+path+")");
		prefs.edit().putString(PREFS_SAVING_PATH, path).commit();
		savingPath = path;
	}
	
	private String savingPathPrimary = null;
	public String getSavingPathPrimary() {
		if (savingPathPrimary==null) {
			savingPathPrimary = Environment.getExternalStorageDirectory().getPath() + "/SpyCamera";
		}
		log.v(this, "getSavingPathPrimary():"+savingPathPrimary);
		return savingPathPrimary;
	}
	
	public String getSavingPathExternal() {
		String data = prefs.getString(PREFS_SAVING_PATH_EXTERNAL, null);
		if (data==null) {
			data = Utility.getExternalSDCardPath();
			if (data!=null) {
				data += "/SpyCamera";
				prefs.edit().putString(PREFS_SAVING_PATH_EXTERNAL, data).commit();
			}
		}
		log.v(this, "getSavingPathExternal():"+data);
		return data;
	}

	public String getVolumeDownAction() {
		String data = prefs.getString(PREFS_VOLUME_DOWN_ACTION, "capture");
		log.v(this, "getVolumeDownAction():"+data);
		return data;
	}

	public String getVolumeUpAction() {
		String data = prefs.getString(PREFS_VOLUME_UP_ACTION, "auto");
		log.v(this, "getVolumeUpAction():"+data);
		return data;
	}

	private Boolean logging = null;
	public boolean isLogging() {
		if (logging==null) {
			logging = prefs.getBoolean(PREFS_LOGGING, true);
		}
		log.v(this, "isLogging():"+logging);
		return logging;
	}

	public boolean isStartupBlackMode() {
		boolean data = prefs.getBoolean(PREFS_STARTUP_BLACK_MODE, false);
		log.v(this, "isStartupBlackMode():"+data);
		return data;
	}

	public void reset() {
		this.autoCaptureDelay = null;
		this.logging = null;
		this.previewWidthSize = null;
		this.savingPath = null;
		this.savingPathPrimary = null;
		this.showToast = null;
		this.useAutoFocus = null;
		this.vibrate = null;
		this.vibrateTime = null;
		this.disableBackgroundService = null;
		this.autoEmailGMailEnable = null;
		this.autoEmailGMailReceiver = null;
		this.autoEmailGMailUsername = null;
		this.autoEmailGMailPassword = null;
	}

	public void setWidgetConfiguration(int id,
			int action, String text) {
		log.v(this, "setWidgetConfiguration(id:"+id+"|action:"+action+"|text:"+text+")");
		prefs.edit().putInt(PREFS_WIDGET_ACTION+id, action).commit();
		prefs.edit().putString(PREFS_WIDGET_TEXT+id, text).commit();
	}

	public void deleteWidgetConfiguration(int id) {
		log.v(this, "clearWidgetConfiguration(id:"+id+")");
		prefs.edit().remove(PREFS_WIDGET_ACTION+id).commit();
		prefs.edit().remove(PREFS_WIDGET_TEXT+id).commit();
	}

	public int getWidgetConfigurationAction(int id) {
		int data = prefs.getInt(PREFS_WIDGET_ACTION+id, -1);
		log.v(this, "getWidgetConfigurationAction(id:"+id+"):"+data);
		return data;	
	}
	
	public String getWidgetConfigurationText(int id) {
		String data = prefs.getString(PREFS_WIDGET_TEXT+id, "default");
		log.v(this, "getWidgetConfigurationText(id:"+id+"):"+data);
		return data;	
	}
	
	public int getImageRotation() {
		int data = 0;
		try {
			data = Integer.parseInt(prefs.getString(PREFS_IMAGE_ROTATION, "0"));
		} catch (NumberFormatException e) {}
		log.v(this, "getImageRotation():"+data);
		return data;	
	}

	public void setImageRotation(int rotation) {
		log.v(this, "setImageRotation(rotation:"+rotation+")");
		prefs.edit().putString(PREFS_IMAGE_ROTATION, ""+rotation).commit();
	}

	public boolean isYUVDecodeAlternate() {
		boolean data = prefs.getBoolean(PREFS_YUV_DECODE_ALT, false);
		log.v(this, "isYUVDecodeAlternate():"+data);
		return data;
	}

	public boolean isDisplayedMinimizeExperimentalNotice() {
		boolean data = prefs.getBoolean(PREFS_MINIMIZE_EXPERIMENTAL_NOTICE, false);
		log.v(this, "isDisplayedMinimizeExperimentalNotice():"+data);
		return data;
	}

	public void setDisplayMinimizeExperimentalNotice() {
		log.v(this, "setDisplayMinimizeExperimentalNotice()");
		prefs.edit().putBoolean(PREFS_MINIMIZE_EXPERIMENTAL_NOTICE, true).commit();
	}
	
	private Boolean disableBackgroundService = null; 
	public boolean isDisableBackgroundService() {
		if (disableBackgroundService==null) {
			disableBackgroundService = prefs.getBoolean(PREFS_DISABLE_BACKGROUND_SERVICE, false);
		} 
		log.v(this, "isDisableBackgroundService():"+disableBackgroundService);
		return disableBackgroundService;
	}

	private Boolean autoEmailGMailEnable = null;
	public boolean isAutoEmailGMailEnabled() {
		if (autoEmailGMailEnable==null) {
			autoEmailGMailEnable = prefs.getBoolean(PREFS_AUTO_EMAIL_GMAIL_ENABLE, false);
		}
		log.v(this, "isAutoEmailGMailEnabled():"+autoEmailGMailEnable);
		return autoEmailGMailEnable;
	}

	private String autoEmailGMailReceiver = null;
	public String getAutoEmailGMailReceiver() {
		if (autoEmailGMailReceiver==null) {
			autoEmailGMailReceiver = prefs.getString(PREFS_AUTO_EMAIL_GMAIL_RECEIVER, "");
		}
		log.v(this, "getAutoEmailGMailReceiver():"+autoEmailGMailReceiver);
		return autoEmailGMailReceiver;
	}

	private String autoEmailGMailUsername = null;
	public String getAutoEmailGMailUsername() {
		if (autoEmailGMailUsername==null) {
			autoEmailGMailUsername = prefs.getString(PREFS_AUTO_EMAIL_GMAIL_USERNAME, "");
		}
//		log.v(this, "getAutoEmailGMailUsername():"+autoEmailGMailUsername);
		return autoEmailGMailUsername;
	}
	
	private String autoEmailGMailPassword = null;
	public String getAutoEmailGMailPassword() {
		if (autoEmailGMailPassword==null) {
			autoEmailGMailPassword = prefs.getString(PREFS_AUTO_EMAIL_GMAIL_PASSWORD, "");
		}
//		log.v(this, "getAutoEmailGMailPassword():"+autoEmailGMailPassword);
		return autoEmailGMailPassword;
	}
	
	public void setStartAppInit() {
		String date = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(new Date());
		log.v(this, "setStartAppInit():"+date);
		prefs.edit().putString(PREFS_STARTAPP_LASTINIT, date).commit();
	}
	
	public boolean isStartAppInitToday() {
		boolean data = false;
		String date = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(new Date());
		if (prefs.getString(PREFS_STARTAPP_LASTINIT, "").equals(date)) {
			data = true;
		}
		log.v(this, "isStartAppInitToday():"+data);
		return data;
	}

}
