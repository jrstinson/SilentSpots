package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.NotificationManager;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class DetailsActivity extends AppCompatActivity {
 GoogleMap map; // noninteractive, displays single marker
 @Override protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_details);
  // place id passed as extra
  // get place details from Firebase or wherever if present,
  // get the place default info from Google Maps otherwise
  SupportMapFragment fragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
  fragment.getMapAsync(map -> {
   this.map = map;
   // placeholder example, set from current place id
   LatLng nku = new LatLng(39.0323317,-84.4647653);
   map.moveCamera(CameraUpdateFactory.newLatLngZoom(nku, 16));
   MarkerOptions marker = new MarkerOptions();
   marker.position(nku);
   marker.title("NKU");
   marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
   map.addMarker(marker);
   final NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
   if (!manager.isNotificationPolicyAccessGranted()) {
       Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
       startActivity(intent);
   }
   map.setOnMapClickListener((location) -> {
    try {
     startActivityForResult(new PlacePicker.IntentBuilder().build(this), 2);
    } catch(Exception ignored) { }
   });
  });
 }
 public boolean onCreateOptionsMenu(Menu menu) {
  getMenuInflater().inflate(R.menu.menu, menu);
  // set switch title and icon to opposite of parent activity
  return(true);
 }
 public boolean onOptionsItemSelected(MenuItem item) {
  int id = item.getItemId();
  if(id == R.id.add) {
   if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, new String[]{
     Manifest.permission.ACCESS_FINE_LOCATION}, 1);
   } else {
    try {
     startActivityForResult(new PlacePicker.IntentBuilder().build(this), 1);
    } catch (Exception ignored) { }
   }
   return(true);
  } else if(id == R.id.view) {
   // open opposite of parent activity
   return(true);
  } else {
   return(super.onOptionsItemSelected(item));
  }
 }
 protected void onActivityResult(int request, int result, Intent data) {
  if(request == 1 && result == RESULT_OK) {
   Place place = PlacePicker.getPlace(this, data);
   Intent details = new Intent(this, DetailsActivity.class);
   details.putExtra("location", place.getId());
   startActivity(details);
  } else if(request == 2 && result == RESULT_OK) {
   Place place = PlacePicker.getPlace(this, data);
   // update current details activity
  }
 }
}