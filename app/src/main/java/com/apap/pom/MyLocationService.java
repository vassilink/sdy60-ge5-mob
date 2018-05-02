package com.apap.pom;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.crash.FirebaseCrash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Service for track user locations and send information to activity
 */

public class MyLocationService extends Service implements
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks
{

    //Debug Tag
    public static final String TAG = MyLocationService.class.getSimpleName() + " ^^^SERVICE^^^";

    //Το πλαίσιο (θα το χρησιμοποιήσουμε για να πάρουμε τον φάκελο που θα αποθηκευτούν τα αρχεία
    Context c;

    //Το αρχείο που θα αποθηκευτoύν τα trackpoints τμήματα της διαδρομής με το google play location service
    File segmentsOfTrackPointsFileGoogle;

    //Το αρχείο που θα αποθηκεύει τα waypoints τμήματα της διαδρομής με το google play location service
    File segmentsOfWayPointsFileGoogle;

    // Milliseconds ανά δευτερόλεπτο
    private static final int MILLISECONDS_PER_SECOND = 1000;

    //Συχνότητα ενημέρωσης σε δευτερόλεπτα
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;//8;

    // Συχνότητα ενημέρωσης σε milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;

    // Η γρηγορότερη συχνότητα ενημέρωσης σε δευτερόλεπτα
    private static final int FASTEST_INTERVAL_IN_SECONDS = 5;

    //Ανώτατο όριο ενημέρωσης σε milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

    //Google API Client
    private GoogleApiClient mGoogleApiClient; // Ο πελάτης (client) θέσης

    //Location Request
    private LocationRequest mLocationRequest; //Αιτείται τις θέσεις του χρήστη

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ",java.util.Locale.getDefault());//Το format του χρόνου
    LatLng bestCoordinatesOfLocation;//Οι καλύτερες συντεταγμένες (για κάθε 2 θέσεις)
    float bestCoordinatesAccuracy;//Η καλύτερη ακρίβεια θέσης (για κάθε 2 θέσεις)
    int counterForUILocations=0;
    ArrayList<LatLng> coordinatesOfLocationsList = new ArrayList<LatLng>();
    boolean numberOfLocationsGreaterThanZero=false;//Αν έχει βρεθεί έστω μία θέση

    //------------------------------------------------------------------------------------------
    boolean userHasGoneOutOfRegion=false;
    public int cityOfInterest;
    private static String town = "Greece";
    //------------------------------------------------------------------------------------------

    ArrayList<LatLng> coordinatesOfTagLocationsList = new ArrayList<LatLng>();

    ArrayList<String> pathTypeArrayList= new ArrayList<String>();//Μία παράλληλη λίστα με την από επάνω (taglistLoc) που περιέχει τα είδη της διαδρομής

    Location mCurrentLocationGoogle;//Η τρέχουσα θέση του χρήστη από την Google play location service

    public float totalDistance=0;//Αθροιστής που μετράει την συνολική απόσταση που διένυσε ο χρήστης σε μέτρα

    PowerManager.WakeLock wakeLock;//Θα χρησιμοποιηθεί για να κλειδώσει τον επεξεργαστή όταν η υπηρεσία τρέχει, ώστε να μπορούμε να παίρνουμε συνέχεια τις νέες θέσεις του χρήστη

    private static boolean isRunning = false;//Στην αρχή η υπηρεσία δεν "τρέχει"
    private static boolean fixed=false;//Αν υπάρχει "φιξάρισμα" Location
    private static boolean locationHasFirstFixedEvent=false;//Αν έχει υπάρξει πρώτο "φιξάρισα"

    // Παρακολουθεί όλους τους τρέχοντες εγγεγραμμένους πελάτες. Στην περίπτωση μας είναι μόνο ένας (την φορά), αλλά το βάλαμε προς χάρη γενίκευσης (και πιθανής μελλοντικής επέκτασης)
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    Messenger mClient;//Ο messenger που στέλνονται τα μηνύματα στον πελάτη


    public LatLngBounds MapCenterArea = new LatLngBounds(new LatLng(37.97153, 23.70961), new LatLng(37.98119, 23.73619));

    static final int MSG_REGISTER_CLIENT = 1;//Μήνυμα για να εγγραφεί ο πελάτης (από τον πελάτη στην υπηρεσία)
    static final int MSG_UNREGISTER_CLIENT = 2;//Μήνυμα για να απεγγραφεί ο πελάτης (από τον πελάτη στην υπηρεσία)
    static final int MSG_SET_LAST_LOCATION = 3;//Μήνυμα με την τελευταία θέση του χρήστη (από την υπηρεσία στον πελάτη)
    static final int MSG_SET_LOCATION_LOST=4;
    static final int MSG_SET_LOCATION_FIXED=5;//Μήνυμα ότι η πρώτη σωστή θέση του χρήστη βρέθηκε (από την υπηρεσία στον πελάτη)
    static final int MSG_REQUEST_POINTS_OF_POLYLINE_AND_TAGS=6;//Μήνυμα που ζητάει τις θέσεις του χρήστη μέχρι τώρα (από τον πελάτη στην υπηρεσία)
    static final int MSG_SEND_POINTS_OF_POLYLINE_AND_TAGS=7;//Μήνυμα που στέλνει τις θέσεις του χρήστη μέχρι τώρα (από την υπηρεσία στον πελάτη)
    static final int MSG_REQUEST_ALL_LOCATIONS_AND_TOTAL_DISTANCE_BEFORE_STOP=8;//Μήνυμα που ζητάει όλες τις θέσεις του χρήστη μέχρι τώρα, για τελευταία φορά
    static final int MSG_SEND_ALL_LOCATIONS_AND_TOTAL_DISTANCE_BEFORE_STOP=9;
    static final int MSG_REQUEST_CURRENT_LOCATION_FOR_TAG=10;
    static final int MSG_SEND_CURRENT_TAG_LOCATION=11;
    static final int MSG_GOOGLE_PLAY_SERVICE_RESULT_CODE=14;//Μήνυμα με το κώδικα λάθους κατά την σύνδεση της google play service (από την υπηρεσία στον πελάτη)
    static final int MSG_SEND_OUT_OF_REGION=15;

    //Καλείται από τις υπηρεσίες θέσης όταν η σύνδεση του πελάτη (θέσης) τελειώσει επιτυχώς
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d(TAG, "*** onConnected");

        //Create the location request and set the parameters
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //Καλείται όταν ο πελάτης θέσης δεν καταφέρει να συνδεθεί
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.d(TAG, "*** onConnectionFailed");

        if (connectionResult.hasResolution()) {
            /*
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
            */
            Toast.makeText(getApplicationContext(), "Location services connection failed with code " + connectionResult.getErrorCode(), Toast.LENGTH_LONG).show();
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    //Ο messenger που ορίζεται από την υπηρεσία ώστε οι πελάτες να μπορούν να στέλνουν μηνύματα.
    Handler mIncomingHandler = new Handler(new IncomingHandlerCallback());
    final Messenger mMessenger = new Messenger(mIncomingHandler);



    //Ο Handler που χειρίζεται τα μηνύματα που στέλνουν οι πελάτες
    class IncomingHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {

            Log.d(TAG, "*** handleMessage:"+msg.what);
            switch (msg.what) {
                case MSG_REGISTER_CLIENT://Ο πελάτης στέλνει μήνυμα με τον messenger του και αιτείται εγγραφή
                    mClients.add(msg.replyTo);//Προσθέτει τον messenger του client στον οποίο θα απαντάει η υπηρεσία
                    break;

                case MSG_UNREGISTER_CLIENT://Ο πελάτης στέλνει μήνυμα με τον messenger του και αιτείται απεγγραφή
                    mClients.remove(msg.replyTo);//Αφαιρεί τον messenger του client στον οποίο απαντούσε η υπηρεσία
                    break;

                case MSG_REQUEST_POINTS_OF_POLYLINE_AND_TAGS://Ο πελάτης αιτείται τις θέσεις του χρήστη (και τα tags)
                    mClient = msg.replyTo;//Ο messenger του πελάτη που αιτήθηκε το μήνυμα

                    Log.d(TAG, "*** handleMessage MSG_REQUEST_POINTS_OF_POLYLINE_AND_TAGS list size:"+ coordinatesOfLocationsList.size());

                    if(coordinatesOfLocationsList.size() > 1){//Η υπηρεσία στέλνει τις θέσεις
                        sendArrayListLocationToUI(coordinatesOfLocationsList,pathTypeArrayList,coordinatesOfTagLocationsList,totalDistance,mClient);
                    }
                    break;

                case MSG_REQUEST_ALL_LOCATIONS_AND_TOTAL_DISTANCE_BEFORE_STOP://Ο πελάτης αιτείται τις θέσεις του χρήστη (και τα tags) για τελευταία φορά
                    mClient = msg.replyTo;//Ο messenger του πελάτη που αιτήθηκε το μήνυμα

                    sendNumberOfTagsAndDistanceToUI(numberOfLocationsGreaterThanZero,coordinatesOfTagLocationsList.size(),totalDistance,mClient,userHasGoneOutOfRegion);
                    break;

                case MSG_REQUEST_CURRENT_LOCATION_FOR_TAG://Ο πελάτης αιτείται την τρέχουσα θέση του χρήστη (για το tag)
                    mClient = msg.replyTo;//Ο messenger του πελάτη που αιτήθηκε το μήνυμα
                    String pathType = msg.getData().getString("pathType");
                    pathTypeArrayList.add(pathType);//Η λίστα με το είδη της διαδρομής (Παράλληλη λίστα με την taglistLoc)

                    //Θα βάλουμε tag στο αρχείο gpx που δημιουργεί το GPS μόνο αν έχουμε φιξάρισμα GPS αλλιώς μας είναι άχρηστο και λάθος σημείο


                    //Ενημέρωση του UI και του gpx αρχείου από τον fused provider για τα waypoints
                    //mCurrentLocationGoogle = mLocationClient.getLastLocation();//Η τρέχουσα θέση από την google play service

                    //Αφού στο UI θα χρησιμοποιήσουμε τον fused provider
                    double currentLatitude = mCurrentLocationGoogle.getLatitude();
                    double currentLongitude = mCurrentLocationGoogle.getLongitude();

                    coordinatesOfTagLocationsList.add(new LatLng(currentLatitude,currentLongitude));//Η λίστα με τις τοποθεσίες που έχουν ετικέτες (Παράλληλη λίστα με την pathTypeArrayList)
                    try{
                        Bundle bundle = new Bundle();
                        bundle.putDouble("currentLatitude", currentLatitude);
                        bundle.putDouble("currentLongitude", currentLongitude);
                        bundle.putString("pathType",pathType);
                        Message msg2 = Message.obtain(null, MSG_SEND_CURRENT_TAG_LOCATION);
                        msg2.setData(bundle);
                        mClient.send(msg2);
                    }
                    catch(RemoteException e){
                        //Ο πελάτης έχει καταρρεύσει
                        FirebaseCrash.logcat(Log.ERROR, TAG, "handleMessage 1");
                        FirebaseCrash.report(e);
                    }


                    String segmentOfWaypointGoogle = "<wpt lat=\"" + mCurrentLocationGoogle.getLatitude() + "\" lon=\"" + mCurrentLocationGoogle.getLongitude() + "\"><time>" + df.format(new Date(mCurrentLocationGoogle.getTime())) + "</time>"
                            + "<name>" + pathType +"</name>" + "<hdop>" +mCurrentLocationGoogle.getAccuracy() +"</hdop>"+ "</wpt>\n";

                    try {
                        FileOutputStream fOut = openFileOutput(segmentsOfWayPointsFileGoogle.getName(),
                                MODE_APPEND);
                        OutputStreamWriter osw = new OutputStreamWriter(fOut);
                        osw.write(segmentOfWaypointGoogle);
                        osw.flush();
                        osw.close();

                    } catch (FileNotFoundException e) {

                        e.printStackTrace();
                        FirebaseCrash.logcat(Log.ERROR, TAG, "handleMessage 2");
                        FirebaseCrash.report(e);
                    } catch (IOException e) {

                        e.printStackTrace();
                        FirebaseCrash.logcat(Log.ERROR, TAG, "handleMessage 3");
                        FirebaseCrash.report(e);
                    }

                    break;

                default:
                    break;
            }
            return true;//Δηλώνει ότι η handleMessage χειρίστηκε το μήνυμα
        }
    }

    //Στέλνει ότι η πρώτη ακριβής θέση του χρήστη βρέθηκε
    private void sendLocationFixedToUI(int msgCode){

        for (int i=mClients.size()-1; i>=0; i--) {//Στέλνει το μήνυμα σε όλους του εγγεγραμένους πελάτες
            try {
                Message msg = Message.obtain(null, msgCode);
                mClients.get(i).send(msg);// Ο mClients.get(i) είναι ο Messenger που θα στείλει το μήνυμα
            }
            catch (RemoteException e) {
                // Ο πελάτης έχει "πεθάνει". Τον βγάζουμε από την λίστα. Περνάμε την λίστα από το τέλος προς την αρχή επομένως είναι ασφαλές να το κάνουμε μέσα στο βρόχο.
                mClients.remove(i);
                FirebaseCrash.logcat(Log.ERROR, TAG, "sendLocationFixedToUI");
                FirebaseCrash.report(e);
            }
        }
    }

    //Στέλνει αν έχουν εντοπιστεί θέσεις, τα tags και την απόσταση στην activity του UI
    private void sendNumberOfTagsAndDistanceToUI(boolean numberOfLocationsDifferentZero, int numberOfTags,Float distance, Messenger mClient,boolean hasGoneOut){
        try{
            Bundle bundle = new Bundle();
            bundle.putBoolean("numberOfLocationsDifferentZero",numberOfLocationsDifferentZero);
            bundle.putInt("numberOfTags", numberOfTags);
            bundle.putFloat("totalDistance", distance);
            bundle.putBoolean("hasGoneOutOfTown", hasGoneOut);
            bundle.putString("town", town);
            Message msg = Message.obtain(null, MSG_SEND_ALL_LOCATIONS_AND_TOTAL_DISTANCE_BEFORE_STOP);
            msg.setData(bundle);
            mClient.send(msg);
        }
        catch(RemoteException e){
            //Ο πελάτης έχει καταρρεύσει
            FirebaseCrash.logcat(Log.ERROR, TAG, "sendNumberOfTagsAndDistanceToUI Failed Service");
            FirebaseCrash.report(e);
        }
    }

    //Στέλνουμε την λίστα με τις θέσεις του χρήστη (μόνο στον πελάτη που την αιτήθηκε)-Το msgSendCode δείχνει αν είναι η τελευταί φορά που αιτείται ο πελάτης τις θέσεις
    //ή την αιτείται επειδή μπήκε σε resume ή στην create(). Επίσης, στέλνει όλες τις θέσεις που έχουν ετικέτα και τις ετικέτες
    private void sendArrayListLocationToUI(ArrayList <LatLng> listWithCoordinates,ArrayList<String> pathTypeArrayList,ArrayList<LatLng> listWithTagLocations,Float distance, Messenger mClient){
        try{
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("pathTypeArrayList", pathTypeArrayList);
            bundle.putParcelableArrayList("tagLocationArrayList", listWithTagLocations);
            bundle.putFloat("totalDistance", distance);
            bundle.putParcelableArrayList("coordinatesArrayList",listWithCoordinates);
            Message msg = Message.obtain(null, MSG_SEND_POINTS_OF_POLYLINE_AND_TAGS);
            msg.setData(bundle);
            mClient.send(msg);
        }
        catch(RemoteException e){
            //Ο πελάτης έχει καταρρεύσει
            FirebaseCrash.logcat(Log.ERROR, TAG, "sendArrayListLocationToUI Failed Service");
            FirebaseCrash.report(e);
        }
    }

    //Στέλνει την τελευταία θέση του χρήστη
    //private void sendLastLocationToUI(double latitude,double longitude) {
    private void sendLastLocationToUI(double latitude,double longitude, float speed, float bearing) {
        for (int i=mClients.size()-1; i>=0; i--) {//Θα στείλει τιμές σε όλους τους client που έχουν συνδεθεί (αρχίζοντας από τον τελευταίο που συνδέθηκε).
            try {
                Bundle bundle = new Bundle();
                bundle.putDouble("latitude", latitude);
                bundle.putDouble("longitude", longitude);
                bundle.putFloat("speed", speed);
                bundle.putFloat("bearing", bearing);
                bundle.putDouble("totalDistance", totalDistance);
                Message msg = Message.obtain(null, MSG_SET_LAST_LOCATION);
                msg.setData(bundle);
                mClients.get(i).send(msg);// Ο mClients.get(i) είναι ο Messenger που θα στείλει το μήνυμα
            }
            catch (RemoteException e) {
                // Ο πελάτης έχει "πεθάνει". Τον βγάζουμε από την λίστα. Περνάμε την λίστα από το τέλος προς την αρχή επομένως είναι ασφαλές να το κάνουμε μέσα στο βρόχο.
                mClients.remove(i);
                FirebaseCrash.logcat(Log.ERROR, TAG, "sendLastLocationToUI Failed Service");
                FirebaseCrash.report(e);
            }
        }
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------------------
    //Στέλνει την τελευταία θέση του χρήστη

    private void sendOutOfRegionToUI(String town) {
        for (int i=mClients.size()-1; i>=0; i--) {//Θα στείλει τιμές σε όλους τους client που έχουν συνδεθεί (αρχίζοντας από τον τελευταίο που συνδέθηκε).
            try {
                Bundle bundle = new Bundle();
                //bundle.putDouble("latitude", latitude);
                bundle.putString("town",town );
                Message msg = Message.obtain(null, MSG_SEND_OUT_OF_REGION);
                msg.setData(bundle);
                mClients.get(i).send(msg);// Ο mClients.get(i) είναι ο Messenger που θα στείλει το μήνυμα
            }
            catch (RemoteException e) {
                // Ο πελάτης έχει "πεθάνει". Τον βγάζουμε από την λίστα. Περνάμε την λίστα από το τέλος προς την αρχή επομένως είναι ασφαλές να το κάνουμε μέσα στο βρόχο.
                mClients.remove(i);
                FirebaseCrash.logcat(Log.ERROR, TAG, "sendOutOfRegionToUI Failed Service");
                FirebaseCrash.report(e);
            }
        }
    }
    //----------------------------------------------------------------------------------------------------------------------------------------------------------

    public MyLocationService() {}


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mMessenger.getBinder();//Όταν ο πελάτης συνδέεται στην υπηρεσία, η υπηρεσία γυρίζει μια διεπαφή με τον messenger της ώστε ο πελάτης να στέλνει μηνύματα στην υπηρεσία.
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "*** Service Started.");

        showNotification();//Δείχνει μια ειδοποίηση στον χρήστη ότι η υπηρεσία άρχισε
        isRunning = true;//Η υπηρεσία τρέχει

        c = getApplicationContext();

        //
        cityOfInterest = 0;
        cityOfInterest = ((PomApplication) getApplication()).getCityOfInterest();
        switch(cityOfInterest) {
            case 1:
                town = "Athens";
                MapCenterArea = null;//new LatLngBounds(new LatLng(37.97536, 23.7054), new LatLng(37.97596, 23.749));
                break;
            case 2:
                town = "Corfu";
                MapCenterArea = null;//new LatLngBounds(new LatLng(37.97536, 23.7054), new LatLng(37.97596, 23.749));
                break;
            case 3:
                town = "Athens Center";
                MapCenterArea = new LatLngBounds(new LatLng(37.97153, 23.70961), new LatLng(37.98119, 23.73619));
                break;
            default:
                town = "Greece";
                MapCenterArea = null;
                break;
        }

        //Τα ονόματα των αρχείων που θα αποθηκεύονται τα trackpoints και waypoints από τον fused provider
        segmentsOfTrackPointsFileGoogle = new File(c.getFilesDir(), "segmentOfTrkptGoogle.txt");//Το όνομα του αρχείου που θα αποθηκευτούν τα trackpoints της διαδρομής από την google location
        segmentsOfWayPointsFileGoogle = new File(c.getFilesDir(),"segmentOfWptGoogle.txt");

        //Αν είναι η πρώτη φορά που τρέχουμε το πρόγραμμα, δημιουργούμε τα αρχεία, αλλιώς αν το αρχεία περιέχουν δεδομένα από μια παλιότερη διαδρομή, τα "καθαρίζουμε"

        //Τα αρχεία που παίρνουν τιμές από τον fused provider
        String string3 = "";
        FileWriter fWriter3;
        try{
            fWriter3 = new FileWriter(segmentsOfTrackPointsFileGoogle);
            fWriter3.write(string3);
            fWriter3.flush();
            fWriter3.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "onCreate File 1");
            FirebaseCrash.report(e);
        }

        String string4 = "";
        FileWriter fWriter4;
        try{
            fWriter4 = new FileWriter(segmentsOfWayPointsFileGoogle);
            fWriter4.write(string4);
            fWriter4.flush();
            fWriter4.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "onCreate File 2");
            FirebaseCrash.report(e);
        }

        buildGoogleApiClient();

    }


    /*
     * Build Google API Client and Connect
     */
    protected synchronized void buildGoogleApiClient() {


        Log.d(TAG, "buildGoogleApiClient");


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                //add APIs and scopes
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

    }


    /*
    * Create the location request and set the parameters
    */
    protected void createLocationRequest() {

        Log.d(TAG, "--> createLocationRequest ");

        //Create LocationRequest object
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL);  //sets the rate in milliseconds at which your app prefers to receive location updates
        // Θέτει το διάστημα ανανέωσης στα 8 δευτερόλεπτα - παρατηρήθηκε ότι αν είναι μικρότερο τότε παίρνει (συνήθως) πάντα θέση από το wifi

        mLocationRequest.setFastestInterval(FASTEST_INTERVAL); //sets the fastest rate in milliseconds at which your app can handle location updates

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //sets the priority of the request (request the most precise location possible)

        startPeriodicUpdates();
    }

    //Δείχνει μια ειδοποίηση στον χρήστη ότι η υπηρεσία τρέχει
    private void showNotification() {

        // To Intent που θα ξεκινάει την MainActivity αν πατηθεί η ειδοποίηση
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), (int)System.currentTimeMillis(), intent, 0);

        //Χτίζει την ειδοποίηση
        Notification myNotification  = new Notification.Builder(getApplicationContext())
                .setTicker(getText(R.string.my_location_service_started))//Το πρώτο κείμενο που εμφανίζεται στον χρήστη όταν ξεκινάει η υπηρεσία
                .setContentTitle(getText(R.string.my_location_service_label))//Ο τίτλος της ειδοποίησης")
                .setContentText(getText(R.string.my_location_service_content))//Το κείμενο της ειδοποίηση
                .setSmallIcon(R.mipmap.notification_icon)//Η εικόνα της ειδοποίησης
                .setContentIntent(pIntent)
                //.setAutoCancel(true)
                .build();

        //Ξεκινά την υπηρεσία ώς υπηρεσία προσκηνίου (ώστε να μην είναι υποψήφια προς "σκότωμα" αν υπάρχει λίγη μνήμη
        startForeground(R.string.my_location_service_started, myNotification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        if(servicesConnected()) {//Τσεκάρει αν υπάρχει σύνδεση στις google play services
            mGoogleApiClient.connect();//Συνδέει τον πελάτη θέσης
        }

        //Θέλουμε η υπηρεσία να τρέχει συνέχεια ώστε να παίρνει τις νέες θέσεις του χρήστη (ακόμα και αν η οθόνη του χρήστη έχει κλειδώσει)
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyWakelockTag");//Το wakeLock είναι μόνο για τον επεξεργσατή
        wakeLock.acquire();//Αποκτάται το κλείδωμα του επεξεργαστή

        return START_STICKY; // τρέχει μέχρι να σταματήσει ρητά.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "*** Service Stopped.");

        isRunning = false;//Δηλώνει ότι η υπηρεσία σταμάτησε
        fixed = false;//Δηλώνει ότι δεν έχει βρεθεί μια ακριβής (νέα) θέση του χρήστη

        //-----------------------------------------------------------------------------------------------------------------------
        if(servicesConnected()) {//Αν οι google play servicew είναι συνδεδεμένες
            // Εάν ο πελάτης θέσης είναι συνδεδεμένος
            if (mGoogleApiClient.isConnected()) {
                stopPeriodicUpdates();//Σταματά την ζήτηση για νέες θέσεις του χρήστη
            }

            // Μετά το κάλεσμα της disconnect(), ο πελάτης θέσης θεωρείται "πεθαμένος".
            mGoogleApiClient.disconnect();
        }

        //Απελευθερώνουμε το κλείδωμα στον επεξεργστή, αφού δεν θέλουμε άλλο να παίρνουμε συνέχεια τις θέσεις του χρήστη
        wakeLock.release();
    }


    //Δηλώνει αν η υπηρεσία τρέχει
    public static boolean isRunning(){
        return isRunning;
    }

    //Δηλωνει αν η θέση είναι "φιξαριμένη"
    public static boolean locationIsFixed(){
        return fixed;
    }

    public static boolean locationHasFirstFixedEvent(){
        return locationHasFirstFixedEvent;
    }

    //Γυρίζει την απόσταση δύο σημείων σε μέτρα
    public float distance (double lat_a, double lng_a, double lat_b, double lng_b )
    {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return Float.valueOf(Double.toString(distance * meterConversion));
    }


    /*
     * Calculate Distance in meters of two locations
     */
    private static long calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        long distanceInMeters = Math.round(6371000 * c);
        return distanceInMeters;
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d(TAG, "onLocationChanged: " + location);


        if(location==null) return;


        //Θεωρούμε ότι η πρώτη νέα (ακριβής) θέση έχει βρεθεί αν η ακρίβεια είναι κάτω των 37m (Σχετικά καλή ακρίβεια WiFi)
        if (location.getAccuracy()<=37.0 && fixed == false){
            fixed = true;//Είναι φιξαρισμένος ο fused provider
            locationHasFirstFixedEvent=true;
            sendLocationFixedToUI(MSG_SET_LOCATION_FIXED);//Ενημερώνει το UI
        }

        //Θα αρχίσουμε να καταγράφουμε μόνο όταν o fused provider είναι φιξαρισμένος για πρώτη φορά (από προηγουμένως)
        //Παίρνουμε όλες τις θέσεις από εκεί και έπειτα (οι κακές θα εξαλειφθούν στον server. Απλά αν ακρίβεια είναι μεγαλύτερη από 47 δεν θα ενημερώνουμε το
        //UI επομένως θα βγάλουμε ένα μήνυμα στον χρήστη, ώστε να καταλαβαίνει γιατί η διαδρομή του δεν ανανεώνεται στο UI
        if(locationHasFirstFixedEvent == true){


            fixed = (location.getAccuracy()<=47.0);
            if (fixed) { //Έχει αποκτηθεί "φιξάρισμα"
                fixed = true;
                sendLocationFixedToUI(MSG_SET_LOCATION_FIXED);//Ενημερώνει το UI

                //Το γεωγραφικό πλάτος, μήκος και η ακρίβεια της θέσης που βρέθηκε
                Double lat =  location.getLatitude();
                Double lng =  location.getLongitude();
                float accur = location.getAccuracy();

                //-----------------------------------------------------------------------------------------------
                //CHECK GEOGRAPHIC LIMITATION AND UPDATE UI
/*
                if(MapCenterArea!=null && !MapCenterArea.contains(new LatLng(lat, lng)))
                {
                    userHasGoneOutOfRegion = true;//Για την περίπτωση που η mainactivity δεν υπάρχει
                    sendOutOfRegionToUI(town);//Για την περίπτωση που υπάρχει η mainactivity
                }
*/
                //-----------------------------------------------------------------------------------------------

                //Βάζουμε κάθε 2 εντοπισμούς θέσεις στην ArrayList για να μην γεμίσει η μνήμη
                counterForUILocations=counterForUILocations+1;

                numberOfLocationsGreaterThanZero=true;//Έχει βρεθεί τουλάχιστον μία θέση

                //Στο UI στέλνει την καλύτερη θέση που έχει εντοπιστεί (όταν έχει βρει 2 θέσεις)
                if(counterForUILocations==3 || counterForUILocations==1){//Αν είναι η 3η έχουν βρεθεί προηγουμένως 2, άρα ξαναμετράει από την αρχή
                    counterForUILocations=1;
                    //Θεωρεί την πρώτη θέση σαν την καλύτερη
                    bestCoordinatesOfLocation=new LatLng(lat,lng );
                    bestCoordinatesAccuracy=accur;
                }

                //Βάζουμε αυτήν που έχει την καλύτερη ακρίβεια από τις 2
                if(counterForUILocations>1 && accur <=bestCoordinatesAccuracy){
                    bestCoordinatesOfLocation=new LatLng(lat,lng );
                    bestCoordinatesAccuracy=accur;
                }

                if(counterForUILocations==2){//Αν είναι η 2η θέση βάζει την θέση με την καλύτερη ακρίβεια στο UI

                    coordinatesOfLocationsList.add(bestCoordinatesOfLocation);

                    //Calculate Speed
                    double speed = 0.00;

                    //Calculate speed from location and time
                    if (mCurrentLocationGoogle != null) {
                        long meters = calculateDistance(location.getLatitude(), location.getLongitude(), mCurrentLocationGoogle.getLatitude(), mCurrentLocationGoogle.getLongitude());
                        long msec = location.getTime() - mCurrentLocationGoogle.getTime();
                        speed = (double)((3600*meters)/msec);
                    }
                    //if there is speed from location
                    if (location.hasSpeed())
                    {
                        //get location speed
                        speed = location.getSpeed();
                    }

                    sendLastLocationToUI(bestCoordinatesOfLocation.latitude,bestCoordinatesOfLocation.longitude, (float)speed, location.getBearing());//Στέλνουμε τη νέα καλύτερη θέση στην Activity
                    if (coordinatesOfLocationsList.size()>=2){//Μετράει την απόσταση που έχει διανυθεί κάθε 2 θέσεις
                        totalDistance = totalDistance + distance (coordinatesOfLocationsList.get(coordinatesOfLocationsList.size()-2).latitude, coordinatesOfLocationsList.get(coordinatesOfLocationsList.size()-2).longitude,  bestCoordinatesOfLocation.latitude,  bestCoordinatesOfLocation.longitude);
                    }

                }

            } else { // To "φιξάρισμα" έχει χαθεί

                fixed = false;

                if (locationHasFirstFixedEvent){
                    sendLocationFixedToUI(MSG_SET_LOCATION_LOST);//Ενημερώνει το UI
                }

            }

            //Keep last location
            mCurrentLocationGoogle = location;//Η τρέχουσα θέση από την google play service


            //Καταγράφει τα trackpoints στο gpx αρχείο που δημιουργείται από τον fused provider
            String segment = "<trkpt lat=\"" + location.getLatitude() + "\" lon=\"" + location.getLongitude() + "\">"
                    + "<time>" + df.format(new Date(location.getTime())) + "</time>"
                    + "<hdop>" +location.getAccuracy() + "</hdop>" + "</trkpt>\n";

            try {

                FileOutputStream fOut = openFileOutput(segmentsOfTrackPointsFileGoogle.getName(),
                        MODE_APPEND);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);
                osw.write(segment);
                osw.flush();
                osw.close();

            } catch (FileNotFoundException e) {

                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "onLocationChanged 1");
                FirebaseCrash.report(e);

            } catch (IOException e) {

                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "onLocationChanged 2");
                FirebaseCrash.report(e);
            }

        }
    }


    //Σε απάντηση του αιτήματος για να ξεκινήσουν οι ενημερώσεις θέσης στείλε ένα αίτημα στις υπηρεσίες θέσης
    private void startPeriodicUpdates() {

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


            //Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            //if(location!=null){
                //Get new Location and set inform user text and update current location coordinates
                //handleNewLocation(location);
            //}
        }

    }

    //Σε απάντηση του αιτήματος για να σταματήσουν οι ενημερώσεις θέσης στείλε ένα αίτημα στις υπηρεσίες θέσης
    private void stopPeriodicUpdates() {


        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

    }

    //Βεβαιωνόμαστε ότι οι υπηρεσίες του Google Play είναι διαθέσιμες πριν από την υποβολή του αιτήματος.
    //Γυρίζει true αν οι υπηρεσίες του Google Play είναι διαθέσιμες, αλλιώς false
    private boolean servicesConnected() {

        //Ελέγχει ότι οι υπηρεσίες του Google Play είναι διαθέσιμες
        //int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // Εάν οι υπηρεσίες του Google Play είναι διαθέσιμες
        if (mGoogleApiClient!=null){//ConnectionResult.SUCCESS == resultCode) {
            // Στη λειτουργία εντοπισμού σφαλμάτων καταγράφει την κατάσταση
            //Log.d(TAG, getString(R.string.play_services_available));

            // Συνέχισε
            return true;
            // Οι υπηρεσίες του Google Play δεν είναι διαθέσιμες για κάποιο λόγο
        }
        else {
            //Στείλε ένα μήνυμα στην activity με το resultCode, ώστε αυτή να εμφανίσει ένα errorDialog στον χρήστη
            for (int i=mClients.size()-1; i>=0; i--) {//Θα στείλει τιμές σε όλους τους client που έχουν συνδεθεί (αρχίζοντας από τον τελευταίο που συνδέθηκε).
                //Πάντως, στην περιπτωση μας έχουμε για client μόνο την MainActivity.class που μάλιστα είναι foreground, αφού "μόλις" ο χρήστης έχει πατήσει το κουμπί
                //"StartRoute"
                try {
                    Bundle bundle = new Bundle();
                    bundle.putInt("result_code", 1);//resultCode); apap???

                    Message msg = Message.obtain(null, MSG_GOOGLE_PLAY_SERVICE_RESULT_CODE);
                    msg.setData(bundle);
                    mClients.get(i).send(msg);// Ο mClients.get(i) είναι ο Messenger που θα στείλει το μήνυμα
                }
                catch (RemoteException e) {
                    // Ο πελάτης έχει "πεθάνει". Τον βγάζουμε από την λίστα. Περνάμε την λίστα από το τέλος προς την αρχή επομένως είναι ασφαλές να το κάνουμε μέσα στο βρόχο.
                    //Πάντως, στην περίπτωση μας ο client θα είναι "ζωντανός" (το πιθανότερο),  αφού "μόλις" ο χρήστης έχει πατήσει το κουμπί "StartRoute"
                    mClients.remove(i);
                    FirebaseCrash.logcat(Log.ERROR, TAG, "servicesConnected");
                    FirebaseCrash.report(e);
                }
            }
            return false;
        }
    }

}
