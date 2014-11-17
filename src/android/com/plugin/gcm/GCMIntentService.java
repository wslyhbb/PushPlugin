package com.plugin.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
@SuppressLint("NewApi")
public class GCMIntentService extends IntentService {
	
	private static final String TAG = "GCMIntentService";
	
	public GCMIntentService() {
		super("GCMIntentService");
	}
	
	@Override
    protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent");

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		
		if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
			// The getMessageType() intent parameter must be the intent you received
	        // in your BroadcastReceiver.
	        String messageType = gcm.getMessageType(intent);

	        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				// if we are in the foreground, just surface the payload, else post it to the statusbar
	            if (PushPlugin.isInForeground()) {
					extras.putBoolean("foreground", true);
	                PushPlugin.sendExtras(extras);
				}
	            else {
					extras.putBoolean("foreground", false);
	
	                // Send a notification if there is a message
	                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
	                    createNotification(extras);
	                }
	            }
	        }
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
	}
	
	public void createNotification(Bundle extras)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		int defaults = Notification.DEFAULT_ALL;

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {}
		}
		
		NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(this)
				.setDefaults(defaults)
				.setSmallIcon(this.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(extras.getString("title"))
				.setTicker(extras.getString("title"))
				.setContentIntent(contentIntent)
				.setAutoCancel(true);
		
		String message = extras.getString("message");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}
		
		int notId = 0;
		
		try {
			notId = Integer.parseInt(extras.getString("notId"));
		}
		catch(NumberFormatException e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
		}
		catch(Exception e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
		}
		
		mNotificationManager.notify(notId, mBuilder.build());
	}
}