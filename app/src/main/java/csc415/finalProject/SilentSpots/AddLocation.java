package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class AddLocation extends FragmentActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationProviderClient;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_location);

        queue = Volley.newRequestQueue(this);

        String text = "Add a location by searching for one above, pressing the 'My Location' button, or by long pressing a location on the map!";

        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(this, text, duration);

        toast.show();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        Places.initialize(getApplicationContext(), "AIzaSyCdcnMssDUkAhRDPYtuYToZVsaFv84N7Ag");

        PlacesClient placesClient = Places.createClient(this);


        AutocompleteSupportFragment autocompleteFrag = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFrag.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));


        autocompleteFrag.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Intent data = new Intent();
                data.setData(Uri.parse(place.getId()));
                setResult(RESULT_OK, data);

                finish();
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFrag);
        mapFragment.getMapAsync(this);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            try {
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(this, location -> googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16)));
            } catch (Exception ignored) {
            }
        }
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);

        googleMap.setMyLocationEnabled(true);

        googleMap.setOnMyLocationButtonClickListener(() -> {
            LatLng currentLatLng = googleMap.getCameraPosition().target;

            BigDecimal lat = new BigDecimal(currentLatLng.latitude).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lng = new BigDecimal(currentLatLng.longitude).setScale(4, RoundingMode.HALF_UP);


            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat.doubleValue() + "," + lng.doubleValue() + "&key=AIzaSyBLmMc3Orkr-IpcHanxaZrCcJ_JWpULFc0";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
                try {

                    JSONArray array = response.getJSONArray("results");
                    JSONObject geometry = array.getJSONObject(1);
                    String place_id = geometry.getString("place_id");

                    Intent data = new Intent();
                    data.setData(Uri.parse(place_id));
                    setResult(RESULT_OK, data);

                    finish();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> Log.println(Log.ERROR, "VolleyError", "Volley request failed"));
            queue.add(jsonObjectRequest);

            return false;
        });


        Geocoder geocoder = new Geocoder(this);
        googleMap.setOnMapLongClickListener(latLng -> {
            googleMap.addMarker(new MarkerOptions().position(latLng));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));

            BigDecimal lat = new BigDecimal(latLng.latitude).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lng = new BigDecimal(latLng.longitude).setScale(4, RoundingMode.HALF_UP);
            LatLng coarseLatLng = new LatLng(lat.doubleValue(), lng.doubleValue());

            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat.doubleValue() + "," + lng.doubleValue() + "&key=AIzaSyBLmMc3Orkr-IpcHanxaZrCcJ_JWpULFc0";
            Log.println(Log.ERROR, "VolleyError", url);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
                try {

                    JSONArray array = response.getJSONArray("results");
                    JSONObject geometry = array.getJSONObject(1);
                    String place_id = geometry.getString("place_id");

                    Intent data = new Intent();
                    data.setData(Uri.parse(place_id));
                    setResult(RESULT_OK, data);

                    finish();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> Log.println(Log.ERROR, "VolleyError", "Volley request failed"));

            queue.add(jsonObjectRequest);


        });


    }
}
