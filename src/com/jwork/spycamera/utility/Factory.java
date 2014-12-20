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

import android.app.Activity;
import android.os.Handler;

import com.jwork.spycamera.MainController;
import com.jwork.spycamera.MainFragment;
import com.jwork.spycamera.MainHandler;

/**
 * @author Jimmy Halim
 */
public class Factory {

	private static Factory instance = null;
	
	public static void reset() {
		instance = null;
	}
	
	public synchronized static Factory getInstance() {
		if (instance==null) {
			instance = new Factory();
		}
		return instance;
	}

	private MainHandler mainHandler;
	public synchronized MainHandler getMainHandler(MainFragment mainFragment) {
		if (mainHandler==null) {
			mainHandler = new MainHandler(mainFragment);
		} else {
			mainHandler.setFragment(mainFragment);
		}
		return mainHandler;
	}

	private MainController mainController;
	public synchronized MainController getMainController(Activity activity,
			Handler handler) {
		if (mainController==null) {
			mainController = new MainController(activity, handler);
		} else {
			mainController.setActivity(activity);
			mainController.setUIHandler(handler);
		}
		return mainController;
	}
	
}
