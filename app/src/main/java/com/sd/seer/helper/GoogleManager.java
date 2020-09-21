package com.sd.seer.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.SensorsClient;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.tasks.Task;
import com.sd.seer.common.Constants;

import java.util.concurrent.TimeUnit;

public class GoogleManager {

    private static final String TAG = GoogleManager.class.getSimpleName();

    private static Activity mContext;

    private static GoogleSignInAccount mAccount;

    private static SensorsClient mClient;

    public static void startSignIn(Activity context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(context, gso);

        Intent signInIntent = client.getSignInIntent();
        context.startActivityForResult(signInIntent, Constants.REQUEST_CODE_GOOGLE_START);
    }

    public static boolean checkAndRequestPermission(Activity context, Intent data) {
        mContext = context;
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.m
            if (!GoogleSignIn.hasPermissions(account, Constants.FITNESS_OPTIONS)) {
                GoogleSignIn.requestPermissions(
                        mContext, // your context
                        Constants.REQUEST_CODE_GOOGLE_PERMISSIONS, // e.g. 1
                        account,
                        Constants.FITNESS_OPTIONS);
            } else {
                mAccount = account;
                return true;
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
        return false;
    }

    public static void registerListener(Context context, DataType dataType, Integer dataSourceType,
                                        OnDataPointListener dataPointListener) {
        getSensorsClient(context)
                .findDataSources(
                        new DataSourcesRequest.Builder()
                                .setDataTypes(dataType)
                                .setDataSourceTypes(dataSourceType)
                                .build())
                .addOnSuccessListener((dataSources) -> {
                    dataSources.forEach(dataSource -> {
                        Log.i(TAG, "Data source found: " + dataSource.toString());
                        Log.i(TAG, "Data Source type: " + dataSource.getDataType().getName());

                        // Let's register a listener to receive Context data!
                        if (dataSource.getDataType().equals(dataType)) {
                            Log.i(TAG, "Data source found!  Registering.");

                            mAccount = GoogleSignIn
                                    .getAccountForExtension(mContext, Constants.FITNESS_OPTIONS);

                            getSensorsClient(context)
                                    .add(
                                            new SensorRequest.Builder()
                                                    .setDataSource(dataSource)
                                                    .setDataType(dataType)
                                                    .setSamplingRate(Constants.PERIOD_MILLIS, TimeUnit.MILLISECONDS)
                                                    .setFastestRate(Double.valueOf (Constants.PERIOD_MILLIS * 0.7).intValue(), TimeUnit.MILLISECONDS)
                                                    .setAccuracyMode(3) // high accuracy
                                                    .build(),
                                            dataPointListener)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Log.i(TAG, "Listener registered!");
                                        } else {
                                            Log.e(TAG, "Listener not registered.", task.getException());
                                        }
                                    });
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "No data source available"));
    }

    private static SensorsClient getSensorsClient(Context context) {
        if(mClient == null) {
            mClient = Fitness.getSensorsClient(context, mAccount);
        }
        return mClient;
    }

    public static GoogleSignInAccount getAccount() {
        return mAccount;
    }

}
