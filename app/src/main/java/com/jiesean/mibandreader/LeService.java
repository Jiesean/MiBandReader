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
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class LeService extends Service {

    private String TAG = "LeService";
    private String mTargetDeviceName = "MI";

    //自定义binder，用于service绑定activity之后为activity提供操作service的接口
    private LocalBinder mBinder = new LocalBinder();
    private Handler mHandler;
    private int SCAN_PERIOD = 30;//设置扫描时限
    private boolean mScanning = false;

    //bluetooth
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private LeGattCallback mLeGattCallback;

    //UUID
    //震动char:写入 0x01 或者 0x02 时手环都会震动，01强度弱于 02
    UUID shockCharUUID = java.util.UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    //计步char:读取该UUID下的value数组 第0 个数据就是 步数
    UUID stepCharUUID = java.util.UUID.fromString("0000ff06-0000-1000-8000-00805f9b34fb");

    //Characteristic
    BluetoothGattCharacteristic shockChar;
    BluetoothGattCharacteristic stepChar;
    BluetoothGattCharacteristic batteryChar;

    public LeService() {
    }

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
                        mScanning = false;
                        mBluetoothLeScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);
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
            //Log.d(Tag, "onScanResult");
            if(result != null){
                //此处，我们尝试连接MI 设备
                if (result.getDevice().getName() != null && mTargetDeviceName.equals(result.getDevice().getName())) {
                    //扫描到我们想要的设备后，立即停止扫描
                    result.getDevice().connectGatt(LeService.this, false, mLeGattCallback);
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }
        }
    }

    /**
     * gatt连接结果的返回
     */
    private class LeGattCallback extends BluetoothGattCallback {

        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote GATT server
         *
         * @param gatt 返回连接建立的gatt对象
         * @param status 返回的是此次gatt操作的结果，成功了返回0
         * @param newState 每次client连接或断开连接状态变化，STATE_CONNECTED 0，STATE_CONNECTING 1,STATE_DISCONNECTED 2,STATE_DISCONNECTING 3
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange status:" + status + "  newState:" + newState);
            if (status == 0) {
                gatt.discoverServices();
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
            Log.d(TAG, "onServicesDiscovered status" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                for (final BluetoothGattService bluetoothGattService : services) {
                    List<BluetoothGattCharacteristic> charc = bluetoothGattService.getCharacteristics();

                    for (BluetoothGattCharacteristic charac : charc) {
                        if (charac.getUuid() == shockCharUUID) {
                            //设备 震动特征值
                            shockChar = charac;
                        } else if (charac.getUuid() == stepCharUUID) {
                            //设备 步数
                            stepChar = charac;
                            gatt.readCharacteristic(stepChar);
                        } else if (charac.getUuid().toString().equals("")) {
                            //设备 电量特征值
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
            Log.d(TAG, "onCharacteristicChanged");
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
            Log.d(TAG, "onCharacteristicWrite");
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
            Log.d(TAG, "onCharacteristicRead");
            if (stepChar == characteristic) {
                System.out.println("走了" + stepChar.getValue()[0]);
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
        }

    };


}
