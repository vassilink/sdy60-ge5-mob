package com.apap.pom;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

/**
 * Google Play Game Start Activity (Leaderboard - Achievements)
 */

public class GoogleGamesActivity extends AppCompatActivity implements
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    /////////////////////////////////////////////
    //Debug Tag
    public static final String TAG = GoogleGamesActivity.class.getSimpleName();

    //Google API Client
    private GoogleApiClient mGoogleApiClient;

    //Sign In code
    private static int RC_SIGN_IN = 9001;

    //Flags
    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;
    boolean mExplicitSignOut = false;
    boolean mInSignInFlow = false;  // set to true when you're in the middle of the
                                    // sign in flow, to know you should not attempt
                                    // to connect in onStart()

    //Ids for Google Activities
    private static int REQUEST_ACHIEVEMENTS = 1;
    private static int REQUEST_LEADERBOARD = 2;

    //Analytics
    private Tracker mTracker;
    private FirebaseAnalytics mFirebaseAnalytics;

    //For call user logout function
    UserFunctions userFunctions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_games);

        // Obtain the shared Tracker instance.
        PomApplication application = ((PomApplication) getApplication());
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("GOOGLE GAMES");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setCurrentScreen(this, "GOOGLE_GAMES", "GoogleGamesActivity");

        //Check if user logged in (from database) - else load login activity
        userFunctions = new UserFunctions(getApplicationContext());

        // my_child_toolbar is defined in the layout file
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        //Show toolbar icon
        ab.setIcon(R.mipmap.ic_launcher);

        //Buttons
        findViewById(R.id.games_sign_in_button).setOnClickListener(this);
        findViewById(R.id.games_sign_out_button).setOnClickListener(this);
        findViewById(R.id.imgBtnLeaderboard).setOnClickListener(this);
        findViewById(R.id.imgBtnAchievements).setOnClickListener(this);

        // Create the Google Api Client with access to the Play Games services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                // add other APIs and scopes here as needed
                .build();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mInSignInFlow && !mExplicitSignOut) {
            // auto sign in
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // The player is signed in.
        // If there is a button: Hide the sign-in button and allow the player to proceed.

        // hide the sign-in button
        findViewById(R.id.games_sign_in_button).setVisibility(View.GONE);
        // show sign-out button
        //findViewById(R.id.games_sign_out_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Attempt to reconnect
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {


        Log.d(TAG, "**** onConnectionFailed G mResolvingConnectionFailure:"+mResolvingConnectionFailure
                + "  mSignInClicked:"+mSignInClicked
                + "  mAutoStartSignInFlow:"+mAutoStartSignInFlow);

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

                FirebaseCrash.logcat(Log.ERROR, TAG, "Google Games onConnectionFailed");
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

        Log.d(TAG, "**** onActivityResult G requestCode:"+requestCode + "  resultCode:"+resultCode);

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

                FirebaseCrash.logcat(Log.ERROR, TAG, "Google Games G onActivityResult requestCode:"+requestCode+" resultCode:"+resultCode);
            }
        }
    }


    @Override
    public void onClick(View view) {

        // Obtain the shared Tracker instance.
        PomApplication application = ((PomApplication) getApplication());

        // FirebaseAnalytics
        Bundle bundle = new Bundle();

        switch(view.getId()) {

            case R.id.games_sign_in_button:
                // start the asynchronous sign in flow
                mSignInClicked = true;
                mGoogleApiClient.connect();

                // FirebaseAnalytics
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "GOOGLE_SIGN_IN");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                break;

            case R.id.games_sign_out_button:
                // sign out.
                mSignInClicked = false;

                // user explicitly signed out, so turn off auto sign in
                mExplicitSignOut = true;
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Games.signOut(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }

                // show sign-in button, hide the sign-out button
                //findViewById(R.id.games_sign_in_button).setVisibility(View.VISIBLE);
                findViewById(R.id.games_sign_out_button).setVisibility(View.GONE);

                // FirebaseAnalytics
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "GOOGLE_SIGN_OUT");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                break;

            case R.id.imgBtnLeaderboard:
                // Obtain the shared Tracker instance.
                mTracker = application.getDefaultTracker();
                mTracker.setScreenName("GOOGLE GAMES REQUEST LEADERBOARD");
                mTracker.send(new HitBuilders.ScreenViewBuilder().build());

                // FirebaseAnalytics
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "REQUEST_LEADERBOARD");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                //Start Leaderboard Activity
                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient, getString(R.string.leaderboard_id),
                        LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC), REQUEST_LEADERBOARD);
                break;

            case R.id.imgBtnAchievements:
                // Obtain the shared Tracker instance.
                mTracker = application.getDefaultTracker();
                mTracker.setScreenName("GOOGLE GAMES REQUEST ACHIEVEMENTS");
                mTracker.send(new HitBuilders.ScreenViewBuilder().build());

                // FirebaseAnalytics
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "REQUEST_ACHIEVEMENTS");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                //Start Achievements Activity
                startActivityForResult(Games.Achievements.getAchievementsIntent(mGoogleApiClient), REQUEST_ACHIEVEMENTS);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rank, menu);
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
                Intent review_intent = new Intent(GoogleGamesActivity.this, ReviewPathActivity.class);
                startActivity(review_intent);
                finish();
                return  true;
            case R.id.submenu_settings:
                Intent settings_intent = new Intent(GoogleGamesActivity.this, SettingsActivity.class);
                startActivity(settings_intent);
                finish();
                return  true;
            case R.id.submenu_log_out:
                //Clear previous saved login values
                userFunctions.logoutUser();
                Intent login_intent = new Intent(GoogleGamesActivity.this, SignInActivity.class);
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


