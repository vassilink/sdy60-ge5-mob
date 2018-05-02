package com.apap.pom;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;


/**
 * Settings of application
 */

public class SettingsActivity extends AppCompatActivity {

    //Debug Tag
    public static final String TAG = SettingsActivity.class.getSimpleName();

    //Analytics
    private FirebaseAnalytics mFirebaseAnalytics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setCurrentScreen(this, "SETTINGS", "SettingsActivity");

        // my_child_toolbar is defined in the layout file
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        //Show toolbar icon
        ab.setIcon(R.mipmap.ic_launcher);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }

    /*
     * Settings Fragment
     */
    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        //Analytics
        private Tracker mTracker;
        private FirebaseAnalytics mFirebaseAnalytics;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            Log.d(TAG, "--->onCreate");

            // Obtain the shared Tracker instance.
            PomApplication application = ((PomApplication) this.getActivity().getApplication());
            mTracker = application.getDefaultTracker();
            mTracker.setScreenName("SETTINGS");
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());

            // Obtain the FirebaseAnalytics instance.
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this.getActivity().getApplicationContext());

            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

            ListPreference mode = (ListPreference) findPreference("appMode");
            mode.setSummary(mode.getEntry());

            ListPreference language = (ListPreference) findPreference("appLanguage");
            language.setSummary(language.getEntry());
        }


        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            Preference pref = findPreference(key);
            ListPreference lp;
            switch(key){
                case "appMode":
                    lp = (ListPreference) pref;
                    pref.setSummary(lp.getEntry());
                    mFirebaseAnalytics.setUserProperty("appMode", lp.getValue());
                    break;

                case "appMapRoads":
                    if(sharedPreferences.getBoolean("appMapRoads", false)) mFirebaseAnalytics.setUserProperty("appMode", getResources().getString(R.string.msg_enabled));
                    else mFirebaseAnalytics.setUserProperty("appMode", getResources().getString(R.string.msg_disabled));
                    break;

                case "appMapWalkPaths":
                    if(sharedPreferences.getBoolean("appMapWalkPaths", false)) mFirebaseAnalytics.setUserProperty("showWalkPaths", getResources().getString(R.string.msg_enabled));
                    else mFirebaseAnalytics.setUserProperty("showWalkPaths", getResources().getString(R.string.msg_disabled));
                    break;

                case "appMapPendPaths":
                    if(sharedPreferences.getBoolean("appMapPendPaths", false)) mFirebaseAnalytics.setUserProperty("showPendPaths", getResources().getString(R.string.msg_enabled));
                    else mFirebaseAnalytics.setUserProperty("showPendPaths", getResources().getString(R.string.msg_disabled));
                    break;

                case "appLanguage":
                    lp = (ListPreference) pref;
                    pref.setSummary(lp.getEntry());
                    mFirebaseAnalytics.setUserProperty("appLanguage", lp.getValue());
                    Toast.makeText(getActivity().getApplicationContext(), R.string.msg_language_change, Toast.LENGTH_LONG).show();
                    break;

                default:
                    break;
            }

        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
