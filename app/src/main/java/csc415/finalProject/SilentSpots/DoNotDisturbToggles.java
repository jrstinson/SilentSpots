/*
HOW TO IMPLEMENT DO NOT DISTURB METHODS
---------------------------------------

1) This file, DoNotDisturbToggles.java, must be included in the package. It is here, but if you use
it somewhere else you will need to change the package declaration below.

2) Each method needs a boolean (on/off) and a NotificationManager object. NotificationManager
objects are declared as such:

    final NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

The method will also turn off DnD if passed a false. If a switch is used, this is handled by the listener but if
done some other way we will either need to pass it a boolean some other way or change this class.


WHAT THEY DO
------------

fullDND - Does what it says on the tin. Full Do Not Disturb

alarmsDND - disables everything except alarms

messageOnly - only allows messages to be received, no calls or other sounds

mediaMode - disables everything except sounds from music, games, etc.

starredOnly - Only allows calls from starred contacts

TODO: figure out how to allow messages and calls from starred contacts at the same time. Not sure why they made that difficult, but it is.

 */

package csc415.finalProject.SilentSpots;

import android.annotation.TargetApi;
import android.app.NotificationManager;

class DoNotDisturbToggles {

    @TargetApi(23)
    static void fullDND(boolean isChecked, NotificationManager manager) {
        if (isChecked) {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        } else {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }

    @TargetApi(23)
    static void alarmsDND(boolean isChecked, NotificationManager manager) {
        NotificationManager.Policy policy = new NotificationManager.Policy(NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS, 0, 0);

        if (isChecked) {
            manager.setNotificationPolicy(policy);
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        } else {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }

    @TargetApi(23)
    static void messageOnly(boolean isChecked, NotificationManager manager) {
        NotificationManager.Policy policy = new NotificationManager.Policy(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES, 0, NotificationManager.Policy.PRIORITY_SENDERS_ANY);

        if (isChecked) {
            manager.setNotificationPolicy(policy);
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        } else {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }

    @TargetApi(28)
    static void mediaMode(boolean isChecked, NotificationManager manager) {
        NotificationManager.Policy policy = new NotificationManager.Policy(NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA, 0, 0);

        if (isChecked) {
            manager.setNotificationPolicy(policy);
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        } else {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }

    @TargetApi(23)
    static void starredOnly(boolean isChecked, NotificationManager manager) {
        NotificationManager.Policy policy = new NotificationManager.Policy(NotificationManager.Policy.PRIORITY_CATEGORY_CALLS, NotificationManager.Policy.PRIORITY_SENDERS_STARRED, NotificationManager.Policy.PRIORITY_SENDERS_STARRED);

        if (isChecked) {
            manager.setNotificationPolicy(policy);
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        } else {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }
}
