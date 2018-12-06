package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.Circle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;


public class DetailsActivity extends AppCompatActivity {
    GoogleMap map; // noninteractive, displays single marker
    TextView titleView;
    TextView addressView;
    TextView radiusView;
    RadioGroup rg;
    double radius;
    FirebaseFirestore firestore;
    FirebaseAuth fireauth;
    String user;
    CollectionReference storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        titleView = findViewById(R.id.title);
        addressView = findViewById(R.id.address);
        radiusView = findViewById(R.id.radius);
        rg = findViewById(R.id.radioGroup);
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Intent startingIntent = getIntent();

        firestore = FirebaseFirestore.getInstance();
        fireauth = FirebaseAuth.getInstance();
        user = fireauth.getCurrentUser().getUid();
        storage = firestore.collection("users").document(user).collection("rules");

        DocumentReference docRef = storage.document(startingIntent.getStringExtra("rule"));
        docRef.get().addOnCompleteListener(doctask -> {
            DocumentSnapshot document = doctask.getResult();
            switch (document.get("setting").toString()) {
                case "None":
                    rg.check(R.id.radio_none);
                    break;
                case "Full":
                    rg.check(R.id.radioFull);
                    break;
                case "Starred":
                    rg.check(R.id.radioStarred);
                    break;
                case "Messages":
                    rg.check(R.id.radioMessage);
                    break;
                case "Alarms":
                    rg.check(R.id.radioAlarms);
                    break;
            }
            titleView.append(" " + document.get("title"));
            addressView.append(" " + document.get("address"));
            radiusView.append(" " + document.get("radius"));
            radius = (double) document.get("radius");

            rg.setOnCheckedChangeListener((group, checkedId) -> {
                RadioButton radioButton = group.findViewById(checkedId);
                if (null != radioButton) {
                    docRef.update("setting", radioButton.getTag());
                }
            });

            fragment.getMapAsync(map -> {
                this.map = map;
                String place = document.get("place").toString();
                GeoDataClient geodata = Places.getGeoDataClient(this);
                geodata.getPlaceById(place).addOnCompleteListener(placetask -> {
                    PlaceBufferResponse places = placetask.getResult();
                    Place myPlace = places.get(0);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace.getLatLng(), 16));

                    map.addCircle(new CircleOptions()
                            .center(myPlace.getLatLng())
                            .radius((double) document.get("radius"))
                            .strokeColor(Color.BLACK)
                            .fillColor(0x220000FF)
                            .strokeWidth(1)

                    );

                });

                map.setOnMapClickListener((location) -> {
                    try {
                        startActivityForResult(new PlacePicker.IntentBuilder().build(this), 2);
                    } catch (Exception ignored) {
                    }
                });
            });

        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        // todo set switch title and icon to opposite of parent activity
        return (true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                try {
                    startActivityForResult(new PlacePicker.IntentBuilder().build(this), 1);
                } catch (Exception ignored) {
                }
            }
            return (true);
        } else if (id == R.id.view) {
            // todo open opposite of parent activity
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
            return (true);
        } else {
            return (super.onOptionsItemSelected(item));
        }
    }

    protected void onActivityResult(int request, int result, Intent data) {
        if (request == 1 && result == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            Intent details = new Intent(this, DetailsActivity.class);

            //Dialogue box for Radius and Title
            AlertDialog.Builder radius = new AlertDialog.Builder(DetailsActivity.this);
            radius.setMessage("Set Title and Radius")
                    .setTitle("Input");
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            final EditText input = new EditText(this);
            input.setHint("Radius");
            final EditText input2 = new EditText(this);
            input2.setHint("Title");
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
        } else if (request == 2 && result == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            // todo update current details activity
        }
    }
}