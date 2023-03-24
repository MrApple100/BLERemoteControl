package com.mrapple100.bleremotecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;


public class BluetoothScanner {

    private static final String TAG = BluetoothScanner.class.getSimpleName();
    private static final long SCAN_PERIOD = 1000; // 1 seconds
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Handler mHandler;
    private boolean mScanning;
    private Context context;
    private BluetoothDevice bluetoothDevice;
    private ActivityViewModel activityViewModel;

    public BluetoothScanner(ActivityViewModel activityViewModel,Context context) {
        this.context = context;
        mHandler = new Handler(Looper.getMainLooper());
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        this.activityViewModel = activityViewModel;
    }

    public void startScan(ArrayList<ScanFilter> filters) {
        if (!mScanning) {
            Log.i(TAG, "Starting scan...");
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setReportDelay(0)
                    .build();
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    BluetoothDevice device = result.getDevice();
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                        return;
                    }
                    Log.i(TAG, "Found device: " + device.getName() + " (" + device.getAddress() + ")");
                  //  Toast.makeText(context, "Found device: " + device.getName() + " (" + device.getAddress() + ")", Toast.LENGTH_SHORT).show();

                    bluetoothDevice = device;
                    activityViewModel.getFoundDevice().setValue(device);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    for (ScanResult result : results) {
                        BluetoothDevice device = result.getDevice();
                        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                            return;
                        }
                        Log.i(TAG, "Found device: " + device.getName() + " (" + device.getAddress() + ")");
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.e(TAG, "Scan failed with error code: " + errorCode);
                    Toast.makeText(context, "Scan failed with error code: ", Toast.LENGTH_SHORT).show();

                }
            };



            mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
            mHandler.postDelayed(() -> stopScan(), SCAN_PERIOD);
            mScanning = true;
        } else {
            Log.i(TAG, "Scan already in progress");
            Toast.makeText(context, "Scan already in progress", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopScan() {
        if (mScanning) {
            Log.i(TAG, "Stopping scan...");
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mBluetoothLeScanner.stopScan(mScanCallback);
            mScanning = false;
        } else {
            Log.i(TAG, "No scan in progress");
        }

    }
}

