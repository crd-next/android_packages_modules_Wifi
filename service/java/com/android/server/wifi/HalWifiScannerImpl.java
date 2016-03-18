/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.server.wifi.scanner.ChannelHelper;
import com.android.server.wifi.scanner.HalChannelHelper;

/**
 * WifiScanner implementation that takes advantage of the gscan HAL API
 * The gscan API is used to perform background scans and wpa_supplicant is used for onehot scans.
 * @see com.android.server.wifi.WifiScannerImpl for more details on each method
 */
public class HalWifiScannerImpl extends WifiScannerImpl implements Handler.Callback {
    private static final String TAG = "HalWifiScannerImpl";
    private static final boolean DBG = false;

    private final WifiNative mWifiNative;
    private final ChannelHelper mChannelHelper;
    private final SupplicantWifiScannerImpl mSupplicantScannerDelegate;
    private final boolean mHalBasedPnoSupported;

    public HalWifiScannerImpl(Context context, WifiNative wifiNative, Looper looper) {
        mWifiNative = wifiNative;
        mChannelHelper = new HalChannelHelper(wifiNative);
        mSupplicantScannerDelegate =
                new SupplicantWifiScannerImpl(context, wifiNative, mChannelHelper, looper);

        // Check if ePNO is supported by the HAL.
        int halFeatureSet = mWifiNative.getSupportedFeatureSet();
        mHalBasedPnoSupported =
                ((halFeatureSet & WifiManager.WIFI_FEATURE_HAL_EPNO)
                        == WifiManager.WIFI_FEATURE_HAL_EPNO);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.w(TAG, "Unknown message received: " + msg.what);
        return true;
    }

    @Override
    public void cleanup() {
        mSupplicantScannerDelegate.cleanup();
    }

    @Override
    public boolean getScanCapabilities(WifiNative.ScanCapabilities capabilities) {
        return mWifiNative.getScanCapabilities(capabilities);
    }

    @Override
    public ChannelHelper getChannelHelper() {
        return mChannelHelper;
    }

    public boolean startSingleScan(WifiNative.ScanSettings settings,
            WifiNative.ScanEventHandler eventHandler) {
        return mSupplicantScannerDelegate.startSingleScan(settings, eventHandler);
    }

    @Override
    public WifiScanner.ScanData getLatestSingleScanResults() {
        return mSupplicantScannerDelegate.getLatestSingleScanResults();
    }

    @Override
    public boolean startBatchedScan(WifiNative.ScanSettings settings,
            WifiNative.ScanEventHandler eventHandler) {
        if (settings == null || eventHandler == null) {
            Log.w(TAG, "Invalid arguments for startBatched: settings=" + settings
                    + ",eventHandler=" + eventHandler);
            return false;
        }
        return mWifiNative.startScan(settings, eventHandler);
    }

    @Override
    public void stopBatchedScan() {
        mWifiNative.stopScan();
    }

    @Override
    public void pauseBatchedScan() {
        mWifiNative.pauseScan();
    }

    @Override
    public void restartBatchedScan() {
        mWifiNative.restartScan();
    }

    @Override
    public WifiScanner.ScanData[] getLatestBatchedScanResults(boolean flush) {
        return mWifiNative.getScanResults(flush);
    }

    @Override
    public boolean setPnoList(WifiNative.PnoSettings settings,
            WifiNative.PnoEventHandler eventHandler) {
        if (mHalBasedPnoSupported) {
            return mWifiNative.setPnoList(settings, eventHandler);
        } else {
            return mSupplicantScannerDelegate.setPnoList(settings, eventHandler);
        }
    }

    @Override
    public boolean resetPnoList(WifiNative.PnoSettings settings) {
        if (mHalBasedPnoSupported) {
            return mWifiNative.resetPnoList();
        } else {
            return mSupplicantScannerDelegate.resetPnoList(settings);
        }
    }

    @Override
    public boolean shouldScheduleBackgroundScanForPno(boolean isConnectedPno) {
        if (mHalBasedPnoSupported) {
            return true;
        } else {
            return mSupplicantScannerDelegate.shouldScheduleBackgroundScanForPno(isConnectedPno);
        }
    }

    @Override
    public boolean setHotlist(WifiScanner.HotlistSettings settings,
            WifiNative.HotlistEventHandler eventHandler) {
        return mWifiNative.setHotlist(settings, eventHandler);
    }

    @Override
    public void resetHotlist() {
        mWifiNative.resetHotlist();
    }

    @Override
    public boolean trackSignificantWifiChange(WifiScanner.WifiChangeSettings settings,
            WifiNative.SignificantWifiChangeEventHandler handler) {
        return mWifiNative.trackSignificantWifiChange(settings, handler);
    }

    @Override
    public void untrackSignificantWifiChange() {
        mWifiNative.untrackSignificantWifiChange();
    }
}
