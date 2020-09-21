package com.sd.seer.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.sd.seer.MainActivity;
import com.sd.seer.R;
import com.sd.seer.common.Constants;
import com.sd.seer.helper.GoogleManager;
import com.sd.seer.model.BPM;
import com.sd.seer.model.Location;
import com.sd.seer.model.Tracking;
import com.sd.seer.rest.HistoryService;
import com.sd.seer.rest.ServiceFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RunnerService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final Integer NOTIFICATION_ID = 1;

    private static final HistoryService mHistoryService = ServiceFactory.getServiceInstance(HistoryService.class);
    private static final String TAG = RunnerService.class.getSimpleName();

    private static Tracking tracking =
            new Tracking(Collections.synchronizedList(new ArrayList<BPM>()),
                    Collections.synchronizedList(new ArrayList<Location>()));

    public static void start(Activity activity) {
        Intent serviceIntent = new Intent(activity, RunnerService.class);
        serviceIntent.putExtra("inputExtra", "Seer");
        ActivityCompat.startForegroundService(activity, serviceIntent);
    }

    public static void stop(Activity activity) {
        Intent serviceIntent = new Intent(activity, RunnerService.class);
        activity.stopService(serviceIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification(input));
        //do heavy work on a background thread
        startBpmTracking();
        startLocationTracking();
        startUpload();
        //stopSelf();
        return START_STICKY;
    }

    private Notification buildNotification(String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(content)
                .setContentText("Running")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(pendingIntent)
                .build();
    }

    //TODO: Implement
    private void startBpmTracking() {
    }

    private void startLocationTracking() {
        GoogleManager.registerListener(this, DataType.TYPE_LOCATION_SAMPLE,
                DataSource.TYPE_RAW, dataPoint -> {
                    tracking.getLocations().add(
                            new Location(new Date(), dataPoint.getValue(Field.FIELD_LATITUDE).asFloat(),
                                    dataPoint.getValue(Field.FIELD_LONGITUDE).asFloat()));
                });
    }

    private void startUpload() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (tracking.getBpms()) {
                    synchronized (tracking.getLocations()) {
                        mHistoryService.putTracking(GoogleManager.getAccount().getEmail(),
                                new Tracking(new ArrayList<>(tracking.getBpms()), new ArrayList<>(tracking.getLocations())))
                                .enqueue(new Callback<Tracking>() {

                            @Override
                            public void onResponse(Call<Tracking> call, Response<Tracking> response) {
                                if(response.isSuccessful()) {
                                    Log.d(TAG, "Tracking uploaded");
                                    if(response.body().getBpms() != null)
                                        tracking.getBpms().removeAll(response.body().getBpms());
                                    if(response.body().getLocations() != null)
                                        tracking.getLocations().removeAll(response.body().getLocations());
                                } else onFailure(call, new Exception());
                            }

                            @Override
                            public void onFailure(Call<Tracking> call, Throwable t) {
                                Log.e(TAG, "Tracking upload failed");
                                getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,
                                        buildNotification("Failing to update. Please check connectivity"));
                            }
                        });
                    }
                }
            }
        }, 0, Constants.PERIOD_MILLIS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}
