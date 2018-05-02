package com.apap.pom;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.crash.FirebaseCrash;
import org.json.JSONObject;
import java.io.IOException;


/**
 * User related functions
 */

public class UserFunctions {

    /////////////////////////////////////////////

    //Debug Tag
    public static final String TAG = UserFunctions.class.getSimpleName();

    //Urls
    private static String pathsTypesURL;
    private static String loginRegisterURL;
    private static String balanceURL;

    //Json parser
    private JSONParser jsonParser;

    //TAGs for calls
    private static String GETPATHS_TAG = "getPathTypes";        //Tag for get paths Types
    private static String LOGIN_REGISTER_TAG = "log_reg";       //Tag for login-register
    private static String BALANCE_TAG = "getPlayerBalance";     //Tag for get player balance

    //Save logged in user values
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_UID = "uid";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_PATH_SET = "path_set";
    private static final String KEY_BALANCE = "balance";

    //Preferences
    public static final String PREFS_NAME = "com.apap.pom.USER_PREFS";
    SharedPreferences user_prefs;

    //Context
    public static Context userContext;

    /////////////////////////////////////////////

    /*
     * Constructor
     */
    public UserFunctions(Context cntx){
        jsonParser = new JSONParser();

        userContext = cntx;

        user_prefs =  userContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        pathsTypesURL = ((PomApplication) cntx.getApplicationContext()).getServerUrl() + "/request_log_reg_store_path.php";
        loginRegisterURL = ((PomApplication) cntx.getApplicationContext()).getServerUrl() + "/request_log_reg_store_path.php";
        balanceURL = ((PomApplication) cntx.getApplicationContext()).getServerUrl() + "/request_log_reg_store_path.php";
    }

    /*
     * Get from Server types of paths for requested path set
     */
    public JSONObject getPathsTypes(Integer path_set) {

        // Building Parameters
        ContentValues params = new ContentValues();
        params.put("tag", GETPATHS_TAG);
        params.put("path_set", path_set);

        // Get JSON string from URL
        JSONObject json = null;
        try {
            json = jsonParser.getJSONFromUrl(pathsTypesURL, params);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "getPathsTypes");
            FirebaseCrash.report(e);
        }
        return json;
    }

    /*
     * Get from Server if exist a balance from player
     */
    public JSONObject getPlayerBalance(Integer player){
        // Building Parameters
        ContentValues params = new ContentValues();
        params.put("tag", BALANCE_TAG);
        params.put("player_id", player);

        // Get JSON string from URL
        JSONObject json = null;
        try {
            json = jsonParser.getJSONFromUrl(balanceURL, params);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "getPlayerBalance");
            FirebaseCrash.report(e);
        }
        return json;
    }

    /*
     * Save logged in user values
     */
    public boolean saveLogInUser(String name, String email, Integer uid, String createdDate, Integer pathSet) {


        Log.d(TAG, "saveLogInUser name:"+name+" email:"+email+" uid:"+uid+" createdDate:"+createdDate);

        SharedPreferences.Editor editor = user_prefs.edit();
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putInt(KEY_UID, uid);
        editor.putString(KEY_CREATED_AT, createdDate);
        editor.putInt(KEY_PATH_SET, pathSet);
        editor.putInt(KEY_BALANCE, 0);
        return (editor.commit());
    }

    /*
     * User Login Request to Server - NEW METHOD
     */
    public JSONObject loginRegisterUser(String name, String email, String password){
        // Building Parameters
        ContentValues params = new ContentValues();
        params.put("tag", LOGIN_REGISTER_TAG);
        String nm = name.replace(' ', '^');
        params.put("name", nm);
        params.put("email", email);
        String pass = password.replace(' ', '^');
        params.put("password", pass);

        // Get JSON string from URL
        JSONObject json = null;
        try {
            json = jsonParser.getJSONFromUrl(loginRegisterURL, params);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "loginRegisterUser");
            FirebaseCrash.report(e);
        }
        return json;
    }


    /*
     * Get User Login Status
     */
    public boolean isUserLoggedIn(){

        String name = user_prefs.getString(KEY_NAME, null);
        String email = user_prefs.getString(KEY_EMAIL, null);
        Integer uid = user_prefs.getInt(KEY_UID, 0);
        String date = user_prefs.getString(KEY_CREATED_AT, null);
        //Integer pathSet = user_prefs.getInt(KEY_PATH_SET, 1);
        //Integer balance = user_prefs.getInt(KEY_BALANCE, 0);

        //Log.d(TAG, "isUserLoggedIn name:"+name+" email:"+email+" uid:"+uid+" date:"+date+" pathSet:"+pathSet+" balance:"+balance);

        if(name!=null && email!=null && uid>0 && date!=null) return true;
        else return false;
    }


    /*
     * Logout User (and reset database)
     */
    public boolean logoutUser(){

        SharedPreferences.Editor editor = user_prefs.edit();
        editor.putString(KEY_NAME, null);
        editor.putString(KEY_EMAIL, null);
        editor.putInt(KEY_UID, 0);
        editor.putString(KEY_CREATED_AT, null);
        editor.putInt(KEY_PATH_SET, 0);
        editor.putInt(KEY_BALANCE, 0);
        return (editor.commit());
    }

    /*
     * Return uid of user from device database
     */
    public int getUserUid(){
        return (user_prefs.getInt(KEY_UID, 0));
    }

    /*
     * Return email of user from device database
     */
    public String getUserEmail(){
        return (user_prefs.getString(KEY_EMAIL, "a@b"));
    }

    /*
    * Return path set of user from device database
    */
    public int getUserPathSet(){
        return (user_prefs.getInt(KEY_PATH_SET, 0));
    }

    /*
    * Return balance of user from device database
    */
    public int getUserBalance(){
        return (user_prefs.getInt(KEY_BALANCE, 0));
    }

    /*
     * Update user balance
     */
    public boolean setUserBalance(int balance){
        SharedPreferences.Editor editor = user_prefs.edit();
        editor.putInt(KEY_BALANCE, balance);
        return (editor.commit());
    }
}

