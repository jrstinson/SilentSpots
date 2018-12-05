package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity {
    GoogleMap map;
    ArrayList<Place> locations = new ArrayList<>();
    FirebaseFirestore firestore;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        firestore = FirebaseFirestore.getInstance();
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(map -> {
            this.map = map;
            LatLng nku = new LatLng(39.0323317, -84.4647653);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(nku, 16));
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
            MarkerOptions marker = new MarkerOptions();
            marker.position(nku);
            marker.title("NKU");
            marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            map.addMarker(marker);


        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.getItem(1).setTitle("List");
        menu.getItem(1).setIcon(R.drawable.list);
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
            details.putExtra("location", place.getId());

            //Dialogue box for Radius and Title
            AlertDialog.Builder radius = new AlertDialog.Builder(MapActivity.this);
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
        }
    }
}