package com.ith8.bleconnection;


import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ith8.bleconnection.app.CommonMethod;
import com.ith8.bleconnection.app.DeviceAdapter;
import com.ith8.bleconnection.app.DeviceModel;

import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;



public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, DeviceAdapter.ItemClickedListner {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner scanner;
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private ScanCallback callback;


    private static final int RC_SATE = 101;
    private static final int REQUEST_ENABLE_BT = 201;
    private boolean connected = false;
    private static final String TAG = "MainActivity";





    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback cc = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
//                      super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult: " + result.getDevice() + "cal;backType" + callbackType);
            if (!isScann) {
                BluetoothDevice devices = result.getDevice();
                DeviceModel model = new DeviceModel();
                model.setAddress(devices.getAddress());
                model.setName(devices.getName());
                boolean fetch = devices.fetchUuidsWithSdp();


                model.setRssi(calculateByRange(result.getRssi()));
//                     list.add(model);
                adapter.setItem(model);
            } else {
                Log.d(TAG, "onScanResult: not scanned ");
            }


        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
//                      super.onBatchScanResults(results);
            for (ScanResult rs : results
            ) {
                Log.d(TAG, "onBatchScanResults: " + rs.getScanRecord().getServiceData().get(0));
                Log.d(TAG, "onBatchScanResults: " + rs.getScanRecord().getManufacturerSpecificData().get(0));
                Log.d(TAG, "onBatchScanResults UI iids: " + rs.getScanRecord().getServiceUuids().get(0).getUuid());

            }
        }

        @Override
        public void onScanFailed(int errorCode) {
//                      super.onScanFailed(errorCode);
            Log.d(TAG, "onScanFailed: " + errorCode);
        }

    };
    private List<BluetoothGattService> list = null;
    private boolean isScann = false;
    private boolean isPermission=false;
    private   BluetoothGattService devices;

    private void setDeviceConnected() {
        Toast.makeText(this, "Device Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

            }
        });
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
//            if (TextUtils.isEmpty(Prefs.getString(CommonMethod.DEVICE_NAME))) {
            createAdapter();
            checkDeviceAvailable();
//            } else {
//                startService(new Intent(this, BleService.class));
//            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createAdapter() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(Collections.emptyList(), this);
        recyclerView.setAdapter(adapter);


    }

    private void checkDeviceAvailable() {
        boolean isBleBluetooth = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (isBleBluetooth) {
            Log.d(TAG, "checkDeviceAvailable: ");
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

            checkBleAdapter();
        } else {
            Toast.makeText(this, "Device is not able connect due to ble bluetooth not have  ", Toast.LENGTH_SHORT).show();
        }

    }

    private void checkBleAdapter() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            askForPermission();
        }

    }



    @AfterPermissionGranted(RC_SATE)
    public void askForPermission() {
        if (checkLocationPermission()) {
            isPermission=true;

            checkForLocationOnOff();

        } else {
            EasyPermissions.requestPermissions(this, "You must grant this permission for your login", RC_SATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN);

        }
    }

    private boolean checkLocationPermission() {
        return EasyPermissions
                .hasPermissions(this, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == RC_SATE) {
//            listener.grantedAllPermission();
            startBleScan();
            return;
        }

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == RC_SATE) {
//            listener.notGrantedAllPermission();
//            listener=null;
   isPermission=false;

        }
    }







    private void checkForLocationOnOff() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "No GPS", Toast.LENGTH_SHORT).show();
        } else {
            startBleScan();
        }


    }

    private void startBleScan() {
        Log.d(TAG, "startBleScan: ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.getBluetoothLeScanner().startScan(getLeScanCallback());


        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback getLeScanCallback() {
        callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
//                      super.onScanResult(callbackType, result);
                Log.d(TAG, "onScanResult: " + result.getDevice() + "cal;backType" + callbackType);
                BluetoothDevice devices = result.getDevice();

                DeviceModel model = new DeviceModel();
                model.setAddress(devices.getAddress());
                model.setName(devices.getName());
                boolean fetch = devices.fetchUuidsWithSdp();


                model.setRssi(calculateByRange(result.getRssi()));
//                     list.add(model);
                adapter.setItem(model);

                devices.getBluetoothClass().getMajorDeviceClass();
//                Log.d(TAG, "onScanResult UUId: "+  devices.getUuids()[0].getUuid());

////                mDeviceData = result.getScanRecord().getServiceData();
//                Log.d(TAG, "onScanResult getServiceData: "+result.getScanRecord().getServiceData());
//                Log.d(TAG, "onScanResult: "+result.getScanRecord().getServiceUuids());
//                Log.d(TAG, "onScanResult Bound: "+  devices.getBondState());
//                Log.d(TAG, "onScanResult getBluetoothClass : "+  devices.getBluetoothClass().toString());


            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
//                      super.onBatchScanResults(results);
                for (ScanResult sr : results) {
                    Log.i("ScanResult - Results", sr.toString());
                }

                Log.d(TAG, "onBatchScanResults: ");
            }

            @Override
            public void onScanFailed(int errorCode) {
//                      super.onScanFailed(errorCode);
                Log.d(TAG, "onScanFailed: ");
            }
        };

        return callback;
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            checkBleAdapter();
            Log.d(TAG, "onActivityResult: ");
        }
    }

    private void stopScanner() {
        Handler mHandler = new Handler();
        mHandler.removeCallbacks(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.d(TAG, "run: stopScanner");
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(getLeScanCallback());

                }
            }
        });

    }


    @Override
    public void onItemClicked(int position, DeviceModel model) {
        Log.d(TAG, "onItemClicked: ");
        isScann = true;
        Intent intent = new Intent(this, BleService.class);
        intent.putExtra(CommonMethod.DEVICE_ADDRESS, model.getAddress());
        intent.putExtra(CommonMethod.DEVICE_NAME, model.getName());
        startService(intent);
        stopScanner();


    }

    private int calculateByRange(int rssi) {
        return ((100 + rssi) * 10) / 100;
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG, "displayGattServices gattService : " + new Gson().toJson(gattServices));
//
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattService.getCharacteristics()) {
                Log.d(TAG, "displayGattServices gattCharacteristic: " + new Gson().toJson(gattCharacteristic));
//                charas.add(gattCharacteristic);
            }
//
//            }
//
        }
    }
    private void displayGattServices(BluetoothGattService gattServices) {
        if (gattServices == null) return;


//        // Loops through available GATT Services.
//        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG, "displayGattServices gattService : " + new Gson().toJson(gattServices));
//
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattServices.getCharacteristics()) {
                Log.d(TAG, "displayGattServices gattCharacteristic: " + new Gson().toJson(gattCharacteristic));
//                charas.add(gattCharacteristic);
            }
//
//            }
//
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();


    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }


}
