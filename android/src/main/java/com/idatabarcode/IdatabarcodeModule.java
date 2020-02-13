package com.idatabarcode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Objects;

public class IdatabarcodeModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;

    private static ScannerInterface scanner;
    private static boolean isReading = false;
    private static IntentFilter intentFilter;
    private static BroadcastReceiver scanReceiver;
    private static IdatabarcodeModule instance = null;

    private static final String RES_ACTION = "android.intent.action.SCANRESULT";

    public IdatabarcodeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        instance = this;
    }

    public static IdatabarcodeModule getInstance() {
        return instance;
    }

    public void onKeyDownEvent(int keyCode, KeyEvent event) {
        Log.v("Down keyCode", keyCode + "");
        WritableMap map = Arguments.createMap();
        map.putString("event", "down");
        sendEvent("test", map);
        if (keyCode == 190 || keyCode == 188 || keyCode == 189) {
            if (!isReading) {
                StartRead();
            }
        }
        // center key 190
        // right side 188
        // left side 189
    }

    public void onKeyUpEvent(int keyCode, KeyEvent event) {
        Log.v("Up keyCode", keyCode + "");
        WritableMap map = Arguments.createMap();
        map.putString("event", "up");
        sendEvent("test", map);
        if (keyCode == 190 || keyCode == 188 || keyCode == 189) {
            if (isReading) {
                StopRead();
            }
        }
    }

    @Override
    public String getName() {
        return "Idatabarcode";
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        Shutdown();
    }

    private class ScannerResultReceiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), RES_ACTION)) {
                final String scanResult = intent.getStringExtra("value");
                int barocodelen = intent.getIntExtra("length", 0);
                String type = intent.getStringExtra("type");

                WritableMap map = Arguments.createMap();
                map.putString("barcode", scanResult);
                map.putString("type", type);
                sendEvent("barcode", map);
//				tvScanResult.append("Length：" + barocodelen + "  Type:" + type + "  CodeBar：" + scanResult);
//				tvScanResult.append("条码长度："+barocodelen+"  条码类型"+type+"  条码："+scanResult);
            }
        }
    }

    private void StartRead() {
        if (!isReading) {
            scanner.scan_start();
            isReading = true;
        }
    }

    private void StopRead() {
        if (isReading) {
            scanner.scan_stop();
//			this.reactContext.unregisterReceiver(scanReceiver);
            scanner.continceScan(false);
            isReading = false;
        }
    }

    @ReactMethod
    private void Shutdown() {
        if (scanner != null) {
            StopRead();
            this.reactContext.unregisterReceiver(scanReceiver);
            scanner = null;
            intentFilter = null;
            scanReceiver = null;
        }
    }

    @ReactMethod
    private void initScanner(Promise promise) {
        try {
            if (scanner == null) {
                scanner = new ScannerInterface(this.reactContext);
                scanner.unlockScanKey();
                scanner.setOutputMode(1);

                intentFilter = new IntentFilter(RES_ACTION);
                scanReceiver = new ScannerResultReceiver();
                this.reactContext.registerReceiver(scanReceiver, intentFilter);
                promise.resolve(true);
            }
            promise.resolve(false);
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }
}
