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

import java.util.Arrays;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.Toast;

import com.jwork.spycamera.model.FailedProcessData;
import com.jwork.spycamera.utility.ConfigurationUtility;
import com.jwork.spycamera.utility.LogUtility;
import com.jwork.spycamera.utility.Utility;

/**
 * @author Jimmy Halim
 */
public class MainHandler extends Handler {
	
	public static final int WHAT_SET_PREVIEW_IMAGE = 0;
	public static final int WHAT_HIDE_COMPONENT = 1;
	public static final int WHAT_SHOW_COMPONENT = 2;
	public static final int WHAT_DISABLE_COMPONENT = 3;
	public static final int WHAT_SHOW_TOAST = 4;
	public static final int WHAT_SHOW_FAILED_PROCESS = 5;
	public static final int WHAT_HIDE_CONTROL_COMPONENT = 6;
	public static final int WHAT_SHOW_CONTROL_COMPONENT = 7;
	public static final int WHAT_SET_STATE_UI = 8;
	public static final int WHAT_SHOW_VIDEO_RECORDING_EXPERIMENTAL_NOTICE = 9;
	public static final int WHAT_SHOW_CRASH_DIALOG = 10;
	public static final int WHAT_SHOW_MINIMIZE_EXPERIMENTAL_NOTICE = 11;
	private static final int[] IMAGE_AUTO_COMPONENT_SHOWN = new int[] {R.id.btnAuto, R.id.svPreview, R.id.btnIncreaseSize, R.id.btnDecreaseSize, R.id.btnBlack, R.id.btnSetting};
	private static final int[] IMAGE_AUTO_COMPONENT_SHOWN_COMPATIBILITY = new int[] {R.id.btnAuto, R.id.svPreview, R.id.btnIncreaseSize, R.id.btnDecreaseSize, R.id.btnBlack};
	private static final int[] IMAGE_FACE_COMPONENT_SHOWN = new int[] {R.id.btnFace, R.id.svPreview, R.id.btnIncreaseSize, R.id.btnDecreaseSize, R.id.btnBlack, R.id.btnSetting} ;
	private static final int[] IMAGE_FACE_COMPONENT_SHOWN_COMPATIBILITY = new int[] {R.id.btnFace, R.id.svPreview, R.id.btnIncreaseSize, R.id.btnDecreaseSize, R.id.btnBlack} ;
	private static final int[] VIDEO_RECORDING_COMPONENT_SHOWN = new int[] {R.id.btnVideo, R.id.svPreview, R.id.btnIncreaseSize, R.id.btnDecreaseSize, R.id.btnBlack, R.id.btnSetting}; 
	private static final int[] VIDEO_RECORDING_COMPONENT_SHOWN_COMPATIBILITY = new int[] {R.id.btnVideo, R.id.svPreview, R.id.btnIncreaseSize, R.id.btnDecreaseSize, R.id.btnBlack};

	private MainFragment fragment;
	private ConfigurationUtility prefs;
	private LogUtility log;

	public MainHandler(MainFragment fragment) {
		this.fragment = fragment;
		this.prefs = ConfigurationUtility.getInstance(Utility.getGlobalContext());
		this.log = LogUtility.getInstance();
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case WHAT_SET_PREVIEW_IMAGE:
			setPreviewImage(msg.arg1, msg.arg2);
			break;
		case WHAT_HIDE_COMPONENT:
			hideComponent(msg.arg1, msg.arg2);
			break;
		case WHAT_SHOW_COMPONENT:
			showComponent(msg.arg1, msg.arg2);
			break;
		case WHAT_DISABLE_COMPONENT:
			disableComponent(msg.arg1);
			break;
		case WHAT_SHOW_TOAST:
			String tempString = null;
			if (msg.obj instanceof Integer) {
				tempString = fragment.getString((Integer)msg.obj);
			} else {
				tempString = (String)msg.obj;
			}
			showToast(msg.arg1==1?true:false,tempString, msg.arg2);
			break;
		case WHAT_SHOW_FAILED_PROCESS:
			FailedProcessData tempFPD = (FailedProcessData)msg.obj;
			showFailedProcess(tempFPD.getThrowable(), tempFPD.getTitle(), 
					tempFPD.getReport(), tempFPD.getExit(), tempFPD.isForceExit(), tempFPD.getFlag());
			break;
		case WHAT_HIDE_CONTROL_COMPONENT:
			hideControlComponent();
			break;
		case WHAT_SHOW_CONTROL_COMPONENT:
			restoreControlComponent();
			break;
		case WHAT_SET_STATE_UI:
			setStateUI(msg.arg1, msg.arg2, msg.obj!=null);
			break;
		case WHAT_SHOW_VIDEO_RECORDING_EXPERIMENTAL_NOTICE:
			showVideoRecordingExperimentalNotice();
			break;
		case WHAT_SHOW_CRASH_DIALOG:
			showCrashDialog(msg.arg1);
			break;
		case WHAT_SHOW_MINIMIZE_EXPERIMENTAL_NOTICE:
			showMinimizeExperimentalNotice();
			break;
		}
	}
	
	private void showCrashDialog(int type) {
		final AlertDialog ad = new AlertDialog.Builder(Utility.getGlobalContext()).create();
		ad.setTitle("Error Report");
		if (type==1) {
			ad.setMessage("OutOfMemory error was detected.\nImage capture resolution changed to resolutions with *(star) to use less memory.\nNote that this resolution mode can make shutter sound, don't forget to mute your phone");
		} else {
			ad.setMessage("An error was detected. Reporting it will support faster bug fixing.");
		}
		ad.setButton(AlertDialog.BUTTON_POSITIVE, "Report it", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				prefs.clearCrashed();

				Intent intent = new Intent(fragment.getActivity(), SpyCamActivity.class);
				fragment.getActivity().finish();
				fragment.getActivity().startActivity(intent);
				ad.dismiss();
				fragment.getController().sendEmailCrash(true);
			}
		});
		ad.setButton(AlertDialog.BUTTON_NEGATIVE, "Not now", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				prefs.clearCrashed();
				
				Intent intent = new Intent(fragment.getActivity(), SpyCamActivity.class);
				fragment.getActivity().finish();
				fragment.getActivity().startActivity(intent);
				ad.dismiss();
			}
		});
		ad.show();
	}

	private void showVideoRecordingExperimentalNotice() {
		final AlertDialog ad = new AlertDialog.Builder(Utility.getGlobalContext()).create();
		ad.setTitle(R.string.video_recording);
		ad.setMessage(fragment.getString(R.string.video_recording_notice));
		ad.setButton(AlertDialog.BUTTON_POSITIVE, fragment.getString(R.string.i_understand), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				fragment.getController().videoRecording();
			}
		});
		ad.show();
	}

	private void setStateUI(int stateBefore, int stateNew, boolean compatibility) {
		log.v(this, "setStateUI(before:"+stateBefore+"after:"+stateNew+")");
		View view = fragment.getView();
		if (view==null) {
			log.w(this, "Null fragment.getView()");
			return;
		}
		switch (stateNew) {
		case MainController.STATE_IDLE:
			showAllControlComponentExcept(new int[]{});
			((Button)view.findViewById(R.id.btnSetting)).setText(R.string.setting);
			switch (stateBefore) {
			case MainController.STATE_IMAGE_AUTO:
				((Button)view.findViewById(R.id.btnAuto)).setText(R.string.auto);
				break;
			case MainController.STATE_IMAGE_FACE:
				((Button)view.findViewById(R.id.btnFace)).setText(R.string.face);
				break;
			case MainController.STATE_VIDEO_RECORDING:
				((Button)view.findViewById(R.id.btnVideo)).setText(R.string.video);
				break;
			}
			break;
		case MainController.STATE_IMAGE_AUTO:
			if (compatibility) {
				hideAllControlComponentExcept( IMAGE_AUTO_COMPONENT_SHOWN_COMPATIBILITY);
			} else {
				hideAllControlComponentExcept( IMAGE_AUTO_COMPONENT_SHOWN);
			}
			((Button)view.findViewById(R.id.btnAuto)).setText(R.string.stop);
			((Button)view.findViewById(R.id.btnSetting)).setText(R.string.minimize);
			break;
		case MainController.STATE_IMAGE_FACE:
			if (compatibility) {
				hideAllControlComponentExcept(IMAGE_FACE_COMPONENT_SHOWN_COMPATIBILITY);
			} else {
				hideAllControlComponentExcept(IMAGE_FACE_COMPONENT_SHOWN);
			}
			((Button)view.findViewById(R.id.btnFace)).setText(R.string.stop);
			((Button)view.findViewById(R.id.btnSetting)).setText(R.string.minimize);
			break;
		case MainController.STATE_VIDEO_RECORDING:
			if (compatibility) {
				hideAllControlComponentExcept(VIDEO_RECORDING_COMPONENT_SHOWN_COMPATIBILITY);
			} else {
				hideAllControlComponentExcept(VIDEO_RECORDING_COMPONENT_SHOWN);
			}
			((Button)view.findViewById(R.id.btnVideo)).setText(R.string.stop);
			((Button)view.findViewById(R.id.btnSetting)).setText(R.string.minimize);
			break;
		}
		
	}

	private int[] controlList = new int[]{R.id.btnAuto, R.id.btnBlack, R.id.btnCapture, R.id.btnDecreaseSize
			, R.id.btnFace, R.id.btnHelp, R.id.btnIncreaseSize, R.id.btnSetting, R.id.btnSwitchCam
			, R.id.btnVideo, R.id.svPreview};
	private int[] visibilityState = new int[controlList.length];
	private void hideControlComponent() {
		log.v(this, "hideControlComponent()");
		int i = 0;
		for (int id : controlList) {
			View view = fragment.getView().findViewById(id);
			visibilityState[i++] = view.getVisibility();
			view.setVisibility(View.INVISIBLE);
		}
	}
	private void restoreControlComponent() {
		log.v(this, "restoreControlComponent()");
		int i = 0;
		for (int id : controlList) {
			View view = fragment.getView().findViewById(id);
			view.setVisibility(visibilityState[i++]);
		}
	}
	private void hideAllControlComponentExcept(int[] except) {
		log.v(this, "hideAllControlComponentExcept(exceptTotal:"+except.length+")");
		View view = fragment.getView();
		if (view==null) {
			log.w(this, "Null fragment.getView()");
			return;
		}
		Arrays.sort(except);
		for (int id : controlList) {
			if (Arrays.binarySearch(except, id)<0) {
				view.findViewById(id).setVisibility(View.INVISIBLE);
			}
		}
	}
	private void showAllControlComponentExcept(int[] except) {
		log.v(this, "showAllControlComponentExcept(exceptTotal:"+except.length+")");
		View view = fragment.getView();
		if (view==null) {
			log.w(this, "Null fragment.getView()");
			return;
		}
		Arrays.sort(except);
		for (int id : controlList) {
			if (Arrays.binarySearch(except, id)<0) {
				if (id==R.id.btnFace && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					view.findViewById(id).setVisibility(View.INVISIBLE);
				} else if (id==R.id.btnVideo && Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
					fragment.getView().findViewById(id).setVisibility(View.INVISIBLE);
				} else {
					fragment.getView().findViewById(id).setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private void showComponent(int id, int counter) {
		log.v(this, "showComponent(id:"+id+"|c:"+counter+")"+fragment.getView());
		try {
			View view = fragment.getView().findViewById(id);
			view.setVisibility(View.VISIBLE);
			view.bringToFront();
		} catch (NullPointerException e) {
			if (counter<10) {
				Message msg =  new Message();
				msg.what = MainHandler.WHAT_SHOW_COMPONENT;
				msg.arg1 = id;
				msg.arg2 = counter+1;
				this.sendMessageAtTime(msg, 2000);
			}
		}

	}

	private void showToast(boolean forceShow, final String message, final int lengthShort) {
		log.v(this, "showToast(message:"+message+")");
		if (fragment.getActivity()==null || message==null) {
			log.w(this, "null activity("+fragment.getActivity()+")/null message("+message+")");
			return;
		}
		if (forceShow || (prefs.isShowToast() && fragment.getLayoutBlack().getVisibility()!=View.VISIBLE)) {
					Toast.makeText(fragment.getActivity(), message, lengthShort).show();
		}
	}

	private void disableComponent(int id) {
		log.v(this, "disableComponent(id:"+id+")");
		fragment.getView().findViewById(id).setEnabled(false);
	}

	private void hideComponent(int id, int counter) {
		log.v(this, "hideComponent(id:"+id+"|c:"+counter+")");
		try {
			View view = fragment.getView().findViewById(id);
			view.setVisibility(View.INVISIBLE);
		} catch (NullPointerException e) {
			if (counter<10) {
				Message msg =  new Message();
				msg.what = MainHandler.WHAT_HIDE_COMPONENT;
				msg.arg1 = id;
				msg.arg2 = counter+1;
				this.sendMessageAtTime(msg, 2000);
			}
		}
	}

	private void setPreviewImage(int width, int height) {
		log.v(this, "setPreviewImage(width:"+width+",height:"+height+")");
		SurfaceView svPreview = fragment.getSvPreview();
		LayoutParams params = svPreview.getLayoutParams();
		params.width = width;
		params.height = height;
		svPreview.setLayoutParams(params);
		svPreview.invalidate();
	}
	
	private void showFailedProcess(final Throwable ex, String message, String captionSendReport, String captionExit
			, final boolean forceExit, final String prefsString) {
		log.v(this, "showFailedProcess(message:"+message+")");
		final AlertDialog ad = new AlertDialog.Builder(Utility.getGlobalContext()).create();
		ad.setTitle("Error Report");
		ad.setMessage(message);
		ad.setButton(AlertDialog.BUTTON_POSITIVE, captionSendReport, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (prefsString!=null) {
					prefs.putBoolean(prefsString, true);
				}
				fragment.getController().sendEmailCrash(false);
				ad.dismiss();
				if (forceExit) {
					fragment.getActivity().finish();
				}
			}
		});
		ad.setButton(AlertDialog.BUTTON_NEGATIVE, captionExit, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ad.dismiss();
				if (forceExit) {
					fragment.getActivity().finish();
				}
			}
		});
		ad.show();
	}

	public void setFragment(MainFragment fragment) {
		this.fragment = fragment;
	}

	private void showMinimizeExperimentalNotice() {
		final AlertDialog ad = new AlertDialog.Builder(Utility.getGlobalContext()).create();
		ad.setTitle(R.string.minimize);
		ad.setMessage(fragment.getString(R.string.minimize_notice));
		ad.setButton(AlertDialog.BUTTON_POSITIVE, fragment.getString(R.string.i_understand), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				fragment.getController().openSetting();
			}
		});
		ad.show();
	}
	
}
