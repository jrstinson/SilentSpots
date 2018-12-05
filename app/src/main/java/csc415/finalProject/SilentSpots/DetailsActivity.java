package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.app.NotificationManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class DetailsActivity extends AppCompatActivity {
    GoogleMap map; // noninteractive, displays single marker
    TextView titleView;
    TextView addressView;
    TextView radiusView;
    FirebaseFirestore firestore;
    RadioGroup rg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        firestore = FirebaseFirestore.getInstance();

        // place id passed as extra
        // get place details from Firebase or wherever if present,
        // get the place default info from Google Maps otherwise
        rg = findViewById(R.id.radioGroup);
        firestore = FirebaseFirestore.getInstance();
        Intent startingIntent = getIntent();
        DocumentReference docRef = firestore.collection("rules").document(startingIntent.getStringExtra("location"));
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    switch((String) document.get("setting")){
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
                    titleView.append(" "+document.get("title"));
                    addressView.append(" "+document.get("address"));
                    radiusView.append(" "+document.get("radius"));
                }
            }
        });
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = group.findViewById(checkedId);
                if (null != radioButton) {
                    firestore.runTransaction(new Transaction.Function<Void>() {
                        @Override
                        public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                            transaction.update(docRef, "setting", radioButton.getTag());
                            return null;
                        }
                    });
                }
            }
        });
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(map -> {
            this.map = map;
            // placeholder example, set from current place id
            titleView = findViewById(R.id.title);
            addressView = findViewById(R.id.address);
            radiusView = findViewById(R.id.radius);

            String id = startingIntent.getStringExtra("location");
            GeoDataClient mGeoDataClient = Places.getGeoDataClient(this);
            mGeoDataClient.getPlaceById(id).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                    if (task.isSuccessful()) {
                        PlaceBufferResponse places = task.getResult();
                        Place myPlace = places.get(0);
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace.getLatLng(), 16));
                        MarkerOptions marker = new MarkerOptions();
                        marker.position(myPlace.getLatLng());
                        marker.title(myPlace.getName().toString());
                        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        map.addMarker(marker);

                    } else {
                        Log.println(Log.WARN, "test", "ID not found");
                    }

                }
            });

            map.setOnMapClickListener((location) -> {
                try {
                    startActivityForResult(new PlacePicker.IntentBuilder().build(this), 2);
                } catch (Exception ignored) {
                }
            });
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        // set switch title and icon to opposite of parent activity
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
            // open opposite of parent activity
            return (true);
        } else {
            return (super.onOptionsItemSelected(item));
        }
    }

    protected void onActivityResult(int request, int result, Intent data) {
        if (request == 1 && result == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            Intent details = new Intent(this, DetailsActivity.class);
            details.putExtra("location", place.getId());

            //Dialogue box for Radius and Title
            AlertDialog.Builder radius = new AlertDialog.Builder(DetailsActivity.this);
            radius.setMessage("Set Radius and Title")
                    .setTitle("Input");
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            final EditText input = new EditText(this);
            input.setHint("Radius");
            final EditText input2 = new EditText(this);
            input2.setHint("Title");
            layout.addView(input);
            layout.addView(input2);
            radius.setView(layout);

            radius.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    double value = Double.valueOf(input.getText().toString());
                    String value2 = input2.getText().toString();
                    Rule rule = new Rule();
                    rule.place = place.getId();
                    rule.title = value2;
                    rule.address = (String)place.getAddress();
                    rule.radius = value;
                    rule.setting = "None";
                    rule.coordinates = new GeoPoint(place.getLatLng().latitude, place.getLatLng().longitude);
                    firestore.collection("rules").document(rule.place).set(rule);
                    startActivity(details);
                }
            });

            radius.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });
            radius.show();
        } else if (request == 2 && result == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            // update current details activity
        }
    }
}