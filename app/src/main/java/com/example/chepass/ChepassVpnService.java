package com.example.chepass;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;

import tun2socks.Tun2socks;
import tun2socks.Tun2socksStartOptions;

public class ChepassVpnService extends VpnService {
    protected static final String TAG = "chepassVPN";
    public static String FLAG_VPN_START = "0";
    public static String FLAG_VPN_STOP = "1";
    private Notification notification;
    private static final String PRIVATE_VLAN4_CLIENT = "172.19.0.1";
    private static final String PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1";
    private ParcelFileDescriptor mInterface;
    BrookConnection b;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction().equals(FLAG_VPN_START)) {
            Log.e("onStartCommand: ",  "" + intent.getStringExtra("address"));
            run(intent.getStringExtra("address"), intent.getStringExtra("pass"));
            return START_STICKY;
        }
        else if(intent != null && intent.getAction().equals(FLAG_VPN_STOP)) {
            Log.i(TAG, "Stopping VPN tunnel");
            this.onDestroy();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Closing vpn");
        Tun2socks.stop();
        stopSelf();

        stopForeground(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel("chepass");
        }
        if (mInterface != null){
            try {
                mInterface.close();
            } catch (IOException e) { /* Ignore a close error here */ }
        }
        Log.i(TAG, "Closing brook");
        try{
            b.kill();
            b.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRevoke() {
        // If vpn was revoked, let's close and destroy everything
        this.onDestroy();
        //Util.reportRevoked(this);
    }

    public void run(String address, String pass) {
        Log.i(TAG, "Create Notification");
        createNotification();
        startForeground(1, notification);
        Log.i(TAG, "Starting brook");
        b = new BrookConnection();
        b.address = address;
        b.pass = pass;
        b.start();
        Log.i(TAG, "Configuring vpn service");
        configure();
    }

    private void createNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        NotificationChannelCompat notificationChannel =
                new NotificationChannelCompat.Builder(
                        "vpn_service", NotificationManagerCompat.IMPORTANCE_DEFAULT)
                        .setName("Vpn service")
                        .build();
        notificationManager.createNotificationChannel(notificationChannel);

        PendingIntent contentPendingIntent =
                PendingIntent.getActivity(this, 2, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        notification =
                new NotificationCompat.Builder(this, notificationChannel.getId())
                        .setContentTitle("Vpn service")
                        .setContentText("Testing Tun2Socks")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setOnlyAlertOnce(true)
                        .setOngoing(true)
                        .setAutoCancel(true)
                        .setShowWhen(false)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)
                        .setContentIntent(contentPendingIntent)
                        .build();
    }

    private void configure()
    {
        Log.i(TAG, "Configure");
        if (mInterface != null) {
            Log.i(TAG, "Using the previous interface");
            return;
        }

        try {
            Builder builder = new Builder();
            builder.setSession("chepass");
            builder.setMtu(1500);//Util.tunVPN_MTU);
            builder.addAddress(PRIVATE_VLAN4_CLIENT, 30);
            builder.addAddress(PRIVATE_VLAN6_CLIENT, 126);
            builder.addDnsServer("8.8.8.8");
            builder.addDnsServer("8.8.4.4");
            builder.addDnsServer("1.1.1.1");
            builder.addDnsServer("1.0.0.1");
            builder.addDnsServer("2001:4860:4860::8888");
            builder.addDnsServer("2001:4860:4860::8844");
            builder.addDisallowedApplication(getPackageName());
            builder.addRoute("0.0.0.0", 0);
            builder.addRoute("::", 0);

            mInterface = builder.establish();

            Tun2socksStartOptions tun2socksStartOptions = new Tun2socksStartOptions();
            tun2socksStartOptions.setTunFd(mInterface.getFd());
            tun2socksStartOptions.setSocks5Server("127.0.0.1:8080");
            tun2socksStartOptions.setEnableIPv6(true);
            tun2socksStartOptions.setAllowLan(true);
            tun2socksStartOptions.setMTU(1500);
            tun2socksStartOptions.setFakeIPRange("24.0.0.0/8");

            try{
                new Thread(() -> {
                    Tun2socks.start(tun2socksStartOptions);
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            this.onDestroy();
        }
    }
}
