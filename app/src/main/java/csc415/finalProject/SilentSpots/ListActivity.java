package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.NotificationManager;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;

public class ListActivity extends AppCompatActivity {
 RecyclerView locationsview;
 ArrayList<Place> locations = new ArrayList<>();
 // Place.getName, getAddress, getId, getLatLng, getPlaceTypes, getViewport
 // NKU, HH KY USA, id?, { latitude: 100, longitude: -100 }, [ Place.TYPE_UNIVERSITY ], LatLangBounds
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_list);
  locationsview = findViewById(R.id.locations);
  locationsview.setAdapter(new itemadapter(this, locations));
  locationsview.setLayoutManager(new LinearLayoutManager(this));

 }
 public boolean onCreateOptionsMenu(Menu menu) {
  getMenuInflater().inflate(R.menu.menu, menu);
  menu.getItem(1).setTitle("Map");
  menu.getItem(1).setIcon(R.drawable.map);
  return(true);
 }
 public boolean onOptionsItemSelected(MenuItem item) {
  int id = item.getItemId();
  if(id == R.id.add) {
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
  } else if(id == R.id.view) {
   startActivity(new Intent(this, MapActivity.class));
   return(true);
  } else {
   return(super.onOptionsItemSelected(item));
  }
 }
 protected void onActivityResult(int request, int result, Intent data) {
  if(request == 1 && result == RESULT_OK) {
   Place place = PlacePicker.getPlace(this, data);
   locations.add(place);
   locationsview.getAdapter().notifyItemInserted(locations.size());
   Intent details = new Intent(this, DetailsActivity.class);
   details.putExtra("location", place.getId());
   startActivity(details);
  }
 }
}
class itemadapter extends RecyclerView.Adapter<itemadapter.holder> {
 ArrayList<Place> locations;
 LayoutInflater inflater;
 public itemadapter(Context context, ArrayList<Place> locations) {
  inflater = LayoutInflater.from(context);
  this.locations = locations;
 }
 @NonNull public itemadapter.holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
  return(new holder(inflater.inflate(R.layout.item, parent, false), this));
 }
 public void onBindViewHolder(@NonNull itemadapter.holder holder, int position) {
  holder.itemtext.setText(locations.get(position).getName());
 }
 public int getItemCount() {
  return(locations.size());
 }
 class holder extends RecyclerView.ViewHolder {
  TextView itemtext;
  itemadapter adapter;
  holder(View itemview, itemadapter adapter) {
   super(itemview);
   itemtext = itemview.findViewById(R.id.item);
   this.adapter = adapter;
   itemview.setOnClickListener(view -> {
    int index = getLayoutPosition();
    // start details activity for locations[index]
   });
  }
 }
}