package csc494.finalProject.SilentSpots;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class DetailsActivity extends AppCompatActivity {
    GoogleMap map; // non-interactive, displays single marker
    TextView titleView;
    TextView addressView;
    TextView radiusView;
    NumberPicker timerPicker;
    TimePicker alarmPicker;
    RadioGroup rg;
    RadioGroup rg2;
    double radius;
    FirebaseFirestore firestore;
    FirebaseAuth fireauth;
    String user;
    CollectionReference storage;
    private static final int PLACE_PICKER_ACCESS_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
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
            }
            switch (document.get("clock").toString()) {
                case "None":
                    rg2.check(R.id.radio_none2);
                    break;
                case "Alarm":
                    rg2.check(R.id.radioAlarm);
                    alarmPicker = findViewById(R.id.alarmTimePicker);
                    alarmPicker.setHour(Integer.parseInt(document.get("large").toString()));
                    alarmPicker.setMinute(Integer.parseInt(document.get("small").toString()));
                    setAlarmVisibility();
                    break;
                case "Timer":
                    rg2.check(R.id.radioTimer);
                    timerPicker = findViewById(R.id.timerPicker);
                    timerPicker.setValue(Integer.parseInt(document.get("small").toString()));
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
                        alarmPicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
                            docRef.update("large", hourOfDay);
                            docRef.update("small", minute);
                        });
                    }
                    else if (checkedId==R.id.radioTimer){
                        setTimerVisibility();
                        timerPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                            docRef.update("small", newVal);
                        });
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
            showPlacePicker();
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
            Place place = PlacePicker.getPlace(this, data);
            Intent details = new Intent(this, DetailsActivity.class);

            //Dialogue box for Radius and Title
            AlertDialog.Builder radius = new AlertDialog.Builder(DetailsActivity.this);
            radius.setMessage("Set Title and Radius in meters")
                    .setTitle("Input");
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            final EditText input = new EditText(this);
            input.setHint("Radius");
            input.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
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
                rule.clock="None";
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
        } else if (request == 2 && result == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            // todo update current details activity
        }
    }
    private void setAlarmVisibility(){
        timerPicker = findViewById(R.id.timerPicker);
        timerPicker.setVisibility(View.GONE);
        alarmPicker = findViewById(R.id.alarmTimePicker);
        alarmPicker.setIs24HourView(true);
        alarmPicker.setVisibility(View.VISIBLE);
    }

    private void setTimerVisibility() {
        alarmPicker = findViewById(R.id.alarmTimePicker);
        alarmPicker.setVisibility(View.GONE);
        timerPicker = findViewById(R.id.timerPicker);
        timerPicker.setVisibility(View.VISIBLE);
        timerPicker.setMinValue(1);
        timerPicker.setMaxValue(600);
    }

    private void setNoVisibility() {
        alarmPicker = findViewById(R.id.alarmTimePicker);
        alarmPicker.setVisibility(View.GONE);
        timerPicker = findViewById(R.id.timerPicker);
        timerPicker.setVisibility(View.GONE);
    }
}