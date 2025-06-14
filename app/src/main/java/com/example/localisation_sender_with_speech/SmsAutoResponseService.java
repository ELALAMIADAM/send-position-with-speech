package com.example.localisation_sender_with_speech;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class SmsAutoResponseService extends Service {
    
    private static final String TAG = "SmsAutoResponseService";
    private static final String CHANNEL_ID = "SmsAutoResponseChannel";
    private static final int NOTIFICATION_ID = 2;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SMS Auto-Response Service created");
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SMS Auto-Response Service started");
        
        if (intent != null && "START_AUTO_RESPONSE".equals(intent.getAction())) {
            startForeground(NOTIFICATION_ID, createNotification());
            Log.d(TAG, "SMS Auto-Response Service running in foreground");
        }
        
        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SMS Auto-Response Service destroyed");
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Auto-Response Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps SMS auto-response feature active");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Auto-Response Active")
                .setContentText("Automatically responding to location requests")
                .setSmallIcon(R.drawable.ic_sms)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
} 