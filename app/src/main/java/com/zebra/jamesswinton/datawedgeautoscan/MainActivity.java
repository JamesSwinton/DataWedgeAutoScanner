package com.zebra.jamesswinton.datawedgeautoscan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.zebra.jamesswinton.datawedgeautoscan.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Data Binding
    private ActivityMainBinding mDataBinding;

    // DW Intent Filter
    private final IntentFilter DataWedgeIntentFilter = new IntentFilter();

    // DW Action Keys
    private static final String DataWedgeScanAction = "com.royalmail.scan.ACTION";
    private static final String DataWedgeApiAction = "com.symbol.datawedge.api.ACTION";
    private static final String DataWedgeResultCategory = "com.symbol.datawedge.api.RESULT_CATEGORY";
    private static final String NotificationAction  = "com.symbol.datawedge.api.NOTIFICATION_ACTION";
    private static final String ResultAction = "com.symbol.datawedge.api.RESULT_ACTION";

    // DW Extra Name Keys
    private static final String NotificationBundleExtra = "com.symbol.datawedge.api.NOTIFICATION";
    private static final String ScannerInputPluginExtra = "com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN";
    private static final String SoftScanTriggerExtra = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER";
    private static final String RegisterForNotificationExtra = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION";
    private static final String NotificationTypeExtra = "com.symbol.datawedge.api.NOTIFICATION_TYPE";
    private static final String ApplicationNameExtra = "com.symbol.datawedge.api.APPLICATION_NAME";
    private static final String GetScannerStatusExtra = "com.symbol.datawedge.api.GET_SCANNER_STATUS";
    private static final String ResultsScannerStatusExtra = "com.symbol.datawedge.api.RESULT_SCANNER_STATUS";

    // DW Extra Value Keys
    private static final String ScannerStatusExtra = "STATUS";
    private static final String ScannerStatusNotificationExtra = "SCANNER_STATUS";
    private static final String StartScanningExtra = "START_SCANNING";
    private static final String SuspendPluginExtra = "SUSPEND_PLUGIN";
    private static final String ResumePluginExtra = "RESUME_PLUGIN";
    private static final String SendResultExtra = "SEND_RESULT";

    // Suspend / Resume Delay & Handler
    private static final int DelayBetweenSuspendResume = 2000;
    private final Runnable ResumeScannerRunnable = this::resumeScanner;
    private final Handler SuspendResumeHandler = new Handler(Looper.getMainLooper());

    // SDF for Logging
    private final SimpleDateFormat LoggingDateFormat = new SimpleDateFormat("HH:mm:ss.SSS",
            Locale.getDefault());

    // holder
    private boolean mScanDataReceived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for Notifications
        Bundle registerForNotificationsBundle = new Bundle();
        registerForNotificationsBundle.putString(ApplicationNameExtra, getPackageName());
        registerForNotificationsBundle.putString(NotificationTypeExtra,
                ScannerStatusNotificationExtra);
        Intent registerForNotificationsIntent = new Intent(DataWedgeApiAction);
        registerForNotificationsIntent.putExtra(RegisterForNotificationExtra,
                registerForNotificationsBundle);
        sendBroadcast(registerForNotificationsIntent);

        // Register Receiver for Scan & Notifications
        DataWedgeIntentFilter.addAction(ResultAction);
        DataWedgeIntentFilter.addAction(NotificationAction);
        DataWedgeIntentFilter.addAction(DataWedgeScanAction);
        DataWedgeIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(DataWedgeScanDataBroadcastReceiver, DataWedgeIntentFilter);

        // Start Scanner
        getScannerStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(DataWedgeScanDataBroadcastReceiver);
    }

    private void getScannerStatus() {
        Intent i = new Intent(DataWedgeApiAction);
        i.putExtra(GetScannerStatusExtra,"");
        i.putExtra(SendResultExtra, String.valueOf(true));
        i.putExtra(DataWedgeResultCategory, Intent.CATEGORY_DEFAULT);
        sendBroadcast(i);
    }

    private void triggerScan() {
        logToScreen("Triggering Scan");
        Intent i = new Intent(DataWedgeApiAction);
        i.putExtra(SoftScanTriggerExtra, StartScanningExtra);
        sendBroadcast(i);
    }

    private void suspendScanner() {
        logToScreen("Suspending Scanner");

        Intent dwIntent = new Intent(DataWedgeApiAction);
        dwIntent.putExtra(ScannerInputPluginExtra, SuspendPluginExtra);
        sendBroadcast(dwIntent);
    }

    private void resumeScanner() {
        logToScreen("Resuming Scanner");

        // Update Holder
        mScanDataReceived = false;

        // Resume Scanner
        Intent dwIntent = new Intent(DataWedgeApiAction);
        dwIntent.putExtra(ScannerInputPluginExtra, ResumePluginExtra);
        sendBroadcast(dwIntent);
    }

    private void logToScreen(String log) {
        mDataBinding.scanData.append("\n");
        mDataBinding.scanData.append(log);
        mDataBinding.scanData.append(" @ ");
        mDataBinding.scanData.append(LoggingDateFormat.format(System.currentTimeMillis()));
        mDataBinding.scanData.append("\n");
        mDataBinding.scrollView.post(() -> mDataBinding.scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private final BroadcastReceiver DataWedgeScanDataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction == null) {
                return;
            }

            // Handle Notification
            switch (intentAction) {
                case DataWedgeScanAction:
                    // Update holder
                    mScanDataReceived = true;

                    // Log Scan
                    logToScreen("Scan Received");

                    // Suspend Scanner
                    suspendScanner();
                    break;
                case NotificationAction:
                    // Handle Scanner Status
                    if (intent.hasExtra(NotificationBundleExtra)) {
                        Bundle notificationBundle = intent.getBundleExtra(NotificationBundleExtra);
                        handleScannerStatus(notificationBundle.getString(ScannerStatusExtra));
                    }
                    break;
                case ResultAction:
                    // Handle Scanner Status
                    if (intent.hasExtra(ResultsScannerStatusExtra)) {
                        handleScannerStatus(intent.getStringExtra(ResultsScannerStatusExtra));
                    }
                    break;
            }
        }
    };

    private void handleScannerStatus(String scannerStatus) {
        switch (scannerStatus) {
            case "WAITING":
                Log.i(this.getClass().getName(), "Scanner Waiting");

                // Start Scan
                if (!mScanDataReceived) {
                    logToScreen("Scanner Ready");
                    triggerScan();
                } else {
                    logToScreen("State change due to successful scan, awaiting suspension");
                }
                break;
            case "SCANNING":
                Log.i(this.getClass().getName(), "Scanner Scanning");
                break;
            case "CONNECTED":
                Log.i(this.getClass().getName(), "BT Scanner Connected");
                break;
            case "DISCONNECTED":
                Log.i(this.getClass().getName(), "BT Scanner Disconnected");
                break;
            case "IDLE":
                Log.i(this.getClass().getName(), "Scanner Idle, probably suspended");
                logToScreen("Scanner Idle / Suspended");

                // Resume Scanner after Delay
                SuspendResumeHandler.removeCallbacks(ResumeScannerRunnable);
                SuspendResumeHandler.postDelayed(ResumeScannerRunnable, DelayBetweenSuspendResume);

                break;
            case "DISABLED":
                Log.i(this.getClass().getName(), "Scanner Disabled");
                break;
        }
    }
}