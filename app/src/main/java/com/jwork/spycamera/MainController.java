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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.jwork.spycamera.model.FailedProcessData;
import com.jwork.spycamera.utility.ConfigurationUtility;
import com.jwork.spycamera.utility.CrashHandler;
import com.jwork.spycamera.utility.GMailSender;
import com.jwork.spycamera.utility.LogUtility;
import com.jwork.spycamera.utility.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import javax.mail.AuthenticationFailedException;

/**
 * @author Jimmy Halim
 */
public class MainController implements OnZoomChangeListener, PreviewCallback, AutoFocusCallback
        , PictureCallback, ShutterCallback, Serializable {

    public static final int STATE_IDLE = 0;
    private int state = STATE_IDLE;
    public static final int STATE_IMAGE_SINGLE = 1;
    public static final int STATE_IMAGE_AUTO = 2;
    public static final int STATE_IMAGE_FACE = 3;
    public static final int STATE_VIDEO_RECORDING = 4;
    public static final int WHAT_START_AUTOSHOOT = 1;
    public static final int WHAT_STOP_AUTOSHOOT = 2;
    public static final int WHAT_CONTINUE_AUTOSHOOT = 3;
    public static final int WHAT_START_FACESHOOT = 4;
    public static final int WHAT_STOP_FACESHOOT = 5;
    public static final int WHAT_START_VIDEO = 6;
    public static final int WHAT_STOP_VIDEO = 7;
    /**
     *
     */
    private static final long serialVersionUID = 4120558915025481125L;
    private final static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault());
    private LogUtility log;
    private Activity activity;
    private Handler handler;
    private Camera camera;
    private SurfaceHolder shPreview;
    private ConfigurationUtility config;

    private boolean isHolderReady = false;
    private boolean isCameraConfigure = false;
    private boolean isInAutoFocus = false;
    private boolean isBlackScreen = false;
    private int cameraId;
    private long lastCapture = 0;
    private long lastStartAutofocus = 0;
    private long avgAutofocusTime = -1;
    private String[][] cameraPreviewSizes;
    private String[][] videoQualities;
    private boolean crashReport = false;
    private int zoomCurrent = 0;
    private int defaultOrientation;
    private int currentCamcorderProfileId;
    private Parameters cameraParameters;
    private boolean isImageHighRes;
    private byte[] bSnapShot;
    private Camera.Size previewSize;
    private Camera.Size previewSizeHighest;
    private Camera.Size captureSize;
    private Camera.Size captureSizeHighest;
    private MediaRecorder recorder;
    private CamcorderProfile currentCamcorderProfile = null;
    private boolean zoomRunning;
    private boolean isZoomErrorHaveDisplayed = false;
    private int tempPreviewWidth = 1;

    private boolean isTakingPicture = false;
    private boolean isUIDisplayed = false;

    private int ringerMode;

    private SurfaceView svPreview;

    private Handler handlerCamera = new MainControllerHandler(this);
    private boolean widgetAction = false;

    ;
    private AutoShootLocalAsync autoImageTask;
    private GMailSenderTask mailTask = null;
    private ArrayList<File> imagesForEmail = new ArrayList<File>();

    public MainController(Activity activity, Handler handler) {
        this.activity = activity;
        this.handler = handler;
        this.log = LogUtility.getInstance();
        this.config = ConfigurationUtility.getInstance(activity);
    }

    public void initData() {
        log.v(this, "initData()");

        crashReport = checkPreviousCrash();
        isBlackScreen = false;

        if (!crashReport && CameraTaskService.state == CameraTaskService.WHAT_STOP) {
            Message msg = new Message();
            msg.what = MainHandler.WHAT_SET_PREVIEW_IMAGE;
            msg.arg1 = config.getPreviewWidthSize();
            msg.arg2 = msg.arg1;
            handler.sendMessage(msg);

            if (!Utility.showChangelogNew(true, activity, (camera == null)) && config.isStartupBlackMode()) {
                switchBlackScreen();
            }
        }

    }

    @SuppressLint("NewApi")
    public void startCamera(SurfaceView svPreview) {
        log.v(this, "startCamera()");
        this.svPreview = svPreview;
        if (!crashReport) {
            cameraId = 0;
            try {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
                    camera = Camera.open();
                    cameraPreviewSizes = new String[1][];
                    videoQualities = new String[1][];
                    Message msg = new Message();
                    msg.what = MainHandler.WHAT_HIDE_COMPONENT;
                    msg.arg1 = R.id.btnSwitchCam;
                    handler.sendMessage(msg);
                } else {
                    int total = Camera.getNumberOfCameras();
                    if (total > 2) {
                        total = 2;
                    } else if (total == 1) {
                        Message msg = new Message();
                        msg.what = MainHandler.WHAT_HIDE_COMPONENT;
                        msg.arg1 = R.id.btnSwitchCam;
                        handler.sendMessage(msg);
                    }

                    cameraPreviewSizes = new String[total][];
                    videoQualities = new String[total][];

                    cameraId = config.getCurrentCamera();

                    if (total > 1) {
                        int anotherCameraId = (cameraId == 0) ? 1 : 0;
                        String temp = config.getCameraPreviewSizes(anotherCameraId);
                        if (temp == null) {
                            camera = Camera.open(anotherCameraId);
                            StringBuffer data = new StringBuffer();
                            cameraPreviewSizes[anotherCameraId] = Utility.cameraSizeSupport(camera.getParameters(), data);
                            camera.release();
                            config.setCameraPreviewSizes(anotherCameraId, data.toString());
                        } else {
                            cameraPreviewSizes[anotherCameraId] = temp.split("#");
                        }
                        temp = config.getVideoQualityList(anotherCameraId);
                        if (temp == null) {
                            StringBuffer data = new StringBuffer();
                            videoQualities[anotherCameraId] = Utility.camcorderProfileSupport(anotherCameraId, data);
                            config.setVideoQualityList(anotherCameraId, data.toString());
                        } else {
                            videoQualities[anotherCameraId] = temp.split("#");
                        }
                    }
                    camera = Camera.open(cameraId);
                }
                cameraParameters = camera.getParameters();

                String temp = config.getCameraPreviewSizes(cameraId);
                if (temp == null) {
                    // Get preview sizes
                    StringBuffer data = new StringBuffer();
                    cameraPreviewSizes[cameraId] = Utility.cameraSizeSupport(camera.getParameters(), data);
                    config.setCameraPreviewSizes(cameraId, data.toString());
                } else {
                    cameraPreviewSizes[cameraId] = temp.split("#");
                }
                temp = config.getVideoQualityList(cameraId);
                if (temp == null) {
                    StringBuffer data = new StringBuffer();
                    videoQualities[cameraId] = Utility.camcorderProfileSupport(cameraId, data);
                    config.setVideoQualityList(cameraId, data.toString());
                } else {
                    videoQualities[cameraId] = temp.split("#");
                }


                String configSize = config.getImageCaptureSize(cameraId);
                if (configSize != null) {
                    if (configSize.endsWith("*")) {
                        isImageHighRes = true;
                    } else {
                        isImageHighRes = false;
                    }
                } else {
//					if (!config.isHaveOutOfMemoryIssue()) {
                    isImageHighRes = false;
//					} else {
//						isImageHighRes = true;
//					}
                }
                // Analyze preview/capture size
                int highestPreview = 0;
                int highestCapture = 0;
                this.previewSize = null;
                this.previewSizeHighest = null;
                this.captureSize = null;
                this.captureSizeHighest = null;
                for (String previewSize : cameraPreviewSizes[cameraId]) {
                    int w = 0;
                    int h = 0;
                    String size = null;
                    if (previewSize.endsWith("*")) {
                        size = previewSize.substring(0, previewSize.length() - 1);
                    } else {
                        size = previewSize;
                    }
                    String[] temp2 = size.split("x");
                    try {
                        w = Integer.parseInt(temp2[0]);
                    } catch (NumberFormatException e) {
                    }
                    try {
                        h = Integer.parseInt(temp2[1]);
                    } catch (NumberFormatException e) {
                    }

                    if (previewSize.endsWith("*") && (highestCapture < w * h)) {
                        captureSizeHighest = camera.new Size(w, h);
                        highestCapture = w * h;
                    } else if (!previewSize.endsWith("*") && (highestPreview < w * h)) {
                        previewSizeHighest = camera.new Size(w, h);
                        highestPreview = w * h;
                    }

                    if (configSize != null && previewSize.equals(configSize)) {
                        if (isImageHighRes) {
                            this.captureSize = camera.new Size(w, h);
                            this.previewSize = null;
                        } else {
                            this.previewSize = camera.new Size(w, h);
                            this.captureSize = null;
                        }
                    }
                }
                if (previewSize == null) {
                    previewSize = previewSizeHighest;
                }

                if (isImageHighRes && this.captureSize == null) {
                    this.captureSize = captureSizeHighest;
                }

                if (isImageHighRes) {
                    config.setImageCaptureSize(cameraId, captureSize.width + "x" + captureSize.height + "*");
                } else {
                    config.setImageCaptureSize(cameraId, previewSize.width + "x" + previewSize.height);
                }

                refreshImagePreviewSize();

                if (shPreview != null) {
                    try {
                        camera.setPreviewDisplay(shPreview);
                    } catch (IOException e) {
                        log.w(this, e);
                    }
                }

                //Video Quality
                int videoprofile = config.getVideoRecordingQuality(cameraId);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                        log.d(this, "Current camcorder profile : " + videoprofile);
                        currentCamcorderProfile = CamcorderProfile.get(cameraId, videoprofile);
                        currentCamcorderProfileId = videoprofile;
                    }
                } catch (IllegalArgumentException e) {
                    log.d(this, "Fail getting camcorder profile, change using : " + CamcorderProfile.QUALITY_LOW);
                    try {
                        currentCamcorderProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
                        currentCamcorderProfileId = CamcorderProfile.QUALITY_LOW;
                    } catch (IllegalArgumentException e2) {
                        log.d(this, "Still fail getting camcorder profile, disabling video recording");
                        showToast(true, Toast.LENGTH_LONG, "Unable to initialize video recording, disabling the feature");
                        Message msg = new Message();
                        msg.what = MainHandler.WHAT_DISABLE_COMPONENT;
                        msg.arg1 = R.id.btnVideo;
                        handler.sendMessage(msg);
                    }
                }
            } catch (RuntimeException re) {
                log.w(this, re);
                crashReport = true;
                if (re.getMessage().toLowerCase(Locale.getDefault()).contains("connect")) {
                    config.clear(false);
                    Message msg = new Message();
                    msg.what = MainHandler.WHAT_SHOW_FAILED_PROCESS;
                    msg.obj = new FailedProcessData(re
                            , "Failed initializing camera. Please try to reboot your phone manually and run the application again."
                            , "After reboot still error"
                            , "OK", true, null);
                    handler.sendMessage(msg);
                } else if (re.getMessage().toLowerCase(Locale.getDefault()).contains("getparameters")) {
                    config.clear(false);
                    Message msg = new Message();
                    msg.what = MainHandler.WHAT_SHOW_FAILED_PROCESS;
                    msg.obj = new FailedProcessData(re
                            , "Failed getting camera parameters. Sending crash report will help me fix it."
                            , "Send Report"
                            , "I've sent it", true, null);
                    handler.sendMessage(msg);
                } else {
                    throw re;
                }
            }

            if (isCameraConfigure) {
                startCameraPreview(shPreview);
            } else if (isHolderReady) {
                configureCamera(shPreview);
            }
        }
    }

    private void refreshImagePreviewSize() {
        Message msg = new Message();
        msg.what = MainHandler.WHAT_SET_PREVIEW_IMAGE;
        msg.arg1 = config.getPreviewWidthSize();
        msg.arg2 = calculatePreviewHeight(msg.arg1);
        if (msg.arg2 < 1) {
            msg.arg2 = 1;
        }
        handler.sendMessage(msg);
    }

    private int calculatePreviewHeight(int width) {
        int height = -1;
        float ratio = 1;
        if (isImageHighRes) {
            if (captureSize == null) {
                log.v(this, "calculatePreviewHeight(width:" + width + "):" + width);
                return width;
            }
            ratio = (float) captureSize.height / (float) captureSize.width;
        } else {
            if (previewSize == null) {
                log.v(this, "calculatePreviewHeight(width:" + width + "):" + width);
                return width;
            }
            ratio = (float) previewSize.height / (float) previewSize.width;
        }
        if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            ratio = 1 / ratio;
        }
        height = (int) (width * ratio);
        log.v(this, "calculatePreviewHeight(width:" + width + "):" + height);
        return height;
    }

    public void showToast(boolean force, int length, Object message) {
        log.v(this, "showToast(force:" + force + "|length:" + length + "|message:" + message.toString() + ")");
        if (!isUIDisplayed || this.widgetAction) {
            return;
        }
        Message msg = new Message();
        msg.what = MainHandler.WHAT_SHOW_TOAST;
        msg.arg1 = force ? 1 : 0;
        msg.arg2 = length;
        msg.obj = message;
        handler.sendMessage(msg);
    }

    public synchronized void configureCamera(SurfaceHolder holder) {
        log.v(this, "configureCamera()");
        isHolderReady = true;
        shPreview = holder;
        if (camera == null) {
            log.w(this, "configureCamera: camera is null");
            return;
        }
        if (!isCameraConfigure) {
            if (crashReport) {
                return;
            }
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;

            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            //If Naturally landscape (tablets)
            log.v(this, "Display pixels: " + dm.widthPixels + "x" + dm.heightPixels + "|Rotation:" + rotation);
            if (
                    ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && dm.widthPixels > dm.heightPixels)
                            || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && dm.widthPixels < dm.heightPixels)
                    ) {
                rotation += 1;
                if (rotation > 3) {
                    rotation = 0;
                }
            }
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 90;
                    defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    degrees = 0;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                        defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                    } else {
                        defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                        activity.setRequestedOrientation(defaultOrientation);
                    }
                    break;
                case Surface.ROTATION_180:
                    degrees = 270;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                        defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    } else {
                        defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                        activity.setRequestedOrientation(defaultOrientation);
                    }
                    break;
                case Surface.ROTATION_270:
                    degrees = 180;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                        defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    } else {
                        defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                        activity.setRequestedOrientation(defaultOrientation);
                    }
                    break;
            }
            try {
                refreshImagePreviewSize();
                log.d(this, "Rotation: " + rotation + "|Degree:" + degrees + "|Orientation:" + defaultOrientation);
                camera.setDisplayOrientation(degrees);
            } catch (RuntimeException re) {
                if (!config.getBoolean(ConfigurationUtility.PREFS_ERRORREPORT_SETDISPLAYORIENTATION, false)) {
                    Message msg = new Message();
                    msg.what = MainHandler.WHAT_SHOW_FAILED_PROCESS;
                    msg.obj = new FailedProcessData(re
                            , "Failed setting preview orientation. Sending crash report will help me fix it."
                            , "Send Report"
                            , "OK", false, ConfigurationUtility.PREFS_ERRORREPORT_SETDISPLAYORIENTATION);
                    handler.sendMessage(msg);
                } else {
                    showToast(true, Toast.LENGTH_LONG, "Failed setting preview orientation.");
                }
            }

            if (isImageHighRes) {
                cameraParameters.setPictureSize(captureSize.width, captureSize.height);
                cameraParameters.setPictureFormat(ImageFormat.JPEG);
                cameraParameters.setRotation((cameraId == 0) ? 90 : 270);
            }
            log.i(this, "previewSize.width : " + previewSize.width + "x" + previewSize.height);
            cameraParameters.setPreviewSize(previewSize.width, previewSize.height);
            cameraParameters.setPreviewFormat(ImageFormat.NV21);
            if (cameraParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            } else {
                log.d(this, "Focus mode auto not supported : " + Arrays.toString(cameraParameters.getSupportedFocusModes().toArray()));
            }
            camera.setParameters(cameraParameters);

            startCameraPreview(shPreview);
            isCameraConfigure = true;
        } else {
            log.w(this, "Camera already configured");
            refreshImagePreviewSize();
        }
    }

    public void startCameraPreview(SurfaceHolder holder) {
        log.v(this, "startCameraPreview()");
        try {
            log.i(this, "Starting preview");
            camera.setZoomChangeListener(this);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            camera.setPreviewCallback(this);
        } catch (IOException e) {
            showToast(true, Toast.LENGTH_LONG, "Failed initializing camera preview");
            log.e(this, e);
        } catch (RuntimeException re) {
            if (re.getMessage().toLowerCase(Locale.getDefault()).contains("startpreview")) {
                config.clear(false);
                Message msg = new Message();
                msg.what = MainHandler.WHAT_SHOW_FAILED_PROCESS;
                msg.obj = new FailedProcessData(re
                        , "Failed starting camera preview. Sending crash report will help me fix it."
                        , "Send Report"
                        , "I've sent it", false, null);
                handler.sendMessage(msg);
            } else {
                throw re;
            }
        }
    }

    public void stopCamera() {
        log.v(this, "stopCamera()");
        if (!crashReport) {
            if (state == STATE_VIDEO_RECORDING) {
                try {
                    stopVideoRecording();
                } catch (RuntimeException e) {
                    log.w(this, e);
                }
            }

            try {
                camera.cancelAutoFocus();
            } catch (Throwable e) {
            }
            try {
                camera.setPreviewCallback(null);
            } catch (Throwable e) {
            }
            try {
            } catch (Throwable e) {
                log.w(this, e);
            }
            isCameraConfigure = false;
            setState(STATE_IDLE);
        }
    }

    private void stopVideoRecording() {
        log.v(this, "stopVideoRecording()|state:" + state);
        if (recorder != null) {
            showToast(false, Toast.LENGTH_SHORT, R.string.message_stopVideo);

            setState(STATE_IDLE);
            try {
                recorder.stop();
                recorder.reset();
                recorder.release();
                camera.lock();
            } catch (RuntimeException re) {
                showToast(false, Toast.LENGTH_SHORT, "Encounter error on stopping video recording");
                log.w(this, re);
            }
            try {
                camera.reconnect();
            } catch (IOException e) {
                log.w(this, e);
            }
        }
    }

    public void switchCamera() {
        if (cameraPreviewSizes == null || cameraPreviewSizes.length <= 1) {
            return;
        }
        int activeCamera = config.getCurrentCamera();
        if (activeCamera == 0) {
            activeCamera = 1;
        } else {
            activeCamera = 0;
        }
        config.setCurrentCamera(activeCamera);
        stopCamera();
        startCamera(svPreview);
    }

    public void switchBlackScreen() {
        log.v(this, "switchBlackScreen()|isBlackScreen:" + isBlackScreen);
        if (!isBlackScreen) {
            Message msg = new Message();
            msg.what = MainHandler.WHAT_SHOW_COMPONENT;
            msg.arg1 = R.id.blackLayout;
            handler.sendMessage(msg);

            showToast(true, Toast.LENGTH_SHORT, activity.getString(R.string.hint_blackmode));

            tempPreviewWidth = config.getPreviewWidthSize();
            config.setPreviewWidthSize(1);
            refreshImagePreviewSize();
        } else {
            Message msg = new Message();
            msg.what = MainHandler.WHAT_HIDE_COMPONENT;
            msg.arg1 = R.id.blackLayout;
            handler.sendMessage(msg);
            config.setPreviewWidthSize(tempPreviewWidth);
            refreshImagePreviewSize();
        }
        isBlackScreen = !isBlackScreen;
    }

    public void imageCapture() {
        log.v(this, "imageCapture()");
        if (state == STATE_IDLE) {
            setState(STATE_IMAGE_SINGLE);
            startAutoFocus();
        }
    }

    private void startAutoFocus() {
        log.v(this, "startAutoFocus()");
        if (isCameraConfigure) {
            lastStartAutofocus = System.currentTimeMillis();
            if (config.isUseAutoFocus()) {
                try {
                    log.d(this, "current autofocus mode: " + cameraParameters.getFocusMode());
                    if (!isInAutoFocus) {
                        isInAutoFocus = true;
                        camera.autoFocus(this);
                    } else {
                        log.w(this, "skipping autofocus, still in middle of autofocus");
                    }
                } catch (RuntimeException re) {
                    log.e(this, re);
                    onAutoFocus(true, camera);
                }
            } else {
                log.d(this, "Canceling autofocus, calling onAutoFocus directly");
                onAutoFocus(true, camera);
            }
        } else {
            log.w(this, "Camera is not configured, cannot startAutoFocus");
        }
    }

    public void autoImageCapture() {
        log.v(this, "autoImageCapture()");
        if (state == STATE_IDLE) {
            if (config.isDisableBackgroundService()) {
                autoImageCaptureStart();
                autoImageCaptureTaskStart();
            } else {
                Intent intent = new Intent(activity, CameraTaskService.class);
                Messenger messenger = new Messenger(handlerCamera);
                intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
                intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_START_AUTOSHOT);
                activity.startService(intent);
            }
        } else if (state == STATE_IMAGE_AUTO) {
            if (config.isDisableBackgroundService()) {
                autoImageCaptureStop();
                autoImageCaptureTaskStop();
            } else {
                Intent intent = new Intent(activity, CameraTaskService.class);
                Messenger messenger = new Messenger(handlerCamera);
                intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
                intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_STOP);
                activity.stopService(intent);
            }
        }
    }

    public void openSetting() {
        if (state == STATE_IDLE) {
            Intent intent = new Intent(activity, SpyCamPrefsActivity.class);

            if (cameraPreviewSizes[0] != null) {
                intent.putExtra("cameraPreviewSizes0", cameraPreviewSizes[0]);
            }
            if (cameraPreviewSizes.length > 1 && cameraPreviewSizes[1] != null) {
                intent.putExtra("cameraPreviewSizes1", cameraPreviewSizes[1]);
            }
            intent.putExtra("cameraNumber", cameraPreviewSizes.length);
            activity.startActivity(intent);
        } else {
            // if it's not idle the button is for minimize
            if (!config.isDisplayedMinimizeExperimentalNotice()) {
                Message msg = new Message();
                msg.what = MainHandler.WHAT_SHOW_MINIMIZE_EXPERIMENTAL_NOTICE;
                handler.sendMessage(msg);
                config.setDisplayMinimizeExperimentalNotice();
                return;
            } else {
                activity.finish();
            }
        }
    }

    public void autoFaceCapture() {
        log.v(this, "autoFaceCapture()");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            showToast(true, Toast.LENGTH_SHORT, "Facedetection is only supported for Android 4.0 or newer");
            return;
        }
        if (camera == null) {
            showToast(true, Toast.LENGTH_SHORT, "Camera is not initialized yet");
            return;
        }
        if (state == STATE_IDLE) {
            if (config.isDisableBackgroundService()) {
                autoFaceCaptureStart();
            } else {
                Intent intent = new Intent(activity, CameraTaskService.class);
                Messenger messenger = new Messenger(handlerCamera);
                intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
                intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_START_FACESHOT);
                activity.startService(intent);
            }
        } else if (state == STATE_IMAGE_FACE) {
            if (config.isDisableBackgroundService()) {
                autoFaceCaptureStop();
            } else {
                Intent intent = new Intent(activity, CameraTaskService.class);
                Messenger messenger = new Messenger(handlerCamera);
                intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
                intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_STOP);
                activity.stopService(intent);
            }
        }
    }

    public void videoRecording() {
        log.v(this, "videoRecording()");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            showToast(true, Toast.LENGTH_SHORT, "Video recording is only supported for Android 2.3 or newer");
            return;
        }
        if (!config.isDisplayedVideoExperimentalNotice()) {
            Message msg = new Message();
            msg.what = MainHandler.WHAT_SHOW_VIDEO_RECORDING_EXPERIMENTAL_NOTICE;
            handler.sendMessage(msg);
            config.setDisplayVideoExperimentalNotice();
            return;
        }
        if (state == STATE_IDLE) {
            if (config.isDisableBackgroundService()) {
                videoRecordingStart();
            } else {
                Intent intent = new Intent(activity, CameraTaskService.class);
                Messenger messenger = new Messenger(handlerCamera);
                intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
                intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_START_VIDEO_RECORDING);
                activity.startService(intent);
            }
        } else {
            if (config.isDisableBackgroundService()) {
                videoRecordingStop();
            } else {
                Intent intent = new Intent(activity, CameraTaskService.class);
                Messenger messenger = new Messenger(handlerCamera);
                intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
                intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_STOP);
                activity.stopService(intent);
            }
        }

    }

    private void startVideoRecording() {
        File directory = new File(config.getSavingPath());
        if (!directory.exists()) {
            directory.mkdir();
        }
        String outputFile = null;
        try {
            try {
                camera.unlock();
            } catch (Throwable re) {
                log.w(this, re);
            }
            recorder = new MediaRecorder();
            recorder.setCamera(camera); // start failed: -19
            recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            recorder.setProfile(currentCamcorderProfile);

            outputFile = directory.getAbsolutePath() + "/SpyVideo_" + SDF.format(new Date()) + ".mp4";
            recorder.setOutputFile(outputFile);
            recorder.setPreviewDisplay(shPreview.getSurface());
            recorder.prepare();
            recorder.start();   // Recording is now started

            addFileToMediaScanner(new File(outputFile));

            lastCapture = 0;
            showToast(false, Toast.LENGTH_SHORT, activity.getString(R.string.message_startVideo, directory.getAbsolutePath() + "/SpyVideo_" + SDF.format(new Date()) + ".mp4"));
            setState(STATE_VIDEO_RECORDING);
        } catch (IllegalStateException e) {
            log.w(this, e);
            showToast(true, Toast.LENGTH_SHORT, "Failed starting video recording");
            if (recorder != null) {
                recorder.reset();
                recorder.release();
            }
            try {
                camera.lock();
                camera.reconnect();
            } catch (Throwable e1) {
            }
            Message msg = new Message();
            msg.what = MainHandler.WHAT_SHOW_FAILED_PROCESS;
            msg.obj = new FailedProcessData(e
                    , "Failed starting video recording.\nThis feature is still experimental.\nYou can try other quality setting.\nReporting it will help me solve issues"
                    , "Report"
                    , "OK", false, null);
            handler.sendMessage(msg);

            Intent intent = new Intent(activity, CameraTaskService.class);
            Messenger messenger = new Messenger(handlerCamera);
            intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
            intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_STOP);
            activity.stopService(intent);

            try {
                File failedFile = new File(outputFile);
                if (failedFile.exists()) {
                    failedFile.delete();
                }
            } catch (Exception e2) {
            }

        } catch (IOException e) {
            log.w(this, e);
            showToast(true, Toast.LENGTH_SHORT, "Failed starting video recording");
            if (recorder != null) {
                recorder.reset();
                recorder.release();
            }
            try {
                camera.lock();
                camera.reconnect();
            } catch (Throwable e1) {
            }
            Message msg = new Message();
            msg.what = MainHandler.WHAT_SHOW_FAILED_PROCESS;
            msg.obj = new FailedProcessData(e
                    , "Failed starting video recording.\nThis feature is still experimental.\nYou can try other quality setting.\nReporting it will help me solve issues"
                    , "Report"
                    , "OK", false, null);
            handler.sendMessage(msg);

            Intent intent = new Intent(activity, CameraTaskService.class);
            Messenger messenger = new Messenger(handlerCamera);
            intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
            intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_STOP);
            activity.stopService(intent);

            try {
                File failedFile = new File(outputFile);
                if (failedFile.exists()) {
                    failedFile.delete();
                }
            } catch (Exception e2) {
            }
        } catch (RuntimeException e) {
            log.w(this, e);
            showToast(true, Toast.LENGTH_SHORT, "Failed starting video recording");
            if (recorder != null) {
                recorder.reset();
                recorder.release();
            }
            try {
                camera.lock();
                camera.reconnect();
            } catch (Throwable e1) {
            }
            Message msg = new Message();
            msg.what = MainHandler.WHAT_SHOW_FAILED_PROCESS;
            msg.obj = new FailedProcessData(e
                    , "Failed starting video recording.\nThis feature is still experimental.\nYou can try other quality setting.\nReporting it will help me solve issues"
                    , "Report"
                    , "OK", false, null);
            handler.sendMessage(msg);

            Intent intent = new Intent(activity, CameraTaskService.class);
            Messenger messenger = new Messenger(handlerCamera);
            intent.putExtra(CameraTaskService.EXTRA_MESSENGER, messenger);
            intent.putExtra(CameraTaskService.EXTRA_ACTION, CameraTaskService.WHAT_STOP);
            activity.stopService(intent);

            try {
                File failedFile = new File(outputFile);
                if (failedFile.exists()) {
                    failedFile.delete();
                }
            } catch (Exception e2) {
            }
        } finally {
        }
    }

    public void incPreviewSize(int maxWidth) {
        int w = config.getPreviewWidthSize() + 10;
        int max = maxWidth * 3 / 4;
        if (w > max) {
            w = max;
        }
        config.setPreviewWidthSize(w);
        refreshImagePreviewSize();
    }

    public void decPreviewSize() {
        int w = config.getPreviewWidthSize() - 10;
        if (w < 1) {
            w = 1;
        }
        config.setPreviewWidthSize(w);
        refreshImagePreviewSize();
    }

    @Override
    public void onZoomChange(int arg0, boolean arg1, Camera arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //		log.v(this, "onPreviewFrame()");
        bSnapShot = data;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        log.v(this, "onAutoFocus(success:" + success + ")");
        isInAutoFocus = false;
        if (bSnapShot == null) {
            showToast(true, Toast.LENGTH_SHORT, "Image data not found");
            log.w(this, "Image data not found");
            setState(STATE_IDLE);
        } else if (cameraParameters == null) {
            showToast(true, Toast.LENGTH_SHORT, "Image parameter not found");
            log.w(this, "Image parameter not found");
            setState(STATE_IDLE);
        } else if (bSnapShot != null) {
            long completeAutofocus = System.currentTimeMillis();
            synchronized (camera) {
                //					log.d(this, "lastCapture : " + lastCapture + "|" + avgAutofocusTime);
                try {
                    if (!isTakingPicture) {
                        //average autofocus speed
                        if (avgAutofocusTime == -1) {
                            avgAutofocusTime = completeAutofocus - lastStartAutofocus;
                        } else {
                            avgAutofocusTime += completeAutofocus - lastStartAutofocus;
                            avgAutofocusTime /= 2;
                        }
                        log.d(this, "Average Focus Time : " + avgAutofocusTime);

                        if (isImageHighRes) {
                            isTakingPicture = true;
                            log.d(this, "Calling takePicture");
                            camera.takePicture(this, null, null, this);
                            lastCapture = System.currentTimeMillis();
                        } else {
                            saveImage(true);
                        }
                    } else {
                        log.w(this, "Ignoring the capture request because still in middle taking picture");
                    }

                    if (state == STATE_IMAGE_SINGLE || state == STATE_IMAGE_FACE) {
                        if (isCameraConfigure) {
                            try {
                                camera.cancelAutoFocus();
                            } catch (RuntimeException e) {
                            }
                        }
                    }
                } catch (IOException e) {
                    log.w(this, e);
                    if (isCameraConfigure) {
                        try {
                            camera.cancelAutoFocus();
                        } catch (RuntimeException e2) {
                        }
                    }
                }
            }
        }
    }

    private void saveImage(boolean yuv) throws IOException {
        log.d(this, "Calling saveImage(yuv:" + yuv + ")");
        FileOutputStream filecon = null;
        try {
            File directory = new File(config.getSavingPath());
            if (!directory.exists()) {
                directory.mkdir();
            }
            File file = new File(directory.getAbsolutePath() + "/SpyPhoto_" + SDF.format(new Date()) + ".jpg");
            filecon = new FileOutputStream(file);
            int rotation = config.getImageRotation();
            int[] imageRotate = new int[2];
            if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                imageRotate[0] = 90;
                imageRotate[1] = -90;
            } else if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                imageRotate[0] = 0;
                imageRotate[1] = 0;
            } else if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                imageRotate[0] = 180;
                imageRotate[1] = 180;
            } else if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                imageRotate[0] = -90;
                imageRotate[1] = 90;
            }

            //Create Image from preview data - byte[]
            // converting to RGB for rotation
            int[] rgbData = null;
            if (yuv) {
//				log.d(this, "Rotating image : " + imageRotate[cameraId] + "|memory:"+Runtime.getRuntime().maxMemory());
                rotation += imageRotate[cameraId];
                if (rotation > 360) {
                    rotation -= 360;
                }
                if (!config.isYUVDecodeAlternate()) {
                    //USING Android YuvImage
                    Size size = cameraParameters.getPreviewSize();
                    YuvImage yuvImage = new YuvImage(bSnapShot, cameraParameters.getPreviewFormat(),
                            size.width, size.height, null);
                    yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, filecon);
                    int width = yuvImage.getWidth();
                    int height = yuvImage.getHeight();
                    yuvImage = null;
                    filecon.close();
                    filecon = null;
                    boolean rotationSuccess = true;
                    if (rotation != 0) {
                        rotationSuccess = rotateImageFile(file, rotation);
                    }
                    String msg = file.getAbsolutePath() + " : " + width + "x" + height;
                    if (!rotationSuccess) {
                        msg += " (not enough memory to rotate image)";
                    }
                    showToast(false, Toast.LENGTH_SHORT, msg);
                } else {
                    //USING custom YuvDecoder
                    log.d(this, "Using custom YUV decoder");
                    Size size = cameraParameters.getPreviewSize();
                    rgbData = new int[size.width * size.height];
                    decodeYUV(rgbData, bSnapShot, size.width, size.height);
                    Bitmap bitmap = Bitmap.createBitmap(rgbData, size.width, size.height, Bitmap.Config.ARGB_8888);
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    if (rotation != 0) {
                        log.v(this, "rotating image : " + rotation);
                        Matrix m = new Matrix();
                        m.setRotate(rotation, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
                        Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, size.width, size.height, m, false);
                        bitmap.recycle();
                        bitmap = null;
                        bitmap2.compress(CompressFormat.JPEG, 100, filecon);
                        bitmap2.recycle();
                        bitmap2 = null;
                    } else {
                        bitmap.compress(CompressFormat.JPEG, 100, filecon);
                        bitmap.recycle();
                    }
                    showToast(false, Toast.LENGTH_SHORT, file.getAbsolutePath() + " : " + width + "x" + height);
                }
            } else {
                filecon.write(bSnapShot);
                filecon.close();
                boolean rotationSuccess = true;
                if (rotation != 0) {
                    rotationSuccess = rotateImageFile(file, rotation);
                }
                String msg = file.getAbsolutePath() + " : " + cameraParameters.getPictureSize().width + "x" + cameraParameters.getPictureSize().height;
                if (!rotationSuccess) {
                    msg += " (not enough memory to rotate image)";
                }
                showToast(false, Toast.LENGTH_SHORT, msg);
            }
            if (config.isVibrate()) {
                Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(config.getVibrateTime());
            }

            //add it media scanner for Gallery
            addFileToMediaScanner(file);

            //Check if auto email gmail enabled
            if (config.isAutoEmailGMailEnabled()) {
                sendEmailViaGMail(file);
            }

            if (state == STATE_IMAGE_SINGLE) {
                setState(STATE_IDLE);
            }

            lastCapture = System.currentTimeMillis();
        } catch (RuntimeException e) {
            log.w(this, e);
            Message msg = new Message();
            msg.what = MainHandler.WHAT_SHOW_FAILED_PROCESS;
            msg.obj = new FailedProcessData(e
                    , "Failed saving image. Please try to change the image resolution in setting.\nSending the report will help me solve the issue."
                    , "Send Report"
                    , "OK", false, null);
            handler.sendMessage(msg);
        } finally {
            if (filecon != null) {
                try {
                    filecon.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private boolean rotateImageFile(File file, int rotation) throws FileNotFoundException {
        log.v(this, "rotateImageFile(rotation:" + rotation + ")");
        Boolean rotationSuccess = false;
        Bitmap bitmapOri = null;
        Bitmap bitmapRotate = null;
        FileOutputStream filecon = null;
        try {
            bitmapOri = BitmapFactory.decodeFile(file.getAbsolutePath());
            Matrix mat = new Matrix();
            mat.postRotate(rotation);
            bitmapRotate = Bitmap.createBitmap(bitmapOri, 0, 0,
                    bitmapOri.getWidth(), bitmapOri.getHeight(), mat, true);
            bitmapOri.recycle();
            bitmapOri = null;
            filecon = new FileOutputStream(file);
            bitmapRotate.compress(CompressFormat.JPEG, 100, filecon);
            bitmapRotate.recycle();
            bitmapRotate = null;
            rotationSuccess = true;
        } catch (OutOfMemoryError e) {
            log.w(this, e);
            System.gc();
        } finally {
            if (bitmapOri != null) {
                bitmapOri.recycle();
                bitmapOri = null;
            }
            if (bitmapRotate != null) {
                bitmapRotate.recycle();
                bitmapRotate = null;
            }
            if (filecon != null) {
                try {
                    filecon.close();
                } catch (IOException e) {
                }
                filecon = null;
            }
        }
        return rotationSuccess;
    }

    private void addFileToMediaScanner(File f) {
        log.v(this, "addFileToMediaScanner(f:" + f.getPath() + ")");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        activity.sendBroadcast(mediaScanIntent);
    }

    public void decodeYUV(int[] out, byte[] fg, int width, int height)
            throws NullPointerException, IllegalArgumentException {
        log.v(this, "decodeYUV(out:" + out + "|fg:" + (fg != null ? fg.length : null) + "|w:" + width + "|h:" + height + ")");
        int sz = width * height;
        if (out == null)
            throw new NullPointerException("buffer out is null");
        if (out.length < sz)
            throw new IllegalArgumentException("buffer out size " + out.length
                    + " < minimum " + sz);
        if (fg == null)
            throw new NullPointerException("buffer 'fg' is null");

        if (fg.length < sz)
            throw new IllegalArgumentException("buffer fg size " + fg.length
                    + " < minimum " + sz);

        int i, j;
        int Y, Cr = 0, Cb = 0;
        for (j = 0; j < height; j++) {
            int pixPtr = j * width;
            final int jDiv2 = j >> 1;
            for (i = 0; i < width; i++) {
                Y = fg[pixPtr];
                if (Y < 0)
                    Y += 255;
                if ((i & 0x1) != 1) {
                    final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
                    Cb = fg[cOff];
                    if (Cb < 0)
                        Cb += 127;
                    else
                        Cb -= 128;
                    Cr = fg[cOff + 1];
                    if (Cr < 0)
                        Cr += 127;
                    else
                        Cr -= 128;
                }
                int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                if (R < 0)
                    R = 0;
                else if (R > 255)
                    R = 255;
                int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1)
                        + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
                if (G < 0)
                    G = 0;
                else if (G > 255)
                    G = 255;
                int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
                if (B < 0)
                    B = 0;
                else if (B > 255)
                    B = 255;
                out[pixPtr++] = (0xff000000 + (B << 16) + (G << 8) + R);
            }
        }

    }

    @Override
    public void onShutter() {
        log.v(this, "onShutter()");
//		unmute();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        log.v(this, "onPictureTaken(data" + (data != null ? data.length : null) + ")");
        bSnapShot = data;
        try {
            saveImage(false);
            camera.startPreview();
            isTakingPicture = false;
        } catch (IOException e) {
            log.w(this, e);
        }
    }

    public void setState(int state) {
        log.v(this, "setState(state:" + state + ")|widgetAction:" + widgetAction);
        isInAutoFocus = false;
        if (this.widgetAction == true && state == STATE_IDLE) {
            activity.finish();
        }
        if (state == this.state) {
            return;
        }
        if (isUIDisplayed) {
            Message msg = new Message();
            msg.what = MainHandler.WHAT_SET_STATE_UI;
            msg.arg1 = this.state;
            this.state = state;
            msg.arg2 = state;
            if (config.isDisableBackgroundService()) {
                msg.obj = Boolean.TRUE;
            }
            handler.sendMessage(msg);
        } else {
            this.state = state;
        }
    }

    public void blackScreenClick() {
        if (state == STATE_IDLE) {
            imageCapture();
        }
    }

    public boolean onScalePreview(ScaleGestureDetector sgd) {
        log.v(this, "onScale " + sgd.getScaleFactor() + "-" + cameraParameters.getMaxZoom());
        if (cameraParameters.isZoomSupported() || cameraParameters.isSmoothZoomSupported()) {
            if (sgd.getScaleFactor() > 1) {
                if (cameraParameters.getZoom() < cameraParameters.getMaxZoom()) {
                    zoomCurrent = cameraParameters.getZoom() + 1;
                    zoomRunning = true;
                    if (cameraParameters.isSmoothZoomSupported()) {
                        if (!zoomRunning) {
                            camera.startSmoothZoom(zoomCurrent);
                        }
                    } else {
                        cameraParameters.setZoom(zoomCurrent);
                        camera.setParameters(cameraParameters);
                    }
                    log.i(this, "Zoom to : " + zoomCurrent);
                }
            } else if (sgd.getScaleFactor() < 1) {
                if (cameraParameters.getZoom() > 0) {
                    zoomCurrent = cameraParameters.getZoom() - 1;
                    zoomRunning = true;
                    if (cameraParameters.isSmoothZoomSupported()) {
                        if (!zoomRunning) {
                            camera.startSmoothZoom(zoomCurrent);
                        }
                    } else {
                        cameraParameters.setZoom(zoomCurrent);
                        camera.setParameters(cameraParameters);
                    }
                    log.i(this, "Zoom to : " + zoomCurrent);
                }
            }
        } else {
            if (!isZoomErrorHaveDisplayed) {
                isZoomErrorHaveDisplayed = true;
                showToast(true, Toast.LENGTH_SHORT, "Zoom not supported");
            }
        }
        return true;
    }

    private boolean checkPreviousCrash() {
        boolean justCrashed = config.isCrashed();
        if (!justCrashed) {
            return false;
        }
        final File file = new File(config.getCrashLogFilePath());
        if (!file.exists()) {
            showToast(true, Toast.LENGTH_LONG, "An error was detected but no report generated");
            return false;
        }

        File fileType = new File(config.getCrashTypeFilePath());
        int type = 0;
        if (fileType.exists()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(fileType);
                type = fis.read();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }

//		if (type==1) {
//			config.setOutOfMemoryIssue();
//		}

        Message msg = new Message();
        msg.what = MainHandler.WHAT_SHOW_CRASH_DIALOG;
        msg.arg1 = type;
        handler.sendMessage(msg);
        return true;
    }

    public String errorReport() {
        StringBuffer info = new StringBuffer();
        info.append("cameraId: " + this.cameraId + "\n");
        info.append("defaultOrientation: " + this.defaultOrientation + "\n");
        info.append("zoomCurrent: " + this.zoomCurrent + "\n");
        info.append("isHolderReady: " + this.isHolderReady + "\n");
        info.append("isCameraConfigure: " + this.isCameraConfigure + "\n");
        info.append("isImageHighRes: " + this.isImageHighRes + "\n");
        info.append("isBlackScreen: " + this.isBlackScreen + "\n");
        info.append("isTakingPicture: " + this.isTakingPicture + "\n");
        info.append("isInAutoFocus: " + this.isInAutoFocus + "\n");
        info.append("widgetAction: " + this.widgetAction + "\n");
        info.append("state: " + this.state + "\n");
        info.append("previewSize: ");
        if (this.previewSize != null) {
            info.append(this.previewSize.width + "x" + this.previewSize.height + "\n");
        } else {
            info.append("null\n");
        }
        info.append("captureSize: ");
        if (this.captureSize != null) {
            info.append(this.captureSize.width + "x" + this.captureSize.height + "\n");
        } else {
            info.append("null\n");
        }
        info.append("cameraParameters: ");
        try {
            Camera.Parameters temp = camera.getParameters();
            if (temp != null) {
                this.cameraParameters = temp;
            }
        } catch (RuntimeException e) {
        }
        if (this.cameraParameters != null) {
            info.append("\n" + "-getFocusMode(): " + this.cameraParameters.getFocusMode());
            info.append("\n" + "-getPreviewSize: ");
            if (this.cameraParameters.getPreviewSize() == null) {
                info.append("null");
            } else {
                info.append(this.cameraParameters.getPreviewSize().width + "x" + this.cameraParameters.getPreviewSize().height);
            }
            info.append("\n" + "-getPreviewFormat: " + this.cameraParameters.getPreviewFormat());
            info.append("\n" + "-getSupportedPreviewFormats(): ");
            for (Integer i : this.cameraParameters.getSupportedPreviewFormats()) {
                info.append(i + ",");
            }
            info.append("\n" + "-getSupportedPreviewSizes(): ");
            for (Camera.Size i : this.cameraParameters.getSupportedPreviewSizes()) {
                info.append(i.width + "x" + i.height + ",");
            }
            info.append("\n" + "-getPictureSize: " + this.cameraParameters.getPictureSize().width + "x" + this.cameraParameters.getPictureSize().height);
            info.append("\n" + "-getPictureFormat: " + this.cameraParameters.getPictureFormat());
            info.append("\n" + "-getSupportedPictureFormats(): ");
            for (Integer i : this.cameraParameters.getSupportedPictureFormats()) {
                info.append(i + ",");
            }
            info.append("\n" + "-getSupportedPictureSizes(): ");
            for (Camera.Size i : this.cameraParameters.getSupportedPictureSizes()) {
                info.append(i.width + "x" + i.height + ",");
            }
        } else {
            info.append("null\n");
        }
        info.append("\ncurrent video-quality: " + this.currentCamcorderProfileId + "\n");
        info.append("\nvideo-qualities back-cam: ");
        if (this.videoQualities != null && this.videoQualities[0] != null) {
            for (String quality : videoQualities[0]) {
                info.append(quality + ", ");
            }
        } else {
            info.append("\nnull\n");
        }
        info.append("\nvideo-qualities front-cam: ");
        if (this.videoQualities != null && this.videoQualities.length >= 2 && this.videoQualities[1] != null) {
            for (String quality : videoQualities[1]) {
                info.append(quality + ", ");
            }
        } else {
            info.append("\nnull\n");
        }
        return info.toString();
    }

    public void forceOrientation() {
        log.v(this, "forceOrientation() to " + defaultOrientation);
        activity.setRequestedOrientation(defaultOrientation);
    }

    public void sendEmailCrash(boolean previous) {
        log.disableLogging();
        if (!previous) {
            log.deleteErrorLog();
            if (config.isLogging()) {
                log.renameToErrorLog();
            }
        }
        final File file = new File(config.getCrashLogFilePath());
        CrashHandler.getInstance(null, null).sendEmail(file);
    }

    public boolean pressVolumeDown() {
        log.v(this, "pressVolumeDown()");
        String type = config.getVolumeDownAction();
        if (type.equals("capture")) {
            imageCapture();
            return true;
        } else if (type.equals("auto")) {
            autoImageCapture();
            return true;
        } else if (type.equals("face")) {
            autoFaceCapture();
            return true;
        } else if (type.equals("video")) {
            videoRecording();
            return true;
        }
        return false;
    }

    public boolean pressVolumeUp() {
        log.v(this, "pressVolumeUp()");
        String type = config.getVolumeUpAction();
        if (type.equals("capture")) {
            imageCapture();
            return true;
        } else if (type.equals("auto")) {
            autoImageCapture();
            return true;
        } else if (type.equals("face")) {
            autoFaceCapture();
            return true;
        } else if (type.equals("video")) {
            videoRecording();
            return true;
        }
        return false;
    }

    public void uiResume(SurfaceView svPreview) {
        isUIDisplayed = true;
        if (!crashReport) {
            if (CameraTaskService.state == CameraTaskService.WHAT_STOP) {
                ringerMode = Utility.getSound(activity);
                Utility.setSound(activity, AudioManager.RINGER_MODE_SILENT);
                startCamera(svPreview);
                config.reset();
                if (config.isLogging()) {
                    log.enableLogging(activity);
                } else {
                    log.disableLogging();
                }
                if (isBlackScreen && config.isShowToast()) {
                    showToast(true, Toast.LENGTH_SHORT, activity.getString(R.string.hint_blackmode));
                }
            } else if (CameraTaskService.state == CameraTaskService.WHAT_START_AUTOSHOT
                    || CameraTaskService.state == CameraTaskService.WHAT_START_FACESHOT
                    || CameraTaskService.state == CameraTaskService.WHAT_START_VIDEO_RECORDING) {
                final AlertDialog ad = new AlertDialog.Builder(activity).create();
                ad.setTitle("Running Process");
                if (CameraTaskService.state == CameraTaskService.WHAT_START_AUTOSHOT) {
                    ad.setMessage("Auto mode is activated");
                } else if (CameraTaskService.state == CameraTaskService.WHAT_START_FACESHOT) {
                    ad.setMessage("Face mode is activated");
                } else {
                    ad.setMessage("Video mode is activated");
                }
                ad.setButton(AlertDialog.BUTTON_POSITIVE, "Stop", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(activity, CameraTaskService.class);
                        activity.stopService(intent);
                        intent = new Intent(activity, SpyCamActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                });
                ad.setButton(AlertDialog.BUTTON_NEGATIVE, "Hide", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                });
                ad.show();
            }
        }
    }

    public void uiPause() {
        log.v(this, "uiPause()");
        isUIDisplayed = false;
        if (MainController.this.widgetAction) {
            deactivateWidgetAction();
        }
        if (config.isDisableBackgroundService() || state == STATE_IDLE || state == STATE_IMAGE_SINGLE) {
            stopCamera();
            Utility.setSound(activity, ringerMode);
            isHolderReady = false;
        } else {
            activity.finish();
        }
    }

    public boolean pressBack() {
        if (isBlackScreen) {
            config.setPreviewWidthSize(tempPreviewWidth);
            activity.finish();
            return true;
        } else if (state != STATE_IDLE) {
            Intent intent = new Intent(activity, CameraTaskService.class);
            activity.stopService(intent);
        }
        return false;

    }

    public boolean pressMenu() {
        if (isBlackScreen) {
            switchBlackScreen();
            return true;
        }
        return false;
    }

    public boolean onScaleBlackScreen(ScaleGestureDetector sgd) {
        if (sgd.getScaleFactor() < 1 && isBlackScreen) {
            switchBlackScreen();
        }
        return true;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setUIHandler(Handler handler) {
        this.handler = handler;
    }

    public void activateWidgetAction() {
        this.widgetAction = true;
        if (!config.isDisableBackgroundService()) {
            tempPreviewWidth = config.getPreviewWidthSize();
            config.setPreviewWidthSize(1);
        }
        refreshImagePreviewSize();
    }

    public void deactivateWidgetAction() {
        this.widgetAction = false;
        config.setPreviewWidthSize(tempPreviewWidth);
    }

    public void callWidgetAction(final int action) {
        log.v(this, "callWidgetAction(action:" + action + ")");
        new Thread() {
            @Override
            public void run() {
                activateWidgetAction();

                if (CameraTaskService.state == CameraTaskService.WHAT_START_AUTOSHOT
                        || CameraTaskService.state == CameraTaskService.WHAT_START_FACESHOT
                        || CameraTaskService.state == CameraTaskService.WHAT_START_VIDEO_RECORDING) {
                    Intent intent = new Intent(activity, CameraTaskService.class);
                    activity.stopService(intent);
                    activity.finish();
                    return;
                }

                while (!isCameraConfigure || bSnapShot == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
                if (config.isDisableBackgroundService()) {
                    switchBlackScreen();
                }
                switch (action) {
                    case 1:
                        imageCapture();
                        break;
                    case 2:
                        autoImageCapture();
                        break;
                    case 3:
                        autoFaceCapture();
                        break;
                    case 4:
                        videoRecording();
                        break;
                    default:
                        activity.finish();
                        break;
                }
            }
        }.start();
    }

    private void autoImageCaptureStart() {
        setState(STATE_IMAGE_AUTO);
        showToast(false, Toast.LENGTH_SHORT, activity.getString(R.string.message_startAuto, (config.getAutoCaptureDelay() / 1000)));
        startAutoFocus();
        if (MainController.this.widgetAction && !config.isDisableBackgroundService()) {
            openSetting(); //minimize
        }
    }

    private void autoImageCaptureStop() {
        setState(STATE_IDLE);
        if (!isUIDisplayed) {
            uiPause();
        } else {
            showToast(false, Toast.LENGTH_SHORT, R.string.message_stopAuto);
        }
    }

    private void autoImageCaptureTaskStart() {
        autoImageTask = new AutoShootLocalAsync(config.getAutoCaptureDelay());
        autoImageTask.start();
    }

    private void autoImageCaptureTaskStop() {
    }

    public void videoRecordingStart() {
        startVideoRecording();
        if (state == STATE_VIDEO_RECORDING && MainController.this.widgetAction && !config.isDisableBackgroundService()) {
            openSetting();//minimize
        }
    }

    public void videoRecordingStop() {
        if (state == STATE_VIDEO_RECORDING) {
            stopVideoRecording();
            if (!isUIDisplayed) {
                uiPause();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void autoFaceCaptureStart() {
        setState(STATE_IMAGE_FACE);
        camera.setFaceDetectionListener(new FaceDetectionListener() {
            @Override
            public void onFaceDetection(Face[] faces, Camera camera) {
                long delay = config.getAutoCaptureDelay();
                if (System.currentTimeMillis() - lastCapture > delay - avgAutofocusTime && faces.length > 0) {
                    log.d(this, "Face detected : " + faces.length);
                    startAutoFocus();
                }
            }
        });
        if (cameraParameters.getMaxNumDetectedFaces() > 0) {
            camera.startFaceDetection();
            showToast(false, Toast.LENGTH_SHORT, R.string.message_startFace);
        }
        if (state == STATE_IMAGE_FACE && MainController.this.widgetAction && !config.isDisableBackgroundService()) {
            openSetting();//minimize
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void autoFaceCaptureStop() {
        setState(STATE_IDLE);
        camera.setFaceDetectionListener(null);
        if (cameraParameters.getMaxNumDetectedFaces() > 0) {
            camera.stopFaceDetection();
            showToast(false, Toast.LENGTH_SHORT, R.string.message_stopFace);
        }
        if (!isUIDisplayed) {
            uiPause();
        }
    }

    public void sendEmailViaGMail(final File file) {
        imagesForEmail.add(file);
        if (mailTask == null || !mailTask.isAlive()) {
            mailTask = new GMailSenderTask();
            mailTask.start();
        }
    }

    static class MainControllerHandler extends Handler {
        private MainController mainController;

        public MainControllerHandler(MainController mainController) {
            this.mainController = mainController;
        }

        @Override
        public void handleMessage(Message msg) {
            mainController.log.v(mainController + ".handlerCamera", "handleMessage(msg:" + msg.what + "|" + msg.arg1 + "|" + msg.arg2 + ")");
            switch (msg.what) {
                case WHAT_START_AUTOSHOOT:
                    mainController.autoImageCaptureStart();
                    break;
                case WHAT_CONTINUE_AUTOSHOOT:
                    mainController.startAutoFocus();
                    break;
                case WHAT_STOP_AUTOSHOOT:
                    mainController.autoImageCaptureStop();
                    break;
                case WHAT_START_VIDEO:
                    mainController.videoRecordingStart();
                    break;
                case WHAT_STOP_VIDEO:
                    mainController.videoRecordingStop();
                    break;
                case WHAT_START_FACESHOOT:
                    mainController.autoFaceCaptureStart();
                    break;
                case WHAT_STOP_FACESHOOT:
                    mainController.autoFaceCaptureStop();
                    break;
            }
        }
    }

    private class AutoShootLocalAsync extends Thread {

        int sleep = 0;

        public AutoShootLocalAsync(int sleep) {
            this.sleep = sleep;
        }

        @Override
        public void run() {
            try {
                while (state == STATE_IMAGE_AUTO) {
                    log.v(this, "run(sleep:" + sleep + ")");
                    Thread.sleep(sleep);
                    if (state == STATE_IMAGE_AUTO) {
                        startAutoFocus();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    class GMailSenderTask extends Thread {

        @Override
        public void run() {
            GMailSender sender = new GMailSender(config.getAutoEmailGMailUsername(), config.getAutoEmailGMailPassword());
            int counter = 0;
            File file = null;
            while (imagesForEmail.size() > 0) {
                try {
                    if (!Utility.isOnline(activity)) {
                        log.d(this, "No network, sleep 5seconds");
                        sleep(5000);
                        continue;
                    }
                    counter++;
                    file = imagesForEmail.get(0);
                    sender.sendMail(
                            "SC-OS " + SDF.format(new Date())
                            , config.getAutoEmailGMailReceiver()
                            , file);
                    imagesForEmail.remove(file);
                    counter = 0;
                } catch (AuthenticationFailedException e) {
                    log.e(this, e);
                    if (counter == 1) {
                        showToast(false, Toast.LENGTH_SHORT, activity.getString(R.string.error_auto_email_auth));
                    } else if (counter > 5) {
                        imagesForEmail.remove(file);
                        counter = 0;
                    }
                } catch (Exception e) {
                    if (counter == 1) {
                        showToast(false, Toast.LENGTH_SHORT, activity.getString(R.string.error_auto_email_others));
                    } else if (counter > 5) {
                        imagesForEmail.remove(file);
                        counter = 0;
                    }
                    log.e(this, e);
                }
            }
        }

    }
}
