package com.apap.pom;

/**
 * Path Types class
 */

public class PathsTypes {

    private int pathSetNum;
    private String pathSetName;
    private int pathSetOrder;

    // constructors
    public PathsTypes(){

    }

    //setters
    public void setPathSetNum(int pathSetNum) { this.pathSetNum = pathSetNum;  }
    public void setpathSetName(String pathSetName) { this.pathSetName = pathSetName; }
    public void setpathSetOrder(int pathSetOrder) { this.pathSetOrder = pathSetOrder; }

    //getters
    public int getPathSetNum() { return this.pathSetNum; }
    public String getpathSetName() { return this.pathSetName; }
    public int getpathSetOrder() { return this.pathSetOrder; }
}
