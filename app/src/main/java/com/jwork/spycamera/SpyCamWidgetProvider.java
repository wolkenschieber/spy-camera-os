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
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.jwork.spycamera.utility.ConfigurationUtility;
import com.jwork.spycamera.utility.LogUtility;

public class SpyCamWidgetProvider extends AppWidgetProvider {

	private LogUtility log = LogUtility.getInstance();

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		log.v(this, "onEnabled");
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		log.v(this, "onDisabled");
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		log.v(this, "onDeleted:"+Arrays.toString(appWidgetIds));
		for (int id:appWidgetIds) {
			ConfigurationUtility.getInstance(context).deleteWidgetConfiguration(id);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		log.v(this, "onUpdate:"+Arrays.toString(appWidgetIds));
		ConfigurationUtility config = ConfigurationUtility.getInstance(context);
		for (int id : appWidgetIds) {
			int action = config.getWidgetConfigurationAction(id);
			if (action==-1) {
				action = 0;
			}
			Intent intent = new Intent(context, SpyCamActivity.class);
			intent.setAction(SpyCamActivity.ACTION_WIDGET[action]);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

			RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
					R.layout.widget_camera);
			remoteViews.setTextViewText(R.id.widgetText, config.getWidgetConfigurationText(id));
			remoteViews.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);
			appWidgetManager.updateAppWidget(id, remoteViews);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		log.v(this, "onReceive:"+id);
		if (id!=-1) {
			onUpdate(context, AppWidgetManager.getInstance(context), new int[]{id});
		} else {
			ComponentName thisWidget = new ComponentName(context, SpyCamWidgetProvider.class);
			int[] ids=AppWidgetManager.getInstance(context).getAppWidgetIds(thisWidget);
			onUpdate(context, AppWidgetManager.getInstance(context), ids);
		}
	}

	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
			int appWidgetId){
		LogUtility log = LogUtility.getInstance();
		log.v(SpyCamWidgetProvider.class, "updateAppWidget(id:"+appWidgetId+")");

		new SpyCamWidgetProvider().onUpdate(context, appWidgetManager, new int[]{appWidgetId});
	}

}
