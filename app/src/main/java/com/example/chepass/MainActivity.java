package com.example.chepass;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    boolean serviceNaMaNaDe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.startVpnService);
        final EditText editCode = findViewById(R.id.editCode);

        editCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                button.setEnabled(!s.toString().equals(""));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        serviceNaMaNaDe = isMyServiceRunning();
        if(serviceNaMaNaDe)
            button.setText("Disconnect");
        button.setOnClickListener(v -> {
            Context ctx = this.getApplicationContext();
            if (!serviceNaMaNaDe) {
                Intent vpn = ChepassVpnService.prepare(ctx);
                serviceNaMaNaDe = true;
                if (vpn != null) {
                    //get vpn permission for first time
                    startActivityForResult(vpn, 1);
                } else {
                    new Authenticate(this.getApplicationContext(), editCode.getText().toString(), (String address, String pass) -> {
                        Intent intent = new Intent(ctx, ChepassVpnService.class);
                        intent.setAction(ChepassVpnService.FLAG_VPN_START);
                        intent.putExtra("address", address);
                        intent.putExtra("pass", pass);
                        ContextCompat.startForegroundService(ctx, intent);
                        button.setText("Disconnect");
                    }).Auth();
                }
            } else {
                Intent intent = new Intent(ctx, ChepassVpnService.class);
                intent.setAction(ChepassVpnService.FLAG_VPN_STOP);
                ContextCompat.startForegroundService(ctx, intent);
                serviceNaMaNaDe = false;
                button.setText("Connect");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            final Button button = findViewById(R.id.startVpnService);
            final EditText editCode = findViewById(R.id.editCode);
            new Authenticate(this.getApplicationContext(), editCode.getText().toString(), (String address, String pass) -> {
                Intent intent = new Intent(this, ChepassVpnService.class);
                intent.setAction(ChepassVpnService.FLAG_VPN_START);
                intent.putExtra("address", address);
                intent.putExtra("pass", pass);
                ContextCompat.startForegroundService(this, intent);
                button.setText("Disconnect");
            }).Auth();
        } else {
            Toast.makeText(this, "Really!?", Toast.LENGTH_LONG).show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ChepassVpnService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
