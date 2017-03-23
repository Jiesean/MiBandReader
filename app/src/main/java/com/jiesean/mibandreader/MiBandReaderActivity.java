package com.jiesean.mibandreader;

import android.bluetooth.BluetoothDevice;
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
import android.widget.Button;
import android.widget.TextView;

public class MiBandReaderActivity extends AppCompatActivity {

    private String TAG = "MiBandReaderActivity";

    private TextView mDisplayStateTV ;
    private TextView mStepTV;
    private TextView mDeviceInfoTV;
    private TextView mBatteryInfoTV;
    private Button mAlertBtn;
    private Button mScanBtn;
    private Button mVibrateBtn;
    private Button mBondBtn;
    private Button mVibrateLedBtn;
    private Button mConnectBtn;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver onReceive");

            if (intent.getAction().equals("state")) {

                if(intent.getStringExtra("state").equals("0")){//断开连接
                    mDisplayStateTV.append("断开连接\n");
                    updateConnectionStateUI(false);
                }
                if(intent.getStringExtra("state").equals("1")){//连接成功
                    mDisplayStateTV.append("连接到目标设备\n");
                    updateConnectionStateUI(true);
                }
                else if (intent.getStringExtra("state").equals("3")) {//扫描超时
                    mDisplayStateTV.append("扫描超时，重新扫描\n");
                }
                else if (intent.getStringExtra("state").equals("4")) {//开启实时计步通知
                    mDisplayStateTV.append("开始计步\n");
                }
                else{
                    String deviceAddress = intent.getStringExtra("state");
                    mDisplayStateTV.append("扫描到目标设备： " + deviceAddress + "\n");
                    mBondBtn.setEnabled(true);
                    mScanBtn.setEnabled(false);
                }
            }
            else if (intent.getAction().equals("step")){
                mStepTV.setText(intent.getStringExtra("step"));
            }
            else if (intent.getAction().equals("battery")){
                mBatteryInfoTV.setText(intent.getStringExtra("battery"));
            }
            else if(intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                mDisplayStateTV.append("绑定目标设备 "+"\n");
                mConnectBtn.setEnabled(true);
                mBondBtn.setEnabled(false);
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
        mDeviceInfoTV = (TextView) findViewById(R.id.device_info_tv);
        mBatteryInfoTV = (TextView) findViewById(R.id.bettery_info_tv);
        mAlertBtn = (Button) findViewById(R.id.alert_btn);
        mAlertBtn.setEnabled(false);
        mScanBtn = (Button) findViewById(R.id.scan_btn);
        mVibrateLedBtn = (Button) findViewById(R.id.vibrate_led_btn);
        mVibrateLedBtn.setEnabled(false);
        mBondBtn = (Button) findViewById(R.id.bond_btn);
        mBondBtn.setEnabled(false);
        mVibrateBtn = (Button) findViewById(R.id.vibrate_btn);
        mVibrateBtn.setEnabled(false);
        mConnectBtn = (Button) findViewById(R.id.connect_btn);
        mConnectBtn.setEnabled(false);

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
        if (view.getId() == R.id.alert_btn) {
            mService.startAlert();
        }
        if (view.getId() == R.id.bond_btn) {
            int result = mService.bondTarget();
            if (result == 1) {
                mDisplayStateTV.append("绑定目标设备 "+"\n");
                mConnectBtn.setEnabled(true);
                mBondBtn.setEnabled(false);
            }
            else if (result == -1){
                mDisplayStateTV.append("绑定失败 "+"\n");
            }
        }
        if (view.getId() == R.id.vibrate_btn) {
            mService.vibrateWithoutLed();
        }
        if (view.getId() == R.id.vibrate_led_btn) {
            mService.vibrateWithLed();
        }
        if (view.getId() == R.id.connect_btn) {
            mService.connectToGatt();
        }


    }

    private void initBluetooth(){

        boolean bluetoothStatte = mService.initBluetooth();
        if (bluetoothStatte == false) {
            mDisplayStateTV.setText("您的设备不支持蓝牙！\n");
        }else{
            boolean leScannerState = mService.initLeScanner();
            if (leScannerState == true) {
                mDisplayStateTV.setText("蓝牙已就绪！\n");
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("state");
        intentFilter.addAction("step");
        intentFilter.addAction("battery");
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        return intentFilter;
    }

    private void updateConnectionStateUI(boolean enable){

        String deviceName = enable?("MI"):("未连接");
        mDeviceInfoTV.setText(deviceName);

        mBatteryInfoTV.setText("");
        mStepTV.setText("");
        mAlertBtn.setEnabled(enable);
        mScanBtn.setEnabled(!enable);
        mVibrateBtn.setEnabled(enable);
        mVibrateLedBtn.setEnabled(enable);
        mConnectBtn.setEnabled(!enable);


    }
}
