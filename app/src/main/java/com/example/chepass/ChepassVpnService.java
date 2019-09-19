package com.example.chepass;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import tun2socks.*;

public class ChepassVpnService extends VpnService implements Runnable {
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(3);
    private static final int BUFSIZE = 4096;
    protected static final String TAG = "chepassVPN";
    public static int FLAG_VPN_START = 0;
    public static int FLAG_VPN_STOP = 1;
    private ParcelFileDescriptor mInterface;
    private Thread mThread;
    private Future<?> deviceToTunnelFuture;
    private Future<?> tunnelToDeviceFuture;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String prefix = getPackageName();
        int action = intent.getIntExtra(prefix + ".ACTION", 0);

        if(action==FLAG_VPN_START) {
            Log.i(TAG, "Starting VPN tunnel");
            // Stop the previous session by interrupting the thread.
            if (mThread != null) {
                mThread.interrupt();
            }
            mThread = new Thread(this, "ki4aVPNThread");
            mThread.start();
        }
        else if(action==FLAG_VPN_STOP) {
            Log.i(TAG, "Stopping VPN tunnel");
            this.onDestroy();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Closing");
        tunnelToDeviceFuture.cancel(true);
        deviceToTunnelFuture.cancel(true);
        if (mThread != null) {
            mThread.interrupt();
        }
        if (mInterface != null){
            try {
                mInterface.close();
            } catch (IOException e) { /* Ignore a close error here */ }
        }
    }

    @Override
    public void onRevoke() {
        // If vpn was revoked, let's close and destroy everything
        this.onDestroy();
        //Util.reportRevoked(this);
    }

    @Override
    public void run() {
        Log.i(TAG, "Starting");
        configure();
    }

    private void forwardVpnServiceToTunnel(FileDescriptor vpnFileDescriptor){
        final FileOutputStream vpnOutput = new FileOutputStream(vpnFileDescriptor);
        Tun2socks.startSocks(new PacketFlow() {
            @Override
            public void writePacket(byte[] buffers) {
                try {
                    vpnOutput.write(buffers);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "127.0.0.1", 8080);
    }

    private void forwardTunnelToVpnService(FileDescriptor vpnFileDescriptor){
        final FileInputStream vpnInput = new FileInputStream(vpnFileDescriptor);
        byte[] buffer = new byte[BUFSIZE];
        int w;
        while (true) {
            // blocking receive
            w = -1;
            try {
                w = vpnInput.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            if (w == -1) {
                break;
            }
            Tun2socks.inputPacket(buffer);
        }
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
            builder.addAddress("85.85.85.85", 24);
            builder.addDnsServer("8.8.8.8");
            Log.i(TAG, "Routing all traffic");
            int o1 = 95, o2 = 179, o3 = 201, o4 = 97;
            int _1, _2, _3, _4;
            //octave 1
            for(_1=1; _1 < 255; _1++) {
                if (_1 == 127 || _1 == o1)
                    continue;
                builder.addRoute(_1 + ".0.0.0", 8); //Redirect all traffic
            }
            //octave 2
            for(_2=1; _2 < 255; _2++) {
                if (_2 == o2)
                    continue;
                builder.addRoute(_1 + "." + _2 + ".0.0", 16); //Redirect all traffic
            }
            //octave 3
            for(_3=1; _3 < 255; _3++) {
                if (_3 == o3)
                    continue;
                builder.addRoute(_1 + "." + _2 + "." + _3 + ".0", 24); //Redirect all traffic
            }
            //octave 4
            for(_4=1; _4 < 255; _4++) {
                if (_4 == o4)
                    continue;
                builder.addRoute(_1 + "." + _2 + "." + _3 + "." + _4, 32); //Redirect all traffic
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                builder.allowFamily(android.system.OsConstants.AF_INET6);
            }

            mInterface = builder.establish();

            deviceToTunnelFuture = EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    forwardVpnServiceToTunnel(mInterface.getFileDescriptor());
                }
            });
            tunnelToDeviceFuture = EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    forwardTunnelToVpnService(mInterface.getFileDescriptor());
                }
            });

            if (mInterface == null){
                this.onDestroy();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            this.onDestroy();
        }
    }
}
