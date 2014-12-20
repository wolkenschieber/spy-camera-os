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
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;

import com.jwork.spycamera.utility.ConfigurationUtility;
import com.jwork.spycamera.utility.LogUtility;

public class SpyCamWidgetConfigure extends Activity {

	private LogUtility log = LogUtility.getInstance();
	private int mAppWidgetId = -1;
	private Spinner spinnerAction;
	private EditText txtText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log.v(this, "onCreate");
		
		setContentView(R.layout.widget_configure);
		spinnerAction = (Spinner) findViewById(R.id.widgetAction);
		txtText = (EditText) findViewById(R.id.widgetText);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
		    mAppWidgetId = extras.getInt(
		            AppWidgetManager.EXTRA_APPWIDGET_ID, 
		            AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		log.d(this, "mAppWidgetId: " + mAppWidgetId);
	}
	
	public void onClickOK(View view) {
		log.v(this, "onClickOK");

		int action = spinnerAction.getSelectedItemPosition();
		if (action==3 && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			Toast.makeText(this, "Facedetection is only supported for Android 4.0 or newer", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		
		ConfigurationUtility prefs = ConfigurationUtility.getInstance(getApplicationContext());
		prefs.setWidgetConfiguration(mAppWidgetId, action, txtText.getText().toString());
		
		setResult(RESULT_OK, resultValue);

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
		RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(),
				R.layout.widget_camera);
		appWidgetManager.updateAppWidget(mAppWidgetId, views);
		SpyCamWidgetProvider.updateAppWidget(this, appWidgetManager, mAppWidgetId);

		finish();
	}
	
	public void onClickCancel(View view) {
		log.v(this, "onClickCancel");
		setResult(RESULT_CANCELED);
		finish();
	}
	
}
