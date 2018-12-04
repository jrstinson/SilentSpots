package csc415.finalProject.SilentSpots;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.NotificationManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AddPlaceRequest;
import com.google.android.gms.location.places.GeoDataApi;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

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
import java.util.concurrent.TimeUnit;

public class ListActivity extends AppCompatActivity {
    public static String fileName = "userLocationIDs.txt";
    RecyclerView locationsview;
    ArrayList<Place> locations = new ArrayList<>();

    // Place.getName, getAddress, getId, getLatLng, getPlaceTypes, getViewport
    // NKU, HH KY USA, id?, { latitude: 100, longitude: -100 }, [ Place.TYPE_UNIVERSITY ], LatLangBounds
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //This is how you must declare every notification manager
        final NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        //this doesn't have to be here, but it MUST be called before trying to use the manager
        if (!manager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
        setContentView(R.layout.activity_list);
        ArrayList<String> currentLocations = readFile(this);
        itemadapter m = new itemadapter(this, locations);
        //TODO Populate the list of locations. Requires findPlaceByID, which is a huge pain in the ass but I'll figure it out
        GeoDataClient mGeoDataClient = Places.getGeoDataClient(this);
        for (int i = 0; i < currentLocations.size(); i++) {
            mGeoDataClient.getPlaceById(currentLocations.get(i)).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                    if (task.isSuccessful()) {
                        PlaceBufferResponse places = task.getResult();
                        Place myPlace = places.get(0);
                        locations.add(myPlace);
                        Log.println(Log.WARN, "test", myPlace.getId());
                        m.notifyDataSetChanged();
                    } else {
                        Log.println(Log.WARN, "test", "ID not found");
                    }

                }
            });
        }
        Log.println(Log.WARN, "test", new Integer(locations.size()).toString());
        locationsview = findViewById(R.id.locations);
        locationsview.setAdapter(m);
        locationsview.setLayoutManager(new LinearLayoutManager(this));

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

    public void writeToFile(Place place, Context ctx) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileName, Context.MODE_APPEND);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            bw.append(place.getId());
            bw.newLine();
            bw.flush();
            fileOutputStream.flush();
            bw.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static ArrayList<String> readFile(Context ctx) {
        String path = ctx.getFilesDir().getAbsolutePath() + "/";
        ArrayList<String> lines = new ArrayList<>();
        String line;
        try {
            File file = new File(path + fileName);
            if (file.exists()) {
                FileInputStream is = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while ((line = reader.readLine()) != null) lines.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lines;
    }

    protected void onActivityResult(int request, int result, Intent data) {
        if (request == 1 && result == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            locations.add(place);
            writeToFile(place, this);
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

    @NonNull
    public itemadapter.holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return (new holder(inflater.inflate(R.layout.item, parent, false), this));
    }

    public void onBindViewHolder(@NonNull itemadapter.holder holder, int position) {
        holder.itemtext.setText(locations.get(position).getName());
    }

    public int getItemCount() {
        return (locations.size());
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