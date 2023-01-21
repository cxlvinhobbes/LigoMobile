package com.example.ligomobileapplication;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.Context;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import android.icu.text.UFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Vibrator;

import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;

import android.util.Log;

import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nix.nixsensor_lib.IDeviceListener;
import com.nix.nixsensor_lib.NixDevice;
import com.nix.nixsensor_lib.NixDeviceCommon;
import com.nix.nixsensor_lib.NixDeviceScanner;
import com.nix.nixsensor_lib.NixScannedColor;
import com.nix.nixsensor_lib.NixScannedSpectralData;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Objects;




public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_BLUETOOTH = 1000;
    TextView RGBDisplay;
    Hashtable<String,BluetoothDevice> devices;
    private NixDevice nixDevice;

    private static NixDeviceScanner nixDeviceScanner;
    private static final String TAG = MainActivity.class.getSimpleName();


    private final IDeviceListener mDeviceCallback = new IDeviceListener() {
        @Override
        public void onConnectingStarted() {
        }

        @Override
        public void onDeviceReady() {
            Log.d("Nix device(" + nixDevice.getAddress() + ")", "Ready");
        }

        @Override
        public void onScanComplete(ArrayList<NixScannedColor> scannedColorsList, ArrayList<NixScannedSpectralData> unused) {
            NixScannedColor firstcolour = scannedColorsList.get(0);
            short[] scanRGB = firstcolour.tosRgbValue();
            StringBuilder RGBValues = new StringBuilder();
            for(short val: scanRGB){
                RGBValues.append(Short.toString(val) + ", ");
            }
            RGBDisplay.setText(RGBValues);
            Log.d("RGB: ", RGBValues.toString());
        }

        @Override
        public void onDeviceDisconnected() {

        }

        @Override
        public void onBatteryChanged() {
        }

        @Override
        public void onUsbConnectionChanged() {
        }

        @Override
        public void onApiLocked(String serialNumber) {
        }

        @Override
        public void onBluetoothError(int errorCode, String errorMessage) {
        }

        @Override
        public void onScanProgress(int code, String message) {
        }
    };
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    // For the NixExample app, only allow non-Spectro devices. This can be
                    // determined by examining the BluetoothDevice name.

                    // NOTE: On some Android versions, a bug can cause BluetoothDevice.getName() to
                    // return null when the rate of device discovery is high. In this case, the name
                    // A method provided in the NixDeviceScanner class can be used to parse the name
                    // from the scan record directly.
                    String deviceName = NixDeviceScanner.parseAdvertisementDataToName(scanRecord);
                    String deviceAddress = device.getAddress();
                    NixDeviceCommon.DeviceType deviceType = NixDeviceCommon.DeviceType.findByAdvertisingName(deviceName);

                    switch (deviceType) {
                        case mini:
                        case mini2:
                            Log.d("Device:", deviceName + ", " + deviceAddress);
                            if(!devices.containsKey(deviceAddress)){
                                devices.put(deviceAddress,device);
                            }
                        case pro:
                        case pro2:
                        case qc:
                            // Continue
                            break;
                        case spectro2:
                        case unknown:
                            // Not supported in this app ... return
                            return;
                    }
                    if(devices.size() == 2){
                        nixDeviceScanner.stopDevicesScan();
                    }
                }
            };

    // Provides list of run-time permissions needed for using Bluetooth
    private static String[] requiredBluetoothPermissions() {
        ArrayList<String> requiredPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Android R and older require access to location for BLE search
            requiredPermissions
                    .add(Manifest.permission.ACCESS_COARSE_LOCATION);
            requiredPermissions
                    .add(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            // Android S and newer require Bluetooth 'Scan' and 'Connect'
            requiredPermissions
                    .add(Manifest.permission.BLUETOOTH_SCAN);
            requiredPermissions
                    .add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        return requiredPermissions.toArray(new String[0]);
    }
    // Checks if all permissions required for Bluetooth have been granted
    private static boolean isBluetoothPermissionGranted(Activity activity) {
        ArrayList<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredBluetoothPermissions())
            if (ContextCompat.checkSelfPermission(activity, permission) !=
                    PackageManager.PERMISSION_GRANTED)
                missingPermissions.add(permission);
        return missingPermissions.isEmpty();
    }
    // Requests permissions necessary for using Bluetooth
    public static void requestBluetoothPermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                requiredBluetoothPermissions(),
                PERMISSION_REQUEST_BLUETOOTH);
    }



    // Activity functions
    // Bluetooth Val

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RGBDisplay = (TextView) findViewById(R.id.RGBDisplay);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !isBluetoothPermissionGranted(this))
            requestBluetoothPermissions(this);

        nixDeviceScanner = new NixDeviceScanner(this);

    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        boolean allGranted = true;
        for (int result : grantResults)
            allGranted &= (result == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case PERMISSION_REQUEST_BLUETOOTH: {
                if (allGranted) {
                    Log.d(TAG, "Bluetooth permissions granted");
                } else {
                    Log.e(TAG, "Can't access Bluetooth permissions, " +
                            "scan will be impossible");
                }
            } break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void BeginNixDeviceScan(View view) {
        devices = new Hashtable<String,BluetoothDevice>();
        nixDeviceScanner.startDevicesScan(mLeScanCallback);
    }

    public void NixConnect(View view) {
        Log.d("nix connection status: ", "trying to connect");
        connectNix();
    }

    class nixConnector implements Runnable {
        public void run() {
            // function to be sent to thread
            connectNix();
        }
    }

    private void connectNix(){
        nixDevice = new NixDevice(this,devices.get("FC:45:C3:E4:1B:9A"),mDeviceCallback);
    }

    public void BeginScan(View view) {
        Log.d("Position: ", "Trying to begin scan");
        nixDevice.runSingleScan(NixDevice.SCAN_TYPE_D65,true);


    }
}