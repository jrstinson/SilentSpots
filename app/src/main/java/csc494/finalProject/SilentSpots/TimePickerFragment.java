package csc494.finalProject.SilentSpots;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.fragment.app.DialogFragment;

public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {
    FirebaseFirestore firestore;
    FirebaseAuth fireauth;
    String user;
    CollectionReference storage;
    int hour;
    int minute;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        firestore = FirebaseFirestore.getInstance();
        fireauth = FirebaseAuth.getInstance();
        user = fireauth.getCurrentUser().getUid();
        storage = firestore.collection("users").document(user).collection("rules");
        Intent startingIntent = getActivity().getIntent();
        DocumentReference docRef = storage.document(startingIntent.getStringExtra("rule"));
        docRef.get().addOnCompleteListener(doctask -> {
            DocumentSnapshot document = doctask.getResult();
            setHour(Integer.parseInt(document.get("large").toString()));
            setMinute(Integer.parseInt(document.get("small").toString()));
        });
        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));

    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        firestore = FirebaseFirestore.getInstance();
        fireauth = FirebaseAuth.getInstance();
        user = fireauth.getCurrentUser().getUid();
        storage = firestore.collection("users").document(user).collection("rules");
        Intent startingIntent = getActivity().getIntent();
        DocumentReference docRef = storage.document(startingIntent.getStringExtra("rule"));
        docRef.update("large", hourOfDay);
        docRef.update("small", minute);
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }
}
