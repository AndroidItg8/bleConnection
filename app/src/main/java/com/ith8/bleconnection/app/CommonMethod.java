package com.ith8.bleconnection.app;

import java.util.UUID;

public class CommonMethod {
    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEVICE_NAME = "DEVICE_NAME";
    public static final String CONNECTED = "CONNECTED";

    public final static UUID DATA_ENABLE = UUID.fromString("0000ffa3-0000-1000-8000-00805f9b34fb");
    public final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    // DAQ Specific UUIDs
    public final static UUID TEMP_SERVICE_UUID =UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb");
//    public final static UUID TEMP_SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");



    // BLE UUIDs
    public final static UUID SENSOR_ON_OFF =UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb");
    public static final String DEVICE = "DEVICE";



//    UUID=ebb60732-98e0-47ba-ac35-6ae0d5e386aa
}

//    /BleConnectionManager: Callback is null
//            2019-03-30 13:08:10.193 1525-1525/com.ith8.bleconnection D/BluetoothGatt: connect() - device: C7:67:8F:D5:87:93, auto: false
//            2019-03-30 13:08:10.193 1525-1525/com.ith8.bleconnection D/BluetoothGatt: registerApp()
//2019-03-30 13:08:10.194 1525-1525/com.ith8.bleconnection D/BluetoothGatt: registerApp() - UUID=2464a081-db13-4c7f-9623-837e9227b8de
//}

