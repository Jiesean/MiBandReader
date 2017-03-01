package com.jiesean.mibandreader;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private TextView mStepTV;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver onReceive");

            if (intent.getAction().equals("state")) {
                if(intent.getStringExtra("state").equals("0")){
                    mDisplayStateTV.append("断开连接\n");
                }
                else if (intent.getStringExtra("state").equals("3")) {
                    mDisplayStateTV.append("扫描超时，重新扫描\n");
                }
                else if (intent.getStringExtra("state").equals("4")) {
                    mDisplayStateTV.append("开始计步\n");
                }
                else{
                    String deviceAddress = intent.getStringExtra("state");
                    mDisplayStateTV.append("连接上设备地址： " + deviceAddress + "\n");
                }
            }
            else if (intent.getAction().equals("step")){
//                mStepTV.setText(Integer.parseInt(intent.getStringExtra("step"), 16));
                mStepTV.setText(intent.getStringExtra("step"));
            }
            else if (intent.getAction().equals("bettry")){

            }

            if(intent.getStringExtra("ConnectionState") != null){

            }
        }
    };



    //service connection
    LeService.LocalBinder mService;
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = (LeService.LocalBinder) service;

            if (mService != null) {
               initBluetooth();
            }
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
        mStepTV = (TextView) findViewById(R.id.step_info_tv);

        //开启蓝牙连接的服务
        Intent serviceIntent = new Intent(MiBandReaderActivity.this, LeService.class);
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, makeGattUpdateIntentFilter());
    }

    public void handleClickEvent(View view){
        if (view.getId() == R.id.scan_btn) {
            mService.startLeScan();
        }
        if (view.getId() == R.id.shock_btn) {
            mService.startAlert(2);
        }
    }

    private void initBluetooth(){

        boolean bluetoothStatte = mService.initBluetooth();
        if (bluetoothStatte == false) {
            mDisplayStateTV.setText("您的设备不支持蓝牙！\n");
        }else{
            boolean leScannerState = mService.initLeScanner();
            if (leScannerState == true) {
                mDisplayStateTV.setText("LeScanner已就绪！\n");
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("state");
        intentFilter.addAction("step");
        intentFilter.addAction("bettry");
//        intentFilter.addAction("state");
        return intentFilter;
    }
}
