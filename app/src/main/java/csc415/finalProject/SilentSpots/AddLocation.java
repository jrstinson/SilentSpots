package csc415.finalProject.SilentSpots;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;

public class AddLocation extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_location);

        Places.initialize(getApplicationContext(), "AIzaSyBLmMc3Orkr-IpcHanxaZrCcJ_JWpULFc0");

        PlacesClient placesClient = Places.createClient(this);

        AutocompleteSupportFragment autocompleteFrag = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFrag.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        autocompleteFrag.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Intent data = new Intent();
                data.setData(Uri.parse(place.getId()));
                setResult(RESULT_OK, data);

                finish();
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
