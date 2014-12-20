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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.jwork.spycamera.MainController;

/**
 * @author Jimmy Halim
 */
public class CrashHandler implements UncaughtExceptionHandler {
	
	private static CrashHandler instance;
	private Activity activity;
	private ConfigurationUtility prefs;
	private LogUtility log;
	private UncaughtExceptionHandler defaultUEH;
	private Factory factory;
	
	private CrashHandler(Activity context, UncaughtExceptionHandler defaultUEH) {
		this.activity = context;
		this.defaultUEH = defaultUEH;
		this.prefs = ConfigurationUtility.getInstance(context);
		this.log = LogUtility.getInstance();
		this.factory = Factory.getInstance();
	}

	public synchronized static CrashHandler getInstance(Activity context, UncaughtExceptionHandler defaultUEH) {
		if (instance==null) {
			instance = new CrashHandler(context, defaultUEH);
		}
		return instance;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		MainController controller = null;
		ConfigurationUtility config = ConfigurationUtility.getInstance(null);
		try {
			log.e(this, ex);
			log.disableLogging();
			log.deleteErrorLog();
			log.renameToErrorLog();
			controller = factory.getMainController(null, null);
			createErrorReportFile(ex);
			File directory = new File(config.getSavingPathPrimary());
			File file = new File( directory.getAbsolutePath()+ "/error.trace");
			FileOutputStream trace = null;
			try {
				trace = new FileOutputStream(file);
				if (ex.toString().indexOf("java.lang.OutOfMemoryError")>=0) {
					trace.write(1);
				} else {
					trace.write(0);
				}
			} finally {
				try {
					trace.close();
				} catch (Exception e) {}
			}
			prefs.clear(true);
		} catch (Throwable ignore) {
			log.e(this, ignore);
		} finally {
			try {
				controller.stopCamera();
			} catch (Throwable e) {}
			defaultUEH.uncaughtException(thread, ex);
		}
	}

	public void sendEmail(File file)
	{
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));
			String line;
			StringBuffer report = new StringBuffer();
			while ((line=br.readLine())!=null) {
				report.append(line);
				report.append("\n");
			}
	
			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			String subject = "[Spy Camera OS] Error report";
			sendIntent.putExtra(Intent.EXTRA_EMAIL,
					new String[] {"jwork.spy.camera.os@gmail.com"});
			sendIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
			
			File logFile = new File(ConfigurationUtility.getInstance(null).getSavingPathPrimary()+File.separator+"loggingError.txt"); 
			
			if (logFile.exists()) {
				sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));
			}
			
			sendIntent.setType("message/rfc822");
	
			activity.startActivity(Intent.createChooser(sendIntent, "Email:"));
	
		} catch(Exception e) {
			Log.v("sendmail", e.toString());
		} finally {
			if (br!=null) {
				try {
					br.close();
				} catch (IOException e) {}
			}
		}
	}

	private File createErrorReportFile(Throwable ex) {
		MainController controller = factory.getMainController(null, null);
		ConfigurationUtility config = ConfigurationUtility.getInstance(null);
		String info = controller.errorReport();
		
		String report = Utility.createErrorReport(ex, activity, info);
		log.e(this, "report " + report);
		File file = null;
		FileOutputStream trace = null;
		try {
			File directory = new File(config.getSavingPathPrimary());
			if (!directory.exists()) {
				directory.mkdir();
			}
			file = new File( directory.getAbsolutePath()+ "/stack.trace");
			trace = new FileOutputStream(file);
			trace.write(report.getBytes());
		} catch(IOException ioe) {
			log.w(this, ioe);
		} finally {
			if (trace!=null) {
				try {
					trace.close();
				} catch (IOException e) {}
			}
		}
		return file;
	}

}
