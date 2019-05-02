package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListActivity extends AppCompatActivity {
    RecyclerView locationsView;
    FirebaseFirestore firebaseFirestore;
    FirebaseAuth firebaseAuth;
    String user;
    CollectionReference storage;
    private GeofencingClient geofencingClient;
    List<Geofence> geofenceList;
    PendingIntent geofencePendingIntent;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceList = new ArrayList<>();
        Activity context = this;
        setContentView(R.layout.activity_list);
        locationsView = findViewById(R.id.locations);

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            user = currentUser.getUid();
            Log.println(Log.WARN, "blarg", user);
            storage = firebaseFirestore.collection("users").document(user).collection("rules");
            storage.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot rules = task.getResult();
                    List<Rule> ruleList = rules.toObjects(Rule.class);
                    List<DocumentSnapshot> list = rules.getDocuments();
                    if (!ruleList.isEmpty()) {
                        for (int i = 0; i < rules.size(); i++) {
                            geofenceList.add(new Geofence.Builder().setRequestId(list.get(i).getId())
                                    .setCircularRegion(ruleList.get(i).coordinates.getLatitude(), ruleList.get(i).coordinates.getLongitude(), (float) ruleList.get(i).radius)
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                    .build());
                            Log.println(Log.WARN, "test", geofenceList.get(0).getRequestId());
                        }

                        try {
                            geofencingClient.addGeofences(geofencingRequest(), getGeofencePendingIntent()).addOnSuccessListener(context, aVoid -> {
                            }).addOnFailureListener(context, e -> Log.println(Log.WARN, "ListActivity", e.getLocalizedMessage()));
                        } catch (SecurityException e) {
                            Log.println(Log.WARN, "ListActivity", e.getLocalizedMessage());
                        }
                    }

                }
            });

            storageAdapter();
        } else {
            firebaseAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user = task.getResult().getUser().getUid();
                    Log.println(Log.INFO, "ListActivity: User=", user);
                    storage = firebaseFirestore.collection("users").document(user).collection("rules");
                    storage.get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            QuerySnapshot rules = task1.getResult();
                            List<Rule> ruleList = rules.toObjects(Rule.class);
                            List<DocumentSnapshot> list = rules.getDocuments();
                            if (!ruleList.isEmpty()) {
                                for (int i = 0; i < rules.size(); i++) {
                                    geofenceList.add(new Geofence.Builder().setRequestId(list.get(i).getId())
                                            .setCircularRegion(ruleList.get(i).coordinates.getLatitude(), ruleList.get(i).coordinates.getLongitude(), (float) ruleList.get(i).radius)
                                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                            .build());
                                    Log.println(Log.WARN, "test", geofenceList.get(0).getRequestId());
                                }

                                try {
                                    geofencingClient.addGeofences(geofencingRequest(), getGeofencePendingIntent()).addOnSuccessListener(context, aVoid -> {
                                    }).addOnFailureListener(context, e -> Log.println(Log.WARN, "ListActivity", e.getLocalizedMessage()));
                                } catch (SecurityException e) {
                                    Log.println(Log.WARN, "ListActivity", e.getLocalizedMessage());
                                }
                            }

                        }
                    });
                    storageAdapter();
                }
            });
        }
        locationsView.setLayoutManager(new LinearLayoutManager(this));

        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (!manager.isNotificationPolicyAccessGranted()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please allow Silent Spots to manage your Do Not Disturb Settings");
            builder.setPositiveButton("Okay", (dialog, which) -> {
                startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                dialog.dismiss();
            });
            builder.create().show();
        }
    }

    private GeofencingRequest geofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        geofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    void storageAdapter() {
        Intent details = new Intent(this, DetailsActivity.class);

        storage = firebaseFirestore.collection("users").document(user).collection("rules");
        Query query = storage.orderBy("title");
        FirestoreRecyclerOptions<Rule> options = new FirestoreRecyclerOptions.Builder<Rule>()
                .setQuery(query, Rule.class).build();

        FirestoreRecyclerAdapter adapter = new FirestoreRecyclerAdapter<Rule, Holder>(options) {
            @Override
            public void onBindViewHolder(Holder holder, int position, Rule rule) {
                String text = rule.title + "\n" + rule.setting;
                holder.itemtext.setText(text);
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
        locationsView.setAdapter(adapter);
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
                    startActivityForResult(new Intent(this, AddLocation.class),1 );
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
            Places.initialize(getApplicationContext(), "AIzaSyCdcnMssDUkAhRDPYtuYToZVsaFv84N7Ag");

            PlacesClient placesClient = Places.createClient(this);
            String placeID = data.getDataString();
            List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);

            FetchPlaceRequest requestPlace = FetchPlaceRequest.builder(placeID, placeFields).build();

            placesClient.fetchPlace(requestPlace).addOnSuccessListener((response) ->{
                Place place = response.getPlace();

                Log.println(Log.WARN, "addressTest", place.getAddress());
                Log.println(Log.WARN, "latLangTest", place.getLatLng().toString());
                Intent details = new Intent(this, DetailsActivity.class);

                //Dialogue box for Radius and Title
                AlertDialog.Builder radius = new AlertDialog.Builder(ListActivity.this);
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

class Holder extends RecyclerView.ViewHolder {
    TextView itemtext;

    Holder(View itemview) {
        super(itemview);
        itemtext = itemview.findViewById(R.id.item);
    }
}
