package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    GoogleMap map;
    FirebaseFirestore firestore;
    FirebaseAuth fireauth;
    String user;
    CollectionReference storage;
    private static final int PLACE_PICKER_ACCESS_CODE = 102;
    private static final int MAP_LOCATION_ENABLED_CODE = 103;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firestore = FirebaseFirestore.getInstance();
        fireauth = FirebaseAuth.getInstance();
        user = fireauth.getCurrentUser().getUid();
        storage = firestore.collection("users").document(user).collection("rules");


        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(map -> {
            Context ctx = this;
            this.map = map;
            storage.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot rules = task.getResult();
                    List<Rule> ruleList = rules.toObjects(Rule.class);
                    List<LatLng> latLngs = new ArrayList<>();
                    if (!ruleList.isEmpty()) {
                        for (Rule rule : ruleList) {
                            latLngs.add(new LatLng(rule.coordinates.getLatitude(), rule.coordinates.getLongitude()));
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 16));
                            enableMapLocation();
                            map.addCircle(new CircleOptions().center(new LatLng(rule.coordinates.getLatitude(), rule.coordinates.getLongitude()))
                                    .radius(rule.radius)
                                    .fillColor(0x220000FF)
                                    .strokeColor(Color.BLACK)
                                    .strokeWidth(1)).setClickable(true);

                        }
                    }
                }
            });
        });
    }

    @AfterPermissionGranted(MAP_LOCATION_ENABLED_CODE)
    private void enableMapLocation() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

        if (EasyPermissions.hasPermissions(this, permissions)) {
            try {
                map.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
                Log.println(Log.WARN, "MapActivity", ignored.getLocalizedMessage());
            }
        } else {
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(this, MAP_LOCATION_ENABLED_CODE, permissions)
                            .build()
            );
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.getItem(1).setTitle("List");
        menu.removeItem(R.id.view);
        return (true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.navigateUpTo(this.getParentActivityIntent());
            return true;
        }
        if (id == R.id.add) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                try {
                    startActivityForResult(new Intent(this, AddLocation.class),1 );
                } catch (Exception ignored) {
                }
            }
            return (true);
        } else if (id == R.id.view) {
            startActivity(new Intent(this, ListActivity.class));
            return (true);
        } else {
            return (super.onOptionsItemSelected(item));
        }
    }

    @AfterPermissionGranted(PLACE_PICKER_ACCESS_CODE)
    private void showPlacePicker() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

        if (EasyPermissions.hasPermissions(this, permissions)) {
            try {
                startActivityForResult(new PlacePicker.IntentBuilder().build(this), 1);
            } catch (Exception ignored) {
                Log.println(Log.WARN, "MapActivity", ignored.getLocalizedMessage());
            }
        } else {
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(this, PLACE_PICKER_ACCESS_CODE, permissions)
                            .build()
            );
        }
    }

    protected void onActivityResult(int request, int result, Intent data) {
        if (request == 1 && result == RESULT_OK) {
            Places.initialize(getApplicationContext(), "AIzaSyCdcnMssDUkAhRDPYtuYToZVsaFv84N7Ag");

            PlacesClient placesClient = Places.createClient(this);
            String placeID = data.getDataString();
            List<com.google.android.libraries.places.api.model.Place.Field> placeFields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.NAME, com.google.android.libraries.places.api.model.Place.Field.LAT_LNG, com.google.android.libraries.places.api.model.Place.Field.ADDRESS);

            FetchPlaceRequest requestPlace = FetchPlaceRequest.builder(placeID, placeFields).build();

            placesClient.fetchPlace(requestPlace).addOnSuccessListener((response) ->{
                com.google.android.libraries.places.api.model.Place place = response.getPlace();
                Log.println(Log.WARN, "addressTest", place.getAddress());
                Log.println(Log.WARN, "latLangTest", place.getLatLng().toString());
                Intent details = new Intent(this, DetailsActivity.class);

                //Dialogue box for Radius and Title
                AlertDialog.Builder radius = new AlertDialog.Builder(MapActivity.this);
                radius.setMessage("Set nickname and radius (in meters) for "+place.getAddress())
                        .setTitle("Add Location");
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText input = new EditText(this);
                input.setHint("Radius");
                final EditText input2 = new EditText(this);
                input2.setHint("Nickname");
                layout.addView(input2);
                layout.addView(input);
                radius.setView(layout);

                radius.setPositiveButton("Ok", (dialog, whichButton) -> {
                    double value = Double.valueOf(input.getText().toString());
                    String value2 = input2.getText().toString();
                    Rule rule = new Rule();
                    rule.place = place.getId();
                    rule.title = value2;
                    rule.address = (String) place.getAddress();
                    rule.radius = value;
                    rule.setting = "None";
                    Log.println(Log.WARN, "ruleTest", rule.address);
                    rule.coordinates = new GeoPoint(place.getLatLng().latitude, place.getLatLng().longitude);
                    storage.add(rule).addOnCompleteListener(task -> {
                        details.putExtra("rule", task.getResult().getId());
                        startActivity(details);
                    });
                });

                radius.setNegativeButton("Cancel", (dialog, whichButton) -> {
                    // Canceled.
                });
                radius.show();
            }).addOnFailureListener((exception) ->{
				
            });
        }
    }
}