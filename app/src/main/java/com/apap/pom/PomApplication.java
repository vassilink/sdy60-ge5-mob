package com.apap.pom;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Application class
 */
public class PomApplication extends Application {

    //Analytics
    private Tracker mTracker;

    //add this to solve problem with API Level < 20
    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    // local server
    private String serverUrl = "http://webeasy.gr/projects/eap/sdy60/ge5/pom";
    //private String serverUrl = "http://94.130.5.72/~webeasy/projects/eap/sdy60/ge5/pom";

    // test server
    //private String serverUrl = "http://.../pom";

    //live server
    //private String serverUrl = "...";

    public String getServerUrl(){
        return serverUrl;
    }
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    //Set city of interest: 0 - Greece, 1 - Athens, 2 - Corfu, 3 - Athens Historical Center
    //private int cityOfInterest = 0;
    //private int cityOfInterest = 1;
    //private int cityOfInterest = 2;
    private int cityOfInterest = 1;

    public int getCityOfInterest() {
        return cityOfInterest;
    }
    public void setCityOfInterest(int cityOfInterest) {
        this.cityOfInterest = cityOfInterest;
    }

    //Check if app is first time to start
    private boolean onCreateApp = true;
    public boolean getOnCreateApp() {return onCreateApp;}
    public void setOnCreateApp(boolean onCreateApp) {this.onCreateApp=onCreateApp;}

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker("UA-58907-9");//R.xml.global_tracker);
        }
        return mTracker;
    }
}
