package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.NotificationManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    GoogleMap map;
    FirebaseFirestore firestore;
    FirebaseAuth fireauth;
    String user;
    CollectionReference storage;

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
            storage.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        QuerySnapshot rules = task.getResult();
                        List<Rule> ruleList = rules.toObjects(Rule.class);
                        List<LatLng> latLngs = new ArrayList<>();
                        if (!ruleList.isEmpty()) {
                            for (Rule rule : ruleList) {
                                latLngs.add(new LatLng(rule.coordinates.getLatitude(), rule.coordinates.getLongitude()));
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 16));
                                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    map.setMyLocationEnabled(true);
                                }
                                map.addCircle(new CircleOptions().center(new LatLng(rule.coordinates.getLatitude(), rule.coordinates.getLongitude()))
                                        .radius(rule.radius)
                                        .fillColor(0x220000FF)
                                        .strokeColor(Color.BLACK)
                                        .strokeWidth(1)).setClickable(true);
                                
                            }
                        }
                    }
                }
            });


        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.getItem(1).setTitle("List");
        menu.removeItem(R.id.view);
        return (true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
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
            startActivity(new Intent(this, ListActivity.class));
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
   AlertDialog.Builder radius = new AlertDialog.Builder(MapActivity.this);
   radius.setMessage("Set Title and Radius in meters")
    .setTitle("Input");
   LinearLayout layout = new LinearLayout(this);
   layout.setOrientation(LinearLayout.VERTICAL);
   final EditText input = new EditText(this);
   input.setHint("Radius (In Meters)");
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
        }
    }
}