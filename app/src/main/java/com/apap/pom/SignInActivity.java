package com.apap.pom;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Sign In with User Google Account
 * (Register and LogIn)
 */


public class SignInActivity extends AppCompatActivity implements
        View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    //Debug Tag
    private static final String TAG = SignInActivity.class.getSimpleName();

    //Google API Client
    private GoogleApiClient mGoogleApiClient;

    //Buttons
    private SignInButton btnSignIn;
    private Button btnSignOut;
    private Button btnRevokeAccess;

    //Progress Dialog
    private ProgressDialog mProgressDialog;

    //Flag come from Log Out
    private boolean fromLogOut;

    //Google Sign In Value
    private static final int RC_SIGN_IN = 9001;

    //Error in login
    String err_login=null;

    // JSON nodes
    private static String KEY_SUCCESS = "success";
    private static String KEY_UID = "uid";
    private static String KEY_NAME = "name";
    private static String KEY_EMAIL = "email";
    private static String KEY_CREATED_AT = "created_at";
    private static String KEY_PATH_SET = "path_set";
    private static String KEY_ERROR = "error";
    private static String KEY_ERROR_MSG = "error_msg";

    //Analytics
    private Tracker mTracker;
    private FirebaseAnalytics mFirebaseAnalytics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_in);

        // Obtain the shared Tracker instance.
        PomApplication application = ((PomApplication) getApplication());
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("LOGIN");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Obtain the Firebase Analytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setCurrentScreen(this, "LOGIN", "SignInActivity");

        Intent intent = getIntent();
        fromLogOut = intent.getExtras().getBoolean("logout");

        //Buttons
        btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
        btnSignOut = (Button) findViewById(R.id.btn_sign_out);
        btnRevokeAccess = (Button) findViewById(R.id.btn_revoke_access);

        //From Log Out Button
        if(fromLogOut){
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "BUTTON_LOGOUT");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            btnSignIn.setVisibility(View.VISIBLE);
        }

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //.requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        //Google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        //Buttons Listeners
        btnSignIn.setOnClickListener(this);
        btnSignOut.setOnClickListener(this);
        btnRevokeAccess.setOnClickListener(this);

        //Check if there is a network connection
        ConnectionDetector mConnectionDetector = new ConnectionDetector(getApplicationContext());
        if(mConnectionDetector.isNetworkConnected()<=0 || mConnectionDetector.isInternetAvailable()==false) {
            //Info message about connectivity problem
            Toast.makeText(getApplicationContext(), R.string.internet_connection_required, Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Sign In
     */
    private void signIn() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "BUTTON_SIGN_IN");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "BUTTON");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    /*
     * Sign Out
     */
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        updateUI(false);
                    }
                });
    }

    /*
     * Revoke Access
     */
    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        updateUI(false);
                    }
                });
    }


    /*
     * Save Signed In user values
     */
    private void handleSignInResult(GoogleSignInResult result) {

        Log.d(TAG, "handleSignInResult:" + result.isSuccess());

        if (result.isSuccess()) {

            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            //Get account values
            String name = acct.getDisplayName();
            String email = acct.getEmail();
            String id = acct.getId();

            //Log.e(TAG, "Name: " + name);
            //Log.e(TAG, "email: " + email);
            //Log.e(TAG, "Id: " + id);

            // Check and Register User
            err_login = null;
            new SignInUser().execute(name, email, id);

        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btn_sign_in:
                signIn();
                break;

            case R.id.btn_sign_out:
                signOut();
                break;

            case R.id.btn_revoke_access:
                revokeAccess();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if(fromLogOut) return;

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.

            //Log.d(TAG, "Got cached sign-in");

            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.

            //Log.d(TAG, "NOT cached sign-in");
/*
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {

                    handleSignInResult(googleSignInResult);
                }
            });
*/
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.e(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void updateUI(boolean isSignedIn) {
        if (isSignedIn) {
            btnSignIn.setVisibility(View.GONE);
            //btnSignOut.setVisibility(View.VISIBLE);
            //btnRevokeAccess.setVisibility(View.VISIBLE);
        } else {
            btnSignIn.setVisibility(View.VISIBLE);
            //btnSignOut.setVisibility(View.GONE);
            //btnRevokeAccess.setVisibility(View.GONE);
        }
    }

    /**
     * Background Async Task for login through HTTP request
     * */
    class SignInUser extends AsyncTask<String, String, Boolean> {

        /*
         * Show dialog indicator before start background thread
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(SignInActivity.this);
            mProgressDialog.setMessage("Sign in, please wait...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }


        @Override
        protected Boolean doInBackground(String... strings) {

            UserFunctions userFunction = new UserFunctions(getApplicationContext());

            // Get JSON string from URL
            JSONObject json = userFunction.loginRegisterUser(strings[0], strings[1], strings[2]);
            if (json == null) {
                err_login = "Error in user Login!";
                return false;
            }
            Log.d(TAG, "json:"+json.toString());

            try {
                if (json.getString(KEY_SUCCESS) != null) {

                    String res = json.getString(KEY_SUCCESS);
                    if(Integer.parseInt(res) == 1) {

                        //User successfully login

                        //Get login values from server response
                        JSONObject json_user = json.getJSONObject("player");

                        if (json_user == null) {
                            err_login = "Error in user Login!";
                            return false;
                        }

                        //Clear previous saved login values
                        userFunction.logoutUser();

                        //Save login values
                        userFunction.saveLogInUser(json_user.getString(KEY_NAME), json_user.getString(KEY_EMAIL), json.getInt(KEY_UID), json_user.getString(KEY_CREATED_AT), json_user.getInt(KEY_PATH_SET));

                        //Toast.makeText(getApplicationContext(), "Welcome "+strings[0], Toast.LENGTH_LONG).show();

                    }else if(Integer.parseInt(res) == 2){
                        //User successfully register

                        //Get login values from server response
                        JSONObject json_user = json.getJSONObject("player");
                        if (json_user == null) {
                            err_login = "Error in user Registration!";
                            return false;
                        }

                        //Clear previous saved login values
                        userFunction.logoutUser();


                        //Save login values
                        userFunction.saveLogInUser(json_user.getString(KEY_NAME), json_user.getString(KEY_EMAIL), json.getInt(KEY_UID),  json_user.getString(KEY_CREATED_AT), json_user.getInt(KEY_PATH_SET));

                        //Toast.makeText(getApplicationContext(), "Welcome "+strings[0], Toast.LENGTH_LONG).show();

                    }else{
                        //user login error
                        err_login = "Error occurred in Login/Registration";
                        return false;
                    }
                }else if (json.getString(KEY_ERROR) != null){
                    if(Integer.parseInt(json.getString(KEY_ERROR)) > 0){
                        err_login = json.getString(KEY_ERROR_MSG);
                    }else{
                        err_login = "Error occurred in Login/Registration";
                    }
                    return false;
                }else{
                    err_login = "Error occurred in Login/Registration";
                    return false;
                }

            } catch (JSONException e) {
                e.printStackTrace();
                err_login = e.toString();
                FirebaseCrash.logcat(Log.ERROR, TAG, "SignInUser");
                FirebaseCrash.report(e);
                return false;
            }
            return true;
        }


        @Override
        protected void onPostExecute(Boolean result) {

            //Log.d(TAG, "---> onPostExecute result:"+ result);

            // Close dialog
            mProgressDialog.dismiss();

            if(result){
                //Success

                //Start Main activity
                Intent main = new Intent(getApplicationContext(), MainActivity.class);
                main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(main);
                finish();
            }else{
                //Error

                // Update UI from Background Thread
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(err_login!=null){
                            updateUI(false);
                            Toast.makeText(getApplicationContext(), err_login, Toast.LENGTH_LONG).show();
                        }else{
                            updateUI(true);
                        }
                    }
                });
            }
        }
    }

}
