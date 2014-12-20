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
import android.content.Intent;
import android.os.Bundle;

import com.jwork.spycamera.utility.LogUtility;

public class NotificationActivity extends Activity {

    private LogUtility log;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = LogUtility.getInstance();
        log.v(this, "onCreate");
        setContentView(R.layout.activity_notification);
		Intent intent = new Intent(this, CameraTaskService.class);
		stopService(intent);
		finish();
    }

}