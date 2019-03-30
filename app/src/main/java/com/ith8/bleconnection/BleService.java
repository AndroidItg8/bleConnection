package com.ith8.bleconnection;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.ith8.bleconnection.app.BleListener;
import com.ith8.bleconnection.app.CommonMethod;
import com.ith8.bleconnection.app.Prefs;

import java.util.ArrayList;
import java.util.List;


public class BleService extends Service implements BleListener {
    public static final String ACTION_DEVICE_CONNECTED = "ACTION_DEVICE_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public static final String ACTION_BLUETOOTH_GATH_SERVICE = "ACTION_BLUETOOTH_GATH_SERVICE";
    private BleConnectionManager manager;
    private BluetoothManager mBluetoothManager;

  Intent  intent = new Intent();
    private static final String TAG = "BleService";


    public BleService() {
        Log.d(TAG, "BleService: ");
        manager = new BleConnectionManager(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String address = null, name = null; BluetoothDevice device =null;
        if (intent != null && intent.hasExtra(CommonMethod.DEVICE_ADDRESS)) {
            address = intent.getStringExtra(CommonMethod.DEVICE_ADDRESS);
            name = intent.getStringExtra(CommonMethod.DEVICE_NAME);
            Log.d(TAG, "onStartCommand: "+address +"name"+name);
        }
        if (initialize() && address != null && name != null ) {
            selectedDevice(address, name);

        } else {
            reconnectBleDevice();
        }
        return START_STICKY;
    }

    private void selectedDevice(String address, String name) {
        sendToConnect(address, name);
    }

    private void sendToConnect(String address, String name) {
        Log.d(TAG, "sendToConnect: "+address +"name"+name);

        if (manager != null)
            manager.selectedDevice(address, name);
    }

    private boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            Log.d(TAG, "initialize: ");

            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        Log.d(TAG, "initialize Adapter: ");

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        manager.setBluetoothAdapter(mBluetoothAdapter);
        return true;
    }

    private void reconnectBleDevice() {
        if (manager == null)
            manager = new BleConnectionManager(this);

        manager.initCallback();
        checkAvailableDeviceToConnect();
    }

    private void checkAvailableDeviceToConnect() {
        if (Prefs.contains(CommonMethod.DEVICE_ADDRESS) &&
            Prefs.getString(CommonMethod.DEVICE_ADDRESS) != null) {
            sendToConnect(Prefs.getString(CommonMethod.DEVICE_ADDRESS), "name");
        }
    }

    @Override
    public void connectGatt(BluetoothDevice device, BluetoothGattCallback callback) {
        Log.d(TAG, "connectGatt: "+device.getBondState());
        Log.d(TAG, "connectGatt: "+callback.toString());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setBluetoothGatt(device.connectGatt(getApplicationContext(), false, callback, BluetoothDevice.TRANSPORT_LE));
        } else {
            manager.setBluetoothGatt(device.connectGatt(getApplicationContext(), false, callback));
        }

    }

    @Override
    public void onDeviceConnected(String address) {
        Prefs.putString(CommonMethod.DEVICE_ADDRESS, address);
        Prefs.putBoolean(CommonMethod.CONNECTED, true);
        sendBroadcast(ACTION_DEVICE_CONNECTED);
    }

    @Override
    public void onDevicesDisconnect(String actionGattServicesDiscovered) {
        Log.d(TAG, "onDevicesDisconnect: ");

        Prefs.remove(CommonMethod.DEVICE_ADDRESS);
        Prefs.remove(CommonMethod.CONNECTED);
        sendBroadcast(ACTION_GATT_DISCONNECTED);
    }

    @Override
    public void onCharacteristicRead(String actionDataAvailable, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onCharacteristicRead: ");
        Log.d(TAG, "onCharacteristicRead: "+new Gson().toJson(characteristic));

        sendBroadcast(ACTION_DATA_AVAILABLE , characteristic);
    }

    private void sendBroadcast(String available, BluetoothGattCharacteristic characteristic) {

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if(available.equalsIgnoreCase(ACTION_DATA_AVAILABLE)) {
//            if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                int flag = characteristic.getProperties();
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(TAG, "Heart rate format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(TAG, "Heart rate format UINT8.");
                }
                final int heartRate = characteristic.getIntValue(format, 1);
                Log.d(TAG, String.format("Received heart rate: %d", heartRate));
//                intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
            } else {
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    Log.d(TAG, "sendBroadcast: "+new Gson().toJson(data));
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                            stringBuilder.toString());
                }
            }
        sendBroadcast(available);
//        }
    }

    private void sendBroadcast(String action) {
        Log.d(TAG, "SendBroadcast");
        intent.putExtra(action, action);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public void onServiceDiscovered(String action) {
        intent.putExtra(ACTION_GATT_SERVICES_DISCOVERED,action);
//        intent.putParcelableArrayListExtra(ACTION_BLUETOOTH_GATH_SERVICE, (ArrayList<? extends Parcelable>) gatt);
//        Log.d(TAG, "onServiceDiscovered: "+new Gson().toJson(gatt));
//        getSupportedGattServices(gatt.getServices());
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);



    }






}
