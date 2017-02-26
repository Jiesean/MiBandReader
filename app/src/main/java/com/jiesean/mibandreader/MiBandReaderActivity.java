package com.jiesean.mibandreader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MiBandReaderActivity extends AppCompatActivity {

    private String TAG = "MiBandReaderActivity";

    private TextView mDisplayStateTV ;

    //service connection
    LeService.LocalBinder mService;
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = (LeService.LocalBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_band_reader);

        mDisplayStateTV = (TextView) findViewById(R.id.diaplay_state_tv);

        //开启蓝牙连接的服务
        Intent serviceIntent = new Intent(MiBandReaderActivity.this, LeService.class);
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        if (mService != null) {
            boolean bluetoothStatte = mService.initBluetooth();
            if (bluetoothStatte == false) {
                mDisplayStateTV.setText("您的设备不支持蓝牙！");
            }else{
                boolean leScannerState = mService.initLeScanner();
                if (leScannerState == true) {
                    mDisplayStateTV.setText("LeScanner已就绪！");
                    mService.startLeScan();
                }
            }
        }

    }

    public void handleClickEvent(View view){
        if (view.getId() == R.id.scan_btn) {
            mService.startLeScan();
        }
    }
}
