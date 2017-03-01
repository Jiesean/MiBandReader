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

    //Descriptor
    //setNotification Descriptor UUID
    public static final UUID notificationDesUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

}
