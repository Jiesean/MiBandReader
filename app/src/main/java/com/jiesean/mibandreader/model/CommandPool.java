package com.jiesean.mibandreader.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.RunnableFuture;

import static com.jiesean.mibandreader.model.CommandPool.Type.*;
import static com.jiesean.mibandreader.model.CommandPool.Type.read;
import static com.jiesean.mibandreader.model.CommandPool.Type.setNotification;

/**
 * Created by Jiesean on 2017/3/1.
 */

public class CommandPool implements Runnable{

    public enum Type {
        setNotification ,read ,write
    }
    private Context context;
    private BluetoothGatt gatt;
    private LinkedList<Command> pool ;
    private BluetoothGattCharacteristic characteristic;
    private int index = 0 ;

    public CommandPool(Context context, BluetoothGatt gatt){
        this.gatt = gatt;
        this.context = context;
        pool = new LinkedList<>();
    }

    public void addCommand(Type type ,byte[] value, BluetoothGattCharacteristic target){
        Command command = new Command(type, value, target);
        pool.offer(command);
    }

    @Override
    public void run() {
        while(true){
            System.out.println(pool.size());
            if (pool.peek() == null)
            {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            Command commandToExc = pool.peek();
            boolean result = execute(commandToExc.getType(),  commandToExc.getValue(), commandToExc.getTarget());
            if (result) {
                pool.poll();
            }
            else{
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean execute(Type type ,byte[] value, BluetoothGattCharacteristic target){
        boolean result = false;
        switch (type){
            case setNotification:
                result = enableNotification(true, target);
                break;
            case read:
                result = readCharacteristic(target);
                break;
            case write:
                result = writeCharacteristic(target, value);
                break;
        }
        return result;
    }

    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic,byte[] command){
        characteristic.setValue(command);
        boolean result = gatt.writeCharacteristic(characteristic);
        return result;
    }

    private boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic){

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

    private boolean readCharacteristic(BluetoothGattCharacteristic characteristic){
        boolean result = gatt.readCharacteristic(characteristic);
        return result;
    }

    private class Command{
        private int id ;
        private boolean state = false ;
        private byte[] value;
        private Type type;
        private BluetoothGattCharacteristic target;

        Command (Type type, byte[] value, BluetoothGattCharacteristic target){
            this.value = value;
            this.target = target;
            this.type = type;
            id = index;
            index ++;
        }

        int getId(){
            return id;
        }

        void setSsate(boolean state){
            this.state = state;
        }

        boolean getState(){
            return state;
        }

        BluetoothGattCharacteristic getTarget(){
            return target;
        }

        byte[] getValue(){
            return value;
        }

        Type getType(){
            return type;
        }
    }

}
