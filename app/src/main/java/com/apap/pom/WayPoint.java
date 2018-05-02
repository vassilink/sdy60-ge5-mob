package com.apap.pom;

/**
 * Way Point Class
 */

public class WayPoint {

    double lat;
    double lon;
    String name;

    // constructors
    public WayPoint(){

    }

    public WayPoint(double lat,double lon,String name){
        this.lat = lat;
        this.lon = lon;
        this.name = name;

    }

    // setters
    public void setLat(double lat){
        this.lat = lat;
    }

    public void setLon(double lon){
        this.lon = lon;
    }

    public void setName(String name){
        this.name = name;
    }

    // getters
    public double getLat() {
        return this.lat;
    }

    public double getLon() {
        return this.lon;
    }

    public String getName() {
        return this.name;
    }

}
