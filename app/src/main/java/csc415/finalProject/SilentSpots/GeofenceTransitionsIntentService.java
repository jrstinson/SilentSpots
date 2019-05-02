package csc415.finalProject.SilentSpots;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import static android.content.ContentValues.TAG;

public class GeofenceTransitionsIntentService extends IntentService {
    FirebaseFirestore firestore;
    FirebaseAuth fireauth;
    String user;
    CollectionReference storage;
    String name;
    int time;
    int hour;
    int minutes;
    public static final String CHANNEL_ID = "channel_01";

    public GeofenceTransitionsIntentService() {
        super("None");
    }

    public GeofenceTransitionsIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        firestore = FirebaseFirestore.getInstance();
        fireauth = FirebaseAuth.getInstance();

        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        FirebaseUser currentuser = fireauth.getCurrentUser();
        if (currentuser != null) {
            user = currentuser.getUid();

        } else {
            fireauth.signInAnonymously().addOnCompleteListener(task -> user = task.getResult().getUser().getUid());
        }
        storage = firestore.collection("users").document(user).collection("rules");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.println(Log.WARN, "test", "not working");
            return;
        }

        int transition = geofencingEvent.getGeofenceTransition();

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transition == Geofence.GEOFENCE_TRANSITION_EXIT || transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            List<Geofence> geofences = geofencingEvent.getTriggeringGeofences();
            for (Geofence geofence : geofences) {
                Log.println(Log.WARN, "blarg", geofence.getRequestId());
                DocumentReference docRef = storage.document(geofence.getRequestId());
                docRef.get().addOnCompleteListener(doctask -> {
                    DocumentSnapshot document = doctask.getResult();
                    switch (document.get("setting").toString()) {
                        case "None":

                            if (manager.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL)
                                DoNotDisturbToggles.fullDND(false, manager);
                            break;
                        case "Full":
                            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL)
                                DoNotDisturbToggles.fullDND(true, manager);
                            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT)
                                DoNotDisturbToggles.fullDND(false, manager);
                            break;
                        case "Starred":
                            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL)
                                DoNotDisturbToggles.starredOnly(true, manager);
                            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT)
                                DoNotDisturbToggles.starredOnly(false, manager);
                            break;
                        case "Messages":
                            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL)
                                DoNotDisturbToggles.messageOnly(true, manager);
                            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT)
                                DoNotDisturbToggles.messageOnly(false, manager);
                            break;
                        case "Alarms":
                            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL)
                                DoNotDisturbToggles.alarmsDND(true, manager);
                            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT)
                                DoNotDisturbToggles.alarmsDND(false, manager);
                            break;
                        case "Media":
                            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL)
                                DoNotDisturbToggles.mediaMode(true, manager);
                            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT)
                                DoNotDisturbToggles.mediaMode(true, manager);
                    }
                    switch (document.get("clock").toString()) {
                        case "None":
                            break;
                        case "Timer":
                            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER)
                                name = document.get("title").toString();
                                time = Integer.parseInt(document.get("large").toString()) * 60 + Integer.parseInt(document.get("small").toString());
                                DoNotDisturbToggles.setTimer(name, time, this);
                            break;
                        case "Alarm":
                            name = document.get("title").toString();
                            hour = Integer.parseInt(document.get("large").toString());
                            minutes = Integer.parseInt(document.get("small").toString());
                            DoNotDisturbToggles.setAlarm(name, hour, minutes, this);
                            break;
                    }
                });
            }
            String notificationString = getNotificationString(transition);
            notifyUser(notificationString);
        } else {
            Log.e(TAG, "error");
        }
    }

    private void notifyUser(String notificationDetails) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Oreo requires channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence appName = getString(R.string.app_name);
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, appName, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }
        Intent notificationIntent = new Intent(getApplicationContext(), ListActivity.class);

        TaskStackBuilder taskStack = TaskStackBuilder.create(this);
        taskStack.addParentStack(ListActivity.class);
        taskStack.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent =
                taskStack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.general_notification_text))
                .setContentIntent(notificationPendingIntent);

        // channel id for Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        builder.setAutoCancel(true);
        mNotificationManager.notify(0, builder.build());
    }

    private String getNotificationString(int transition) {
        switch (transition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.DND_radius_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.DND_radius_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }
}
