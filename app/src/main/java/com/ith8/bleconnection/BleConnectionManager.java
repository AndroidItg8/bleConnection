package com.ith8.bleconnection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.ith8.bleconnection.app.BleListener;
import com.ith8.bleconnection.app.CommonMethod;


import java.util.List;

import static com.ith8.bleconnection.BleService.ACTION_DATA_AVAILABLE;
import static com.ith8.bleconnection.BleService.ACTION_GATT_DISCONNECTED;
import static com.ith8.bleconnection.BleService.ACTION_GATT_SERVICES_DISCOVERED;
import static com.ith8.bleconnection.app.CommonMethod.SENSOR_ON_OFF;
import static com.ith8.bleconnection.app.CommonMethod.TEMP_SERVICE_UUID;

public class BleConnectionManager implements Handler.Callback {

    private static final int MSG_STATE_DISCOVERD = 2;
    private static final int MSG_STATE_READ = 4;
    private final BleListener listener;
    private final Handler bleHandler;
    private BluetoothGattCallback callback;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private static final String TAG = "BleConnectionManager";
    private String mBluetoothDeviceAddress;
    private boolean connecting = false;
    private String address;




    public BleConnectionManager(BleService callback) {
        this.listener = callback;
        HandlerThread handlerThread = new HandlerThread("BleThread");
        handlerThread.start();
        bleHandler = new Handler(handlerThread.getLooper(), this);
    }

    public void initCallback() {
        callback = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "onConnectionStateChange: " + newState + "connect : ");
//                    mHandler.sendMessage(Message.obtain(null,MSG_STATE_CONNECT,status));
                    listener.onDeviceConnected(gatt.getDevice().getAddress());


                    if (discoverServices()) {
                        Log.i(TAG, "Attempting to start service discovery:");
                        bleHandler.sendMessage(Message.obtain(null,MSG_STATE_DISCOVERD,gatt.getServices()));
                    }else
                    {
                        Log.d(TAG,"Fail to descover service: Error "+status );
                    }
//                    new Handler(Looper.getMainLooper()).post(new Runnable() {
//                        @Override
//                        public void run() {
//                            boolean ans = mBluetoothGatt.discoverServices();
//                            Log.d(TAG, "Discover Services started: " + ans);
//                            Log.d(TAG, "Discover Services started: " + new Gson().toJson(gatt.getServices()));
//
//                        }
//                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    listener.onDevicesDisconnect(ACTION_GATT_DISCONNECTED);
//                    mHandler.sendMessage(Message.obtain(null,MSG_STATE_DISCONNECT,status));


                }
//                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {

//                    notifyServiceDiscovered(gatt, status);
//                    listener.onServiceDiscovered(ACTION_GATT_SERVICES_DISCOVERED,gatt);
//                Thread thread = new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d(TAG, "onServicesDiscovered: gatt" + new Gson().toJson(gatt.getServices()));
//
//                    }
//                };
//                thread.start();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    bleHandler.sendMessage(Message.obtain(null, MSG_STATE_DISCOVERD, gatt.getServices()));
                }


//                Log.d(TAG, "Discover Services started: " + new Gson().toJson(gatt.getServices()));
////                            listener.onDeviceConnected(gatt.getDevice().getAddress());
//                            if (status == BluetoothGatt.GATT_SUCCESS) {
//                                Log.d(TAG, "onServicesDiscovered: "+gatt.getServices());
//
//                            } else {
//                                Log.w(TAG, "onServicesDiscovered received failed: " + status);
//
//                            }


            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                System.out.println("onCharacteristicRead: " + characteristic.toString());
//                System.out.println("onCharacteristicRead status: " + status);
//                System.out.println("onCharacteristicRead: " + gatt.getDevice().getName());
                Log.d(TAG, "onCharacteristicRead: " + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    listener.onCharacteristicRead(ACTION_DATA_AVAILABLE, characteristic);
                    bleHandler.sendMessage(Message.obtain(null, MSG_STATE_READ, gatt));


                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "onCharacteristicWrite: ");
                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    notifyServiceDiscovered(gatt, status);
                    Log.d(TAG, " SUCCESS onCharacteristicWrite: ");

                } else {
                    Log.d(TAG, "onCharacteristicWrite: ");

                }
//                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                //  super.onCharacteristicChanged(gatt, characteristic);
                Log.d(TAG, "characteristics int value 16 : " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0).intValue());
                Log.d(TAG, "characteristics int value 32: " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0).intValue());

            }
        };
    }

    private void notifyServiceDiscovered(BluetoothGatt gatt, int status) {
        BluetoothGattCharacteristic characteristic = null;
        switch (status) {
            case 0:
                Log.d(TAG, "Enabling pressure accel");
                characteristic = gatt.getService(TEMP_SERVICE_UUID).getCharacteristic(SENSOR_ON_OFF);
                characteristic.setValue(new byte[]{0x02});
                break;
        }

        if (characteristic != null)
            gatt.writeCharacteristic(characteristic);
        else
            Log.w(TAG, "Characteristics null. Please check mState=0");

    }

    private boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }


        /*
        // Previously connected device.  Try to reconnect.
        // When we change device.connect(context, autoconnect, callback) autoconnect to false, we cannot use below method there.
        // Device acting strange when changing device autoconnect to true. It connect to only some device. So decided to change autoconnect
        // to false and commenting below code
        */
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
////            mBluetoothGatt.disconnect();
////            mBluetoothGatt.close();
//
//            if (mBluetoothGatt.connect()) {
//                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//                return true;
//            } else {
//                return false;
//            }
//        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        Log.d(TAG, "Trying to create a new connection.");

        mBluetoothDeviceAddress = address;
        if (callback == null) {
            Log.d(TAG, "Callback is null");
            initCallback();
        }
        if (!connecting) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "now in connecting,....");
            listener.connectGatt(device, callback);
            connecting = true;
        } else {
            Log.d(TAG, "not connecting.. connecting is true");
        }

        return true;
    }

    public void selectedDevice(String address, String name) {
        Log.d(TAG, "Selected device Name: " + name);
        if (address != null && !TextUtils.isEmpty(address)) {
            connect(address);
            this.address = address;
        }
    }

    public void setBluetoothAdapter(BluetoothAdapter mBluetoothAdapter) {
        this.mBluetoothAdapter = mBluetoothAdapter;
        Log.d(TAG, "setBluetoothAdapter: ");
    }


    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        if (mBluetoothGatt == null)
            this.mBluetoothGatt = bluetoothGatt;
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case MSG_STATE_READ:
                if (connecting) {
                    listener.onCharacteristicRead(ACTION_DATA_AVAILABLE, (BluetoothGattCharacteristic) msg.obj);
                }
                break;
            case MSG_STATE_DISCOVERD:
//                    listener.onServiceDiscovered(ACTION msg.obj);
                listener.onServiceDiscovered(ACTION_GATT_SERVICES_DISCOVERED);
                break;
        }
        return true;
    }

    private boolean discoverServices() {
        if (mBluetoothGatt == null)
            throw new NullPointerException("mBluetoothGatt still null check it");
        try{
            Thread.sleep(70);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mBluetoothGatt.discoverServices();
    }


//    public BluetoothGattService getSupportedGattService() {
//        if (mBluetoothGatt == null)
//            return null;
//
//        return mBluetoothGatt.getServices();
//    }
//    
}
