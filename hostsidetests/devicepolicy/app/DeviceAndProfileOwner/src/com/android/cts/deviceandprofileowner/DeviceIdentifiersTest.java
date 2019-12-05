/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.cts.deviceandprofileowner;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.android.compatibility.common.util.ShellIdentityUtils;

/**
 * Verifies device identifier access for the profile owner on an organization-owned device.
 * TODO: Use those tests for DeviceOwner instead of
 * DeviceOwnerTest#testDeviceOwnerCanGetDeviceIdentifiers
 */
public class DeviceIdentifiersTest extends BaseDeviceAdminTest {

    private static final String DEVICE_ID_WITH_PERMISSION_ERROR_MESSAGE =
            "An unexpected value was received by the profile owner with the READ_PHONE_STATE "
                    + "permission when invoking %s";
    private static final String NO_SECURITY_EXCEPTION_ERROR_MESSAGE =
            "A profile owner that does not have the READ_PHONE_STATE permission must receive a "
                    + "SecurityException when invoking %s";

    public void testProfileOwnerCanGetDeviceIdentifiersWithPermission() throws Exception {
        // The profile owner with the READ_PHONE_STATE permission should have access to all device
        // identifiers. However since the TelephonyManager methods can return null this method
        // verifies that the profile owner with the READ_PHONE_STATE permission receives the same
        // value that the shell identity receives with the READ_PRIVILEGED_PHONE_STATE permission.
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        try {
            assertEquals(String.format(DEVICE_ID_WITH_PERMISSION_ERROR_MESSAGE, "getDeviceId"),
                    ShellIdentityUtils.invokeMethodWithShellPermissions(telephonyManager,
                            (tm) -> tm.getDeviceId()), telephonyManager.getDeviceId());
            assertEquals(String.format(DEVICE_ID_WITH_PERMISSION_ERROR_MESSAGE, "getImei"),
                    ShellIdentityUtils.invokeMethodWithShellPermissions(telephonyManager,
                            (tm) -> tm.getImei()), telephonyManager.getImei());
            assertEquals(String.format(DEVICE_ID_WITH_PERMISSION_ERROR_MESSAGE, "getMeid"),
                    ShellIdentityUtils.invokeMethodWithShellPermissions(telephonyManager,
                            (tm) -> tm.getMeid()), telephonyManager.getMeid());
            assertEquals(String.format(DEVICE_ID_WITH_PERMISSION_ERROR_MESSAGE, "getSubscriberId"),
                    ShellIdentityUtils.invokeMethodWithShellPermissions(telephonyManager,
                            (tm) -> tm.getSubscriberId()), telephonyManager.getSubscriberId());
            assertEquals(
                    String.format(DEVICE_ID_WITH_PERMISSION_ERROR_MESSAGE, "getSimSerialNumber"),
                    ShellIdentityUtils.invokeMethodWithShellPermissions(telephonyManager,
                            (tm) -> tm.getSimSerialNumber()),
                    telephonyManager.getSimSerialNumber());
            assertEquals(String.format(DEVICE_ID_WITH_PERMISSION_ERROR_MESSAGE, "Build#getSerial"),
                    ShellIdentityUtils.invokeStaticMethodWithShellPermissions(Build::getSerial),
                    Build.getSerial());
        } catch (SecurityException e) {
            fail("The profile owner with the READ_PHONE_STATE permission must be able to access "
                    + "the device IDs: " + e);
        }
    }

    public void testProfileOwnerCannotGetDeviceIdentifiersWithoutPermission() throws Exception {
        // The profile owner without the READ_PHONE_STATE permission should still receive a
        // SecurityException when querying for device identifiers.
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        // Allow the APIs to also return null if the telephony feature is not supported.
        boolean hasTelephonyFeature =
                mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        try {
            String deviceId = telephonyManager.getDeviceId();
            if (hasTelephonyFeature) {
                fail(String.format(NO_SECURITY_EXCEPTION_ERROR_MESSAGE, "getDeviceId"));
            } else {
                assertEquals(null, deviceId);
            }
        } catch (SecurityException expected) {
        }

        try {
            String imei = telephonyManager.getImei();
            if (hasTelephonyFeature) {
                fail(String.format(NO_SECURITY_EXCEPTION_ERROR_MESSAGE, "getImei"));
            } else {
                assertEquals(null, imei);
            }
        } catch (SecurityException expected) {
        }

        try {
            String meid = telephonyManager.getMeid();
            if (hasTelephonyFeature) {
                fail(String.format(NO_SECURITY_EXCEPTION_ERROR_MESSAGE, "getMeid"));
            } else {
                assertEquals(null, meid);
            }
        } catch (SecurityException expected) {
        }

        try {
            String subscriberId = telephonyManager.getSubscriberId();
            if (hasTelephonyFeature) {
                fail(String.format(NO_SECURITY_EXCEPTION_ERROR_MESSAGE, "getSubscriberId"));
            } else {
                assertEquals(null, subscriberId);
            }
        } catch (SecurityException expected) {
        }

        try {
            String simSerialNumber = telephonyManager.getSimSerialNumber();
            if (hasTelephonyFeature) {
                fail(String.format(NO_SECURITY_EXCEPTION_ERROR_MESSAGE, "getSimSerialNumber"));
            } else {
                assertEquals(null, simSerialNumber);
            }
        } catch (SecurityException expected) {
        }

        try {
            String serial = Build.getSerial();
            if (hasTelephonyFeature) {
                fail(String.format(NO_SECURITY_EXCEPTION_ERROR_MESSAGE, "Build#getSerial"));
            } else {
              assertEquals(null, serial);
            }
        } catch (SecurityException expected) {
        }
    }
}