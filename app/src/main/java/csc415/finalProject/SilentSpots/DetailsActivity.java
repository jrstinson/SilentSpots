package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class DetailsActivity extends AppCompatActivity {
    GoogleMap map; // non-interactive, displays single marker
    TextView titleView;
    TextView addressView;
    TextView radiusView;
    Button timerPicker;
    Button alarmPicker;
    RadioGroup rg;
    RadioGroup rg2;
    double radius;
    FirebaseFirestore firestore;
    FirebaseAuth fireauth;
    String user;
    CollectionReference storage;
    DialogFragment newFragment;
    NumberPickerFragment timerFragment;
    private static final int PLACE_PICKER_ACCESS_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        com.google.android.libraries.places.api.Places.initialize(getApplicationContext(), "AIzaSyBLmMc3Orkr-IpcHanxaZrCcJ_JWpULFc0");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        titleView = findViewById(R.id.title);
        addressView = findViewById(R.id.address);
        radiusView = findViewById(R.id.radius);
        rg = findViewById(R.id.radioGroup);
        rg2 = findViewById(R.id.radioGroup2);
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
                case "Media":
                    rg.check(R.id.radioMedia);
                    break;
            }
            switch (document.get("clock").toString()) {
                case "None":
                    rg2.check(R.id.radio_none2);
                    break;
                case "Alarm":
                    rg2.check(R.id.radioAlarm);
                    setAlarmVisibility();
                    newFragment = new TimePickerFragment();
                    newFragment.show(getSupportFragmentManager(), "timePicker");
                    break;
                case "Timer":
                    rg2.check(R.id.radioTimer);
                    newFragment = new TimePickerFragment();
                    newFragment.show(getSupportFragmentManager(), "timePicker");
                    setTimerVisibility();
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

            rg2.setOnCheckedChangeListener((group, checkedId) -> {
                RadioButton radioButton = group.findViewById(checkedId);
                if (null != radioButton) {
                    docRef.update("clock", radioButton.getTag());
                    if (checkedId==R.id.radioAlarm) {
                        setAlarmVisibility();
                        newFragment = new TimePickerFragment();
                        newFragment.show(getSupportFragmentManager(), "timePicker");
                    }
                    else if (checkedId==R.id.radioTimer){
                        setTimerVisibility();
                        newFragment = new TimePickerFragment();
                        newFragment.show(getSupportFragmentManager(), "timePicker");
                    }
                    else {
                        setNoVisibility();
                        docRef.update("large", 0);
                        docRef.update("small",0);
                    }
                }
            });

            fragment.getMapAsync(map -> {
                this.map = map;
                String place = document.get("place").toString();
                List<com.google.android.libraries.places.api.model.Place.Field> placeFields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.NAME, com.google.android.libraries.places.api.model.Place.Field.LAT_LNG, com.google.android.libraries.places.api.model.Place.Field.ADDRESS);
                com.google.android.libraries.places.api.Places.initialize(getApplicationContext(), "AIzaSyBLmMc3Orkr-IpcHanxaZrCcJ_JWpULFc0");

                PlacesClient placesClient = com.google.android.libraries.places.api.Places.createClient(this);
                FetchPlaceRequest requestPlace = FetchPlaceRequest.builder(place, placeFields).build();

                placesClient.fetchPlace(requestPlace).addOnSuccessListener((response) ->{
                    com.google.android.libraries.places.api.model.Place myPlace = response.getPlace();
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
                    startActivityForResult(new Intent(this, AddLocation.class),1 );
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
            com.google.android.libraries.places.api.Places.initialize(getApplicationContext(), "AIzaSyBLmMc3Orkr-IpcHanxaZrCcJ_JWpULFc0");

            PlacesClient placesClient = com.google.android.libraries.places.api.Places.createClient(this);
            String placeID = data.getDataString();
            List<com.google.android.libraries.places.api.model.Place.Field> placeFields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.NAME, com.google.android.libraries.places.api.model.Place.Field.LAT_LNG, com.google.android.libraries.places.api.model.Place.Field.ADDRESS);

            FetchPlaceRequest requestPlace = FetchPlaceRequest.builder(placeID, placeFields).build();

            placesClient.fetchPlace(requestPlace).addOnSuccessListener((response) ->{
                com.google.android.libraries.places.api.model.Place place = response.getPlace();
                Log.println(Log.WARN, "addressTest", place.getAddress());
                Log.println(Log.WARN, "latLangTest", place.getLatLng().toString());
                Intent details = new Intent(this, DetailsActivity.class);

                //Dialogue box for Radius and Title
                AlertDialog.Builder radius = new AlertDialog.Builder(DetailsActivity.this);
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
                    rule.clock = "None";
                    rule.large = 0;
                    rule.small = 0;
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
    private void setAlarmVisibility(){
        timerPicker = findViewById(R.id.timerPicker);
        timerPicker.setVisibility(View.GONE);
        alarmPicker = findViewById(R.id.alarmTimePicker);
        alarmPicker.setVisibility(View.VISIBLE);
    }

    private void setTimerVisibility() {
        alarmPicker = findViewById(R.id.alarmTimePicker);
        alarmPicker.setVisibility(View.GONE);
        timerPicker = findViewById(R.id.timerPicker);
        timerPicker.setVisibility(View.VISIBLE);
    }

    private void setNoVisibility() {
        alarmPicker = findViewById(R.id.alarmTimePicker);
        alarmPicker.setVisibility(View.GONE);
        timerPicker = findViewById(R.id.timerPicker);
        timerPicker.setVisibility(View.GONE);
    }
    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }
    public void showNumberPickerDialog(View v) {
        NumberPickerFragment newFragment = new NumberPickerFragment();
        newFragment.show();
    }
}
