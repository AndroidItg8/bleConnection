package com.ith8.bleconnection.app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

public interface BleListener {
//    void currentState(DeviceState state);
    void connectGatt(BluetoothDevice device, BluetoothGattCallback callback);
    void onDeviceConnected(String address);


    void onDevicesDisconnect(String actionGattServicesDiscovered);

    void onCharacteristicRead(String actionDataAvailable, BluetoothGattCharacteristic characteristic);

    void onServiceDiscovered(String actionGattServicesDiscovered);
}
