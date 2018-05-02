package com.apap.pom;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ServiceConnection;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
//import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
//import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerLevelInfo;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


/**
 * Main Activity:
 * Show map to user with recorder paths
 * Start/Stop record a new path
 */


public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        ActivityCompat.OnRequestPermissionsResultCallback,
        View.OnClickListener, //games
        LocationListener
{

    /////////////////////////////////////////////

    //Debug Tag
    public static final String TAG = MainActivity.class.getSimpleName();

    //Intents Numbers
    private static int INTENT_MAPS_NUM = 1000;
    private static int INTENT_REVIEW_NUM = 2000;
    private static int INTENT_RANK_NUM = 3000;

    //Flag for return to MainActivity from another Activity
    private boolean fromActivityFlag;

    // server url
    private static String allPathsFileUrl;
    private static String allPathsFileName;

    Polyline polyline;// H "πολυγραμμή" που θα δείχνει την διαδρομή του χρήστη.
    private PolylineOptions rectOptions = new PolylineOptions()// Αρχικοποίηση
            // των επιλογών
            // της
            // "πολυγραμμής"
            // που θα
            // δείχνει την
            // διαδρομή του
            // χρήστη
            .width(5).color(Color.GREEN).geodesic(true);

    //List of path Markers
    List<Marker> pathMarkers;


    // Την enableNewLocationAddToPolyline την χρησιμοποιήμε, γιατί στην
    // περίπτωση που η MyLocationService έχει γυρίσει πίσω τις προηγούμενες
    // θέσεις του χρήστη
    // (όταν η MainActivity γίνει onResume ή onCreate ενώ η MyLocationService
    // τρέχει), θέλουμε πρώτα να δημιουργηθεί η polyline του χρήστη που δείχνει
    // την
    // διαδρομή του μέχρι τώρα, και μετά να προστεθούν σε αυτή οι καινούργιες
    // θέσεις του χρήστη
    boolean enableNewLocationAddToPolyline = true;

    // Η firstTimeOnResumeAfterCreated δηλώνει αν είναι η πρώτη φορά που η
    // MainActivity μπαίνει στην onResume(). Αυτή χρησιμοποιείται σε συνδυασμό
    // με το αν η
    // MyLocationService τρέχει, ώστε οι παλιές θέσεις του χρήστη να μην
    // ξαναζητηθούν αν είναι η πρώτη φορά, αφού αν αυτές υπάρχουν έχουν ζητηθεί
    // στην onCreate()
    boolean firstTimeOnResumeAfterCreated = true;

    // Η firstTimeTheActivityIsBindedToMyLocationService δείχνει αν η
    // MainActivity συνδέται για πρώτη φορά στην υπηρεσία. Αν δεν είναι η πρώτη
    // φορά
    // βοηθάει στο να ζητήσουμε τις παλιές θέσεις του χρήστη κατά την σύνδεση
    // στην υπηρεσία (αφού σε αυτή την περίπτωση η MaiActivity έχει καταστραφεί)
    // και όταν
    // ξαναδημιουργείται η υπηρεσία "τρέχει", άρα θα έχει τις παλιές θέσεις του
    // χρήστη
    boolean firstTimeTheActivityIsBindedToMyLocationService = true;

    //Start/Stop Rec. Button
    private ImageButton recButton;

    //TextView Location Text
    private TextView tvLocationText;

    //Progress Bar - while search location of user
    private ProgressBar pbLocationProgress;

    private Button btnSelectPathType; // Επιτρέπει στον χρήστη να υποβάλει το είδος της διαδρομής

    private ImageView ivAppMode;


    //Path set Array Adapter
    //ArrayAdapter<String> pathsArrayAdapter;

    //Last selected path type
    String LastSelectedPathType;

    //Google API Client
    public static GoogleApiClient mGoogleApiClient;

    //map from Google
    private GoogleMap mMap;

    //Map Fragment
    SupportMapFragment mapFragment;

    //My last location
    Location mLastLocation;

    //Location Request
    LocationRequest mLocationRequest;
    boolean locationUpdatesOn;

    //Android Geocoder
    Geocoder geocoder;

    //Messenger for service send messages back
    Messenger mService = null;

    //Activity connected to service or not
    boolean mIsBound;

    //TextView Time Text
    private TextView tvRecordTime;

    //TextView Meters Text
    private TextView tvRecordDistance;

    //TextView Speed Text
    private TextView tvRecordSpeed;

    //State of record path
    private boolean isRecordOn;

    //For call user logout function
    UserFunctions userFunctions;
    private int user_id;
    private String user_email;

    // path sets
    ArrayList<String> pathsTypesName;
    ArrayList<PathsTypes> pathsTypes;

    //App Preferences
    SharedPreferences sharedPrefs;
    String prefMode;
    boolean prefMapRoadsOn;
    String prefLanguage;

    //Google Play Game
    private static int RC_SIGN_IN = 9001;
    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;
    boolean mExplicitSignOut = false;
    boolean mInSignInFlow = false;  // set to true when you're in the middle of the
                                    // sign in flow, to know you should not attempt
                                    // to connect in onStart()

    //Handler for Timer to update UI on record
    private Handler recTimeHandler = new Handler();

    //Total Seconds of recorded path
    long totalRecPathSecs;

    //Total meters of record path
    long totalRecPathMeters;

    //Current Speed of user
    float currentSpeed;

    // Messenger  from client in order service send messages back
    final Messenger mMessenger = new Messenger(new Handler(new IncomingHandlerCallback()));


    // Handler for messages from service MyLocationService
    class IncomingHandlerCallback implements Handler.Callback {

        // Client handle messages (from service MyLocationService) in handleMessage method
        @Override
        public boolean handleMessage(Message msg) {

            Log.d(TAG, "*** handleMessage:" + msg.what);

            switch (msg.what) {
                case MyLocationService.MSG_SET_LAST_LOCATION:
                    // Η MyLocationService στέλνει την τελευταία εντοπισμένη θέση του χρήστη

                    double latitude = msg.getData().getDouble("latitude");// Το
                    // γεωγρφικό
                    // πλάτος
                    // του
                    // χρήστη

                    double longitude = msg.getData().getDouble("longitude");// Το
                    // γεωγραφικό
                    // μήκος
                    // του
                    // χρήστη

                    currentSpeed = msg.getData().getFloat("speed");//Η ταχύτητα με την οποία κινείται ο χρήστης

/*
                    boolean maxSpeed=false;
                    if(prefMode.equals("1")) {
                        //Pedestrian
                        if(currentSpeed>15.00) maxSpeed = true;
                    } else {
                        //Cycle
                        if(currentSpeed>35.00) maxSpeed = true;
                    }

                    if(maxSpeed==true)
                    {

                        doUnbindService();// Ξεσυνδέεται από την υπηρεσία (αν είναι συνδεδεμένη)

                        // Σταματάει την υπηρεσία (αν τρέχει)
                        try {
                            stopService(new Intent(MainActivity.this,
                                    MyLocationService.class));// Σταματάει την υπηρεσία
                        } catch (Throwable t) {
                            Log.e("MainActivity", "Failed to stop the service", t);
                        }

                        if (mMap != null) {
                            mMap.clear();// Καθαρίζει τις polyline (και τα tags)
                            // που έχουν ζωγραφιστεί στον χάρτη ώστε
                            // να είναι "καθαρός" αν γυρίσει ο
                            // χρήστης πίσω
                        }

                        //Stop record path - Click Event
                        recButton.performClick();

                        Toast.makeText(getApplicationContext(), getString(R.string.max_speed_detected), Toast.LENGTH_LONG).show();

                        break;
                    }
*/
                    totalRecPathMeters += msg.getData().getDouble("totalDistance");//Τα συνολικά μέτρα της διαδρομής

                    //currentBearing = msg.getData().getFloat("bearing");//Η κατευθυνση στην οποία κινείται ο χρήστης

                    gotoMyLocation(latitude, longitude, msg.getData().getFloat("bearing"));// Καλείται η gotoMyLocation
                    // ώστε ο χάρτης να
                    // κεντραριστεί στη νέα
                    // θέση, αλλά και να
                    // προστεθεί αυτή στη
                    // polyline
                    break;

                case MyLocationService.MSG_GOOGLE_PLAY_SERVICE_RESULT_CODE:
                    // Λαμβάνεται ο κώδικας αποτελέσματος κατά την αποτυχία σύνδεσης στην google play service

                    int resultCode = msg.getData().getInt("result_code");
                    DisplayErrorDialog(resultCode);// Εμφανίζει έναν διάλογο λάθους
                    break;

                case MyLocationService.MSG_SET_LOCATION_FIXED:
                    // Ο client λαμβάνει μήνυμα ότι η πρώτη θέση του χρήστη βρέθηκε

                    //Hide indication bar
                    pbLocationProgress.setVisibility(View.INVISIBLE);

                    //Enable button for upload path (from now user could press it)
                    btnSelectPathType.setVisibility(View.VISIBLE); // Ώστε ο χρήστης
                    // να μπορεί να
                    // υποβάλει
                    // είδος
                    // μονοπατιού

                    break;

                case MyLocationService.MSG_SET_LOCATION_LOST:

                    //Show indication bar
                    pbLocationProgress.setVisibility(View.VISIBLE);

                    //Disable button for upload path (from now user could press it)
                    btnSelectPathType.setVisibility(View.INVISIBLE); // Ώστε ο
                    // χρήστης να
                    // μην μπορεί να
                    // υποβάλει
                    // είδος
                    // μονοπατιού
                    // (αφού δεν
                    // έχουμε
                    // ακριβής
                    // τοποθεσία)
                    break;

                case MyLocationService.MSG_SEND_POINTS_OF_POLYLINE_AND_TAGS:
                    // Λαμβάνονται οι εντοπισμένες θέσεις του χρήστη μέχρι στιγμής (και τα tags)


                    enableNewLocationAddToPolyline = false;// Εμποδίζει την προσθήκη νέας θέσης στον χάρτη

                    ArrayList<LatLng> arrayOfCoordinates = msg.getData()
                            .getParcelableArrayList("coordinatesArrayList");// Λίστα
                    // με
                    // τις
                    // συντεταγμένες
                    // των
                    // εντοπισμένων
                    // θέσεων
                    // του
                    // χρήστη
                    ArrayList<LatLng> arrayOfTagLocations = msg.getData()
                            .getParcelableArrayList("tagLocationArrayList");// Λίστα
                    // με
                    // τις
                    // θέσεις
                    // του
                    // χρήστη
                    // που
                    // έχουν
                    // tags
                    ArrayList<String> arrayOfPathTypes = msg.getData()
                            .getStringArrayList("pathTypeArrayList");// Λίστα με τα
                    // είδη της
                    // διαδρομής
                    // (παράλληλη
                    // λίστα με
                    // την
                    // arrayOfTagLocations)

                    if (mMap != null) {


                        mMap.clear();// Καθαρίζει τις polyline (και τα tags)
                        // που έχουν ζωγραφιστεί στον χάρτη

                        loadAllPathsIfServiceRunning();

                        // Για να κεντράρει στα γρήγορα τον χάρτη κοντά στην θέση
                        // του χρήστη εάν υπήρχε εντοπισμένη θέση
                        if (arrayOfCoordinates.size() != 0) {
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(
                                            arrayOfCoordinates
                                                    .get(arrayOfCoordinates.size() - 1).latitude,
                                            arrayOfCoordinates
                                                    .get(arrayOfCoordinates.size() - 1).longitude))
                                    .zoom(16.5f).build();
                            mMap.animateCamera(CameraUpdateFactory
                                    .newCameraPosition(cameraPosition));
                        }
                        rectOptions = new PolylineOptions();// Αρχικοποίηση των
                        // επιλογών της νέας
                        // polyline που θα
                        // ζωγραφιστεί
                        rectOptions.width(5).color(Color.GREEN).geodesic(true);


                        if (arrayOfCoordinates.size() != 0) {
                            // Προσθέτει όλες τις εντοπισμένες θέσεις του χρήστη
                            // στην polyline (κατά χρονολογική σειρά) -εάν υπάρχουν
                            // τέτοιες
                            for (int i = 0; i < arrayOfCoordinates.size(); i++) {
                                rectOptions.add(new LatLng(arrayOfCoordinates
                                        .get(i).latitude,
                                        arrayOfCoordinates.get(i).longitude));
                            }
                        }

                        polyline = mMap.addPolyline(rectOptions);// Ζωγραφίζει
                        // την
                        // διαδρομή
                        // του
                        // χρήστη
                        // μέχρι
                        // τώρα

                        // Προσθέτει όλες τις θέσεις του χρήστη με ετικέτα εάν
                        // υπάρχουν
                        if (arrayOfTagLocations.size() != 0) {
                            for (int i = 0; i < arrayOfTagLocations.size(); i++) {
                                mMap.addMarker(new MarkerOptions()
                                        .position(
                                                new LatLng(
                                                        arrayOfTagLocations
                                                                .get(i).latitude,
                                                        arrayOfTagLocations
                                                                .get(i).longitude))
                                        .title(arrayOfPathTypes.get(i))
                                        .icon(BitmapDescriptorFactory
                                                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            }
                        }
                    }

                    enableNewLocationAddToPolyline = true;// Επιτρέπει την προσθήκη νέας θέσης στον χάρτη
                    break;

                case MyLocationService.MSG_SEND_ALL_LOCATIONS_AND_TOTAL_DISTANCE_BEFORE_STOP:
                    // Λαμβάνονται όλες οι εντοπισμένες θέσεις του χρήστη και η απόσταση που διένυσε


                    // Αν έχουν εντοπιστεί θέσεις
                    boolean numberOfLcationsGreaterThanZero = msg.getData()
                            .getBoolean("numberOfLocationsDifferentZero");

                    // Αριθμός των  tags
                    int numberOfTagLocations = msg.getData().getInt("numberOfTags");

                    // Η απόσταση που έχει  διανύσει ο χρήστης
                    float distance = msg.getData().getFloat("totalDistance");

                    boolean userHasGoneOut = msg.getData().getBoolean(
                            "hasGoneOutOfTown");

                    String townOfInterest = msg.getData().getString("town");

                    doUnbindService();// Ξεσυνδέεται από την υπηρεσία (αν είναι συνδεδεμένη)

                    // Σταματάει την υπηρεσία (αν τρέχει)
                    try {
                        stopService(new Intent(MainActivity.this,
                                MyLocationService.class));// Σταματάει την υπηρεσία
                    } catch (Throwable t) {
                        Log.e("MainActivity", "Failed to stop the service", t);
                        FirebaseCrash.logcat(Log.ERROR, TAG, "handleMessage Failed to stop the service 1");
                        FirebaseCrash.report(t);
                    }

                    //if (mMap != null) {
                        //mMap.clear();// Καθαρίζει τις polyline (και τα tags)
                        // που έχουν ζωγραφιστεί στον χάρτη ώστε
                        // να είναι "καθαρός" αν γυρίσει ο
                        // χρήστης πίσω
                    //}

                    // Θα ξεκινήσει την Activity που ο χρήστης μπορεί να σώσει την διαδρομή
                    startSavePathActivity(numberOfLcationsGreaterThanZero,
                            numberOfTagLocations, distance, userHasGoneOut,
                            townOfInterest);
                    break;

                case MyLocationService.MSG_SEND_CURRENT_TAG_LOCATION:
                    // Η θέση που γυρίζει από την αίτηση για το tag

                    double currentLatitude = msg.getData().getDouble("currentLatitude");
                    double currentLongitude = msg.getData().getDouble("currentLongitude");
                    String pathType = msg.getData().getString("pathType");
                    Toast.makeText(getApplicationContext(),
                            "The path type is submitted", Toast.LENGTH_SHORT)
                            .show();

                    addMarkerToMap(currentLatitude, currentLongitude, pathType);
                    break;

                case MyLocationService.MSG_SEND_OUT_OF_REGION:
                    // Η MyLocationService στέλνει την τελευταία εντοπισμένη θέση του χρήστη

/*
                    String town = msg.getData().getString("town");// Το γεωγρφικό πλάτος του χρήστη


                    doUnbindService();// Ξεσυνδέεται από την υπηρεσία (αν είναι συνδεδεμένη)

                    // Σταματάει την υπηρεσία (αν τρέχει)
                    try {
                        stopService(new Intent(MainActivity.this,
                                MyLocationService.class));// Σταματάει την υπηρεσία
                    } catch (Throwable t) {
                        Log.e("MainActivity", "Failed to stop the service", t);
                        FirebaseCrash.logcat(Log.ERROR, TAG, "handleMessage Failed to stop the service 2");
                        FirebaseCrash.report(t);
                    }

                    if (mMap != null) {

                        //mMap.clear();// Καθαρίζει τις polyline (και τα tags)
                        // που έχουν ζωγραφιστεί στον χάρτη ώστε
                        // να είναι "καθαρός" αν γυρίσει ο
                        // χρήστης πίσω
                    }
*/
                    //Stop record path - Click Event
                    recButton.performClick();

                    break;

                default:
                    break;
            }
            return true; // Δηλώνει ότι η handleMessage χειρίστηκε το μήνυμα
        }
    }

    /**
     * Κλάση για την αλληλεπίδραση με την κύρια διεπαφή (interface) της
     * MyLocationService
     */
    private ServiceConnection mConnection = new ServiceConnection() {// Καλείται
        // όταν
        // ο
        // client
        // κάνει
        // σύνδεση
        // (bind)
        // στην
        // υπηρεσία
        public void onServiceConnected(ComponentName className, IBinder service) {// Το
            // σύστημα
            // καλεί
            // αυτήν
            // ώστε
            // να
            // παραδοθεί
            // το
            // IBinder
            // που
            // γυρίζει
            // η
            // onBind()
            // μέθοδος
            // της
            // MyLocationService.


            mService = new Messenger(service); // Ο messenger με τον οποίο
            // στέλνουμε μηνύματα στην
            // υπηρεσία

            if (firstTimeTheActivityIsBindedToMyLocationService == false) { // Αν
                // δεν
                // είναι
                // η
                // πρώτη
                // φορά
                // η
                // υπηρεσία
                // τρέχει
                // και
                // περιέχει
                // τις
                // θέσεις
                // του
                // χρήστη


                try {
                    // Ζητάει τις θέσεις του χρήστη μέχρι τώρα
                    Message msg = Message
                            .obtain(null,
                                    MyLocationService.MSG_REQUEST_POINTS_OF_POLYLINE_AND_TAGS);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // Η υπηρεσία έχει καταρρεύσει
                    FirebaseCrash.logcat(Log.ERROR, TAG, "onServiceConnected Failed service 1");
                    FirebaseCrash.report(e);
                }
            }

            try {
                Message msg = Message.obtain(null,
                        MyLocationService.MSG_REGISTER_CLIENT);// Δημιουργία
                // μηνύματος
                // ώστε ο client
                // να καταγραφεί
                // στην
                // υπηρεσία:
                // MyLocationService
                msg.replyTo = mMessenger;// Η υπηρεσία θα απαντήσει στον
                // mMessenger του client, για αυτό
                // τον στέλνουμε με το μήνυμα
                mService.send(msg);// Στέλνει ένα μήνυμα σε αυτόν τον
                // Handler(mService), δηλ. της υπηρεσίας. Το
                // συγκεκριμένο είναι για να ξέρει σε ποιο
                // handler θα απαντάει η υπηρεσία (αφού
                // γίνεται register o client στου mClients
                // της υπηρεσίας).
            } catch (RemoteException e) {
                // Σε αυτή την περίπτωση η υπηρεσία έχει καταρρεύσει πριν
                // προλάβουμε να κάνουμε κάτι με αυτήν
                FirebaseCrash.logcat(Log.ERROR, TAG, "onServiceConnected Failed service 2");
                FirebaseCrash.report(e);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // Αυτή καλείται όταν η σύνδεση με την υπηρεσία έχει αποσυνδεθεί
            // απροσδόκτητα - η διαδικασία κατάρρευσε

            Log.d(TAG, "---> onServiceDisconnected");

            mService = null;// Αφού η service δεν είναι πια bind, κάνουμε τον
            // messeger με τον οποίο στέλναμε μηνύματα στην
            // υπηρεσία null.
        }
    };


    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    //
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;


    //Save Path
    private static String uploadPathUrl;                //Το Url στο οποίο θα ανέβει η διαδρομή
    public boolean userHasUploadedThePath = false;     //Αν ο χρήστης έχει ανεβάσει το αρχείο στο server
    public TextView tvDistanceMessage;                 //Εμφανίζει μήνυμα πόσους πόντους θα κερίδει ανάλογα με την απόσταση που διένυσε ο χρήστης
    public TextView tvTagLocationsMessage;             //Εμφανίζει μήνυμα πόσους πόντους θα κερίδει ανάλογα με τα tags που έβαλε ο χρήστης
    private int numberofTagLocations;                   //Αριθμός των tags που έβαλε ο χρήστης
    private float totaldistance;                        //Η απόσταση που έχει διανύσει ο χρήστης
    private ProgressDialog pDialog;//Για να δείξει στον χρήστη ότι ανεβαίνει το gpx αρχείο
    //private ProgressDialog psDialog;//Για να δείξει στον χρήστη ότι ανεβαίνει το gpx αρχείο
    JSONParser jParser = new JSONParser();              //Json parser object
    AlertDialog dialogSavePath;
    public long totalPoints;
    //public boolean newPath;
    public long totalKms;

    AlertDialog dialogSelectPathType;

    //Analytics
    private Tracker mTracker;
    private FirebaseAnalytics mFirebaseAnalytics;

    /////////////////////////////////////////////

    /*
     * Update UI (Time, Meters, Speed) while recording path
     */
    public void doWork() {
        runOnUiThread(new Runnable() {
            public void run() {

                try {
                    //Update Time
                    totalRecPathSecs++;
                    int hours = (int) (totalRecPathSecs / 3600);
                    int minutes = (int) ((totalRecPathSecs % 3600) / 60);
                    int seconds = (int) (totalRecPathSecs % 60);
                    String str = getString(R.string.record_time) + String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    tvRecordTime.setText(str);

                    //Update meters
                    str = getString(R.string.record_distance) + totalRecPathMeters + "m";
                    tvRecordDistance.setText(str);

                    //Update Speed
                    //str = getString(R.string.record_speed) + String.format("%.2f", currentSpeed) + "km/h";
                    str = getString(R.string.record_speed) + String.format("%.1f", currentSpeed) + "km/h";
                    tvRecordSpeed.setText(str);

                } catch (Exception e) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "doWork");
                    FirebaseCrash.report(e);
                }

            }
        });
    }

    /*
     * Runnable for update UI while recording a path
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            //Update UI
            doWork();

            //Runnable will run again in 1 sec
            recTimeHandler.postDelayed(this, 1000);
        }
    };


    /*
     * Select and Submit Current Path Type
     */
    public void SelectPathPopUp(){

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        View dialogView = this.getLayoutInflater().inflate(R.layout.popup_select_path, null);

        builder.setTitle(R.string.select_path_type)
                .setView(dialogView)
                .setPositiveButton(R.string.submit_path_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        dialogSelectPathType.dismiss();

                        // User clicked Submit button
                        try {
                            // Στέλνουμε ένα μήνυμα ώστε η υπηρεσία να μας γυρίσει την τωρινή
                            // θέση του χρήστη (που θα μπει το tag του είδους της διαδρομής)
                            Message msg = Message.obtain(null,
                                    MyLocationService.MSG_REQUEST_CURRENT_LOCATION_FOR_TAG);
                            msg.replyTo = mMessenger;
                            Bundle bundle = new Bundle();
                            bundle.putString("pathType", String.valueOf(LastSelectedPathType));
                            msg.setData(bundle);
                            mService.send(msg);
                        } catch (RemoteException e) {
                            // Η υπηρεσία έχει καταρρεύσει
                            FirebaseCrash.logcat(Log.ERROR, TAG, "SelectPathPopUp Failed service 1");
                            FirebaseCrash.report(e);
                        }
                    }
                })
                .setNegativeButton(R.string.submit_path_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // User clicked Discard button
                        dialogSelectPathType.dismiss();

                    }
                });

        //Spinner or radio buttons for select path type
        Spinner spinnerPathType = (Spinner) dialogView.findViewById(R.id.spinnerPathType);

//todo: problem with greek characters, description choices implement from the server
        /*
        if(pathsTypesName.isEmpty()){
        */
            //Populate Spinner from array of default path set
            spinnerPathType.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.path_type_arrays)));
        /*
        }else {
            //Populate Spinner from array of choices downloaded from Server for this user
            spinnerPathType.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_spinner_dropdown_item, pathsTypesName));
        }
        */
        LastSelectedPathType = spinnerPathType.getSelectedItem().toString();

        spinnerPathType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                LastSelectedPathType = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // 3. Get the AlertDialog from create()
        dialogSelectPathType = builder.create();
        dialogSelectPathType.setCancelable(false);
        dialogSelectPathType.setCanceledOnTouchOutside(false);
        dialogSelectPathType.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "*** onCreate");

        // Obtain the shared Tracker instance.
        PomApplication application = ((PomApplication) getApplication());
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("RECORD PATH");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setCurrentScreen(this, "RECORD PATH", "MainActivity");

        //Check Preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefMode = sharedPrefs.getString("appMode", "1");
        prefMapRoadsOn = sharedPrefs.getBoolean("appMapRoads", false);
        prefLanguage = sharedPrefs.getString("appLanguage", "1");


        //Check and set Language
        if(Build.VERSION.SDK_INT>=17) {
            Locale locale;
            if(prefLanguage.equals("1")) locale = new Locale("el");
            else locale = new Locale("en");
            Locale.setDefault(locale);
            Resources resources = this.getResources();
            Configuration configuration = resources.getConfiguration();
            configuration.setLocale(locale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }

        //Check if user logged in (from database) - else load login activity
        userFunctions = new UserFunctions(getApplicationContext());

        if(userFunctions.isUserLoggedIn()){

            //User logged in
            setTitle(R.string.record_path);

            //load the layout only if user logged in
            setContentView(R.layout.activity_main);

            // my_child_toolbar is defined in the layout file
            Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
            setSupportActionBar(myToolbar);

            // Get a support ActionBar corresponding to this toolbar
            ActionBar ab = getSupportActionBar();

            //Show toolbar icon
            ab.setIcon(R.mipmap.ic_launcher);

            //Server Url
            if(prefMode.equals("1")) { //Pedestrian
                allPathsFileUrl = ((PomApplication) getApplicationContext()).getServerUrl() + "/mergeFile/";
                allPathsFileName = "merge_gpx.gpx";
            }else {                     //Cycle
                allPathsFileUrl = ((PomApplication) getApplicationContext()).getServerUrl() + "/mergeFile/";
                allPathsFileName = "merge_gpx_cycle.gpx";
            }

            //Google Play Game
            findViewById(R.id.games_sign_in_button).setOnClickListener(this);
            //findViewById(R.id.games_sign_out_button).setOnClickListener(this);

            //Get Button Start/Stop Rec.
            recButton = (ImageButton) findViewById(R.id.startStopRoute);

            //Get TextView Location Text
            tvLocationText = (TextView) findViewById(R.id.tv_location);

            //TextView Time Text
            tvRecordTime = (TextView) findViewById(R.id.tv_record_time);

            //TextView Meters Text
            tvRecordDistance = (TextView) findViewById(R.id.tv_record_dist);

            //TextView Speed Text
            tvRecordSpeed = (TextView) findViewById(R.id.tv_record_speed);

            //Progress Bar for user location
            pbLocationProgress = (ProgressBar) findViewById(R.id.pbLocationProgress);

            //Get Button Submit Path
            btnSelectPathType = (Button) findViewById(R.id.btnSelectPathType);
            btnSelectPathType.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SelectPathPopUp();
                }
            });

            //Path Set Array Adapter
            //pathsArrayAdapter = null;

            //ImageView App Mode
            ivAppMode = (ImageView) findViewById(R.id.ivAppMode);
            if(prefMode.equals("1")) ivAppMode.setImageResource(R.drawable.walker);
            else ivAppMode.setImageResource(R.drawable.bicycle);

            //Το Url στο οποίο θα ανέβει η διαδρομή
            PomApplication pomApplication = ((PomApplication) getApplicationContext());
            uploadPathUrl = pomApplication.getServerUrl() + "/request_log_reg_store_path.php";
            //pomApplication.setOnCreateApp(false);

            //User
            user_id = userFunctions.getUserUid();
            user_email = userFunctions.getUserEmail();

            //Last Selected Path Type
            LastSelectedPathType = null;

            //Record Flag
            isRecordOn = false;

            //Total Seconds of recorded path
            totalRecPathSecs = 0;

            //Total meters of record path
            totalRecPathMeters = 0;

            //Current User Speed
            currentSpeed = 0;

            //turned location updates off
            //mRequestingLocationUpdates = false;

            //Keep my last location info
            mLastLocation = null;
            mLocationRequest = null;
            locationUpdatesOn = false;

            //Android Goecoder
            geocoder = new Geocoder(this);

            totaldistance = 0;
            numberofTagLocations = 0;
            dialogSavePath = null;
            userHasUploadedThePath = false;
            totalPoints = 0;
            //newPath = false;
            totalKms = 0;
            dialogSelectPathType=null;
            pathMarkers = null;

            fromActivityFlag = false;


            //path sets arrays
            pathsTypesName = new ArrayList<String>();
            pathsTypes = new ArrayList<PathsTypes>();

            //Check for Permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkLocationPermission();
                buildGoogleApiClient();
            } else {
                buildGoogleApiClient();
            }

            // Create the LocationRequest object - For last location
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5 * 1000)          // 5 seconds, in milliseconds
                    .setFastestInterval(1 * 1000);  // 1 second, in milliseconds

            //Map Fragment
            mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.main_map);
            mapFragment.getMapAsync(this);

            //Check GPS and Connectivity
            CheckGpsAndConnectivity();

            //CheckIfServiceIsRunning();// Αν η MyLocationService τρέχει όταν η
            // activity ξεκινάει, θέλουμε να συνδεθούμε
            // αυτόματα σε αυτή.


        }else{
            //User not logged in: go to Login page
            Intent login = new Intent(getApplicationContext(), SignInActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            login.putExtra("logout", false);
            startActivity(login);
            finish();
        }

    }


    @Override
    protected void onResume() {

        Log.d(TAG, "*** onResume");

        super.onResume();

        if (prefMapRoadsOn != sharedPrefs.getBoolean("appMapRoads", false)){

            if(sharedPrefs.getBoolean("appMapRoads", false))
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            else
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_noroads));

            // Add a marker in Corfu Center, and move the camera.
            LatLng latLng;
            if (mLastLocation != null) {
                latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());   //My last location
            } else {
                latLng = new LatLng(37.975525, 23.734904);   //Athens center
            }
            //Set camera position options
            CameraPosition cameraPosition = CameraPosition.builder()
                    .target(latLng)      //start position
                    .zoom(15)           //Streets Level zoom
                    .bearing(0)         //Orientation
                    .tilt(0)            //Viewing angle
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        String backupPrefMode = prefMode;
        //String backupPrefLanguage = prefLanguage;
        prefMode = sharedPrefs.getString("appMode", "1");
        prefMapRoadsOn = sharedPrefs.getBoolean("appMapRoads", false);
        prefLanguage = sharedPrefs.getString("appLanguage", "1");


        if(prefMode.equals("1")) ivAppMode.setImageResource(R.drawable.walker);
        else ivAppMode.setImageResource(R.drawable.bicycle);

        if(!backupPrefMode.equals(prefMode)){
            //Change Mode - Load Map again

            //Check Internet Connection and Download Map

            //Server Url
            if(prefMode.equals("1")) { //Pedestrian
                allPathsFileUrl = ((PomApplication) getApplicationContext()).getServerUrl() + "/mergeFile/";
                allPathsFileName = "merge_gpx.gpx";
            }else {                     //Cycle
                allPathsFileUrl = ((PomApplication) getApplicationContext()).getServerUrl() + "/mergeFile/";
                allPathsFileName = "merge_gpx_cycle.gpx";
            }
            mMap.clear();
            DownloadMap(false);
        }

        //Log.i(TAG, "Setting screen name: " + "MainActivity");
        //mTracker.setScreenName("Image~" + "MainActivity");
        //mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (mGoogleApiClient != null) {

            mGoogleApiClient.connect();
        }

        if (MyLocationService.isRunning()) {


            if (MyLocationService.locationIsFixed()) { // Η locationIsFixed (που
                // την έχουμε υλοποιήσει
                // εμείς) είναι true αν
                // η θέση είναι
                // φιξαριμένη

                pbLocationProgress.setVisibility(View.INVISIBLE);// Τότε
                // εξαφανίζουμε
                // την μπάρα
                // προόδου

                btnSelectPathType.setVisibility(View.VISIBLE);

                tvLocationText.setText(R.string.location);// Εμφανίζουμε
                // για
                // κείμενο
                // Location:

            } else if (MyLocationService.locationHasFirstFixedEvent()
                    && !MyLocationService.locationIsFixed()) {// Η θέση έχει
                // "φιξαριστεί"
                // κάποια στιγμή
                // και δεν είναι
                // "φιξαριμένη"

                pbLocationProgress.setVisibility(View.VISIBLE);// Τότε
                // εμφανίζουμε
                // την μπάρα
                // προόδου

                tvLocationText.setText(R.string.location);// Εμφανίζουμε
                // για
                // κείμενο
                // Location:

            } else if (!MyLocationService.locationHasFirstFixedEvent()
                    && !MyLocationService.locationIsFixed()) {// Η θέση δεν έχει
                // "φιξαριστεί"
                // κάποια στιγμή
                // και δεν είναι
                // "φιξαριμένη"

                pbLocationProgress.setVisibility(View.VISIBLE);// Τότε
                // εμφανίζουμε
                // την μπάρα
                // προόδου

                tvLocationText.setText(R.string.location);
            }
        }




        // Αν η υπηρεσία τρέχει και δεν είναι η πρώτη φορά στο onResume μετά την
        // δημιουργία της υπηρεσίας (που αν ήταν, θα έχουμε στείλει ένα μήνυμα
        // στην
        // υπηρεσία για να μας δώσει τις παλιές θέσεις του χρήστη)
        if (MyLocationService.isRunning()
                && firstTimeOnResumeAfterCreated == false) {


            // Ζητάμε από την υπηρεσία τις παλιές θέσεις του χρήστη
            try {
                Message msg = Message
                        .obtain(null,
                                MyLocationService.MSG_REQUEST_POINTS_OF_POLYLINE_AND_TAGS);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // Η υπηρεσία έχει καταρρεύσει
                FirebaseCrash.logcat(Log.ERROR, TAG, "onResume Failed service 1");
                FirebaseCrash.report(e);
            }
        }
        firstTimeOnResumeAfterCreated = false;// Η activity έχει μπει ήδη μια
        // φορά στην onResume.
    }


    @Override
    protected void onPause() {

        Log.d(TAG, "*** onPause");

        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            if(locationUpdatesOn) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                locationUpdatesOn = false;
            }

            mGoogleApiClient.disconnect();
        }
    }

    /*
     * Check Location Permission
     *
     * Note:  If you are using both NETWORK_PROVIDER and GPS_PROVIDER,
     *        then you need to request only the ACCESS_FINE_LOCATION permission,
     *        because it includes permission for both providers.
     */
    public boolean checkLocationPermission() {


        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
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


    /*
     * Get Request Permission Result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        Log.d(TAG, "--> onRequestPermissionsResult requestCode:" + requestCode);

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
                        //games
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        //mMap.setMyLocationEnabled(true);
                        //
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "Permission Denied!", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }


    /*
     * Build Google API Client and Connect
     */
    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                //add APIs and scopes
                .addApi(LocationServices.API)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES) //games
                .build();

        if (!mInSignInFlow && !mExplicitSignOut) { //games
            // auto sign in

            mGoogleApiClient.connect();
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {


        //games
        if (mResolvingConnectionFailure) {
            // already resolving
            return;
        }

        // if the sign-in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign-in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }
        //

        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "onConnectionFailed");
                FirebaseCrash.report(e);
            }
        } else {
            Log.d(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d(TAG, "*** onConnected ");

        //Set parameters related to my Google Play Game
        InitMyGooglePlayGame();

        // show sign-out button, hide the sign-in button
        findViewById(R.id.games_sign_in_button).setVisibility(View.GONE);
        //findViewById(R.id.games_sign_out_button).setVisibility(View.VISIBLE);


        //Get my last known location
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if(mGoogleApiClient!=null) {

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if(mLastLocation==null){

                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                locationUpdatesOn = true;

            }else {

                if(mMap!=null) {


                    //Set camera position options
                    CameraPosition cameraPosition = CameraPosition.builder()
                            .target(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))      //start position
                            .zoom(15)           //Streets Level zoom
                            .bearing(0)         //Orientation
                            .tilt(0)            //Viewing angle
                            .build();
                    //mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                    locationUpdatesOn = true;

                    //LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

                    //Get Types of Paths
                    int path_set = userFunctions.getUserPathSet();
                    if(path_set==0) path_set = 1;
                    new RequestPathSet(path_set).execute();
                }
            }
        }

    }

    @Override
    public void onLocationChanged(Location location) {


        if(mLastLocation != location && mLastLocation==null)
        {
            //Set camera position options
            CameraPosition cameraPosition = CameraPosition.builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      //current position
                    .zoom(15)           //Streets Level zoom
                    .bearing(0)         //Orientation
                    .tilt(0)            //Viewing angle
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        mLastLocation = location;

        Log.d(TAG, "onLocationChanged mLastLocation LAT-LONG:" + mLastLocation.getLatitude() + "-" + mLastLocation.getLongitude());

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            if(locationUpdatesOn) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                locationUpdatesOn = false;
            }
        }

    }


    @Override
    public void onConnectionSuspended(int i) {

        Log.d(TAG, "--> onConnectionSuspended ");

    }

    //games
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

        Log.d(TAG, "**** onActivityResult M requestCode:"+requestCode + "  resultCode:"+resultCode);


        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                // Bring up an error dialog to alert the user that sign-in
                // failed. The R.string.signin_failure should reference an error
                // string in your strings.xml file that tells the user they
                // could not be signed in, such as "Unable to sign in."
                //BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_failure);

                FirebaseCrash.logcat(Log.ERROR, TAG, "Google Games M onActivityResult requestCode:"+requestCode+" resultCode:"+resultCode);
            }

        }else if(requestCode==INTENT_MAPS_NUM || requestCode==INTENT_REVIEW_NUM || requestCode==INTENT_RANK_NUM) {

            super.onActivityResult(requestCode, resultCode, intent);
            if (resultCode == RESULT_OK) {
                /*
                Intent refresh = new Intent(this, MainActivity.class);
                startActivity(refresh);
                this.finish();
                */
                fromActivityFlag = true;
            }
        }

    }
    //


    /*
     * Google Map is ready
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.e(TAG, "--> onMapReady");

        mMap = googleMap;

        //Disable UI Buttons
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        //Enable UI Buttons
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

        // Add a marker in Town Center, and move the camera.
        LatLng latLng;
        if(mLastLocation!=null)
        {
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());   //My last location
        }else {
            latLng = new LatLng(37.975525, 23.734904);   //Athens center
        }

        //Set camera position options
        CameraPosition cameraPosition = CameraPosition.builder()
                .target(latLng)      //start position
                .zoom(15)           //Streets Level zoom
                .bearing(0)         //Orientation
                .tilt(0)            //Viewing angle
                .build();
        //mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if(mLastLocation!=null) {

            if(locationUpdatesOn) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                locationUpdatesOn = false;
            }
        }

/*
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
*/
        //Disable Set my location ???
        //mMap.setMyLocationEnabled(false);

        CheckIfServiceIsRunning();// Αν η MyLocationService τρέχει όταν η
        // activity ξεκινάει, θέλουμε να συνδεθούμε
        // αυτόματα σε αυτή.

        //Check Internet Connection and Download Map
        DownloadMap(fromActivityFlag);
    }



    private boolean CheckIfServiceIsRunning() {

        // Εάν η υπηρεσία τρέχει όταν η υπηρεσία ξεκινάει, θέλουμε να συνδεθούμε
        // αυτόματα σε αυτήν.
        if (MyLocationService.isRunning()) { // Η isRunning (που την έχουμε
            // υλοποιήσει εμείς) είναι true αν η
            // υπηρεσία τρέχει

            if (MyLocationService.locationIsFixed()) { // Η locationIsFixed (που
                // την έχουμε υλοποιήσει
                // εμείς) είναι true αν
                // η θέση είναι
                // φιξαριμένη

                pbLocationProgress.setVisibility(View.INVISIBLE);// Τότε
                // εξαφανίζουμε
                // την μπάρα
                // προόδου

                btnSelectPathType.setVisibility(View.VISIBLE);

                tvLocationText.setText(R.string.location);

            } else if (MyLocationService.locationHasFirstFixedEvent()
                    && !MyLocationService.locationIsFixed()) {// Η θέση έχει
                // "φιξαριστεί"
                // κάποια στιγμή
                // και δεν είναι
                // "φιξαριμένη"

                pbLocationProgress.setVisibility(View.VISIBLE);// Τότε
                // εμφανίζουμε
                // την μπάρα
                // προόδου

                //R.string.location_try_to_fix_again
                tvLocationText.setText(R.string.location);// Εμφανίζουμε
                // για
                // κείμενο
                // Location:

            } else if (!MyLocationService.locationHasFirstFixedEvent()
                    && !MyLocationService.locationIsFixed()) {// Η θέση δεν έχει
                // "φιξαριστεί"
                // κάποια στιγμή
                // και δεν είναι
                // "φιξαριμένη"

                pbLocationProgress.setVisibility(View.VISIBLE);// Τότε
                // εμφανίζουμε
                // την μπάρα
                // προόδου

                tvLocationText.setText(R.string.location);// Εμφανίζουμε
                // για
                // κείμενο
                // Location:
            }


            firstTimeTheActivityIsBindedToMyLocationService = false;// Αφού η
            // υπηρεσία
            // τρέχει
            // σημαίνει
            // ότι έχει
            // συνδεθεί
            // παλιότερα
            // activity
            // στην
            // υπηρεσία
            doBindService();

            return true;
        }
        return false;
    }


    /*
     * Download all recorded paths from server
     */
    private void DownloadMap(boolean fromResume){

        Log.d(TAG, "---> DownloadMap");

        if(mMap==null){
            Toast.makeText(getApplicationContext(),
                    getString(R.string.unable_to_creat_maps),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        ConnectionDetector mConnectionDetector = new ConnectionDetector(
                getApplicationContext());

        if(mConnectionDetector.isInternetAvailable()){

            // Internet connection available

            // If not from resume: download paths from server
            if(fromResume == false){
                new DownloadGPXFileAsync (MainActivity.this, mMap).execute(allPathsFileUrl, allPathsFileName);
            }
            // else show paths from merge.gpx file (if exist in device)
            else{

                String myNewFileName = "merge.gpx";
                Context mContext=MainActivity.this.getApplicationContext();
                File mFile = new File (mContext.getFilesDir(), myNewFileName);
                if(mFile.exists()){
                    //File exist: show paths on map
                    ParsingGPXForDrawing parsingForDrawing = new ParsingGPXForDrawing(mFile, mMap, this);
                    parsingForDrawing.decodeGPXForTrksegs();
                    parsingForDrawing.decodeGpxForWpts();
                }else{
                    //File does not exist:download paths from server
                    new DownloadGPXFileAsync(MainActivity.this, mMap).execute(allPathsFileUrl, allPathsFileName);
                }
             }
        } //No internet connection
        else{
            String myNewFileName = "merge.gpx";
			Context mContext = MainActivity.this.getApplicationContext();
			File mFile = new File(mContext.getFilesDir(), myNewFileName);

			// If file with paths exist: show it on map
			if (mFile.exists()) {
				ParsingGPXForDrawing parsingForDrawing = new ParsingGPXForDrawing(mFile, mMap, this);

				parsingForDrawing.decodeGPXForTrksegs();

				parsingForDrawing.decodeGpxForWpts();
			}
			// Show message
			else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.unable_to_load_all_paths),
						Toast.LENGTH_SHORT).show();
			}
        }
    }


    /*
     * Check GPS and Connectivity (WiFi/Mobile Data)
     */
    void CheckGpsAndConnectivity(){


        //Check if there is Location Service enabled (GPS - Network)
        LocationManager  myLocationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);

        //Check if Gps is Enabled
        boolean gps_enabled;
        try {
            gps_enabled = myLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "CheckGpsAndConnectivity 1");
            FirebaseCrash.report(e);
            return;
        }

        //Check if  Network available
        boolean network_enabled;
        try {
            network_enabled = myLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "CheckGpsAndConnectivity 2");
            FirebaseCrash.report(e);
            return;
        }


        if(gps_enabled==false || network_enabled==false){


            //Disabled GPS
            showHighAccuracyDisabledAlertToUser(false);
        }


        //Check if there is an Internet connection
        ConnectionDetector mConnectionDetector = new ConnectionDetector(getApplicationContext());
        if (!mConnectionDetector.isInternetAvailable()) {
            //Require Internet Connection
            Toast.makeText(getApplicationContext(),
                    R.string.internet_connection_required,
                    Toast.LENGTH_LONG).show();
        }
    }


    /*
     * Start/Stop Rec. Route Button Handler
     */
    public void onButtonStartStopRecClicked(View v){


        if(isRecordOn==false)
        {

            boolean start_service=false;

            //Check if there is Location Service enabled (GPS - Network)
            LocationManager  myLocationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);
            boolean gps_enabled;
            boolean network_enabled;

            //Check if Gps is Enabled
            try {
                gps_enabled = myLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception e) {
                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "onButtonStartStopRecClicked 1");
                FirebaseCrash.report(e);
                return;
            }

            //Check if  Network available
            try {
                network_enabled = myLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception e) {
                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "onButtonStartStopRecClicked 2");
                FirebaseCrash.report(e);
                return;
            }

            ConnectionDetector mConnectionDetector = new ConnectionDetector(getApplicationContext());

            //GPS and Network providers enabled
            if(gps_enabled==true && network_enabled==true){


                //Need WiFi
                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifi.isWifiEnabled()) {


                    //Check if device has mobile data capability
                    if (mConnectionDetector.hasMobileDatacapability() == false) {


                        //Does not support Mobile Data
                        start_service = true;

                    }else{


                        //Mobile Data capability

                        //Check if Mobile Data is allowed
                        boolean mobileDataAllowed = (Settings.Secure.getInt(getContentResolver(), "mobile_data", 1)==1);

                        if(mobileDataAllowed==true)
                        {


                            start_service = true;

                        }else{


                            //Disabled Mobile Data
                            showMobileDataDisabledToUser();
                        }

                    }

                }else{

                    //Disabled WiFi
                    showWiFiDisabledToUser();
                }
            }else{


                //Disabled GPS
                showHighAccuracyDisabledAlertToUser(true);
            }

            Log.d(TAG, "start_service: " + start_service);

            if(start_service==false) return;

            if (!mConnectionDetector.isInternetAvailable()) {
                //Require Internet Connection
                Toast.makeText(getApplicationContext(),
                        R.string.internet_connection_required,
                        Toast.LENGTH_LONG).show();
                return;
            }

            //Start Record
            isRecordOn = true;

            //Change Image of Button
            recButton.setImageResource(R.drawable.stop_button);

            //Set TextViews Visible
            tvRecordTime.setVisibility(View.VISIBLE);
            tvRecordDistance.setVisibility(View.VISIBLE);
            tvRecordSpeed.setVisibility(View.VISIBLE);


            if(locationUpdatesOn) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                locationUpdatesOn = false;
            }

            //Initialize Google Play Services
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    //buildGoogleApiClient(); //games out
                    mMap.setMyLocationEnabled(true);
                }
            }
            else {
                //buildGoogleApiClient(); //games out
                mMap.setMyLocationEnabled(true);
            }


            //Start Timer for update UI while record path
            totalRecPathSecs=0;
            totalRecPathMeters=0;
            currentSpeed=0;
            recTimeHandler.post(runnable);

            //Save Path vars
            totaldistance = 0;
            numberofTagLocations = 0;
            userHasUploadedThePath = false;
            totalPoints = 0;
            //newPath = false;
            totalKms = 0;

            //*** Start the service
            startService(new Intent(MainActivity.this, MyLocationService.class));

            //Connect to service
            doBindService();

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "REC_BUTTON_START");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        }else{
            //Stop Record
            isRecordOn = false;

            //Stop update UI Timer
            recTimeHandler.removeCallbacks(runnable);

            //Change Image of Button
            recButton.setImageResource(R.drawable.rec_button);

            //Set TextViews INVISIBLE
            tvRecordTime.setVisibility(View.INVISIBLE);
            tvRecordDistance.setVisibility(View.INVISIBLE);
            tvRecordSpeed.setVisibility(View.INVISIBLE);

            mMap.setMyLocationEnabled(false);

            if (mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {


                if(locationUpdatesOn) {
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                    locationUpdatesOn = false;
                }
            }

            //Stop the service
            try {
                Message msg = Message
                        .obtain(null,
                                MyLocationService.MSG_REQUEST_ALL_LOCATIONS_AND_TOTAL_DISTANCE_BEFORE_STOP);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // Η υπηρεσία έχει καταρρεύσει
                FirebaseCrash.logcat(Log.ERROR, TAG, "onButtonStartStopRecClicked 3");
                FirebaseCrash.report(e);
            }

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "REC_BUTTON_STOP");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);


        }
    }


    @Override
    protected void onStop(){
        super.onStop();

        Log.d(TAG, "*** onStop");
    }

    @Override
    public void onBackPressed(){
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "*** onDestroy");

        try {
            doUnbindService();// Αποσυνδεόμαστε από την υπηρεσία
        } catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
            FirebaseCrash.logcat(Log.ERROR, TAG, "onDestroy Failed to unbind from the service");
            FirebaseCrash.report(t);
        }
    }

    // Connect activity to service
    void doBindService() {

        Log.d(TAG, "---> doBindService");

        bindService(new Intent(this, MyLocationService.class), mConnection,
                Context.BIND_AUTO_CREATE);// Εδώ συνδέουμε την υπηρεσία
        mIsBound = true;// για να ξέρουμε αν η υπηρεσία είναι συνδεδεμένη
    }

    //Disconnect from service
    void doUnbindService() {

        Log.d(TAG, "---> doUnbindService mIsBound:"+mIsBound);

        if (mIsBound) {
            // Αν έχουμε λάβει την υπηρεσία, και έτσι έχουμε εγγραφεί σε αυτή,
            // τώρα είναι η ώρα να απεγγραφούμε.
            if (mService != null) {// Αν η υπηρεσία δεν έχει αποσυνδεθεί από
                // κάποιο απρόσμενο λόγο και έχουμε συνδεθεί
                // σε αυτήν
                try {
                    // Στέλνουμε μήνυμα αποσύνδεσης
                    Message msg = Message.obtain(null,
                            MyLocationService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // Δεν υπάρχει κάτι ιδιαίτερο να κάνουμε αν η υπηρεσία έχει
                    // καταρρεύσει
                    FirebaseCrash.logcat(Log.ERROR, TAG, "doUnbindService Failed service");
                    FirebaseCrash.report(e);
                }
            }
            // Αποσυνδέουμε την υπάρχουσα σύνδεση μας
            unbindService(mConnection);// Εδώ γίνεται η αποσύνδεση
            mIsBound = false;// Για να ξέρουμε ότι η υπηρεσία δεν είναι πια συνδεδεμένη

            pbLocationProgress.setVisibility(View.INVISIBLE);// Δεν
            // χρησιμοποιούμε
            // άλλο τον
            // fused
            // provider

            btnSelectPathType.setVisibility(View.INVISIBLE);// Κάνουμε αόρατο το
            // κουμπί με το
            // οποίο ο χρήστης
            // μπορεί να κάνει
            // tag

            tvLocationText.setText(R.string.location);
        }
    }



    // Συνάρτηση για να φορτώσει τις διαδρομές των χρηστών αν τρέχει η υπηρεσία
    // καταγραφής της διαδρομής
    private void loadAllPathsIfServiceRunning() {


        ConnectionDetector mConnectionDetector = new ConnectionDetector(
                getApplicationContext());

        if(mMap != null){

            String myNewFileName = "merge.gpx";
            Context mContext = MainActivity.this.getApplicationContext();
            File mFile = new File(mContext.getFilesDir(), myNewFileName);
            if (mFile.exists()) {

				ParsingGPXForDrawing parsingForDrawing = new ParsingGPXForDrawing(mFile, mMap, this);

				parsingForDrawing.decodeGPXForTrksegs();

				parsingForDrawing.decodeGpxForWpts();

            }else{
                if (mConnectionDetector.isInternetAvailable()) {

				new DownloadGPXFileAsync(MainActivity.this, mMap)
						.execute(allPathsFileUrl, allPathsFileName);

                }else{

				    Toast.makeText(getApplicationContext(),
						getString(R.string.unable_to_load_all_paths),
						Toast.LENGTH_SHORT).show();
                }
            }

        }
    }


    // Κεντράρει τον χάρτη στην θέση του χρήστη και σχηματίζει μια polyline από
    // τις θέσεις που έχει περάσει ο χρήστης
    private void gotoMyLocation(double lat, double lng, float bearing) {


        if (mMap != null) {

            if (enableNewLocationAddToPolyline) {
                // Όταν δηλαδή δεν δημιουργείται ένα polyline με τις παλιές θέσεις του χρήστη
                rectOptions.add(new LatLng(lat, lng));// Προσθέτει την καινούγια θέση του χρήστη
                polyline = mMap.addPolyline(rectOptions);// Και την εμφανίζει εδώ
            }

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lng))
                    .zoom(16.5f)
                    .bearing(bearing)
                    .build();

            mMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));// Κεντράρει τον χάρτη
            // στη νέα θέση του
            // χρήστη

            String location_descr = " ";
            try {
                //Get address from new location latitude and longitude
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

                if (!addresses.isEmpty()) {
                    //Set Marker Title text: Address - Number - City
                    location_descr += addresses.get(0).getThoroughfare() + addresses.get(0).getFeatureName() + " , " + addresses.get(0).getLocality();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            tvLocationText.setText(getResources().getString(R.string.location) + location_descr);

        }
    }

    // Προσθέτει το tag που έβαλε ο χρήστης
    private void addMarkerToMap(double lat, double lng, String pathType) {

        if (mMap != null) {
            Marker newMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .title(pathType)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            //Keep path markers
            if(pathMarkers==null) {
                pathMarkers = new ArrayList<>();
            }
            pathMarkers.add(newMarker);
        }
    }


    // Μέθοδος που ξεκινάει την Activity για την αποθήκευση της διαδρομής (τις
    // στέλνει τα δεδεομένα που εντοπίστηκαν)-αν όμως δεν έχει καταγρφεί καμιά
    // θέση μένει στην mainActivity
    public void startSavePathActivity(boolean locationsDifferentZero,
                                      int numofTagLocations,
                                      Float totDistance,
                                      boolean hasGoneOut,
                                      String town) {



		if (locationsDifferentZero && !hasGoneOut) {

            totaldistance = totDistance;
            numberofTagLocations = numofTagLocations;

            SubmitPath();

		} else if (locationsDifferentZero && hasGoneOut) {

            if(totDistance>0 || numofTagLocations>0)
            {
                Toast.makeText(getApplicationContext(),
                        "Sorry, you have gone out of " + town, Toast.LENGTH_LONG)
                        .show();
                totaldistance = totDistance;
                numberofTagLocations = numofTagLocations;

                SubmitPath();

            }else {
                Toast.makeText(getApplicationContext(),
                        "Sorry, you have gone out of " + town, Toast.LENGTH_LONG)
                        .show();
            }

		} else {
			Toast.makeText(getApplicationContext(),
					"Sorry, the path was not recorded, because of location provider problems",
					Toast.LENGTH_LONG).show();
		}

    }

    /*
     * Open dialog to submit path
     */
    private void SubmitPath(){

        // Obtain the shared Tracker instance.
        //AnalyticsApplication application = (AnalyticsApplication) getApplication();
        PomApplication application = ((PomApplication) getApplication());
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("SAVE PATH");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setCurrentScreen(this, "SAVE PATH", "MainActivity");

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        View dialogView = this.getLayoutInflater().inflate(R.layout.save_path, null);
        //builder.setMessage(R.string.submit_path_msg)
        builder.setTitle(R.string.record_finish)//submit_path_title)
                .setView(dialogView)
                .setPositiveButton(R.string.submit_path_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // User clicked YES button

                        Bundle bundle = new Bundle();
                        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "PATH_UPLOAD_BUTTON");
                        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                        //Αν ο χρήστης έχει ανεβάσει ήδη την διαδρομή, δεν τον αφήνει να την ξανανεβάσει
                        if(userHasUploadedThePath){
                            //Close dialog
                            dialogSavePath.dismiss();

                            Toast.makeText(getApplicationContext(), getString(R.string.msg_already_uploaded), Toast.LENGTH_LONG).show();
                        }
                        else{
                            //Εδώ θα ανέβουν τα GPX αρχεία στον Server

                            //Αν υπάρχει σύνδεδη internet η διαδρομή θα ανέβει - αλλιώς θα βγάλει ένα μήνυμα στον χρήστη
                            ConnectionDetector mConnectionDetector = new ConnectionDetector(getApplicationContext());

                            if(/*mConnectionDetector.isNetworkConnected() == true &&*/  mConnectionDetector.isInternetAvailable()){

                                //Close dialog
                                dialogSavePath.dismiss();

                                //Υπάρχει σύνδεση internet και έτσι καλείται η UploadGpxFileToServer ώστε να ανέβουν τα αρχεία
                                new UploadGpxFileToServer(numberofTagLocations,totaldistance).execute();
                                userHasUploadedThePath = true;//Τα αρχεία ανέβηκαν
                            }
                            else{//Δεν υπάρχει σύνδεση internet
                                Toast.makeText(getApplicationContext(), "You must be connected to the internet", Toast.LENGTH_LONG).show();
                            }
                        }
                        //Close dialog
                        //dialogSavePath.dismiss();
                    }
                })
                .setNegativeButton(R.string.submit_path_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // User clicked NO button
                        dialogSavePath.dismiss();

                        Bundle bundle = new Bundle();
                        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "PATH_DISCARD_BUTTON");
                        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                        showDiscardAlertToUser();

                    }
                });

        //Set messages
        tvDistanceMessage = (TextView)dialogView.findViewById(R.id.distance_message_to_user);
        tvTagLocationsMessage=(TextView)dialogView.findViewById(R.id.tag_locations_message_to_user);

        //Points calculation - Οι πόντοι που θα κερδίσει ο χρήστης
        double points, totalPointsPath;
        long pointsOfTagLocations;
        totalPoints = 0;
        totalKms=0;

        //Calc base points
        points = Math.round(totaldistance) * 0.05;//Ο χρήστης κερδίζει 5 πόντους για κάθε 100 μέτρα


        //get if path is new or already exist
        //newPath = false;

        //!!!MOVE TO SERVER
        /*
        if(newPath==true) {
            //Add 50 bonus points for new path
            points += 50.00;
        }else {
            //Add 5 bonus points for not new path
            points += 5.00;
        }
        */

        //Add 5 bonus points per 1km path
        totalKms = Math.round((totaldistance/1000));
        points += (totalKms*5.00);

        totalPointsPath = Math.round(points*10);

        pointsOfTagLocations = numberofTagLocations * 20;//Ο χρήστης κερδίζει 20 πόντους για κάθε tag location;
        points += pointsOfTagLocations;

        totalPoints = Math.round(points*10);

        Log.d(TAG, "points_submit:"+totalPoints);


        //Το μήνυμα που λέει στον χρήστη πόσους πόντους θα κερδίσει για την απόσταση που διένυσε
        if(totalPointsPath/*totalPoints*/<1){
            //Zero path
            tvDistanceMessage.setText(getString(R.string.first_part_of_zero_distance_message));

            if(numberofTagLocations==0){
                tvTagLocationsMessage.setText(getString(R.string.first_part_of_zero_tag_locations_message_zero));
            }else {
                tvTagLocationsMessage.setText(getString(R.string.first_part_of_tag_locations_message_to_user_zero) + " " + String.valueOf(pointsOfTagLocations) + " "
                        + getString(R.string.second_part_of_tag_locations_message_to_user) + " " + String.valueOf(numberofTagLocations) + " "
                        + getString(R.string.third_part_of_tag_locations_message_to_user));
            }
        }else {
            //Path with distance
            tvDistanceMessage.setText(getString(R.string.first_part_of_distance_message_to_user) + " " + String.format("%.2f", totalPointsPath/*points*/) + " "
                    + getString(R.string.second_part_of_distance_message_to_user) + " " + String.valueOf(Math.round(totaldistance)) + " "
                    + getString(R.string.third_part_of_distance_message_to_user));

            if(numberofTagLocations==0){
                tvTagLocationsMessage.setText(getString(R.string.first_part_of_zero_tag_locations_message));
            }else {
                tvTagLocationsMessage.setText(getString(R.string.first_part_of_tag_locations_message_to_user) + " " + String.valueOf(pointsOfTagLocations) + " "
                        + getString(R.string.second_part_of_tag_locations_message_to_user) + " " + String.valueOf(numberofTagLocations) + " "
                        + getString(R.string.third_part_of_tag_locations_message_to_user));
            }
        }

        //Η δημιουργία του gpx αρχείου που δημιουργείται από τον fused provider
        File fileGoogle = new File(MainActivity.this.getFilesDir(), "pathGoogle.gpx");//Το όνομα του αρχείου που θα αποθηκευτεί η διαδρομή που έχει προκύψει από την google service
        File segmentfileGoogle = new File(MainActivity.this.getFilesDir(), "segmentOfTrkptGoogle.txt");//Tο όνομα του αρχείου που περιέχει το τμήμα με τα trackpoints που έχουν προκύψει από την google service
        File segmentOfWayPointsFileGoogle = new File(MainActivity.this.getFilesDir(), "segmentOfWptGoogle.txt");

        if (fileGoogle.exists()){//Αν το αρχείο περιέχει δεδομένα από μια παλιότερη διαδρομή, το "καθαρίζουμε"
            String string1 = "";
            FileWriter fWriter;
            try{
                fWriter = new FileWriter(fileGoogle);
                fWriter.write(string1);
                fWriter.flush();
                fWriter.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "SubmitPath Write file");
                FirebaseCrash.report(e);
            }
        }

        //Εδώ καταγράφεται η διαδρομή στα δύο GPX αρχεία
        AsynGPXWriter asynWrFile = new AsynGPXWriter(MainActivity.this, fileGoogle,segmentfileGoogle,segmentOfWayPointsFileGoogle);
        asynWrFile.execute();


        // 3. Get the AlertDialog from create()
        dialogSavePath = builder.create();
        dialogSavePath.setCancelable(false);
        dialogSavePath.setCanceledOnTouchOutside(false);
        dialogSavePath.show();
    }

    //Η προειδοποίηση στον χρήστη, ότι η διαδρομή θα χαθεί
    private void showDiscardAlertToUser(){
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(getString(R.string.alter_dialog_for_discard))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok_button_of_alter_dialog_for_discard),
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                dialog.cancel();

                                //Remove new polyline and markers
                                RemoveNewPolylineMarkers();
                            }
                        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel_button_of_alter_dialog_for_discard),
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){

                        dialog.cancel();
                        SubmitPath();
                    }
                });

        android.app.AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }


    /*
     * Remove new polyline and markers from map
     */
    private void RemoveNewPolylineMarkers(){

        if(mMap==null) return;

        //Remove Polyline
        if(polyline!=null){
            polyline.remove();
        }

        //Remove Markers
        if(pathMarkers!=null) {
            for(Marker marker:pathMarkers){
                marker.remove();
            }
            pathMarkers.clear();
        }
    }


    /**
     * Background Async Task για να ανέβουν τα gpx αρχεία - με αίτημα HTTP
     * */
    class UploadGpxFileToServer extends AsyncTask<String, String, String> {

        private int mnumberOfTags;//ο αριθμός των tags (waypoints)
        private float mdistance;
        String data;
        String json = "";
        String KEY_SUCCESS = "success";
        String KEY_MESSAGE = "message";
        String KEY_ERROR = "error";
        String KEY_ERROR_MESSAGE = "error_msg";
        String TAG_MESSAGE = "storageFile";

        public UploadGpxFileToServer(int numberOfTags,float distance){

            mnumberOfTags=numberOfTags;
            mdistance=distance;
        }

        @Override
        protected void onPreExecute() {//Δείχνουμε ότι η διαδρομή ανεβαίνει στον χρήστη
            super.onPreExecute();

            if(prefMode.equals("1")) TAG_MESSAGE = "storageFile"; //Pedestrian
            else TAG_MESSAGE = "storageFileCycle"; //Cycle

            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Uploading path...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {

            String res ="";
            String resMes=null;

            Context c = getApplicationContext();

            // Το όνομα του αρχείου που έχει αποθηκευτεί η διαδρομή από την google service
            File fileGoogle = new File(c.getFilesDir(), "pathGoogle.gpx");

            //Parameters
            ContentValues params = new ContentValues();

            //στο post θα βάλουμε στο tag = storageFile για να ξέρει ο server τι να κάνει
            //params.put("tag", TAG_MESSAGE);
            params.put("tag", TAG_MESSAGE);

            //Εδώ αποκτάται το uid του χρήστη
            UserFunctions userFunction = new UserFunctions(getApplicationContext());
            int uid = userFunction.getUserUid();
            params.put("player_id", uid);//Integer.toString(uid));

            params.put("tagsOfPath", Integer.toString(mnumberOfTags));

            //Το μέτρα της διαδρομής
            int metersOfPath = Math.round(mdistance);
            params.put("meters", Integer.toString(metersOfPath));

            // Get Response JSON string from URL
            JSONObject jObj;
            try {
                jObj = jParser.getJSONFromUrlFile(uploadPathUrl, params, fileGoogle, "pathGoogle.gpx");
                if(jObj==null) {
                    resMes = null;
                    return resMes;
                }
                /*VK added
                if(jObj != null) {
                    //resMes = null;
                    resMes = "jObj is not null!";
                    return resMes;
                }
                */
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error parsing data " + e.toString());

                FirebaseCrash.logcat(Log.ERROR, TAG, "UploadGpxFileToServer doInBackground 1");
                FirebaseCrash.report(e);

                return null;
            }


            try {
                if (jObj.getString(KEY_SUCCESS) != null){

                    res = jObj.getString(KEY_SUCCESS);


                    if(Integer.parseInt(res) == 1){//Αν ήταν επιτυχής η αποθήκευση

                        resMes=jObj.getString(KEY_MESSAGE);//Πάρε το μήνυμα της απόκρισης
                    }
                    else if(Integer.parseInt(res) == 0 && jObj.getString(KEY_ERROR) != null){

                        if(Integer.parseInt(jObj.getString(KEY_ERROR)) == 1){//Αν δεν ήταν επιτυχής η αποθήκευση
                            resMes=jObj.getString(KEY_ERROR_MESSAGE);//Πάρε το μήνυμα λάθους
                        }

                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();

                FirebaseCrash.logcat(Log.ERROR, TAG, "UploadGpxFileToServer doInBackground 2");
                FirebaseCrash.report(e);

                return null;
            }
            return resMes;//Γυρίζει το μήνυμα που θα εμφανιστεί στον χρήστη
        }

        //Όταν τελειώση η προσπάθεια ανεβάσματος της διαδρομής σταμάτησε τον διάλογο - εμφάνισε το κατάλληλο μήνυμα και σταμάτα την activity
        @Override
        protected void onPostExecute(String resMes) {

            // dismiss the dialog after getting all products
            pDialog.dismiss();

            if(resMes==null) {
                Toast.makeText(getApplicationContext(), "Fail to upload path!!!",
                        Toast.LENGTH_LONG).show();
            }else{

                //Update Google Game
                GameUpdatePoints(totalPoints, totalKms);

                //Toast.makeText(getApplicationContext(), "Successfully upload path!", Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(), resMes,
                        Toast.LENGTH_LONG).show();
            }
            //Close dialog
            dialogSavePath.dismiss();
        }
    }



    // Η προειδοποίηση στον χρήστη (ότι πρέπει να ανοίξει τον GPS πάροχο)
    private void showHighAccuracyDisabledAlertToUser(boolean onRecBtn) {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics


        if(onRecBtn) builder.setMessage(getString(R.string.alter_dialog_for_gps));
        else builder.setMessage(getString(R.string.alter_dialog_for_gps_on_start));

        builder.setTitle(R.string.title_alter_dialog_for_gps)
                .setPositiveButton(
                        getString(R.string.ok_button_of_alter_dialog_for_gps),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                            // User clicked Settings button
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(
                        getString(R.string.cancel_button_of_alter_dialog_for_gps),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            // User clicked Cancel button
                            dialog.cancel();
                            }
                })
                .show();
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();


    }

    // Η προειδοποίηση στον χρήστη (ότι πρέπει να ανοίξει το WiFi)
    private void showWiFiDisabledToUser() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(getString(R.string.alter_dialog_for_wifi))
                .setTitle(R.string.title_alter_dialog_for_wifi)
                .setPositiveButton(
                        getString(R.string.ok_button_of_alter_dialog_for_wifi),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked Settings button
                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            }
                        })
                .setNegativeButton(
                        getString(R.string.cancel_button_of_alter_dialog_for_wifi),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked Cancel button
                                dialog.cancel();
                            }
                        })
                .show();
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
    }

    // Η προειδοποίηση στον χρήστη (ότι πρέπει να ανοίξει τα MobileData)
    private void showMobileDataDisabledToUser() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(getString(R.string.alter_dialog_for_mobile_data))
                .setTitle(R.string.title_alter_dialog_for_mobile_data)
                .setPositiveButton(
                        getString(R.string.ok_button_of_alter_dialog_for_mobile_data),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked Settings button
                                //startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName(
                                        "com.android.settings",
                                        "com.android.settings.Settings$DataUsageSummaryActivity"));
                                startActivity(intent);
                            }
                        })
                .setNegativeButton(
                        getString(R.string.cancel_button_of_alter_dialog_for_mobile_data),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked Cancel button
                                dialog.cancel();
                            }
                        })
                .show();
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
    }


    // Εμφανίζει ένα διάλογο λάθους κατά την αποτυχή προσπάθεια σύνδεσης της
    // MyLocationService στην google play service
    private void DisplayErrorDialog(int resultCode) {
        // Εμφανίζει τον διάλογο λάθους
        Toast.makeText(getApplicationContext(), "Error: "+resultCode, Toast.LENGTH_LONG).show();
    }


    /*
     * Get the current version number and name
     */
    private void getVersionInfo() {
        String versionName = "";
        int versionCode = -1;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //textViewVersionInfo.setText(String.format("Version name = %s \nVersion code = %d", versionName, versionCode));
    }

    /*
     * Open About Popup Window
     */
    private void OpenAboutPopup(){
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String versionName = "";
        int versionCode = -1;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String msg="" + getResources().getString(R.string.about_msg) + String.format(" Version: %s %d", versionName, versionCode) + "\r\n" + getResources().getString(R.string.about_details);


        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(msg)//(R.string.about_msg)
                .setTitle(R.string.about_title)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        dialog.dismiss();
                    }
                })
                .show();

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return  true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.submenu_show_maps:
                Intent maps_intent = new Intent(MainActivity.this, MapsActivity.class);
                if(mLastLocation!=null) {
                    maps_intent.putExtra("lat", mLastLocation.getLatitude());
                    maps_intent.putExtra("lon", mLastLocation.getLongitude());
                }else{
                    //Athens
                    maps_intent.putExtra("lat", 37.975525);
                    maps_intent.putExtra("lon", 23.734904);
                }
                startActivityForResult(maps_intent, INTENT_MAPS_NUM);
                return  true;

            case R.id.submenu_review_paths:
                Intent review_intent = new Intent(MainActivity.this, ReviewPathActivity.class);
                startActivityForResult(review_intent, INTENT_REVIEW_NUM);
                return  true;

            case R.id.submenu_rank_list_of_players:
                Intent ranking_intent = new Intent(MainActivity.this, GoogleGamesActivity.class); //NEW WITH GOOGLE PLAY GAMES SERVICES
                startActivityForResult(ranking_intent, INTENT_RANK_NUM);
                return  true;

            case R.id.submenu_settings:
                Intent settings_intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings_intent);
                return true;

            case R.id.submenu_log_out:
                //Clear previous saved login values
                userFunctions.logoutUser();
                Intent login_intent = new Intent(MainActivity.this, SignInActivity.class);
                login_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                login_intent.putExtra("logout", true);
                startActivity(login_intent);
                finish();
                return  true;

            case R.id.submenu_about:
                OpenAboutPopup();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //games
    @Override
    public void onClick(View view) {

        switch(view.getId()) {

            case R.id.games_sign_in_button:
                // start the asynchronous sign in flow
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
        }
    }


    /*
     * Set parameters related to my Google Play Game
     */
    private void InitMyGooglePlayGame(){

        Log.i(TAG, "Game getAppId:" + Games.getAppId(mGoogleApiClient));
        Log.i(TAG, "Game getCurrentPlayerId:" + Games.Players.getCurrentPlayerId(mGoogleApiClient));

        Player player = Games.Players.getCurrentPlayer(mGoogleApiClient);
        Log.i(TAG, "Game getName:" + player.getName());
        Log.i(TAG, "Game getTitle:" + player.getTitle());
        Log.i(TAG, "Game getDisplayName:" + player.getDisplayName());
        Log.i(TAG, "Game getPlayerId:" + player.getPlayerId());

        PlayerLevelInfo playerLevelInfo = player.getLevelInfo();
        Log.i(TAG, "Game getCurrentLevel:" + playerLevelInfo.getCurrentLevel());
        Log.i(TAG, "Game getNextLevel:" + playerLevelInfo.getNextLevel());
        Log.i(TAG, "Game isMaxLevel:" + playerLevelInfo.isMaxLevel());
        Log.i(TAG, "Game getCurrentXpTotal:" + playerLevelInfo.getCurrentXpTotal());

        Games.GamesOptions.builder().setShowConnectingPopup(false);
    }


    /*
     * Get player last score and update it
     */
    //private static
    //public static
    private void updateLeaderboards(final GoogleApiClient googleApiClient, final String leaderboardId, final long increment_score) {
        Games.Leaderboards.loadCurrentPlayerLeaderboardScore(
                googleApiClient,
                leaderboardId,
                LeaderboardVariant.TIME_SPAN_ALL_TIME,
                LeaderboardVariant.COLLECTION_PUBLIC
        ).setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {

            @Override
            public void onResult(Leaderboards.LoadPlayerScoreResult loadPlayerScoreResult) {
                if (loadPlayerScoreResult != null) {
                    if (GamesStatusCodes.STATUS_OK == loadPlayerScoreResult.getStatus().getStatusCode()) {
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
                    }
                }
            }

        });
    }


    /*
     * Update Google Game with Points of last recorded path
     * Input:
     *          new_path:   path is new or already exist (MOVE THIS FUNCTION)
     *          tot_points: total points for this path
     *          kms:        kms of this path
     *  Return:
     *          true: successfully update Google Game
      *         false: Error
     */
    public boolean GameUpdatePoints(/*boolean new_path,*/ long tot_points, long kms){

        Log.d(TAG, ">>>GameUpdatePoints tot_points:" + tot_points + " kms:" + kms);

        if(tot_points<1) return false;

        if (mGoogleApiClient==null){
            //Toast.makeText(this, "mGoogleApiClient is null!!!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!mGoogleApiClient.isConnected()){

            //Toast.makeText(this, "mGoogleApiClient not connected", Toast.LENGTH_SHORT).show();
            return false;
        }


        int total_points_ = (int)tot_points;

        int curr_balance = userFunctions.getUserBalance();
        Log.d(TAG, "GameUpdatePoints curr_balance:" + curr_balance);

        total_points_ += curr_balance;

        Log.d(TAG, "GameUpdatePoints new total_points_:" + total_points_);

        // Call Play Games services API methods


        // Points
        if(total_points_>0) {
            updateLeaderboards(mGoogleApiClient, getString(R.string.leaderboard_id), total_points_);
            userFunctions.setUserBalance(0);
        }else{
            userFunctions.setUserBalance(total_points_);
        }

        Games.Achievements.load(mGoogleApiClient, false).setResultCallback(new ResultCallback<Achievements.LoadAchievementsResult>() {
            @Override
            public void onResult(@NonNull Achievements.LoadAchievementsResult loadAchievementsResult) {
                Iterator<Achievement> aIterator = loadAchievementsResult.getAchievements().iterator();
                Achievement ach;
                while (aIterator.hasNext()) {
                    ach = aIterator.next();

                    Log.d(TAG, "Achievements ach.getCurrentSteps():"+ach.getCurrentSteps());
                    Log.d(TAG, "Achievements ach.getTotalSteps():"+ach.getTotalSteps());
                    Log.d(TAG, "Achievements ach.getState():"+ach.getState());
                    Log.d(TAG, "Achievements ach.getAchievementId():"+ach.getAchievementId());

                    if(ach.getState()!=Achievement.STATE_UNLOCKED) {

//Achievements num of paths
                        if (getString(R.string.achievement_10_paths).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_10_paths");
                            if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                //Just reached
                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_10_paths));
                                bundle.putLong("player_id", user_id);
                                bundle.putString("player_email", user_email);
                                //bundle.putString("achievement_id", getString(R.string.achievement_10_paths));
                                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                            }
                            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_10_paths), 1);
                        }
                        if (getString(R.string.achievement_25_paths).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_25_paths");
                            if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                //Just reached
                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_25_paths));
                                bundle.putLong("player_id", user_id);
                                bundle.putString("player_email", user_email);
                                //bundle.putString("achievement_id", getString(R.string.achievement_25_paths));
                                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                            }
                            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_25_paths), 1);
                        }
                        /*
                        if (getString(R.string.achievement_50_paths).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_50_paths");
                            if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                //Just reached
                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_50_paths));
                                bundle.putLong("player_id", user_id);
                                bundle.putString("player_email", user_email);
                                //bundle.putString("achievement_id", getString(R.string.achievement_50_paths));
                                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                            }
                            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_50_paths), 1);
                        }
                        */
                        /*
                        if (getString(R.string.achievement_75_paths).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_75_paths");
                            if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                //Just reached
                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_75_paths));
                                bundle.putLong("player_id", user_id);
                                bundle.putString("player_email", user_email);
                                //bundle.putString("achievement_id", getString(R.string.achievement_75_paths));
                                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                            }
                            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_75_paths), 1);
                        }
                        */
                        /*
                        if (getString(R.string.achievement_100_paths).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_100_paths");
                            if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                //Just reached
                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_100_paths));
                                bundle.putLong("player_id", user_id);
                                bundle.putString("player_email", user_email);
                                //bundle.putString("achievement_id", getString(R.string.achievement_100_paths));
                                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                            }
                            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_100_paths), 1);
                        }
                        */

                        // New Path - MOVE TO SERVER
                        /*
                        if(newPath==true) {
//Achievements num of new paths
                            if (getString(R.string.achievement_10_new_paths).equals(ach.getAchievementId())) {
                                Log.d(TAG, "Achievements achievement_10_new_paths");
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_10_new_paths));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_10_new_paths));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_10_new_paths), 1);
                            }
                            if (getString(R.string.achievement_25_new_paths).equals(ach.getAchievementId())) {
                                Log.d(TAG, "Achievements achievement_25_new_paths");
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_25_new_paths));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_25_new_paths));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_25_new_paths), 1);
                            }
                            if (getString(R.string.achievement_50_new_paths).equals(ach.getAchievementId())) {
                                Log.d(TAG, "Achievements achievement_50_new_paths");
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_50_new_paths));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_50_new_paths));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_50_new_paths), 1);
                            }
                            if (getString(R.string.achievement_75_new_paths).equals(ach.getAchievementId())) {
                                Log.d(TAG, "Achievements achievement_75_new_paths");
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_75_new_paths));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_75_new_paths));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_75_new_paths), 1);
                            }
                            if (getString(R.string.achievement_100_new_paths).equals(ach.getAchievementId())) {
                                Log.d(TAG, "Achievements achievement_100_new_paths");
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_100_new_paths));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_100_new_paths));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_100_new_paths), 1);
                            }

                        }
                        */

//Achievements num of kms
                        if (getString(R.string.achievement_25_kms).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_25_kms");
                            for(int i=0;i<totalKms;i++) {
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_25_kms));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_25_kms));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_25_kms), 1);
                            }
                        }
                        if (getString(R.string.achievement_50_kms).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_50_kms");
                            for(int i=0;i<totalKms;i++) {
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_50_kms));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_50_kms));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_50_kms), 1);
                            }
                        }
                        /*
                        if (getString(R.string.achievement_75_kms).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_75_kms");
                            for(int i=0;i<totalKms;i++) {
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_75_kms));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_75_kms));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_75_kms), 1);
                            }
                        }
                        */
                        /*
                        if (getString(R.string.achievement_100_kms).equals(ach.getAchievementId())) {
                            Log.d(TAG, "Achievements achievement_100_kms");
                            for(int i=0;i<totalKms;i++) {
                                if (ach.getCurrentSteps() + 1 == ach.getTotalSteps()) {
                                    //Just reached
                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getString(R.string.achievement_100_kms));
                                    bundle.putLong("player_id", user_id);
                                    bundle.putString("player_email", user_email);
                                    //bundle.putString("achievement_id", getString(R.string.achievement_100_kms));
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);
                                }
                                Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_100_kms), 1);
                            }
                        }
                        */

                    }
                }
            }
        });
        return true;
    }


    /**
     * Background Async Task
     * 1. Get from Server types of paths for requested path set
     * 2. Get from Server current balance (points) of the player
     * */
    class RequestPathSet extends AsyncTask<String, String, Boolean> {

        // Number of pathset for the user
        private int pathSet;

        // JSONObject of path set
        JSONObject jsonobject= null;

        // JSONArray of path set
        JSONArray pathsSets = null;

        // Response JSON nodes names
        String KEY_SUCCESS = "success";
        String KEY_ERROR = "error";
        String KEY_ERROR_MESSAGE = "error_msg";
        String KEY_BALANCE = "balance";

        String TAG_PATHS_SETS = "pathstypes";
        String TAG_PATH_SET = "path_set";
        String TAG_NAME= "name";
        String TAG_SET_ORDER= "set_order";

        // Response Message
        String resp_msg=null;

        // Message to inform player
        String msg_inform_player = null;

        //
        public RequestPathSet(int path_set){
            pathSet=path_set;
        }

        /**
         * Before background thread started
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //Clear list of available path types
            //if(pathsArrayAdapter!=null) pathsArrayAdapter.clear();
            pathsTypesName.clear();
        }

        @Override
        protected Boolean doInBackground(String... strings) {

            //Server request
            JSONObject json = userFunctions.getPathsTypes(pathSet);

            //Error
            if(json==null) return false;

            /* Example of response
            {
                "pathstypes":
                [
                    {"path_set":"1","name":"sidewalk","set_order":"1"},
                    {"path_set":"1","name":"crosswalk","set_order":"2"},
                    {"path_set":"1","name":"pedestrian walkway","set_order":"3"},
                    {"path_set":"1","name":"accessible entrance","set_order":"4"},
                    {"path_set":"1","name":"pedestrian bridge","set_order":"5"},
                    {"path_set":"1","name":"pedestrian tunnel","set_order":"6"},
                    {"path_set":"1","name":"trail","set_order":"7"}
                ],
                "success":1
            }
             */

            //Analyze received json object
            try {

                if (json.getString(KEY_SUCCESS) != null) {

                    // Success

                    String result = json.getString(KEY_SUCCESS);
                    if (Integer.parseInt(result) == 1){

                        resp_msg = KEY_SUCCESS;

                        //Get the array of all path types
                        pathsSets = json.getJSONArray(TAG_PATHS_SETS);

                        // Looping for all path types
                        for (int i = 0; i < pathsSets.length(); i++) {

                            jsonobject = pathsSets.getJSONObject(i);

                            PathsTypes newPathSet = new PathsTypes();

                            //Get every json field
                            newPathSet.setPathSetNum(jsonobject.getInt(TAG_PATH_SET));
                            newPathSet.setpathSetName(jsonobject.getString(TAG_NAME));
                            newPathSet.setpathSetOrder(jsonobject.getInt(TAG_SET_ORDER));
                            pathsTypes.add(newPathSet);

                            // Populate spinner with path sets names
                            pathsTypesName.add(jsonobject.getString(TAG_NAME));

                            Log.d(TAG, "name:"+jsonobject.getString(TAG_NAME)+" path_set:"+jsonobject.getInt(TAG_PATH_SET)+ " set_order:"+jsonobject.getInt(TAG_SET_ORDER)+"\r\n");
                        }

                        //Set player id
                        int player = userFunctions.getUserUid();

                        //Get player balance
                        json = userFunctions.getPlayerBalance(player);


                        //Error
                        if(json==null) return false;

                        //Set player balance
                        int curr_balance = userFunctions.getUserBalance();


                        if (json.getString(KEY_SUCCESS) != null) {
                            result = json.getString(KEY_SUCCESS);

                            Log.d(TAG,"result:"+result);

                            if (Integer.parseInt(result) == 1) {
                                resp_msg = KEY_SUCCESS;

                                result = json.getString(KEY_BALANCE);

                                //Update Balance locally

                                //int curr_balance = userFunctions.getUserBalance();
                                int new_balance = Integer.parseInt(result);


                                if(new_balance>0){
                                    msg_inform_player = getString(R.string.get_positive_balance_from_server_1) + " " + new_balance + " " + getString(R.string.get_positive_balance_from_server_2);
                                    //Toast.makeText(getApplicationContext(), msg_inform_player, Toast.LENGTH_LONG).show();
                                }else if(new_balance<0){
                                    msg_inform_player = getString(R.string.get_negative_balance_from_server_1) + " " + Math.abs(new_balance) + " " + getString(R.string.get_negative_balance_from_server_2);
                                    //Toast.makeText(getApplicationContext(), msg_inform_player, Toast.LENGTH_LONG).show();
                                }
                                Log.d(TAG,"OLD BALANCE:" + curr_balance + " NEW BALANCE:" + new_balance);

                                int balance = curr_balance + new_balance;

                                if(balance>0){

                                    //Toast

                                    //Update Player Points and zeroing balance
                                    if (mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {

                                        updateLeaderboards(mGoogleApiClient, getApplicationContext().getResources().getString(R.string.leaderboard_id), balance);

                                        userFunctions.setUserBalance(0);
                                    }
                                }else if(balance<0){

                                    //Toast

                                    //Keep Player Points (update balance)
                                    userFunctions.setUserBalance(balance);
                                }

                                balance = userFunctions.getUserBalance();
                                Log.d(TAG, "BALANCE:" + balance);

                            }else{
                                //Balance: 0

                                //Get error description
                                resp_msg = json.getString(KEY_ERROR_MESSAGE);
                            }
                        }else{
                            //Get error description
                            resp_msg = json.getString(KEY_ERROR_MESSAGE);
                        }
                    }
                    else{
                        //Get error description
                        resp_msg = json.getString(KEY_ERROR_MESSAGE);
                    }
                }
                else{
                    // An error occurred
                    resp_msg="Oops! An error occurred!";
                }

            } catch (JSONException e) {
                e.printStackTrace();

                FirebaseCrash.logcat(Log.ERROR, TAG, "RequestPathSet doInBackground");
                FirebaseCrash.report(e);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            Log.d(TAG, "onPostExecute: " + result + " msg_inform_player:"+msg_inform_player);

            if(msg_inform_player!=null){
                Toast.makeText(getApplicationContext(), msg_inform_player, Toast.LENGTH_LONG).show();
            }
            /*
            if(result==false) {
                //Toast.makeText(getApplicationContext(), "Fail to load path set!!!", Toast.LENGTH_LONG).show();

                //Error - Populate Spinner from array of default path set
                pathsArrayAdapter = new ArrayAdapter<String>(MainActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.path_type_arrays));
            }else{

                //Success - Populate Spinner from array of choices downloaded from Server for this user
                pathsArrayAdapter = new ArrayAdapter<String>(MainActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, pathsTypesName);
            }
            */
        }
    }

}
