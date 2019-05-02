package csc415.finalProject.SilentSpots;

import com.google.firebase.firestore.GeoPoint;

public class Rule {
    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public String getPlace() {
        return place;
    }

    public double getRadius() {
        return radius;
    }

    public GeoPoint getCoordinates() {
        return coordinates;
    }

    public String getSetting() {
        return setting;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public void setCoordinates(GeoPoint coordinates) {
        this.coordinates = coordinates;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    String title;
    String address;
    String place;
    double radius;
    GeoPoint coordinates;
    String setting;
    String clock;
    int large;
    int small;
}