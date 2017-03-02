package com.jiesean.mibandreader.model;

import java.util.UUID;

/**
 * Created by Jiesean on 2017/2/28.
 */

public class Profile {

    //UUID

    //Service
    //Heart Rate
    public static final UUID HEARTRATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");

    //Characteristic
    //alertchar:写入 0x01 或者 0x02 时手环都会震动，01强度弱于 02
    public static final UUID IMMIDATE_ALERT_CHAR_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    //计步char:读取该UUID下的value数组 第0 个数据就是 步数
    public static final UUID STEP_CHAR_UUID = UUID.fromString("0000ff06-0000-1000-8000-00805f9b34fb");
    //电量信息
    public static final UUID BATTERY_CHAR_UUID = UUID.fromString("0000ff0c-0000-1000-8000-00805f9b34fb");
    //用户信息char
    public static final UUID USER_INFO_CHAR_UUID = UUID.fromString("0000ff04-0000-1000-8000-00805f9b34fb");
    //控制点char
    public static final UUID CONTROL_POINT_CHAR_UUID = UUID.fromString("0000ff05-0000-1000-8000-00805f9b34fb");
    //震动
    public static final UUID VIBRATION_CHAR_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    //Descriptor
    //setNotification Descriptor UUID
    public static final UUID notificationDesUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final byte[] SET_LED_RED = {14, 6, 1, 2, 1};
    public static final byte[] SET_LED_BLUE = {14, 0, 6, 6, 1};
    public static final byte[] SET_LED_ORANGE = {14, 6, 2, 0, 1};
    public static final byte[] SET_LED_GREEN = {14, 4, 5, 0, 1};
    public static final byte[][] LED_COLOR = {SET_LED_RED, SET_LED_BLUE, SET_LED_ORANGE, SET_LED_GREEN};

    //vibrate mode
    public static final byte[] VIBRATION_WITH_LED = {1}; //0x00和0x03都是震动两下，伴随着LED
    public static final byte[] VIBRATION_WITHOUT_LED = {4};//震动两下，无LED
    public static final byte[][] VIBRATE_MODE = {VIBRATION_WITH_LED, VIBRATION_WITHOUT_LED };

}
