package com.apap.pom;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.crash.FirebaseCrash;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Parse GPX response from server and draw to Google map
 *
 */

public class ParsingGPXForDrawing {

    //Debug Tag
    public static final String TAG = ParsingGPXForDrawing.class.getSimpleName();

    //App Preferences
    private SharedPreferences sharedPrefs;
    private boolean prefMapWalkPathsOn;
    private boolean prefMapPendPathsOn;

    private File mFile;
    private GoogleMap mGoogleMap;
    private Context mContext;
    public static HashMap<String,Integer> polylinesMap;
    public static HashMap<String,Double> tagsMap;


    public ParsingGPXForDrawing(File File, GoogleMap GoogleMap, Context context){

        this.mFile = File;
        this.mGoogleMap = GoogleMap;
        this.mContext = context;

        polylinesMap = new HashMap<String, Integer>();
        tagsMap = new HashMap<String, Double>();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        prefMapWalkPathsOn = sharedPrefs.getBoolean("appMapWalkPaths", true);
        prefMapPendPathsOn = sharedPrefs.getBoolean("appMapPendPaths", false);

    }

    /*
     * Decode file for Way points
     */
    public void decodeGpxForWpts(){

        //Defines a factory API that enables applications to obtain a parser that produces DOM object trees from XML documents
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try{

            //Creates a new instance of a DocumentBuilder using the currently configured parameters.
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Log.d(TAG, "decodeGpxForWpts mFile: " + mFile.toString());

            FileInputStream fileInputStream = new FileInputStream(mFile);

            //Parse the content of the given InputStream as an XML document and return a new DOM Document object.
            //Conceptually, it is the root of the document tree, and provides the primary access to the document's data.
            Document document = documentBuilder.parse(fileInputStream);
            //--------------------------------------------------------------------
            fileInputStream.close();
            //--------------------------------------------------------------------
            //This is a convenience attribute that allows direct access to the child node that is the document element of the document.
            Element elementRoot = document.getDocumentElement();
            NodeList nodeListOfWpt = elementRoot.getElementsByTagName("wpt");//Nodes list with all wpt


            Log.d(TAG, "decodeGpxForWpts nodeListOfWpt.getLength(): " + nodeListOfWpt.getLength());

            for(int i = 0; i < nodeListOfWpt.getLength(); i++){
                Node node = nodeListOfWpt.item(i);
                //-------------------------------------------------------
                Element waypointElement=(Element)node;//wpt element
                NodeList nodelist_name = waypointElement.getElementsByTagName("name");//waypoint name list
                String name = nodelist_name.item(0).getTextContent();//There is only one name (get the first: index 0)
                //------------------------------------------------------

                NamedNodeMap attributes = node.getAttributes();

                String newLatitude = attributes.getNamedItem("lat").getTextContent();
                Double newLatitude_double = Double.parseDouble(newLatitude);

                String newLongitude = attributes.getNamedItem("lon").getTextContent();
                Double newLongitude_double = Double.parseDouble(newLongitude);


                ArrayList<Integer> pathsidList = new ArrayList<Integer>();
                NodeList nodeListOfPAthIds = waypointElement.getElementsByTagName("pathid");
                Log.d(TAG, "decodeGpxForWpts nodeListOfPAthIds.getLength(): " + nodeListOfPAthIds.getLength());
                for(int j=0;j<nodeListOfPAthIds.getLength();j++){
                    Node nodeOfpathid = nodeListOfPAthIds.item(j);//pathid node
                    Log.d(TAG, "1 pathid:"+nodeOfpathid.getTextContent());
                    pathsidList.add(Integer.parseInt(nodeOfpathid.getTextContent()));
                }

                addWayPointInMap(new WayPoint(newLatitude_double,newLongitude_double,name), pathsidList, mGoogleMap);
            }



        }catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "decodeGpxForWpts 1");
            FirebaseCrash.report(e);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "decodeGpxForWpts 2");
            FirebaseCrash.report(e);
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "decodeGpxForWpts 3");
            FirebaseCrash.report(e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "decodeGpxForWpts 4");
            FirebaseCrash.report(e);
        }
    }


    /*
     * Decode file for line points
     */
    public void decodeGPXForTrksegs(){

        //Defines a factory API that enables applications to obtain a parser that produces DOM object trees from XML documents
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            //Creates a new instance of a DocumentBuilder using the currently configured parameters.
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            FileInputStream fileInputStream = new FileInputStream(mFile);

            //Parse the content of the given InputStream as an XML document and return a new DOM Document object.
            //Conceptually, it is the root of the document tree, and provides the primary access to the document's data.
            Document document = documentBuilder.parse(fileInputStream);

            //------------------------------------------------------------------------------------------------------
            fileInputStream.close();
            //------------------------------------------------------------------------------------------------------

            //This is a convenience attribute that allows direct access to the child node that is the document element of the document.
            Element elementRoot = document.getDocumentElement();
/*
            NodeList nodeListOfPAthIds = elementRoot.getElementsByTagName("pathid");
            Log.d(TAG, "decodeGPXForTrksegs nodeListOfPAthIds.getLength(): " + nodeListOfPAthIds.getLength());
            for(int j=0;j<nodeListOfPAthIds.getLength();j++){
                Node nodeOfpathid = nodeListOfPAthIds.item(j);//pathid node
                Log.d(TAG, "pathid:"+nodeOfpathid.getTextContent());
            }
*/
            NodeList nodeListOfTrseg = elementRoot.getElementsByTagName("trkseg");//trkseg nodes list

            Log.d(TAG, "decodeGPXForTrksegs nodeListOfTrseg.getLength(): " + nodeListOfTrseg.getLength());

            for(int i=0;i<nodeListOfTrseg.getLength();i++){ //For all trkseg

                Element trksegElement = (Element)nodeListOfTrseg.item(i);//trkseg element

                NodeList nodeListOftrkpt = trksegElement.getElementsByTagName("trkpt");//List of trkpt for current trkseg

                ArrayList<LatLng> singleList = new ArrayList<LatLng>();   //clear list


                Log.d(TAG, "decodeGPXForTrksegs nodeListOftrkpt.getLength(): " + nodeListOftrkpt.getLength());


                for(int j=0;j<nodeListOftrkpt.getLength();j++){ //For all trkpt for current trkseg
                    Node nodeOftrkpt = nodeListOftrkpt.item(j);//trkpt node

                    NamedNodeMap attributes = nodeOftrkpt.getAttributes();//attributes of trkpt

                    String newLatitude = attributes.getNamedItem("lat").getTextContent();
                    Double newLatitude_double = Double.parseDouble(newLatitude);

                    String newLongitude = attributes.getNamedItem("lon").getTextContent();
                    Double newLongitude_double = Double.parseDouble(newLongitude);

                    LatLng newLocation = new LatLng(newLatitude_double,newLongitude_double);

                    singleList.add(newLocation);
                }

                //Check if sketch path
                int path_type = 0;
                NodeList nodeListOfNane = trksegElement.getElementsByTagName("name");
                if(nodeListOfNane.getLength()>0){
                    if(nodeListOfNane.item(0).getTextContent().equals("Pending Path")) {
                        path_type = 1;
                    }else if(nodeListOfNane.item(0).getTextContent().equals("Sketch Path")) {
                        path_type = 2;
                    }
                }

                //Check if exist pathId
                ArrayList<Integer> pathsidList = new ArrayList<Integer>();
                NodeList nodeListOfPAthIds = trksegElement.getElementsByTagName("pathid");
                Log.d(TAG, "decodeGPXForTrksegs nodeListOfPAthIds.getLength(): " + nodeListOfPAthIds.getLength());
                for(int j=0;j<nodeListOfPAthIds.getLength();j++){
                    Node nodeOfpathid = nodeListOfPAthIds.item(j);//pathid node
                    Log.d(TAG, "2 pathid:"+nodeOfpathid.getTextContent());
                    pathsidList.add(Integer.parseInt(nodeOfpathid.getTextContent()));
                }

                addPolylineInMap(singleList, pathsidList, path_type, mGoogleMap);
                //--------------------------
                singleList=null;
                //-----------------------
            }

            fileInputStream.close();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "decodeGPXForTrksegs 1");
            FirebaseCrash.report(e);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "decodeGPXForTrksegs 2");
            FirebaseCrash.report(e);
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "decodeGPXForTrksegs 3");
            FirebaseCrash.report(e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            FirebaseCrash.logcat(Log.ERROR, TAG, "decodeGPXForTrksegs 4");
            FirebaseCrash.report(e);
        }
    }

    /*
     * Add a Polyline to Google map
     */
    private void addPolylineInMap(ArrayList<LatLng> trkseg, ArrayList<Integer> pathsid, int path_type, GoogleMap GoogleMap){
        if (GoogleMap != null){

            Log.d(TAG, "addPolylineInMap path_type:" + path_type);

            switch(path_type)
            {
                case 0: //Walk
                    if(!prefMapWalkPathsOn) return;
                    break;
                case 1: //Pending
                    if(!prefMapPendPathsOn) return;
                    break;
                case 2: //Sketch
                    break;
            }
            PolylineOptions rectOptions = new PolylineOptions();

            if(path_type==2) rectOptions.width(6).color(Color.GREEN).geodesic(true); //Sketch (Accepted) Path
            else if(path_type==1) rectOptions.width(6).color(Color.parseColor("#FF8800")).geodesic(true); //Sketch (Pending) Path
            else rectOptions.width(6).color(Color.RED).geodesic(true);  //Walk Path

            for(int i = 0; i < trkseg.size(); i++){
                rectOptions.add(new LatLng(trkseg.get(i).latitude, trkseg.get(i).longitude));
            }
            //GoogleMap.addPolyline(rectOptions);
            Polyline pl = GoogleMap.addPolyline(rectOptions);
            pl.setClickable(true);
            if(!pathsid.isEmpty()) {
                Log.d(TAG, "1 id:" + pl.getId());
                polylinesMap.put(pl.getId(), pathsid.get(0));
            }
        }
        //---------------------------
        trkseg=null;
        //----------------------
    }


    /*
     * Add a way point to Google map
     */
    private void addWayPointInMap(WayPoint wpt, ArrayList<Integer> pathsid, GoogleMap Map){
        if (Map != null){
            // create marker
            MarkerOptions marker = new MarkerOptions().position(new LatLng(wpt.getLat(), wpt.getLon())).title(wpt.getName());
            // ROSE color icon
            // marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
            // adding marker
            //Map.addMarker(marker);
            Marker m = Map.addMarker(marker);
            //if(!pathsid.isEmpty()) {
            //    Log.d(TAG, "2 id:" + m.getId());
            //    tagsMap.put(m.getId(), pathsid.get(0));
            //}

            tagsMap.put(m.getId(), wpt.getLat());
            Log.d(TAG, "tagsMap.size"+tagsMap.size());
        }
        //----------------------------
        wpt=null;
        //----------------------------
    }
}
