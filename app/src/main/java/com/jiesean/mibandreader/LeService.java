package com.jiesean.mibandreader;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.icu.text.LocaleDisplayNames;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.StringBuilderPrinter;

import com.jiesean.mibandreader.model.BatteryInfoParser;
import com.jiesean.mibandreader.model.Profile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LeService extends Service {

    private String TAG = "LeService";
    private String mTargetDeviceName = "MI";

    //自定义binder，用于service绑定activity之后为activity提供操作service的接口
    private LocalBinder mBinder = new LocalBinder();
    private Handler mHandler;
    private Intent intent;
    private int SCAN_PERIOD = 30000;//设置扫描时限
    private boolean mScanning = false;

    //bluetooth
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private LeGattCallback mLeGattCallback;
    private BluetoothGatt mGatt;


    //Characteristic
    BluetoothGattCharacteristic alertChar;
    BluetoothGattCharacteristic stepChar;
    BluetoothGattCharacteristic batteryChar;


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "service onBind()");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "service onCreate()");

        mScanCallback = new LeScanCallback();
        mLeGattCallback = new LeGattCallback();

        mHandler = new Handler();
    }

    /**
     * 继承Binder类，实现localbinder,为activity提供操作接口
     */
    public class LocalBinder extends Binder {
        public boolean initBluetooth(){
            Log.d(TAG, "initBluetooth");

            //init bluetoothadapter.api 21 above
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                return false;
            }
            else if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                boolean bluetoothState = mBluetoothAdapter.enable();
                return bluetoothState;
            }
            return true;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public boolean initLeScanner(){
            Log.d(TAG, "initLeScanner");

            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (mBluetoothLeScanner != null) {
                return true;
            }
            return false;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void startLeScan() {
            Log.d(TAG, "startLeScan");

            mBluetoothLeScanner.startScan(mScanCallback);
            mScanning = true;
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning == true) {
                        Log.d(TAG, "Stop Scan Time Out");
                        mScanning = false;
                        mBluetoothLeScanner.stopScan(mScanCallback);
                        notifyUI("state","3");
                    }
                }
            }, SCAN_PERIOD);
        }

        /**
         * @param extent 震动程度，1：表示轻微，2：表示剧烈
         */
        public void startAlert(int extent){
            Log.d(TAG, "startLeScan extent: " + extent);

            if (mGatt != null) {
                byte[] value ={(byte)0x02};
                alertChar.setValue(value);
                mGatt.writeCharacteristic(alertChar);
            }
        }
    }

    /**
     * LE设备扫描结果返回
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class LeScanCallback  extends ScanCallback {

        /**
         * 扫描结果的回调，每次扫描到一个设备，就调用一次。
         * @param callbackType
         * @param result
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {


            if(result != null){
                //此处，我们尝试连接MI 设备

                Log.d(TAG, "onScanResult DeviceName : " + result.getDevice().getName() + " DeviceAddress : " + result.getDevice().getAddress());
                if (result.getDevice().getName() != null && mTargetDeviceName.equals(result.getDevice().getName())) {
                    //扫描到我们想要的设备后，立即停止扫描
                    mScanning = false;
                    result.getDevice().connectGatt(LeService.this, true, mLeGattCallback);
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }
        }
    }

    /**
     * gatt连接结果的返回
     */
    private class LeGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange status:" + status + "  newState:" + newState);
            if (newState == 2) {
                gatt.discoverServices();
                mGatt = gatt;

                notifyUI("state", gatt.getDevice().getAddress());
            }
            else if(newState == 0){
                mGatt = null;

                notifyUI("state", "0");
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt 返回的是本次连接的gatt对象
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered status : " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();

                if (services != null) {
                    Log.d(TAG, "onServicesDiscovered num: " + services.size());
                }

                for (BluetoothGattService bluetoothGattService : services) {
                    Log.d(TAG, "onServicesDiscovered service: " + bluetoothGattService.getUuid());
                    List<BluetoothGattCharacteristic> charc = bluetoothGattService.getCharacteristics();

                    for (BluetoothGattCharacteristic charac : charc) {
                        if (charac.getUuid().equals(Profile.IMMIDATE_ALERT_CHAR_UUID) ) {
                            Log.d(TAG, "alertChar found!");
                            //设备 震动特征值
                            alertChar = charac;

                            byte[] value ={(byte)0x02};
                            alertChar.setValue(value);
                            mGatt.writeCharacteristic(alertChar);
                        }
                        if (charac.getUuid().equals(Profile.STEP_CHAR_UUID)) {
                            Log.d(TAG, "stepchar found!");
                            //设备 步数
                            stepChar = charac;
                            boolean result = enableCharacNotification(gatt ,true, charac);
                            if (result){
                                notifyUI("state","4");
                            }
                        }
                        if (charac.getUuid().equals(Profile.BATTERY_CHAR_UUID)) {
                            Log.d(TAG, "batterychar found!");
                            //设备 电量特征值
                            batteryChar = charac;
                            gatt.readCharacteristic(charac);
                        }
                    }
                }


            }
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt
         * @param characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged UUID : " + characteristic.getUuid());
//            String step = Integer.toHexString(characteristic.getValue()[0]);
            onNotify(characteristic.getValue());
//            int stepNum = characteristic.getValue()[0];

        }

        public void onNotify(byte[] data) {
            Log.d(TAG, Arrays.toString(data));
            if (data.length == 4) {
                int stepNum = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                notifyUI("step",stepNum + "");
            }
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         *
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite UUID: " + characteristic.getUuid() + "state : " + status);
        }

        /**
         *Callback reporting the result of a characteristic read operation.
         *
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead UUID : " + characteristic.getUuid());

            if (characteristic.getUuid().equals(Profile.BATTERY_CHAR_UUID)) {
                BatteryInfoParser parser = new BatteryInfoParser(characteristic.getValue());
                notifyUI("battery", parser.getLevel() + "");
            }
        }

        /**
         * Callback indicating the result of a descriptor write operation.
         *
         * @param gatt
         * @param descriptor
         * @param status
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite");
            gatt.readCharacteristic(batteryChar);

        }
    }

    private void notifyUI(String type, String data){
        intent = new Intent();
        intent.setAction(type);
        intent.putExtra(type, data);
        sendBroadcast(intent);
    }

    private boolean enableCharacNotification(BluetoothGatt gatt,boolean enable, BluetoothGattCharacteristic characteristic){
        Log.d(TAG,"enableCharacNotification char : " + characteristic);

        if (gatt == null ||characteristic == null)
            return false;
        if (!gatt.setCharacteristicNotification(characteristic, enable))
            return false;
            BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(Profile.notificationDesUUID);
        if (clientConfig == null)
            return false;
        if (enable) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return gatt.writeDescriptor(clientConfig);
    }

}
