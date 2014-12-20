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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.jwork.spycamera.utility.ConfigurationUtility;
import com.jwork.spycamera.utility.LogUtility;

public class CameraTaskService extends Service {

	public static final String EXTRA_MESSENGER = "MESSENGER";
	public static final String EXTRA_TYPE = "TYPE";
	public static final String EXTRA_ACTION = "ACTION";
	public static final String NAME = "CameraService";

	public static final int WHAT_STOP = 0;
	public static final int WHAT_START_AUTOSHOT = 1;
	public static final int WHAT_START_FACESHOT = 2;
	public static final int WHAT_START_VIDEO_RECORDING = 3;

	public static final int NOTIFICATION_ID = 1;
	
	public static int state = WHAT_STOP;

	private LogUtility log = LogUtility.getInstance();
	private boolean running = false;
	private AsyncTask<Integer, ?, ?> task = null;
	private Messenger outMessenger;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log.v(this, "onStartCommand(startId:"+startId+")");
		if (intent==null) {
			log.w(this, getString(R.string.error_start_service_others));
			Toast.makeText(this, R.string.error_start_service_others, Toast.LENGTH_LONG).show();
			stopSelf();
			return START_NOT_STICKY;
		}
		int action = intent.getIntExtra(EXTRA_ACTION, -1);
		outMessenger = (Messenger)intent.getExtras().get(EXTRA_MESSENGER);
		switch (action) {
		case WHAT_START_AUTOSHOT:
			startAutoShot();
			break;
		case WHAT_START_FACESHOT:
			startFaceShot();
			break;
		case WHAT_START_VIDEO_RECORDING:
			startVideoRecording();
			break;
		}
		return START_STICKY;
	}
	
	private void startVideoRecording() {
		CameraTaskService.setState(WHAT_START_VIDEO_RECORDING);
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, createStopNotification("Running"));
		Message backMsg = Message.obtain();
		backMsg.what = MainController.WHAT_START_VIDEO;
		try {
			this.outMessenger.send(backMsg);
		} catch (android.os.RemoteException e1) {
			log.w(this, e1);
		}

	}

	private void startFaceShot() {
		CameraTaskService.setState(WHAT_START_FACESHOT);
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, createStopNotification("Detect"));
		Message backMsg = Message.obtain();
		backMsg.what = MainController.WHAT_START_FACESHOOT;
		try {
			this.outMessenger.send(backMsg);
		} catch (android.os.RemoteException e1) {
			log.w(this, e1);
		}
	}

	@Override
	public void onDestroy() {
		log.v(this, "onDestroy()");
		super.onDestroy();
		running = false;
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID);

		if (state==WHAT_START_AUTOSHOT) {
			Message backMsg = Message.obtain();
			backMsg.what = MainController.WHAT_STOP_AUTOSHOOT;
			try {
				outMessenger.send(backMsg);
			} catch (android.os.RemoteException e1) {
				log.w(this, e1);
			}
		} else if (state==WHAT_START_VIDEO_RECORDING) {
			Message backMsg = Message.obtain();
			backMsg.what = MainController.WHAT_STOP_VIDEO;
			try {
				outMessenger.send(backMsg);
			} catch (android.os.RemoteException e1) {
				log.w(this, e1);
			}
		} else if (state==WHAT_START_FACESHOT) {
			Message backMsg = Message.obtain();
			backMsg.what = MainController.WHAT_STOP_FACESHOOT;
			try {
				outMessenger.send(backMsg);
			} catch (android.os.RemoteException e1) {
				log.w(this, e1);
			}
		}
		setState(WHAT_STOP);
	}
	
	private void startAutoShot() {
		running = true;
		
		Notification notification = createStopNotification("Auto");

		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, notification);

		Message backMsg = Message.obtain();
		backMsg.what = MainController.WHAT_START_AUTOSHOOT;
		try {
			outMessenger.send(backMsg);
		} catch (android.os.RemoteException e1) {
			log.w(this, e1);
		}
		CameraTaskService.setState(WHAT_START_AUTOSHOT);
		if (task!=null) {
			task.cancel(true);
		}
		task = new AutoShootServiceAsync(outMessenger);
		task.execute(ConfigurationUtility.getInstance(this).getAutoCaptureDelay());
		
	}

	private Notification createStopNotification(String label) {
		Intent intent = new Intent(this, NotificationActivity.class);
		intent.putExtra(EXTRA_TYPE, NAME);
		intent.putExtra(EXTRA_ACTION, WHAT_STOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		Notification notification = new NotificationCompat.Builder(this)
		.setContentTitle("SC-OS")
		.setContentText(label)
		.setTicker("SC-OS: " + label)
		.setAutoCancel(true)
		.setSmallIcon(R.drawable.io)
		.setContentIntent(pendingIntent)
		.build();
		return notification;
	}
	
	public static void setState(int state) {
		CameraTaskService.state = state;
	}

	@Override
	public IBinder onBind(Intent intent) {
		log.v(this, "onBind()");
		return null;
	}
	
	class AutoShootServiceAsync extends AsyncTask<Integer, Void, Void> {
		
		private Messenger messenger;
		
		public AutoShootServiceAsync(Messenger messenger) {
			this.messenger = messenger;
		}
		
		@Override
		protected Void doInBackground(Integer... params) {
			try {
				while (state==WHAT_START_AUTOSHOT && running) {
					log.v(this, "doInBackground(sleep:" + params[0] + ")");
					Thread.sleep(params[0]);
					if (state==WHAT_START_AUTOSHOT && running) {
						Message backMsg = Message.obtain();
						backMsg.what = MainController.WHAT_CONTINUE_AUTOSHOOT;
						try {
							messenger.send(backMsg);
						} catch (android.os.RemoteException e1) {
							log.w(this, e1);
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

}
