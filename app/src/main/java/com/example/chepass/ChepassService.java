package com.example.chepass;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class ChepassService extends Service {
    public static final String CHANNEL_ID = "ChepassService";
    sshConnection s;

    /*    @Override
        public void onCreate() {
            Toast.makeText(this, "Service created!", Toast.LENGTH_LONG).show();
        }*/

    @Override
    public void onDestroy() {
        s.kill();
        //s.destroy();
        s = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
            channel.setDescription(CHANNEL_ID);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification
                = new NotificationCompat
                .Builder(getApplicationContext(), CHANNEL_ID)
                .build();

        startForeground(1, notification);

        //do background thread

        s = new sshConnection(getApplication());
        try {
            s.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final Intent vpn = new Intent(getApplicationContext(), ChepassVpnService.class);
        getApplicationContext().startService(vpn);
        //stopSelf();

        return START_NOT_STICKY;
    }
}
