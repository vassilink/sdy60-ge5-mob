package com.apap.pom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Show POM created map
 */

public class MapsActivity extends AppCompatActivity {

    /////////////////////////////////////////////
    //Debug Tag
    public static final String TAG = MapsActivity.class.getSimpleName();

    private WebView browser;
    private String mapUrl;

    //App Preferences
    SharedPreferences sharedPrefs;
    String prefMode;
    boolean prefMapRoadsOn;
    //String prefLanguage;

    //For call user logout function
    UserFunctions userFunctions;

    //Analytics
    private Tracker mTracker;
    private FirebaseAnalytics mFirebaseAnalytics;

    /////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the shared Tracker instance.
        PomApplication application = ((PomApplication) getApplication());
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("SHOW MAP");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setCurrentScreen(this, "SHOW MAP", "MapsActivity");

        //Check if user logged in (from database) - else load login activity
        userFunctions = new UserFunctions(getApplicationContext());

        Intent intent = getIntent();
        //int mapsID = intent.getExtras().getInt("mapsID");
        double lat = intent.getExtras().getDouble("lat");
        double lon = intent.getExtras().getDouble("lon");

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefMode = sharedPrefs.getString("appMode", "1");
        prefMapRoadsOn = sharedPrefs.getBoolean("appMapRoads", false);
        //prefLanguage = sharedPrefs.getString("appLanguage", "1");

        setTitle(R.string.map_of_greece);

        //Server Url
        if(prefMode.equals("1"))
            mapUrl = ((PomApplication) getApplicationContext()).getServerUrl() + "/PathsMap.php?lat="+lat+"&lon="+lon;
        else
            mapUrl = ((PomApplication) getApplicationContext()).getServerUrl() + "/CyclePathsMap.php?lat="+lat+"&lon="+lon;

        setContentView(R.layout.activity_maps);

        browser = (WebView)findViewById(R.id.wvMaps);

        // my_child_toolbar is defined in the layout file
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        //Show toolbar icon
        ab.setIcon(R.mipmap.ic_launcher);

        //Check if there is a network connection
        ConnectionDetector mConnectionDetector = new ConnectionDetector(getApplicationContext());
        if(mConnectionDetector.isNetworkConnected()>0 && mConnectionDetector.isInternetAvailable()==true)
        {
            setUpWebViewDefaults(browser);

            //Load website with map - redirection to open in webview and not with browser
            browser.setWebViewClient(new WebViewClient(){

                public void onPageFinished(WebView view, String url) {
                    //Show info message - inform user tha start async load of paths
                    Toast.makeText(getApplicationContext(), "Τhe map is loading",
                            Toast.LENGTH_LONG).show();
                }

            });

            //Load Map
            browser.loadUrl(mapUrl);

        }else{
            //Info message about connectivity problem
            Toast.makeText(getApplicationContext(), R.string.internet_connection_required, Toast.LENGTH_LONG).show();
            finish();
        }
    }


    /*
     *
     */
    private void setUpWebViewDefaults(WebView webView){

        WebSettings settings = webView.getSettings();

        //Enable Javascript
        settings.setJavaScriptEnabled(true);

        browser.getSettings().setLoadsImagesAutomatically(true);

        //Enable remote debugging through Chrome
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }


    /*
     * Menu related functions
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maps, menu);
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                setResult(RESULT_OK, null);
                finish();
                return  true;

            case R.id.submenu_review_paths:
                Intent review_intent = new Intent(MapsActivity.this, ReviewPathActivity.class);
                startActivity(review_intent);
                finish();
                return  true;

            case R.id.submenu_rank_list_of_players:
                Intent ranking_intent = new Intent(MapsActivity.this, GoogleGamesActivity.class); //NEW WITH GOOGLE PLAY GAMES SERVICES
                startActivity(ranking_intent);
                finish();
                return  true;

            case R.id.submenu_settings:
                Intent edit_intent = new Intent(MapsActivity.this, SettingsActivity.class);
                startActivity(edit_intent);
                return  true;

            case R.id.submenu_log_out:
                //Clear previous saved login values
                userFunctions.logoutUser();
                Intent login_intent = new Intent(MapsActivity.this, SignInActivity.class);
                login_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                login_intent.putExtra("logout", true);
                startActivity(login_intent);
                finish();
                return  true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
