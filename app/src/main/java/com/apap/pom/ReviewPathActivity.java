
/*
Icons:

<div>
 Icons made by
 <a href="http://www.flaticon.com/authors/simpleicon" title="SimpleIcon">SimpleIcon</a>
 from <a href="http://www.flaticon.com" title="Flaticon">www.flaticon.com</a>
 is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a>
 </div>

 */

package com.apap.pom;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.Achievements;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.maps.GeoApiContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Review paths OR Suggest correction of paths
 */

public class ReviewPathActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnPolylineClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    /////////////////////////////////////////////

    //Debug Tag
    public static final String TAG = ReviewPathActivity.class.getSimpleName();

    // Update interval in milliseconds
    private static final long UPDATE_INTERVAL = 8000;

    //Limit of updates in milliseconds
    private static final long FASTEST_INTERVAL = 3000;

    //map from Google
    private GoogleMap mMap;

    //Map Fragment
    SupportMapFragment mapFragment;

    //Google API Client
    GoogleApiClient mGoogleApiClient = null;

    //Location Request
    LocationRequest mLocationRequest;

    //My last location
    Location mLastLocation;

    //My last location load paths
    Location loadPathsLocation;

    //Location Accuracy
    float mAccuracy;

    //URLs
    private static String request_path_url;
    private static String show_no_path_url;
    private static String show_path_url;
    private static String storeReviewURL;

    //TAGs
    private static String REQUEST_TAG = "pathRequestRadius";        //Tag for request paths
    private static String STORE_REVIEW_TAG = "storeReview";         //Tag for review a path
    private static String STORE_SUGGEST_TAG = "storeSuggestedPath"; //Tag for suggest a path
    private static String STORE_REVIEW_SKETCH_TAG = "storeSugPathReview"; //Tag for suggest a path


    private int path_id;                    //Path id
    private int user_id;                    //Player uid
    private String user_email;              //Player email

    //UI Elements
    private ProgressDialog pDialog;         //Για να δείξει στον χρήστη ότι αιτείται ένα μονοπάτι
    private Button btnSubmit;               //Το κουμπί που ο παίκτης θα υποβάλει το review του
    private Button btnDiscard;              //Το κουμπί για να μπορεί ο παίκτης να απορρίψει το μονοπάτι
    private ImageButton btnShowInfo;        //Το κουμπί για να εμφανιστεί πληροφορία στο χρήστη σχετικά με τον τρόπο review ή suggest
    private ImageButton btnUndoLastAction;  //Το κουμπί για την αναίρεση της τελευταίας κίνησης του χρήστη κατά το suggestion
    private Spinner spinnerReview;          //Για επιλογή κριτικής μονοπατιού
    private Spinner spinnerReviewTags;      //Για επιλογή κριτικής ετικετών
    private Spinner spinnerNewPath;         //Για επιλογή νέου μονοπατιού
    private TextView tvSelectPath;          //Για εμφάνιση στο χρήστη οδηγιών

    //Json parser object
    JSONParser jParser = new JSONParser();

    //App Preferences
    SharedPreferences sharedPrefs;
    String prefMode;
    boolean prefMapRoadsOn;
    boolean prefMapWalkPathsOn;
    boolean prefMapPendPathsOn;
    //String prefLanguage;

    //User
    private UserFunctions userFunctions;


    private static final int REQUEST_CHECK_SETTINGS = 2;
    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private boolean mRequestingLocationUpdates = true;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "location_updates_key";
    private static final String LOCATION_KEY = "location_key";

    //Suggested Path from user
    int startSuggestPolyline=0;

    //Lists of points
    List<LatLng> suggestedPlPoints;
    List<LatLng> snappedToRoadPlPoints;

    //Markers
    Marker suggestMarkerStart;
    Marker suggestMarkerEnd;

    //Positions
    LatLng suggestStartPos;
    LatLng suggestEndPos;

    //Polylines
    Polyline lastSelectedPolyline;
    Polyline lastSketchPendingSelectedPolyline;
    Polyline lastSketchAcceptedSelectedPolyline;
    Polyline suggestedPolyline;

    //Other
    private boolean downloadPaths = false;
    int review_sketch;
    public int cityOfInterest;
    private LinearLayout reviewSuggestLayout;
    private PopupWindow infoPopupWindow;
    boolean FirstShowInfoPopup;

    /**
     * The API context used for the Roads and Geocoding web service APIs.
     */
    private GeoApiContext mContext;

    /**
     * The number of points allowed per API request. This is a fixed value.
     */
    private static final int PAGE_SIZE_LIMIT = 100;

    /**
     * Define the number of data points to re-send at the start of subsequent requests. This helps
     * to influence the API with prior data, so that paths can be inferred across multiple requests.
     * You should experiment with this value for your use-case.
     */
    private static final int PAGINATION_OVERLAP = 5;

    //Analytics
    private Tracker mTracker;
    private FirebaseAnalytics mFirebaseAnalytics;

    /////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(TAG, "---> onCreate");

        setContentView(R.layout.activity_review_path);

        // Obtain the shared Tracker instance.
        PomApplication application = ((PomApplication) getApplication());
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("REVIEW PATH");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setCurrentScreen(this, "REVIEW PATH", "ReviewPathActivity");

        // my_child_toolbar is defined in the layout file
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        //Show toolbar icon
        ab.setIcon(R.mipmap.ic_launcher);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefMode = sharedPrefs.getString("appMode", "1");
        prefMapRoadsOn = sharedPrefs.getBoolean("appMapRoads", false);
        prefMapWalkPathsOn = sharedPrefs.getBoolean("appMapWalkPaths", true);
        prefMapPendPathsOn = sharedPrefs.getBoolean("appMapPendPaths", false);
        //prefLanguage = sharedPrefs.getString("appLanguage", "1");

        //Server Paths
        request_path_url = ((PomApplication) getApplicationContext()).getServerUrl() + "/requestPath.php";
        show_path_url = ((PomApplication) getApplicationContext()).getServerUrl() + "/";

        storeReviewURL = ((PomApplication) getApplicationContext()).getServerUrl() + "/storeReview.php";
        show_no_path_url = ((PomApplication) getApplicationContext()).getServerUrl() + "/noPath.php?path=";

        cityOfInterest = 0;
        cityOfInterest = ((PomApplication) getApplication()).getCityOfInterest();

        // Get the widgets reference from XML layout
        reviewSuggestLayout = (LinearLayout) findViewById(R.id.activity_review_path);

        //UI Elements
        spinnerReview = (Spinner) findViewById(R.id.spinnerReviewPath);
        spinnerReviewTags = (Spinner) findViewById(R.id.spinnerReviewTag);
        spinnerNewPath = (Spinner) findViewById(R.id.spinnerNewPath);

        btnSubmit = (Button) findViewById(R.id.btnReviewSubmit);
        btnDiscard = (Button) findViewById(R.id.btnReviewDiscard);
        tvSelectPath = (TextView) findViewById(R.id.tv_review_descr_1);
        btnShowInfo = (ImageButton) findViewById(R.id.btn_show_info);
        btnShowInfo.setOnClickListener(new View.OnClickListener() {

           @Override
           public void onClick(View view) {
                InfoPopup();
           }
        });

        btnUndoLastAction = (ImageButton) findViewById(R.id.btnUndo);
        btnUndoLastAction.setVisibility(View.INVISIBLE);
        btnUndoLastAction.setClickable(false);
        btnUndoLastAction.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                UndoSuggestAction();
            }
        });

        tvSelectPath.setText(R.string.select_path_for_review_1);
        btnSubmit.setText(R.string.submit_review);
        btnSubmit.setClickable(false);
        btnDiscard.setText(R.string.discard_review);

        spinnerReview.setSelection(4);
        spinnerReviewTags.setSelection(4);
        spinnerNewPath.setSelection(0);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        //Map Fragment
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.review_map);
        mapFragment.getMapAsync(this);

        //Initializations
        FirstShowInfoPopup = true;

        suggestMarkerStart = null;
        suggestMarkerEnd = null;
        suggestStartPos = null;
        suggestEndPos = null;

        suggestedPolyline = null;
        suggestedPlPoints=null;
        snappedToRoadPlPoints=null;
        startSuggestPolyline=0;

        mAccuracy = 0;

        lastSelectedPolyline = null;
        lastSketchPendingSelectedPolyline = null;
        lastSketchAcceptedSelectedPolyline = null;

        loadPathsLocation = null;

        review_sketch = 0;

        path_id=0;
        downloadPaths = false;

        //Context
        mContext = new GeoApiContext().setApiKey(getString(R.string.google_maps_key));

        //User Id
        userFunctions = new UserFunctions(getApplicationContext());
        user_id = userFunctions.getUserUid();
        user_email = userFunctions.getUserEmail();
        Log.d(TAG, "user_id:"+user_id+" user_email:"+user_email);
    }


    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "---> onPause");

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }
/*
    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "---> onResume");

        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }
*/
    public void onSaveInstanceState(Bundle savedInstanceState) {


        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mLastLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d(TAG, "---> onConnected");

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onLocationChanged(Location location) {

        if(mLastLocation != location && mLastLocation==null)
        {
            //Set camera position options
            CameraPosition cameraPosition = CameraPosition.builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      //current position
                    .zoom(16)           //Streets Level zoom
                    .bearing(0)         //Orientation
                    .tilt(0)            //Viewing angle
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        mLastLocation = location;
        mAccuracy = location.getAccuracy();

        Log.d(TAG, "onLocationChanged location LAT-LONG:" + location.getLatitude() + "-" + location.getLongitude() + " mAccuracy:"+mAccuracy);

        //if first time OR if location change x meters then download routes again
        if(loadPathsLocation!=null && mAccuracy<=37) {

            Log.d(TAG, "onLocationChanged loadPathsLocation LAT-LONG:" + loadPathsLocation.getLatitude() + "-" + loadPathsLocation.getLongitude());

            Location newLoc = new Location("");
            newLoc.setLatitude(location.getLatitude());
            newLoc.setLongitude(location.getLongitude());

            Location loadLoc = new Location("");
            loadLoc.setLatitude(loadPathsLocation.getLatitude());
            loadLoc.setLongitude(loadPathsLocation.getLongitude());

            float distance = newLoc.distanceTo(loadLoc);

            Log.d(TAG, "onLocationChanged location distance:" + distance);

            //Distance change 500 meters and user does not started a suggestion process
            /* !!! REMOVE ONLY FOR ATHENS TEST
            if (distance > 500 && startSuggestPolyline == 0){

                if(cityOfInterest==2 && distance>200) {
                    downloadPaths = false;
                    mMap.clear();
                }else if(cityOfInterest!=2 && distance>500) {
                    downloadPaths = false;
                    mMap.clear();
                }
            }
            */
        }

        if(!downloadPaths) { //Must download paths

            //Update location of load paths
            loadPathsLocation = new Location("");
            loadPathsLocation = location;

            downloadPaths=true;

            //If exist internet connection: load paths near user current position
            ConnectionDetector mConnectionDetector = new ConnectionDetector(getApplicationContext());
            if (mConnectionDetector.isInternetAvailable()) {
                new RequestPaths(user_id, cityOfInterest).execute();
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "--> onMapReady");

        mMap = googleMap;

        //Set UI Buttons
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);


        // Customise the styling of the base map using a JSON raw file
        // First create a MapStyleOptions object
        // from the JSON styles, then pass this to the setMapStyle
        // method of the GoogleMap object.
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success;
            if(prefMapRoadsOn)
                success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            else
                success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_noroads));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style.", e);
            FirebaseCrash.logcat(Log.ERROR, TAG, "Can't find style.");
            FirebaseCrash.report(e);
        }

        LatLng latLng;
        if (mLastLocation != null) {
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());   //My last location
        } else {
            latLng = new LatLng(37.975525, 23.734904);   //Athens center
        }

        //Set camera position options
        CameraPosition cameraPosition = CameraPosition.builder()
                .target(latLng)      //start position
                .zoom(16)           //Streets Level zoom
                .bearing(0)         //Orientation
                .tilt(0)            //Viewing angle
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        //mMap.setMyLocationEnabled(true);

        //Set Listener for Long Clicks on Map
        mMap.setOnMapLongClickListener(this);

        //Set Listener for Clicks on Polyline
        mMap.setOnPolylineClickListener(this);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    /*
     * Set Google API Client
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES) //games
                .build();
        mGoogleApiClient.connect();
    }


    /*
     * Check permissions
     */
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }


    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /*
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }


    @Override
    public void onMapLongClick(LatLng latLng) {

        Log.d(TAG, "onMapLongClick startSuggestPolyline: "+startSuggestPolyline+ " path_id:"+path_id);

        if (startSuggestPolyline == 0) {

            if(review_sketch>0){
                Toast.makeText(getApplicationContext(), "You are not allowed to suggest Sketch path", Toast.LENGTH_SHORT).show();
                return;
            }

            if(path_id==0){
                Toast.makeText(getApplicationContext(), "You must first select a reference path", Toast.LENGTH_SHORT).show();
                return;
            }

            //Start Polyline
            PolylineOptions rectOptions = new PolylineOptions();
            //rectOptions.width(15).color(Color.GREEN).geodesic(true);
            rectOptions.width(20).color(Color.RED).geodesic(true);
            rectOptions.add(new LatLng(latLng.latitude, latLng.longitude));
            suggestedPolyline = mMap.addPolyline(rectOptions);
            suggestedPlPoints = suggestedPolyline.getPoints();

            if(snappedToRoadPlPoints!=null)
                snappedToRoadPlPoints.clear();

            //Start Marker
            suggestMarkerStart = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latLng.latitude, latLng.longitude))
                    //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker))
                    .title("Start of Suggestion"));
            suggestMarkerStart.showInfoWindow();

            suggestStartPos = new LatLng(latLng.latitude, latLng.longitude);

            startSuggestPolyline=1;

            tvSelectPath.setText(R.string.select_path_for_review_3);
            btnSubmit.setText(R.string.submit_suggestion);
            btnSubmit.setClickable(false);//(true);
            btnDiscard.setText(R.string.discard_suggestion);
            btnUndoLastAction.setVisibility(View.VISIBLE);
            btnUndoLastAction.setClickable(true);
        }else if(startSuggestPolyline == 1) {

            //End Polyline
            suggestedPlPoints.add(new LatLng(latLng.latitude, latLng.longitude));
            suggestedPolyline.setPoints(suggestedPlPoints);

            if(snappedToRoadPlPoints!=null)
                snappedToRoadPlPoints.clear();

            //End Marker
            suggestMarkerEnd = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latLng.latitude, latLng.longitude))
                    //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker))
                    .title("End of Suggestion"));
            suggestMarkerEnd.showInfoWindow();

            suggestEndPos = new LatLng(latLng.latitude, latLng.longitude);

            startSuggestPolyline=2;

            tvSelectPath.setText(R.string.select_path_for_review_3);
            btnSubmit.setText(R.string.submit_suggestion);
            btnSubmit.setClickable(true);
            btnDiscard.setText(R.string.discard_suggestion);
            btnUndoLastAction.setVisibility(View.VISIBLE);
            btnUndoLastAction.setClickable(true);

            //Check connection
            ConnectionDetector mConnectionDetector = new ConnectionDetector(getApplicationContext());
            if (mConnectionDetector.isInternetAvailable()) {
                //Try to Snap to Road suggested polyline
                new snapToRoadPath(user_id).execute();
            }

        }
    }


    /*
     * Undo last suggestion action of selected Path Line
     */
    public void UndoSuggestAction(){

        switch(startSuggestPolyline){
            case 1:

                if(suggestedPolyline!=null) {
                    suggestedPolyline.remove();
                }
                suggestedPolyline = null;

                if(suggestedPlPoints!=null) {
                    suggestedPlPoints.clear();
                }

                if(snappedToRoadPlPoints!=null){
                    snappedToRoadPlPoints.clear();
                }

                if(suggestMarkerStart!=null) {
                    suggestMarkerStart.remove();
                }
                suggestStartPos = null;

                if(suggestMarkerEnd!=null) {
                    suggestMarkerEnd.remove();
                }
                suggestEndPos = null;

                //Update UI Elements
                tvSelectPath.setText(R.string.select_path_for_review_3);
                btnSubmit.setText(R.string.submit_review);
                btnSubmit.setClickable(true);
                btnDiscard.setText(R.string.discard_review);
                btnUndoLastAction.setVisibility(View.INVISIBLE);
                btnUndoLastAction.setClickable(false);

                startSuggestPolyline = 0;
                break;
            case 2:

                if(suggestedPolyline!=null) {
                    suggestedPolyline.remove();
                }
                suggestedPolyline = null;

                if(suggestedPlPoints!=null) {
                    suggestedPlPoints.clear();
                }

                if(snappedToRoadPlPoints!=null){
                    snappedToRoadPlPoints.clear();
                }

                //Start Polyline
                PolylineOptions rectOptions = new PolylineOptions();
                //rectOptions.width(15).color(Color.GREEN).geodesic(true);
                rectOptions.width(20).color(Color.RED).geodesic(true);
                rectOptions.add(new LatLng(suggestStartPos.latitude, suggestStartPos.longitude));
                suggestedPolyline = mMap.addPolyline(rectOptions);
                suggestedPlPoints = suggestedPolyline.getPoints();

                if(suggestMarkerEnd!=null) {
                    suggestMarkerEnd.remove();
                }
                suggestEndPos = null;

                //Update UI Elements
                tvSelectPath.setText(R.string.select_path_for_review_3);
                btnSubmit.setText(R.string.submit_suggestion);
                btnSubmit.setClickable(false);
                btnDiscard.setText(R.string.discard_suggestion);
                btnUndoLastAction.setVisibility(View.VISIBLE);
                btnUndoLastAction.setClickable(true);

                startSuggestPolyline = 1;

                break;
        }
    }



    @Override
    public void onPolylineClick(Polyline polyline) {

        try {
            path_id = ParsingGPXForDrawing.polylinesMap.get(polyline.getId());

            Log.d(TAG, "ID:" + polyline.getId() + " path_id:"+path_id);

            if(path_id>0){
                if(polyline.getWidth()==6 && polyline.getColor()==Color.RED)
                {
                    // SELECT WALK POLYLINE

                    //Deselect last selected Walk Polyline
                    if(lastSelectedPolyline!=null) {
                       lastSelectedPolyline.setWidth(6);
                       lastSelectedPolyline.setColor(Color.RED);
                    }

                    //Deselect last selected Sketch Pending Polyline
                    if(lastSketchPendingSelectedPolyline!=null) {
                        lastSketchPendingSelectedPolyline.setWidth(6);
                        lastSketchPendingSelectedPolyline.setColor(Color.parseColor("#FF8800"));
                    }

                    //Deselect last selected Sketch Accepted Polyline
                    if(lastSketchAcceptedSelectedPolyline!=null) {
                        lastSketchAcceptedSelectedPolyline.setWidth(6);
                        lastSketchAcceptedSelectedPolyline.setColor(Color.GREEN);
                    }

                    //Set flag Sketch Path selected false
                    review_sketch = 0;

                    //Select Walk Polyline
                    polyline.setWidth(16);
                    //int strokeColor = polyline.getColor() ^ 0x00ffffff;
                    //polyline.setColor(strokeColor);
                    polyline.setColor(Color.RED);

                    //Keep selected Walk Polyline
                    lastSelectedPolyline = polyline;

                    //Update UI Elements
                    tvSelectPath.setText(R.string.select_path_for_review_3);
                    if(startSuggestPolyline==0) {
                        btnSubmit.setText(R.string.submit_review);
                        btnSubmit.setClickable(true);
                        btnDiscard.setText(R.string.discard_review);
                    }else {
                        btnSubmit.setText(R.string.submit_suggestion);
                        btnSubmit.setClickable(true);
                        btnDiscard.setText(R.string.discard_suggestion);
                    }

                    //Check and Show info Popup to user
                    if(FirstShowInfoPopup) InfoPopup();
                    FirstShowInfoPopup = false;

                }else if(polyline.getWidth()==16 && polyline.getColor()==Color.RED){

                    // DESELECT WALK POLYLINE

                    //Deselect Walk Polyline
                    polyline.setWidth(6);
                    polyline.setColor(Color.RED);

                    //Set flag Sketch Path selected false
                    review_sketch = 0;

                    //Zeroing selected path id
                    path_id = 0;

                    //Update UI Elements
                    tvSelectPath.setText(R.string.select_path_for_review_1);
                    if(startSuggestPolyline==0) {
                        btnSubmit.setText(R.string.submit_review);
                        btnSubmit.setClickable(false);
                        btnDiscard.setText(R.string.discard_review);
                    }else {
                        btnSubmit.setText(R.string.submit_suggestion);
                        btnSubmit.setClickable(false);
                        btnDiscard.setText(R.string.discard_suggestion);
                    }

                }else if(polyline.getWidth()==6 && polyline.getColor()==Color.parseColor("#FF8800")){

                    // SELECT SKETCH (PENDING) POLYLINE

                    //Deselect last selected Walk Polyline
                    if(lastSelectedPolyline!=null) {
                        lastSelectedPolyline.setWidth(6);
                        lastSelectedPolyline.setColor(Color.RED);
                    }
                    //Deselect last selected Sketch Pending Polyline
                    if(lastSketchPendingSelectedPolyline!=null) {
                        lastSketchPendingSelectedPolyline.setWidth(6);
                        lastSketchPendingSelectedPolyline.setColor(Color.parseColor("#FF8800"));
                    }
                    //Deselect last selected Sketch Pending Polyline
                    if(lastSketchAcceptedSelectedPolyline!=null) {
                        lastSketchAcceptedSelectedPolyline.setWidth(6);
                        lastSketchAcceptedSelectedPolyline.setColor(Color.GREEN);
                    }

                    //Select Sketch Polyline
                    polyline.setWidth(16);
                    polyline.setColor(Color.parseColor("#FF8800"));

                    //Set flag Sketch Path selected true
                    review_sketch = 1;

                    //Keep selected Sketch Pending Polyline
                    lastSketchPendingSelectedPolyline = polyline;

                    //Update UI Elements
                    tvSelectPath.setText(R.string.select_path_for_review_4);
                    btnSubmit.setText(R.string.submit_review);
                    btnSubmit.setClickable(true);
                    btnDiscard.setText(R.string.discard_review);

                    //Check and Show info Popup to user
                    if(FirstShowInfoPopup) InfoPopup();
                    FirstShowInfoPopup = false;

                }else if(polyline.getWidth()==16 && polyline.getColor()==Color.parseColor("#FF8800")){

                    // DESELECT SKETCH (PENDING) POLYLINE

                    //Deselect Sketch Polyline
                    polyline.setWidth(6);
                    polyline.setColor(Color.parseColor("#FF8800"));

                    //Set flag Sketch Path selected false
                    review_sketch = 0;

                    //Zeroing selected path id
                    path_id = 0;

                    //Update UI Elements
                    tvSelectPath.setText(R.string.select_path_for_review_1);
                    btnSubmit.setText(R.string.submit_review);
                    btnSubmit.setClickable(false);
                    btnDiscard.setText(R.string.discard_review);
                }else if(polyline.getWidth()==6 && polyline.getColor()==Color.GREEN){

                    // SELECT SKETCH (ACCEPTED) POLYLINE

                    //Deselect last selected Walk Polyline
                    if(lastSelectedPolyline!=null) {
                        lastSelectedPolyline.setWidth(6);
                        lastSelectedPolyline.setColor(Color.RED);
                    }
                    //Deselect last selected Sketch Pending Polyline
                    if(lastSketchPendingSelectedPolyline!=null) {
                        lastSketchPendingSelectedPolyline.setWidth(6);
                        lastSketchPendingSelectedPolyline.setColor(Color.parseColor("#FF8800"));
                    }
                    //Deselect last selected Sketch Accepted Polyline
                    if(lastSketchAcceptedSelectedPolyline!=null) {
                        lastSketchAcceptedSelectedPolyline.setWidth(6);
                        lastSketchAcceptedSelectedPolyline.setColor(Color.GREEN);
                    }

                    //Select Sketch Polyline
                    polyline.setWidth(16);
                    polyline.setColor(Color.GREEN);

                    //Set flag Sketch Path selected true
                    review_sketch = 2;

                    //Keep selected Sketch Polyline
                    lastSketchAcceptedSelectedPolyline = polyline;

                    //Update UI Elements
                    tvSelectPath.setText(R.string.select_path_for_review_4);
                    btnSubmit.setText(R.string.submit_review);
                    btnSubmit.setClickable(true);
                    btnDiscard.setText(R.string.discard_review);

                    //Check and Show info Popup to user
                    if(FirstShowInfoPopup) InfoPopup();
                    FirstShowInfoPopup = false;

                }else if(polyline.getWidth()==16 && polyline.getColor()==Color.GREEN){

                    // DESELECT SKETCH (ACCEPTED) POLYLINE

                    //Deselect Sketch Polyline
                    polyline.setWidth(6);
                    polyline.setColor(Color.GREEN);

                    //Set flag Sketch Path selected false
                    review_sketch = 0;

                    //Zeroing selected path id
                    path_id = 0;

                    //Update UI Elements
                    tvSelectPath.setText(R.string.select_path_for_review_1);
                    btnSubmit.setText(R.string.submit_review);
                    btnSubmit.setClickable(false);
                    btnDiscard.setText(R.string.discard_review);
                }
            }
        }catch (Exception e){
            path_id=0;
            review_sketch = 0;
            Log.d(TAG, "EXCEPTION ID:" + polyline.getId());
            FirebaseCrash.logcat(Log.ERROR, TAG, "onPolylineClick");
            FirebaseCrash.report(e);
        }
    }



    /*
     * Open Info Popup Window
     * Inform user about possible choices
     */
    public void InfoPopup(){

        // Initialize a new instance of LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        // Inflate the custom layout/view
        View customView = inflater.inflate(R.layout.popup_rev_sug,null);

        // Initialize a new instance of popup window
        infoPopupWindow = new PopupWindow(
                customView,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );

        // Set an elevation value for popup window
        // Call requires API level 21
        if(Build.VERSION.SDK_INT>=21){
            infoPopupWindow.setElevation(5.0f);
        }

        // Get a reference for the custom view close button
        Button closeButton = (Button) customView.findViewById(R.id.btn_close_popup);

        // Set a click listener for the popup window close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dismiss the popup window
                infoPopupWindow.dismiss();
            }
        });

        // Finally, show the popup window at the center location of root relative layout
        infoPopupWindow.showAtLocation(reviewSuggestLayout, Gravity.CENTER,0,0);

    }



    /*
     * Snap to Road (Google Maps - Nearest Roads API) suggested polyline
     */
    class snapToRoadPath extends AsyncTask<String, Boolean, Boolean> {

        private int mplayerID;
        String url;
        LatLng firstPoint = null;
        LatLng lastPoint = null;

        public snapToRoadPath(int playerID) {

            mplayerID = playerID;// Id of player
        }

        /**
         * Show progress indicator before start the background thread
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(ReviewPathActivity.this);
            pDialog.setMessage("Snap To Road Path...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {

            url = "https://roads.googleapis.com/v1/nearestRoads?points=";
            for(int i=0;i<suggestedPlPoints.size();i++){
                LatLng point = suggestedPlPoints.get(i);
                if(i==0) {
                    url += (((int) (Math.pow(10, 6) * point.latitude)) / Math.pow(10, 6)) + "," + (((int) (Math.pow(10, 6) * point.longitude)) / Math.pow(10, 6)) + "|";
                }else {
                    url += (((int) (Math.pow(10, 6) * point.latitude)) / Math.pow(10, 6)) + "," + (((int) (Math.pow(10, 6) * point.longitude)) / Math.pow(10, 6));
                    break;
                }
            }
            url += "&key=AIzaSyBZnGrgDMrBw7Fyg5ReD-JocOVtwiC3cC0"; //url += "&key=AIzaSyAM2ckKb4cwLvf5aGZ_8dPHDqbhf8TIHg8";

            Log.d(TAG, "postURL: " + url);

            // Get JSON string from URL
            JSONObject jsonObj = null;
            try {
                jsonObj = jParser.getJSONFromUrlNoParams(url);
                if(jsonObj==null){
                    Log.e(TAG, "Error:no resp.");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error parsing data " + e.toString());
                FirebaseCrash.logcat(Log.ERROR, TAG, "snapToRoadPath doInBackground Error parsing data 1");
                FirebaseCrash.report(e);
                return false;
            }


            // Parse nearest points
            try {

                // Getting JSON Array node
                JSONArray snappedPoints = jsonObj.getJSONArray("snappedPoints");

                snappedToRoadPlPoints = new ArrayList<LatLng>();

                boolean setFirstPoint=false;
                boolean setSecondPoint=false;

                // looping through All points
                for (int i = 0; i < snappedPoints.length(); i++) {

                    JSONObject point = snappedPoints.getJSONObject(i);


                    //Check return point original reference
                    int originalIndex = point.getInt("originalIndex");
                    //if(originalIndex!=i) continue;

                    JSONObject location = point.getJSONObject("location");
                    double point_lat = location.getDouble("latitude");
                    double point_long = location.getDouble("longitude");

                    Log.d(TAG, ">>>Parse point lat:"+point_lat+" long:"+point_long);

                    Location real = new Location("");
                    Location near = new Location("");
                    float distance=0;

                    near.setLatitude(point_lat);
                    near.setLongitude(point_long);

                    if(setFirstPoint==false && originalIndex==0)
                    {
                        real.setLatitude(suggestStartPos.latitude);
                        real.setLongitude(suggestStartPos.longitude);

                        distance = real.distanceTo(near);

                        Log.d(TAG, ">>>Parse point distance 1:" + distance);

                        if (distance > 20) return false;   //if google returned near point with distance > 20 m far away from start point: do not snap and keep user line

                        firstPoint = new LatLng(point_lat, point_long);

                        Log.d(TAG, ">>>snappedToRoadPlPoints firstPoint");

                        setFirstPoint = true;
                        snappedToRoadPlPoints.add(new LatLng(( ((Math.pow(10, 6) * point_lat)) / Math.pow(10, 6)), ( ((Math.pow(10, 6) * point_long)) / Math.pow(10, 6)) ));

                    }else if(setSecondPoint==false && originalIndex==1){
                    //}else {
                        real.setLatitude(suggestEndPos.latitude);
                        real.setLongitude(suggestEndPos.longitude);

                        distance = real.distanceTo(near);

                        Log.d(TAG, ">>>Parse point distance 2:"+distance);

                        if(distance>20) return false;   //if google returned near point with distance > 20 m far away from end point: do not snap and keep user line

                        lastPoint = new LatLng(point_lat, point_long);

                        Log.d(TAG, ">>>snappedToRoadPlPoints lastPoint");

                        setSecondPoint=true;
                        snappedToRoadPlPoints.add(new LatLng(( ((Math.pow(10, 6) * point_lat)) / Math.pow(10, 6)), ( ((Math.pow(10, 6) * point_long)) / Math.pow(10, 6)) ));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "snapToRoadPath doInBackground Error parsing data 2");
                FirebaseCrash.report(e);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean snapRes) {

            // close dialog
            pDialog.dismiss();

            Log.d(TAG, "onPostExecute: " + snapRes);

            if(snapRes){
                //Successful Snap Sketch path to road
                //Toast.makeText(getApplicationContext(), "SUCCESSFUL SNAP TO ROAD", Toast.LENGTH_LONG).show();

                if(suggestedPolyline!=null) suggestedPolyline.remove();

                //Start Polyline
                PolylineOptions rectOptions = new PolylineOptions();
                rectOptions.width(15).color(Color.BLUE).geodesic(true);
                if(firstPoint!=null)
                    rectOptions.add(new LatLng(firstPoint.latitude, firstPoint.longitude));
                if(lastPoint!=null)
                    rectOptions.add(new LatLng(lastPoint.latitude, lastPoint.longitude));
                suggestedPolyline = mMap.addPolyline(rectOptions);
                suggestedPolyline.setPoints(snappedToRoadPlPoints);

                //Correct Start Marker
                if(suggestMarkerStart!=null) {
                    suggestMarkerStart.remove();
                }
                if(firstPoint!=null) {
                    suggestMarkerStart = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(firstPoint.latitude, firstPoint.longitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker))
                            .title("Start of Suggestion"));
                    suggestMarkerStart.showInfoWindow();

                    suggestStartPos = new LatLng(firstPoint.latitude, firstPoint.longitude);
                }

                //Correct End Marker
                if(suggestMarkerEnd!=null) {
                    suggestMarkerEnd.remove();
                }
                if(lastPoint!=null){
                    suggestMarkerEnd = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lastPoint.latitude, lastPoint.longitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker))
                            .title("End of Suggestion"));
                    suggestMarkerEnd.showInfoWindow();

                    suggestEndPos = new LatLng(lastPoint.latitude, lastPoint.longitude);
                }


            }else{
                //Unsuccessful Snap
                //Toast.makeText(getApplicationContext(), "!!! UNSUCCESSFUL SNAP TO ROAD", Toast.LENGTH_LONG).show();
            }

        }
    }


    /*
     * Background Async Task to load paths near current position (gpx file from server)
     */
    class RequestPaths extends AsyncTask<String, String, String> {

        private int mplayerID;

        //String data;
        String err_msg;
        String path;//gpx file path from server
        private double radiusKm = 0.5;

        // όνομα κόμβου(node) JSON απόκρισης
        String KEY_SUCCESS = "success";
        String KEY_MESSAGE = "message";
        String KEY_ERROR = "error";
        String KEY_ERROR_MESSAGE = "error_msg";


        public RequestPaths(int playerID, int city){

            mplayerID=playerID;//Player Id

            if(city==2) radiusKm = 0.2; //Corfu
            else radiusKm = 0.5; //other
        }

        /**
         * Before background thread started show dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if(prefMode.equals("1")){
                REQUEST_TAG = "pathRequestRadius";
            }
            else{
                REQUEST_TAG = "pathCycleRequestRadius";
            }
            pDialog = new ProgressDialog(ReviewPathActivity.this);
            pDialog.setMessage("Request path...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
            err_msg=null;
            path=null;
        }

        @Override
        protected String doInBackground(String... strings) {

            Log.d(TAG, "postURL: " + request_path_url);

            //Parameters
            ContentValues params = new ContentValues();
            params.put("tag", REQUEST_TAG);
            params.put("playerID", String.valueOf(mplayerID));
            if(mLastLocation!=null)
            {
                params.put("lat", mLastLocation.getLatitude());
                params.put("long", mLastLocation.getLongitude());
            }else {
                params.put("lat", 37.975525);//37.9805748);   //Athens center
                params.put("long", 23.734904);//23.6583773);
            }
            params.put("rad", String.valueOf(radiusKm));

            // Get JSON string from URL
            JSONObject json = null;
            try {
                json = jParser.getJSONFromUrl(request_path_url, params);
                if(json==null){
                    err_msg="Oops! An error occurred!";
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error parsing data " + e.toString());
                err_msg="Oops! An error occurred!";
                FirebaseCrash.logcat(Log.ERROR, TAG, "RequestPaths doInBackground Error parsing data 1");
                FirebaseCrash.report(e);
                return null;
            }


            //Analyze json object and get GPX file path in server
            try {

                if (json.getString(KEY_SUCCESS) != null) {
                    String result = json.getString(KEY_SUCCESS);
                    if (Integer.parseInt(result) == 1){//Success
                        path = "mergeFile/merge_" + String.valueOf(mplayerID) + "_gpx.gpx";//GPX file path in server
                    }
                    else
                    {
                        err_msg = json.getString(KEY_ERROR_MESSAGE);
                    }
                }
                else{
                    //path="Oops! An error occurred!";
                    err_msg="Oops! An error occurred!";
                    //path_id = 0;
                }

            } catch (JSONException e) {
                e.printStackTrace();
                err_msg="Oops! An error occurred!";
                FirebaseCrash.logcat(Log.ERROR, TAG, "RequestPaths doInBackground Error parsing data 2");
                FirebaseCrash.report(e);
                return null;
            }
            return path;
        }

        //Load paths in map
        @Override
        protected void onPostExecute(String path) {

            String url ="";

            // close dialog
            pDialog.dismiss();

            Log.d(TAG, "onPostExecute path:" + path + " err_msg:"+err_msg);

            //Toast.makeText(getApplicationContext(), path + String.valueOf(path_id), Toast.LENGTH_LONG).show();

            if(err_msg!=null){
                url = show_no_path_url + err_msg;// url: error page
                path_id=0;
            }
            else{

                //Enable buttons
                btnSubmit.setVisibility(View.VISIBLE);
                btnDiscard.setVisibility(View.VISIBLE);

                url = show_path_url; //url: show paths
            }

            Log.d(TAG, "onPostExecute url: " + url);

            mMap.clear(); //Clear polylines and tags from map

            if(err_msg==null){
                new DownloadGPXFileAsync (ReviewPathActivity.this, mMap).execute(url, path);
            }

        }
    }


    //Submit button handler
    public void submitReview(View view) {

        if(path_id==0){
            Toast.makeText(getApplicationContext(), "Select a path for review!", Toast.LENGTH_LONG).show();
            return;
        }

        btnUndoLastAction.setVisibility(View.INVISIBLE);
        btnUndoLastAction.setClickable(false);

        //Check internet connection
        ConnectionDetector mConnectionDetector = new ConnectionDetector(getApplicationContext());
        if(mConnectionDetector.isInternetAvailable()){

            boolean review_suggest;
            String cmp_str = getResources().getString(R.string.submit_review);

            Bundle bundle = new Bundle();
            if(cmp_str.equals(btnSubmit.getText())){
                review_suggest = false;
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "REVIEW_BUTTON");
            }
            else{
                review_suggest = true;
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "SUGGEST_BUTTON");
            }
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            tvSelectPath.setText(R.string.select_path_for_review_1);
            btnSubmit.setText(R.string.submit_review);
            btnSubmit.setClickable(false);
            btnDiscard.setText(R.string.discard_review);

            int rated = spinnerReview.getSelectedItemPosition() + 1;            //user selected review for path
            int rated_tags = spinnerReviewTags.getSelectedItemPosition() + 1;   //user selected review for tags
            int new_path = spinnerNewPath.getSelectedItemPosition() + 1;        //user choice for new or not path
            new RequestReviewPath(user_id, path_id, rated, rated_tags, new_path, review_suggest, suggestStartPos, suggestEndPos, mAccuracy, review_sketch).execute();
        }
    }



    /**
     * Background Async Task for submit user review/suggestion
     * */
    class RequestReviewPath extends AsyncTask<String, String, String> {

        private int mplayerID;
        private int mPath_id;
        private int mRated;
        private int mRated_tags;
        private int mNew_path;
        private boolean mRev_sug;
        private LatLng startLatLng;
        private LatLng endLatLng;
        private float mhdop;
        private int mRev_sketch;
        private int numOfTags;
        String data;
        private boolean finish_activity;

        //Type of Review/Sketch on submit
        int reviewOrSketch;

        // Response JSON nodes
        String KEY_SUCCESS = "success";
        String KEY_MESSAGE = "message";
        String KEY_ERROR = "error";
        String KEY_ERROR_MESSAGE = "error_msg";

        public RequestReviewPath(int playerID, int path_id, int rated, int rated_tags, int new_path, boolean rev_sug, LatLng start, LatLng end, float accuracy, int rev_sketch){
            mplayerID=playerID;         //User id
            mPath_id=path_id;           //path id
            mRated=rated;               //review path selection
            mRated_tags=rated_tags;     //review tags selection
            mNew_path = new_path;       //new path selection
            mRev_sug = rev_sug;         //Review OR Suggestion
            startLatLng = start;        //Start suggestion position
            endLatLng = end;            //End suggestion position
            mhdop = accuracy;           //Location accuracy
            mRev_sketch = rev_sketch;   //Type of review/suggestion

            //Get number of loaded tags
            numOfTags = ParsingGPXForDrawing.tagsMap.size();

            Log.d(TAG,"mRated:"+mRated);
            Log.d(TAG,"mRated_tags:"+mRated_tags);
            Log.d(TAG,"mNew_path:"+mNew_path);
            Log.d(TAG,"mPath_id:"+mPath_id);
            Log.d(TAG,"numOfTags:"+numOfTags);

            reviewOrSketch = 0;
            finish_activity = false;
        }

        /**
         * Before background thread started show dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if(prefMode.equals("1")){
                STORE_REVIEW_TAG = "storeReview";                   //Tag for review a path - pedestrian
                STORE_SUGGEST_TAG = "storeSuggestedPath";           //Tag for suggest a - path pedestrian
                STORE_REVIEW_SKETCH_TAG = "storeSugPathReview";     //Tag for review a sketch path - pedestrian
            }
            else{
                STORE_REVIEW_TAG = "storeCycleReview";              //Tag for review a path - cycle
                STORE_SUGGEST_TAG = "storeSugCyclePath";            //Tag for suggest a path - cycle
                STORE_REVIEW_SKETCH_TAG = "storeSugPathCycleReview";//Tag for review a sketch path - cycle
            }

            pDialog = new ProgressDialog(ReviewPathActivity.this);
            pDialog.setMessage("Request path...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }


        @Override
        protected String doInBackground(String... strings) {

            Log.d(TAG, "postURL: " + storeReviewURL + " mRev_sug:" + mRev_sug + " STORE_REVIEW_TAG:" + STORE_REVIEW_TAG + " STORE_SUGGEST_TAG:" + STORE_SUGGEST_TAG);

            ContentValues params = new ContentValues();
            if(!mRev_sug) {

                if(mRev_sketch==1) {
                    //Review Sketch Pending Path

                    reviewOrSketch = 1;

                    // Building Parameters
                    params.put("tag", STORE_REVIEW_SKETCH_TAG);

                }else if(mRev_sketch==2){
                    //Review Sketch Accepted Path

                    reviewOrSketch = 2;

                    // Building Parameters
                    params.put("tag", STORE_REVIEW_SKETCH_TAG);

                }else {
                    //Review Walk Path

                    reviewOrSketch = 0;

                    // Building Parameters
                    params.put("tag", STORE_REVIEW_TAG);
                }
                params.put("player_id", Integer.toString(mplayerID));
                params.put("path_id", Integer.toString(mPath_id));
                params.put("rated", Integer.toString(mRated));
                params.put("new_path", Integer.toString(mNew_path));
//!!!
                params.put("rated_tags", 0);
                //if(numOfTags==0) params.put("rated_tags", 0);
                //else params.put("rated_tags", Integer.toString(mRated_tags));

            }else{
                //Suggestion


                reviewOrSketch = 3;

                // Building Parameters
                params.put("tag", STORE_SUGGEST_TAG);
                params.put("path_id", Integer.toString(mPath_id));
                params.put("player_id", Integer.toString(mplayerID));
                params.put("s_lat", startLatLng.latitude);
                params.put("s_long", startLatLng.longitude);
                params.put("e_lat", endLatLng.latitude);
                params.put("e_long", endLatLng.longitude);
                params.put("pending", 1);
                params.put("hdop", mhdop);
            }

            String message ="Oops! Something goes wrong";

            // Get JSON string from URL
            JSONObject json = null;
            try {
                json = jParser.getJSONFromUrl(storeReviewURL, params);
                if(json==null){
                    message="Oops! Something goes wrong";
                    finish_activity = true;
                    return message;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error parsing data " + e.toString());
                finish_activity = true;
                FirebaseCrash.logcat(Log.ERROR, TAG, "RequestReviewPath doInBackground Error parsing data 1");
                FirebaseCrash.report(e);
                return message;
            }


            try {

                if (json.getString(KEY_SUCCESS) != null) {

                    //Success

                    int result = Integer.parseInt(json.getString(KEY_SUCCESS));

                    Log.d(TAG, "RequestReviewPath KEY_SUCCESS result:" + result);

                    //Success review/suggestion show message from server
                    if ( result == 1){
                        //First Review
                        message = json.getString(KEY_MESSAGE);

                        finish_activity = false;

                        //Update Google Play Game
                        if(!ReviewGameUpdatePoints(reviewOrSketch, true)){
                            finish_activity = true;
                        }


                    }else if(result==2) {
                        //Already reviewed from another player
                        message = json.getString(KEY_MESSAGE);

                        finish_activity = false;

                        //Update Google Play Game
                        if(!ReviewGameUpdatePoints(reviewOrSketch, false)){
                            finish_activity = true;
                        }


                    }else if(result==0){

                        finish_activity = true;

                        if (json.getString(KEY_ERROR) != null) {
                        /*
                        int result = Integer.parseInt(json.getString(KEY_ERROR));
                        if ( result == 1)
                        else if(result==2)
                        else
                        */
                            message = json.getString(KEY_ERROR_MESSAGE);//Get error message from server
                        }
                    }else{
                        message = json.getString(KEY_MESSAGE);//Get error message from server
                        finish_activity = true;
                    }

                } else{
                    //Error
                    message="Oops! Something goes wrong";
                    finish_activity = true;
                }

            } catch (JSONException e) {
                e.printStackTrace();
                finish_activity = true;
                FirebaseCrash.logcat(Log.ERROR, TAG, "RequestReviewPath doInBackground Error parsing data 2");
                FirebaseCrash.report(e);
            }
            return message;
        }

        @Override
        protected void onPostExecute(String message) {

            //close dialog
            pDialog.dismiss();

            Log.d(TAG, "onPostExecute: " + message);

            Toast.makeText(getApplicationContext(), message,
                    Toast.LENGTH_LONG).show();

            if(finish_activity) {
                startActivity(new Intent(ReviewPathActivity.this, ReviewPathActivity.class));//Start new activity again
                finish();
            }
        }

    }



    /*
     * Google Play Game Update Points/Achievements
     */
    public boolean ReviewGameUpdatePoints(int reviewType, boolean firstReview) {

        int totalPoints = 0;


        switch(reviewType)
        {
            case 0: //Review Walk Path
                totalPoints += 150;
                if(firstReview) totalPoints += 30;
                break;

            case 1: //Review Pending Sketch Path
                totalPoints += 150;
                if(firstReview) totalPoints += 30;
                break;

            case 2: //Review Accepted Sketch Path
                totalPoints += 150;
                if(firstReview) totalPoints += 30;
                break;

            case 3: //Add Sketch Path
                if(firstReview) totalPoints += 250;
                else totalPoints += 200;
                break;

            default:
                return false;
        }

        Log.d(TAG, "ReviewGameUpdatePoints totalPoints:" + totalPoints);

        if(totalPoints>0){

            int curr_balance = userFunctions.getUserBalance();
            Log.d(TAG, "ReviewGameUpdatePoints curr_balance:" + curr_balance);

            totalPoints += curr_balance;

            Log.d(TAG, "ReviewGameUpdatePoints new totalPoints:" + totalPoints);

            if(totalPoints>0) {

                //Games.Achievements
                //Achievements review/suggest
                Games.Achievements.load(mGoogleApiClient, false).setResultCallback(new ResultCallback<Achievements.LoadAchievementsResult>() {
                    @Override
                    public void onResult(@NonNull Achievements.LoadAchievementsResult loadAchievementsResult) {
                        Iterator<Achievement> aIterator = loadAchievementsResult.getAchievements().iterator();
                        Achievement ach;
                        while (aIterator.hasNext()) {
                            ach = aIterator.next();

                            //Log.d(TAG, "Achievements ach.getCurrentSteps():"+ach.getCurrentSteps());
                            //Log.d(TAG, "Achievements ach.getTotalSteps():"+ach.getTotalSteps());
                            //Log.d(TAG, "Achievements ach.getState():"+ach.getState());
                            //Log.d(TAG, "Achievements ach.getPlayer.getPlayerId():"+ach.getPlayer().getPlayerId());
                            //Log.d(TAG, "Achievements ach.getAchievementId():"+ach.getAchievementId());

                            if(ach.getState()!=Achievement.STATE_UNLOCKED) {

                                if (getString(R.string.achievement_10_review).equals(ach.getAchievementId())) {
                                    Log.d(TAG, "Achievements achievement_10_review");
                                    if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {

                                        //Just reached
                                        Bundle bundle = new Bundle();
                                        bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_10_review));
                                        //bundle.putString(FirebaseAnalytics.Param.VALUE, getString(R.string.achievement_10_review));
                                        bundle.putLong("player_id", user_id);
                                        bundle.putString("player_email", user_email);
                                        //bundle.putString("achievement_id", getString(R.string.achievement_10_review));
                                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);

                                    }
                                    Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_10_review), 1);
                                }
                                if (getString(R.string.achievement_25_review).equals(ach.getAchievementId())) {
                                    Log.d(TAG, "Achievements achievement_25_review");
                                    if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                        //Just reached
                                        Bundle bundle = new Bundle();
                                        bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_25_review));
                                        //bundle.putString(FirebaseAnalytics.Param.VALUE, getString(R.string.achievement_25_review));
                                        bundle.putLong("player_id", user_id);
                                        bundle.putString("player_email", user_email);
                                        //bundle.putString("achievement_id", getString(R.string.achievement_25_review));
                                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                    }
                                    Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_25_review), 1);
                                }
                                /*
                                if (getString(R.string.achievement_50_review).equals(ach.getAchievementId())) {
                                    Log.d(TAG, "Achievements achievement_50_review");
                                    if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                        //Just reached
                                        Bundle bundle = new Bundle();
                                        bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_50_review));
                                        //bundle.putString(FirebaseAnalytics.Param.VALUE, getString(R.string.achievement_50_review));
                                        bundle.putLong("player_id", user_id);
                                        bundle.putString("player_email", user_email);
                                        //bundle.putString("achievement_id", getString(R.string.achievement_50_review));
                                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                    }
                                    Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_50_review), 1);
                                }
                                */
                                /*
                                if (getString(R.string.achievement_100_review).equals(ach.getAchievementId())) {
                                    Log.d(TAG, "Achievements achievement_100_review");
                                    if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                        //Just reached
                                        Bundle bundle = new Bundle();
                                        bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_100_review));
                                        //bundle.putString(FirebaseAnalytics.Param.VALUE, getString(R.string.achievement_100_review));
                                        bundle.putLong("player_id", user_id);
                                        bundle.putString("player_email", user_email);
                                        //bundle.putString("achievement_id", getString(R.string.achievement_100_review));
                                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                    }
                                    Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_100_review), 1);
                                }
                                */
                            }
                        }
                    }
                });

                updateLeaderboards(mGoogleApiClient, getString(R.string.leaderboard_id), totalPoints);
                userFunctions.setUserBalance(0);
                return true;
            }else{
                userFunctions.setUserBalance(totalPoints);
            }
        }
        return false;
    }


    /*
     * Get player last score and update it
     */
    private void updateLeaderboards(final GoogleApiClient googleApiClient, final String leaderboardId, final long increment_score) {
        Games.Leaderboards.loadCurrentPlayerLeaderboardScore(
                googleApiClient,
                leaderboardId,
                LeaderboardVariant.TIME_SPAN_ALL_TIME,
                LeaderboardVariant.COLLECTION_PUBLIC
        ).setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {

            @Override
            public void onResult(Leaderboards.LoadPlayerScoreResult loadPlayerScoreResult) {

                Log.d(TAG, "updateLeaderboards onResult");

                if (loadPlayerScoreResult != null) {
                    if (GamesStatusCodes.STATUS_OK == loadPlayerScoreResult.getStatus().getStatusCode()) {

                        Log.d(TAG, "updateLeaderboards STATUS_OK");

                        long score = 0;
                        if (loadPlayerScoreResult.getScore() != null) {
                            score = loadPlayerScoreResult.getScore().getRawScore();
                        }

                        Log.d(TAG, "updateLeaderboards score:"+score);
                        Log.d(TAG, "updateLeaderboards increment_score:"+increment_score);

                        Games.Leaderboards.submitScore(googleApiClient, leaderboardId, (score+increment_score));

                        Bundle bundle = new Bundle();
                        //bundle.putLong(FirebaseAnalytics.Param.VALUE, (score+increment_score));
                        bundle.putLong("player_id", user_id);
                        bundle.putString("player_email", user_email);
                        bundle.putLong("player_score", (score+increment_score));
                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.POST_SCORE, bundle);

                        startActivity(new Intent(ReviewPathActivity.this, ReviewPathActivity.class));//Start new activity again
                        finish();
                    }
                }
            }
        });
    }



    /*
     * Discard Review/Suggestion button handler
     */
    public void discardReview(View view) {

        Toast.makeText(getApplicationContext(), "Try loading new path",
                Toast.LENGTH_LONG).show();

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "DISCARD_REVIEW_BUTTON");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        startActivity(new Intent(ReviewPathActivity.this,ReviewPathActivity.class));//Start new activity again
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_review, menu);
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                setResult(RESULT_OK, null);
                finish();
                return  true;

            case R.id.submenu_show_maps:
                Intent maps_intent = new Intent(ReviewPathActivity.this, MapsActivity.class);
                if(mLastLocation!=null) {
                    maps_intent.putExtra("lat", mLastLocation.getLatitude());
                    maps_intent.putExtra("lon", mLastLocation.getLongitude());
                }else{
                    //Athens
                    maps_intent.putExtra("lat", 37.975525);
                    maps_intent.putExtra("lon", 23.734904);
                }
                startActivity(maps_intent);
                finish();
                return  true;
            case R.id.submenu_rank_list_of_players:
                Intent ranking_intent = new Intent(ReviewPathActivity.this, GoogleGamesActivity.class); //NEW WITH GOOGLE PLAY GAMES SERVICES
                startActivity(ranking_intent);
                finish();
                return  true;
            case R.id.submenu_settings:
                Intent edit_intent = new Intent(ReviewPathActivity.this, SettingsActivity.class);
                startActivity(edit_intent);
                return  true;
            case R.id.submenu_log_out:
                //Clear previous saved login values
                userFunctions.logoutUser();
                Intent login_intent = new Intent(ReviewPathActivity.this, SignInActivity.class);
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
