package csc415.finalProject.SilentSpots;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.fragment.app.Fragment;

public class NumberPickerFragment extends Fragment {
    private FirebaseFirestore firestore;
    private FirebaseAuth fireauth;
    private String user;
    private CollectionReference storage;
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.numberpicker_layout, container,
                false);
        return rootView;
    }
    public void show() {
        final Dialog npDialog = new Dialog(getActivity());
        npDialog.setTitle("Set Timer");
        npDialog.setContentView(R.layout.numberpicker_layout);
        Button setBtn = npDialog.findViewById(R.id.setBtn);
        firestore = FirebaseFirestore.getInstance();
        fireauth = FirebaseAuth.getInstance();
        user = fireauth.getCurrentUser().getUid();
        storage = firestore.collection("users").document(user).collection("rules");
        Intent startingIntent = getActivity().getIntent();
        DocumentReference docRef = storage.document(startingIntent.getStringExtra("rule"));
        final NumberPicker numberPicker = npDialog.findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(600);
        numberPicker.setMinValue(1);
        numberPicker.setWrapSelectorWheel(false);
        docRef.get().addOnCompleteListener(doctask -> {
            DocumentSnapshot document = doctask.getResult();
            numberPicker.setValue(Integer.parseInt(document.get("small").toString()));
        });
        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            docRef.update("small", newVal);
        });

        setBtn.setOnClickListener(arg0 -> npDialog.dismiss());
        npDialog.show();
    }
}
