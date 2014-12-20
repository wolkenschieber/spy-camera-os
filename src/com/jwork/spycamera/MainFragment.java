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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.jwork.spycamera.utility.Factory;
import com.jwork.spycamera.utility.LogUtility;
import com.jwork.spycamera.utility.Utility;

/**
 * @author Jimmy Halim
 */
public class MainFragment extends Fragment implements OnClickListener, OnTouchListener, Callback, OnScaleGestureListener {
	
	private MainController controller;
	private Factory factory;
	private MainHandler handler;
	private LogUtility log;
	private Activity activity;
	
	private LinearLayout layoutCenter;
	private LinearLayout layoutBlack;
	private Button btnAuto;
	private Button btnBlack;
	private Button btnCapture;
	private Button btnFace;
	private Button btnVideo;
	private Button btnSwitchCam;
	private Button btnDecSize;
	private Button btnIncSize;
	private Button btnHelp;
	private Button btnSetting;
	private SurfaceView svPreview;
	private SurfaceHolder shPreview;
	private ScaleGestureDetector sgdPreview;
	private ScaleGestureDetector sgdBlack;
	private View view;
	
	public MainFragment() {
		this.factory = Factory.getInstance();
		this.log = LogUtility.getInstance();
		log.v(this, "constructor()");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		log.v(this, "onCreateView()");
		this.activity = getActivity();
		view = inflater.inflate(R.layout.fragment_main, container);
		this.handler = factory.getMainHandler(this);
		this.controller = factory.getMainController(getActivity(), handler);
		initView(view);
		controller.initData();

		return view;
	}

	@SuppressWarnings("deprecation")
	private void initView(View view) {
		log.v(this, "initView()");

		this.layoutBlack = (LinearLayout)view.findViewById(R.id.blackLayout);
		this.layoutBlack.setOnTouchListener(this);
		this.layoutCenter = (LinearLayout)view.findViewById(R.id.linearLayoutCenter);
		
		//Widgets
		this.svPreview = (SurfaceView)view.findViewById(R.id.svPreview);
		this.svPreview.setDrawingCacheQuality(100);
		this.svPreview.setDrawingCacheEnabled(true);
		this.svPreview.setZOrderOnTop(true);
		this.svPreview.setOnTouchListener(this);
		this.shPreview = this.svPreview.getHolder();
		this.shPreview.addCallback(this);
		this.shPreview.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		this.shPreview.setFormat(PixelFormat.TRANSPARENT);
		
		this.btnAuto = (Button)view.findViewById(R.id.btnAuto);
		this.btnAuto.setOnClickListener(this);
		this.btnBlack = (Button)view.findViewById(R.id.btnBlack);
		this.btnBlack.setOnClickListener(this);
		this.btnCapture = (Button)view.findViewById(R.id.btnCapture);
		this.btnCapture.setOnClickListener(this);
		this.btnFace = (Button)view.findViewById(R.id.btnFace);
		this.btnFace.setOnClickListener(this);
		this.btnVideo = (Button)view.findViewById(R.id.btnVideo);
		this.btnVideo.setOnClickListener(this);
		this.btnSwitchCam = (Button)view.findViewById(R.id.btnSwitchCam);
		this.btnSwitchCam.setOnClickListener(this);
		this.btnDecSize = (Button)view.findViewById(R.id.btnDecreaseSize);
		this.btnDecSize.setOnClickListener(this);
		this.btnIncSize = (Button)view.findViewById(R.id.btnIncreaseSize);
		this.btnIncSize.setOnClickListener(this);
		this.btnHelp = (Button)view.findViewById(R.id.btnHelp);
		this.btnHelp.setOnClickListener(this);
		this.btnSetting = (Button)view.findViewById(R.id.btnSetting);
		this.btnSetting.setOnClickListener(this);

		sgdPreview = new ScaleGestureDetector(activity, this);
		sgdBlack = new ScaleGestureDetector(activity, this);

		layoutBlack.setVisibility(View.INVISIBLE);
		layoutBlack.setOnTouchListener(this);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			btnFace.setVisibility(View.INVISIBLE);
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			btnVideo.setVisibility(View.INVISIBLE);
		}

	}

	@Override
	public void onClick(View v) {
		log.v(this, "onClick()");
		if (v==btnCapture) {
			controller.imageCapture();
		} else if (v==btnAuto) {
			controller.autoImageCapture();
		} else if (v==btnSetting) {
			controller.openSetting();
		} else if (v==btnFace) {
			controller.autoFaceCapture();
		} else if (v==btnVideo) {
			controller.videoRecording();
		} else if (v==btnIncSize) {
			controller.incPreviewSize(layoutCenter.getWidth());
		} else if (v==btnDecSize) {
			controller.decPreviewSize();
		} else if (v==btnBlack) {
			controller.switchBlackScreen();
		} else if (v==btnSwitchCam) {
			controller.switchCamera();
		} else if (v==btnHelp) {
			showHelp();
		}
	}

	private void showHelp() {
		AlertDialog dialog = new AlertDialog.Builder(activity).create();
		dialog.setTitle(this.getString(R.string.help));

		WebView wv = new WebView(activity);
		wv.loadData(this.getString(R.string.help_html), "text/html", "utf-8");
		wv.setScrollContainer(true);
		dialog.setView(wv);

		dialog.setButton(AlertDialog.BUTTON_NEGATIVE, this.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Rate It", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Uri uri = Uri.parse("market://details?id=" + activity.getPackageName());
				Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
				try {
					activity.startActivity(myAppLinkToMarket);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(activity, "Failed to find Market application", Toast.LENGTH_LONG).show();
				}
			}
		});
		dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Share It", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Utility.shareIt(activity);
			}
		});
		dialog.show();
	}
	
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		log.v(this, "onTouch(action:" + event.getAction() + "|actionindex:" + event.getActionIndex() + "|downtime:" + event.getDownTime() + "|eventtime:" + event.getEventTime() + ")");
		if (view==layoutBlack) {
			sgdBlack.onTouchEvent(event);
			if (event.getAction()==MotionEvent.ACTION_DOWN) {
				controller.blackScreenClick();
			}
			return true;
		} else if (view==svPreview) {
			sgdPreview.onTouchEvent(event);
			return true;
		}
		return false;
	}

	public SurfaceView getSvPreview() {
		return svPreview;
	}
	
	@Override
	public void onResume() {
		log.v(this, "onResume()");
		super.onResume();
		controller.uiResume(svPreview);
	}
	
	@Override
	public void onPause() {
		log.v(this, "onPause()");
		super.onPause();
		controller.uiPause();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		log.v(this, "surfaceChanged(format:"+format+",width:"+width+",height:"+height+")");
		controller.configureCamera(holder);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		log.v(this, "surfaceCreated()");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		log.v(this, "surfaceDestroyed()");
	}

	@Override
	public boolean onScale(ScaleGestureDetector sgd) {
		if (sgd==sgdPreview) {
			return controller.onScalePreview(sgd);
		} else if (sgd==sgdBlack) {
			return controller.onScaleBlackScreen(sgd);
		}
		return false;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector sgd) {
		log.v(this, "onScaleBegin()");
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector sgd) {
		log.v(this, "onScaleEnd()");
	}


	public Button getBtnAuto() {
		return btnAuto;
	}

	public Button getBtnCapture() {
		return btnCapture;
	}

	public Button getBtnSetting() {
		return btnSetting;
	}

	public Button getBtnFace() {
		return btnFace;
	}
	
	public Button getBtnIncSize() {
		return btnIncSize;
	}
	
	public Button getBtnDecSize() {
		return btnDecSize;
	}

	public Button getBtnBlack() {
		return btnBlack;
	}

	public Button getBtnSwitchCam() {
		return btnSwitchCam;
	}

	public Button getBtnVideo() {
		return btnVideo;
	}

	public Button getBtnHelp() {
		return btnHelp;
	}

	public SurfaceHolder getShPreview() {
		return shPreview;
	}

	public LinearLayout getLayoutBlack() {
		return layoutBlack;
	}
	
	public MainController getController() {
		return controller;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		log.v(this, "onKeyDown(keycode:"+keyCode+")");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return controller.pressBack();
		} else if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN) {
			return controller.pressVolumeDown();
//			//TODO
//			LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.linearLayoutPreview);
//			linearLayout.removeView(svPreview);
//			return true;
		} else if (keyCode==KeyEvent.KEYCODE_VOLUME_UP) {
			return controller.pressVolumeUp();
//			//TODO
//			LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.linearLayoutPreview);
//			linearLayout.addView(svPreview, 1);
//			return true;
		} else if (keyCode==KeyEvent.KEYCODE_MENU) {
			return controller.pressMenu();
		}
		return false;
	}

	public void setVisible() {
		log.v(this, "setVisible()");
		((RelativeLayout)view.findViewById(R.id.wholeLayout)).setVisibility(View.VISIBLE);
	}

	public void callWidgetAction(int action) {
		log.v(this, "callWidgetAction("+action+")");
		controller.callWidgetAction(action);
	}

	public void setVisibleForWidget() {
		log.v(this, "setVisibleForWidget()");
		((RelativeLayout)view.findViewById(R.id.wholeLayout)).setVisibility(View.VISIBLE);
		((LinearLayout)view.findViewById(R.id.linearLayoutLeft)).setVisibility(View.INVISIBLE);
		((LinearLayout)view.findViewById(R.id.linearLayoutRight)).setVisibility(View.INVISIBLE);
		((LinearLayout)view.findViewById(R.id.linearLayoutCenterButton)).setVisibility(View.INVISIBLE);
		((Button)view.findViewById(R.id.btnDecreaseSize)).setVisibility(View.INVISIBLE);
		((Button)view.findViewById(R.id.btnIncreaseSize)).setVisibility(View.INVISIBLE);
		SurfaceView svPreview = ((SurfaceView)view.findViewById(R.id.svPreview));
		ViewGroup.LayoutParams params = svPreview.getLayoutParams();
		params.height = 1;
		params.width = 1;
		svPreview.setLayoutParams(params);
	}
	
}
