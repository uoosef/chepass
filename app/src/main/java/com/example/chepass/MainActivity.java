package com.example.chepass;

import android.content.Intent;
import android.net.VpnService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    boolean serviceNaMaNaDe;
    protected static MainActivity myMainActivity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceNaMaNaDe = false;
        myMainActivity = this;
        final Button button = (Button) findViewById(R.id.startVpnService);
        final Intent i = new Intent(getApplicationContext(), ChepassService.class);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!serviceNaMaNaDe) {
                    getApplicationContext().startService(i);
                    serviceNaMaNaDe = true;
                    Intent vpn = VpnService.prepare(myMainActivity);
                    if(vpn != null) {
                        //get vpn permission for first time
                        startActivityForResult(vpn, 0);
                    }
                    button.setText("Disconnect");
                }else{
                    getApplicationContext().stopService(i);
                    serviceNaMaNaDe = false;
                    button.setText("Connect");
                }
            }
        });
    }
}
