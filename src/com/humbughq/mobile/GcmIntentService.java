package com.humbughq.mobile;

import java.io.IOException;
import java.net.URL;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        sendNotification(intent.getExtras());
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmShowNotificationReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(Bundle msg) {
        mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HumbugActivity.class).addFlags(0), 0);

        String type = msg.getString("recipient_type");
        String title = msg.getString("alert");
        String tag = "zulip";
        String content = msg.getString("content");

        title = msg.getString("sender_full_name");

        if (type.equals("private")) {
            tag = "zulip-pm-" + msg.getString("sender_email");
        } else if (type.equals("stream")) {
            tag = "zulip-mention-" + msg.getString("stream", "");
            content = "Mentioned you on " + msg.getString("stream") + ":\n"
                    + content;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setStyle(
                        new NotificationCompat.BigTextStyle().bigText(content));

        if (msg.containsKey("sender_avatar_url")) {
            // IntentService is a background thread, so blocking is ok
            Bitmap avatar = fetchAvatar(msg.getString("sender_avatar_url"));
            if (avatar != null) {
                builder.setLargeIcon(avatar);
            }
        }

        if (msg.containsKey("time")) {
            long time = Long.parseLong(msg.getString("time")) * 1000;
            Log.i("GCM", "time: " + time);
            builder.setWhen(time);
        }

        builder.setContentIntent(contentIntent);
        mNotificationManager.notify(tag, NOTIFICATION_ID, builder.build());
    }

    private Bitmap fetchAvatar(String url_str) {
        try {
            URL url = new URL(url_str);
            return GravatarAsyncFetchTask.fetch(url);
        } catch (IOException e) {
            ZLog.logException(e);
        }
        return null;
    }
}