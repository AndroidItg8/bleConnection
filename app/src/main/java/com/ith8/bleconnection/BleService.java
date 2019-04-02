package com.ith8.bleconnection;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;




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
    private BluetoothGattService devices;
List<BluetoothGattService> serviceList = new ArrayList<>();

    Intent  intent = new Intent();
    private static final String TAG = "MainBleService";
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: action" + action);
            if (ACTION_DEVICE_CONNECTED.equals(action)) {

                Log.d(TAG, "onReceive: action"+action);
//                if (!connected) {
//                    setDeviceConnected();
//                    connected = true;
//                }
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();


            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
//                connected = false;
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
                Log.d(TAG, "onReceive: action"+action);

            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                if (intent.hasExtra(ACTION_BLUETOOTH_GATH_SERVICE))
//                    list = intent.getParcelableExtra(ACTION_BLUETOOTH_GATH_SERVICE);
                    devices   = intent.getParcelableExtra(ACTION_BLUETOOTH_GATH_SERVICE);
////                displayGattServices(list);
//                displayGattServices(devices);
                    Log.d(TAG, "onReceive: action"+action);


            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BleService.EXTRA_DATA));
                Log.d(TAG, "onReceive: action"+action);

            }
        }
    };

    private void setBroadcastReciver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(EXTRA_DATA);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_BLUETOOTH_GATH_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(gattUpdateReceiver, intentFilter);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setBroadcastReciver();

    }

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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setBluetoothGatt(device.connectGatt(getApplicationContext(), false, callback, BluetoothDevice.TRANSPORT_LE));
        } else {
            manager.setBluetoothGatt(device.connectGatt(getApplicationContext(), false, callback));
        }
        Log.d(TAG, "connectGatt: "+device.getAddress());


    }

    @Override
    public void onDeviceConnected(String address) {
        Log.d(TAG, "onDeviceConnected: address"+address);
        Prefs.putString(CommonMethod.DEVICE_ADDRESS, address);
        Prefs.putBoolean(CommonMethod.CONNECTED, true);
        sendBroadcast(ACTION_DEVICE_CONNECTED);
    }

    @Override
    public void onDevicesDisconnect(String actionGattServicesDiscovered) {
        Log.d(TAG, "onDevicesDisconnect: "+actionGattServicesDiscovered);

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
        Log.d(TAG, "sendBroadcast: characteristic"+ new Gson().toJson(characteristic));

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
    public void onServiceDiscovered(final String action,  BluetoothGattService gatt) {
        if (gatt == null) {
            Log.d(TAG, "service not found!");
        }else {

            Observable.just(gatt).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<BluetoothGattService>() {

                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(BluetoothGattService list) {
//                    Log.d(TAG, "onServiceDiscovered: gathh" + new Gson().toJson(list));
//                Log.d(TAG, "onNext: size"+list.size());
                    serviceList.add(list);
                    Log.d(TAG, "onNext:BluetoothGattService uuid"+list.getUuid().toString());
                    Log.d(TAG, "onNext:BluetoothGattService type"+list.getType());
                    Log.d(TAG, "onNext:BluetoothGattService id "+list.getInstanceId());
                    Log.d(TAG, "onNext:BluetoothGattService Size "+list.getCharacteristics().size());
                    displayGattServices(serviceList);



//
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                            intent.putExtra(ACTION_BLUETOOTH_GATH_SERVICE, list);
//                            intent.putExtra(ACTION_GATT_SERVICES_DISCOVERED, action);
//                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
//                        }

////        Log.d(TAG, "onServiceDiscovered: "+new Gson().toJson(gatt));
////        getSupportedGattServices(gatt.getServices());
                }

                @Override
                public void onError(Throwable e) {
                    Log.d(TAG, "onError: e." + e.getMessage());
                    e.printStackTrace();

                }

                @Override
                public void onComplete() {

                }


            });
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(gattUpdateReceiver!=null)
            unregisterReceiver(gattUpdateReceiver);
    }
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null && gattServices.size()==0) return;

        /**
         * Currently we are working on pressure
         */


//        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
        Log.d(TAG, "displayGattServices gattService : " + new Gson().toJson(gattServices));
//
        for (BluetoothGattCharacteristic gattCharacteristic :
                gattService.getCharacteristics()) {
            Log.d(TAG, "displayGattServices gattCharacteristic: " + new Gson().toJson(gattCharacteristic));
//                charas.add(gattCharacteristic);



        }
//
            }
//
    }

}
