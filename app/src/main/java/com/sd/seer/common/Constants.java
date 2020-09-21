package com.sd.seer.common;

import android.Manifest;

import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;

public class Constants {

    public static final int REQUEST_CODE_ANDROID = 1;
    public static final int REQUEST_CODE_GOOGLE_START = 2;
    public static final int REQUEST_CODE_GOOGLE_PERMISSIONS = 3;
    public static final int REQUEST_CODE_USER_REGISTRATION = 4;

    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.INTERNET,
            Manifest.permission.SEND_SMS,
            Manifest.permission.FOREGROUND_SERVICE
    };

    public static final FitnessOptions FITNESS_OPTIONS = FitnessOptions.builder()
            .addDataType(DataType.TYPE_LOCATION_SAMPLE, FitnessOptions.ACCESS_READ)
            .build();

    public static final String USER_SERVICE_URL_BASE = "https://pa566zwp8f.execute-api.ap-south-1.amazonaws.com/";
    public static final String HISTORY_SERVICE_URL_BASE = "https://pa566zwp8f.execute-api.ap-south-1.amazonaws.com/";

    public static final int PERIOD_MILLIS = 10 * 60 * 1000;

}
