package com.apap.pom;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/******************************************************************
 * Κλάση που γράφει τα GPX αρχεία (καλείται ασύγχρονα από το UI)  *
 * ****************************************************************/
public class AsynGPXWriter extends AsyncTask<Void, Long, Boolean> {

    //Debug Tag
    public static final String TAG = AsynGPXWriter.class.getSimpleName();

    //Files Handlers
    private File mfileGoogle;   //GPX file from google play service
    private File msegmentfileGoogle;//File with trackpoints from google play service
    private File msegmentOfWayPointsFileGoogle;//File with waypoints google play service

    //App Description for GPX file
    private String n ="Tracking by PoM";

    //Context
    private Context mcontext;

    //Progress Dialog
    private ProgressDialog mDialog;

    /*
     * Constructor
     */
    public AsynGPXWriter(Context context,File fileGoogle,File segmentfileGoogle,File segmentOfWayPointsFileGoogle){

        //Files Handlers
        mfileGoogle = fileGoogle;
        msegmentfileGoogle = segmentfileGoogle;
        msegmentOfWayPointsFileGoogle = segmentOfWayPointsFileGoogle;

        //Context
        mcontext=context;

        //Progress Dialog
        mDialog= ProgressDialog.show(mcontext,"Please wait ...","Saving path...",true);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        // GPX File header data
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"PoM\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n";

        //App name and start data
        String name = "<trk>\n<name>" + n + "</name><trkseg>\n";

        //GPX File end data
        String footer = "</trkseg></trk></gpx>";

        //Write File
        try {
            FileWriter writer = new FileWriter(mfileGoogle, false);
            writer.append(header);

            //If WayPoints file exists (if first time user add tags:file does not exist)
            if(msegmentOfWayPointsFileGoogle.exists()){


                //waypoints
                InputStream inputStream1 = new FileInputStream(msegmentOfWayPointsFileGoogle);
                BufferedReader r1 = new BufferedReader(new InputStreamReader(inputStream1));
                StringBuilder totalWpt = new StringBuilder();
                String line1;

                while ((line1 = r1.readLine()) != null) {
                    totalWpt.append(line1);
                }
                r1.close();
                //Write section with waypoints
                writer.append(totalWpt);
            }

            writer.append(name);

            //trackpoints
            InputStream inputStream2 = new FileInputStream(msegmentfileGoogle);

            BufferedReader r2 = new BufferedReader(new InputStreamReader(inputStream2));
            StringBuilder totalTrkp = new StringBuilder();
            String line2;
            while ((line2 = r2.readLine()) != null) {
                totalTrkp.append(line2);
            }
            r2.close();


            //Write section with trackpoints
            writer.append(totalTrkp);
            writer.append(footer);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            //Error in write file
            FirebaseCrash.logcat(Log.ERROR, TAG, "AsynGPXWriter doInBackground Error file");
            FirebaseCrash.report(e);
            return false;
        }

        //Successful write file
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {

        mDialog.dismiss();

        if (result) {
            showToast("The path is saved in device");

        } else {
            showToast("Error Writing Path");
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mcontext.getApplicationContext(), msg, Toast.LENGTH_LONG);
        error.show();
    }

}
