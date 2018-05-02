package com.apap.pom;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.firebase.crash.FirebaseCrash;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Download GPX file with paths from server
 */

public class DownloadGPXFileAsync extends AsyncTask<String, String, Boolean> {


    //Debug Tag
    public static final String TAG = DownloadGPXFileAsync.class.getSimpleName();

    //Increase due to Live Server wake up delay (first user)
    //private static final int ReadHttpReadTimeout = 15000;//10000; // milliseconds
    //private static final int ReadHttpConTimeout  = 20000;//15000; // milliseconds

    private final ProgressDialog mProgressDialog;
    private Context mContext;
    private File mFile;
    private GoogleMap mMap;
    private Exception error;
    private int activityType=0;
    //private BufferedWriter writer = null;
    //private OutputStream os = null;
    //private OutputStreamWriter osw = null;


    public DownloadGPXFileAsync(Context context, GoogleMap GoogleMap){

        error = null;
        mMap = GoogleMap;
        mContext = context.getApplicationContext();

        if(context.getClass().getSimpleName().equals("ReviewPathActivity")) activityType = 1;
        else activityType = 0;

        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMax(100);
        if(activityType==0) mProgressDialog.setMessage("Downloading all paths...");
        else mProgressDialog.setMessage("Downloading paths near you...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgress(0);
        mProgressDialog.show();
    }


    @Override
    protected Boolean doInBackground(String... aurl) {

        try {

            URL url = new URL(aurl[0]+aurl[1]);
            URLConnection conn = url.openConnection();
            conn.connect();

            int lenghtOfFile = conn.getContentLength();
            if(lenghtOfFile==-1) return false; //no connection

            Log.d(TAG, "Lenght of file: " + lenghtOfFile);

            InputStream input = new BufferedInputStream(url.openStream());

            //Το όνομα του αρχείου που θα αποθηκευτεί στην συσκευή και θα περιέχει όλες τις διαδρομές των χρηστών
            //Name of local file with paths
            String myNewFileName;
            if(activityType==0) myNewFileName = "merge.gpx";
            else myNewFileName = "review.gpx";

            mFile = new File(mContext.getFilesDir(), myNewFileName);

            OutputStream output = new FileOutputStream(mFile);

            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                output.write(data, 0, count);
            }

            Log.d(TAG, "Bytes total: " + total);

            output.flush();

            output.close();
            input.close();


        } catch (Exception e) {
            //e.printStackTrace();
            error = e;
            FirebaseCrash.logcat(Log.ERROR, TAG, "DownloadGPXFileAsync 1");
            FirebaseCrash.report(e);
            return false;
        }

        return true;
    }


    protected void onProgressUpdate(String... progress) {

        mProgressDialog.setProgress(Integer.parseInt(progress[0]));
    }

    @Override
    protected void onPostExecute(Boolean result) {

        Log.d(TAG, "onPostExecute result:"+result + " error:"+error);

        mProgressDialog.dismiss();

        if(result) { //Success

            if(activityType==0) Toast.makeText(mContext, "Successful Download Paths!", Toast.LENGTH_SHORT).show();

            ParsingGPXForDrawing parsingForDrawing = new ParsingGPXForDrawing(mFile, mMap, mContext);

            parsingForDrawing.decodeGPXForTrksegs();

            parsingForDrawing.decodeGpxForWpts();
        }else{         //Error
            if(error!=null) {
                if(activityType==0) Toast.makeText(mContext, "Download Paths Error!", Toast.LENGTH_LONG).show();
            }
        }

    }
}
