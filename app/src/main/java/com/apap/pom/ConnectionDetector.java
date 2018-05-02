package com.apap.pom;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * Connectivity Helper
 */

public class ConnectionDetector {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;

    private Context mContext;

    public ConnectionDetector(Context context){
        this.mContext = context;
    }

    /*
    * Check if there is available a Network Connection
    */
    public int isNetworkConnected() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (null != activeNetworkInfo) {

                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if(activeNetworkInfo.getState()==NetworkInfo.State.CONNECTED) return TYPE_WIFI;
                }
                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    if(activeNetworkInfo.getState()==NetworkInfo.State.CONNECTED) return TYPE_MOBILE;
                }
            }
        }catch (Exception e){
            return TYPE_NOT_CONNECTED;
        }
        return TYPE_NOT_CONNECTED;
    }

    /*
     * Check if there is mobile data capability
     */
    public boolean hasMobileDatacapability(){
        try {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if(telephonyManager!=null){
                int res = telephonyManager.getNetworkType();

                Log.d(TAG, "@@@ hasMobileDatacapability res:"+res);

                if(res==TelephonyManager.NETWORK_TYPE_LTE ||
                        res==TelephonyManager.NETWORK_TYPE_EDGE ||
                        res==TelephonyManager.NETWORK_TYPE_HSDPA ||
                        res==TelephonyManager.NETWORK_TYPE_HSPA ||
                        res==TelephonyManager.NETWORK_TYPE_HSPAP) return true;
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }

    /*
     * Check if there is Internet Connection
     */
    public boolean isInternetAvailable() {
        try
        {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                //URL url = new URL("http://www.google.com");
                //URLConnection conn = url.openConnection();
                //conn.setConnectTimeout(5000);
                //conn.connect();
                //Log.d(TAG, "isInternetAvailable ok");
                return true;
            }
        }
        catch (Exception e)
        {
            //Log.e(TAG, "isInternetAvailable err");
            return false;
        }
        return false;
    }
}
