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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.util.Log;

/**
 * @author Jimmy Halim
 */
public class LogUtility {
	
	private java.text.DateFormat formatter;
	private static final String SEPARATOR = "||";
	private static LogUtility instance;
	private boolean logging = false;
	private BufferedWriter fw;
	
	private LogUtility() {
	}
	
	public void enableLogging(Context context) {
		formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		ConfigurationUtility config = ConfigurationUtility.getInstance(context);
		File file = new File(config.getSavingPathPrimary() + File.separator + "logging.txt");
		if (file.exists() && file.isFile() && file.length()>102400) {
			file.delete();
		}
		try {
			file.createNewFile();
			fw = new BufferedWriter(new FileWriter(file, true));
			this.logging = true;
		} catch (IOException e) {
			e(this, e);
		}
	}
	
	public void disableLogging() {
		logging = false;
		if (fw!=null) {
			try {
				fw.flush();
			} catch (IOException e) {}
			try {
				fw.close();
			} catch (IOException e) {}
			fw = null;
		}
	}

	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (fw!=null) {
			try {
				fw.close();
			} catch (Exception e) {}
		}
	}
	
	public synchronized static LogUtility getInstance() {
		if (instance==null) {
			instance = new LogUtility();
		}
		return instance;
	}
	
	public void v(Object obj, String message) {
		if (logging) {
			Log.v(convertTag(obj), message);
			try {
				fw.write(formatter.format(new Date()) + SEPARATOR + "[V]" + SEPARATOR + convertTag(obj) + SEPARATOR + message + "\n");
				fw.flush();
			} catch (IOException e) {}
		}
	}

	private String convertTag(Object obj) {
		if (obj instanceof String) {
			return "c:"+obj.toString();
		}
		return "c:"+obj.getClass().getSimpleName();
	}

	public void v(Class<?> cls, String message) {
		Log.v("c:"+cls.getSimpleName(), message);
		if (logging) {
			try {
				fw.write(formatter.format(new Date()) + SEPARATOR + "[V]" + SEPARATOR + "c:"+cls.getSimpleName() + SEPARATOR + message + "\n");
				fw.flush();
			} catch (IOException e) {}
		}
	}

	public void d(Object obj, String message) {
		if (logging) {
			Log.d(convertTag(obj), message);
			try {
				fw.write(formatter.format(new Date()) + SEPARATOR + "[D]" + SEPARATOR + convertTag(obj) + SEPARATOR + message + "\n");
				fw.flush();
			} catch (IOException e) {}
		}
	}

	public void i(Object obj, String message) {
		if (logging) {
			Log.i(convertTag(obj), message);
			try {
				fw.write(formatter.format(new Date()) + SEPARATOR + "[I]" + SEPARATOR + convertTag(obj) + SEPARATOR + message + "\n");
				fw.flush();
			} catch (IOException e) {}
		}
	}
	
	public void w(Object obj, String message) {
		if (logging) {
			Log.w(convertTag(obj), message);
			try {
				fw.write(formatter.format(new Date()) + SEPARATOR + "[W]" + SEPARATOR + convertTag(obj) + SEPARATOR + message + "\n");
				fw.flush();
			} catch (IOException e) {}
		}
	}
	
	public void e(Object obj, String message) {
		Log.e(convertTag(obj), message);
		if (logging) {
			try {
				fw.write(formatter.format(new Date()) + SEPARATOR + "[E]" + SEPARATOR + convertTag(obj) + SEPARATOR + message + "\n");
				fw.flush();
			} catch (IOException e) {}
		}
	}
	
	public void w(Object obj, Throwable e) {
		if (logging) {
			Log.e(convertTag(obj), e.getClass().getName(), e);
			try {
				fw.write(formatter.format(new Date()) + SEPARATOR + "[W]" + SEPARATOR + convertTag(obj) + SEPARATOR + e + "\n");
				StackTraceElement[] arr = e.getStackTrace();
				for (int i=0; i<arr.length; i++)
				{
					fw.write("    "+arr[i].toString()+"\n");
				}
				Throwable cause = e.getCause();
				if(cause != null) {
					fw.write("Cause: " + cause.toString() + "\n");
					arr = cause.getStackTrace();
					for (int i=0; i<arr.length; i++)
					{
						fw.write("    "+arr[i].toString()+"\n");
					}
				}
				fw.flush();
			} catch (IOException e2) {}
		}
	}
	
	public void e(Object obj, Throwable e) {
		if (logging) {
			Log.e(convertTag(obj), e.getClass().getName(), e);
			try {
				fw.write(formatter.format(new Date()) + SEPARATOR + "[E]" + SEPARATOR + convertTag(obj) + SEPARATOR + e + "\n");
				StackTraceElement[] arr = e.getStackTrace();
				for (int i=0; i<arr.length; i++)
				{
					fw.write("    "+arr[i].toString()+"\n");
				}
				Throwable cause = e.getCause();
				if(cause != null) {
					fw.write("Cause: " + cause.toString() + "\n");
					arr = cause.getStackTrace();
					for (int i=0; i<arr.length; i++)
					{
						fw.write("    "+arr[i].toString()+"\n");
					}
				}
				fw.flush();
			} catch (IOException e2) {}
		}
	}

	public void renameToErrorLog() {
		String savingPath = ConfigurationUtility.getInstance(null).getSavingPathPrimary();
		File file = new File(savingPath +File.separator + "logging.txt");
		File fileTarget = new File(savingPath +File.separator + "loggingError.txt");
//		file.renameTo(new File(savingPath +File.separator + "loggingError.txt"));
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(file);
			out = new FileOutputStream(fileTarget);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (Exception e) {
			}
			try {
				out.close();
			} catch (Exception e) {
			}
		}
	}

	public void deleteErrorLog() {
		String savingPath = ConfigurationUtility.getInstance(null).getSavingPathPrimary();
		File file = new File(savingPath +File.separator + "loggingError.txt");		
		file.delete();
	}

	public void flushLogging(Context context) {
		disableLogging();
		ConfigurationUtility config = ConfigurationUtility.getInstance(context);
		File file = new File(config.getSavingPathPrimary() + File.separator + "logging.txt");
		try {
			fw = new BufferedWriter(new FileWriter(file, true));
			this.logging = true;
		} catch (IOException e) {
			e(this, e);
		}
	}
}
