package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.NotificationManager;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AddPlaceRequest;
import com.google.android.gms.location.places.GeoDataApi;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.Distribution;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ListActivity extends AppCompatActivity {
 RecyclerView locationsview;
 FirebaseFirestore firestore;
 FirebaseAuth fireauth;
 String user;
 CollectionReference storage;
 private GeofencingClient geofencingClient;
 List<Geofence> geofenceList;
 PendingIntent geofencePendingIntent;

 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);

  geofencingClient = LocationServices.getGeofencingClient(this);
  geofenceList = new ArrayList<>();

  setContentView(R.layout.activity_list);
  locationsview = findViewById(R.id.locations);

  firestore = FirebaseFirestore.getInstance();
  fireauth = FirebaseAuth.getInstance();

  FirebaseUser currentuser = fireauth.getCurrentUser();
  if(currentuser != null) {
   user = currentuser.getUid();
   storage = firestore.collection("users").document(user).collection("rules");
   storageadapter();
  } else {
   fireauth.signInAnonymously().addOnCompleteListener(task -> {
    user = task.getResult().getUser().getUid();
    storage = firestore.collection("users").document(user).collection("rules");
    storageadapter();
   });
  }

  locationsview.setLayoutManager(new LinearLayoutManager(this));
  NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
  if (manager.isNotificationPolicyAccessGranted() == false) {
   startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
  }


  storage.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
      @Override
      public void onComplete(@NonNull Task<QuerySnapshot> task) {
          if(task.isSuccessful()){
              QuerySnapshot rules = task.getResult();
              List<Rule> ruleList = rules.toObjects(Rule.class);

              for(Rule rule: ruleList){

                  geofenceList.add(new Geofence.Builder().setRequestId(rule.place)
                  .setCircularRegion(rule.coordinates.getLatitude(),rule.coordinates.getLongitude(), (float) rule.radius)
                          .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                          .setExpirationDuration(Geofence.NEVER_EXPIRE)
                  .build());
              }

          }
      }
  });
  try {
      if(!geofenceList.isEmpty()) {
          geofencingClient.addGeofences(geofencingRequest(), getGeofencePendingIntent()).addOnSuccessListener(this, new OnSuccessListener<Void>() {
              @Override
              public void onSuccess(Void aVoid) {

              }
          }).addOnFailureListener(this, new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {

              }
          });
      }
  }
  catch (SecurityException e){
      Log.println(Log.WARN, "test", "failure");
  }


 }
 private GeofencingRequest geofencingRequest(){
     GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
     builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

     builder.addGeofences(geofenceList);
     return builder.build();
 }
 private PendingIntent getGeofencePendingIntent() {
     if(geofencePendingIntent != null){
         return geofencePendingIntent;
     }
     Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
     geofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
     return geofencePendingIntent;
 }

 void storageadapter() {
  Intent details = new Intent(this, DetailsActivity.class);

  storage = firestore.collection("users").document(user).collection("rules");
  Query query = storage.orderBy("title");
  FirestoreRecyclerOptions<Rule> options = new FirestoreRecyclerOptions.Builder<Rule>()
   .setQuery(query, Rule.class).build();

  FirestoreRecyclerAdapter adapter = new FirestoreRecyclerAdapter<Rule, Holder>(options) {
   @Override public void onBindViewHolder(Holder holder, int position, Rule rule) {
    holder.itemtext.setText(rule.title);
    holder.itemtext.setOnClickListener(view -> {
     String id = getSnapshots().getSnapshot(holder.getAdapterPosition()).getId();
     details.putExtra("rule", id);
     startActivity(details);
    });
    holder.itemtext.setOnLongClickListener(view -> {
     AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemtext.getContext());
     builder.setTitle("Remove Location Rule");
     builder.setMessage("Are you sure?");
     builder.setPositiveButton("REMOVE", (dialog, which) -> {
      String id = getSnapshots().getSnapshot(holder.getAdapterPosition()).getId();
      storage.document(id).delete();
      dialog.dismiss();
     });
     builder.setNegativeButton("CANCEL", (dialog, which) -> {
      dialog.dismiss();
     });
     builder.create().show();
     return (true);
    });
   }

   @Override
   public Holder onCreateViewHolder(ViewGroup group, int type) {
    return (new Holder(LayoutInflater.from(group.getContext()).inflate(R.layout.item, group, false)));
   }
  };
  adapter.startListening();
  locationsview.setAdapter(adapter);
 }

 public boolean onCreateOptionsMenu(Menu menu) {
  getMenuInflater().inflate(R.menu.menu, menu);
  menu.getItem(1).setTitle("Map");
  menu.getItem(1).setIcon(R.drawable.map);
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
   startActivity(new Intent(this, MapActivity.class));
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
   AlertDialog.Builder radius = new AlertDialog.Builder(ListActivity.this);
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
  }
 }
}

class Holder extends RecyclerView.ViewHolder {
 TextView itemtext;

 Holder(View itemview) {
  super(itemview);
  itemtext = itemview.findViewById(R.id.item);
 }
}