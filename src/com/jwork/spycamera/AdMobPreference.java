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
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class AdMobPreference extends Preference {

    public AdMobPreference(Context context, AttributeSet attrs, int defStyle) {super    (context, attrs, defStyle);}
    public AdMobPreference(Context context, AttributeSet attrs) {super(context, attrs);}
    public AdMobPreference(Context context) {super(context);}

    @Override
    protected View onCreateView(ViewGroup parent) {
        // this will create the linear layout defined in ads_layout.xml
        View view = super.onCreateView(parent);

        // the context is a PreferenceActivity
        Activity activity = (Activity)getContext();

        // Create the adView
        AdView adView = new AdView(activity, AdSize.BANNER, "a1515a1ffdbf894");

        ((LinearLayout)view).addView(adView);

        // Initiate a generic request to load it with an ad
        AdRequest request = new AdRequest();
        adView.loadAd(request);     

        return view;    
    }
}